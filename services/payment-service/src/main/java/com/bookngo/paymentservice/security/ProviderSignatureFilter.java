package com.bookngo.paymentservice.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates the X-Provider-Signature header on provider callback requests.
 * The signature is HMAC-SHA256(requestBody, providerSecretKey) encoded as hex.
 * In simulation mode (or when validation is disabled), the presence of the header
 * with the expected simulated value is sufficient.
 */
@Slf4j
@Component
public class ProviderSignatureFilter extends OncePerRequestFilter {

    private static final String CALLBACK_PATH = "/api/v1/payments/provider/callback";
    // Sentinel principal UUID for provider callback requests
    private static final UUID PROVIDER_PRINCIPAL = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final String providerSecretKey;
    private final boolean signatureValidationEnabled;

    public ProviderSignatureFilter(
            @Value("${app.payment.provider-secret-key:simulated-provider-secret}") String providerSecretKey,
            @Value("${app.payment.signature-validation-enabled:true}") boolean signatureValidationEnabled) {
        this.providerSecretKey = providerSecretKey;
        this.signatureValidationEnabled = signatureValidationEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !CALLBACK_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String signature = request.getHeader("X-Provider-Signature");

        if (signatureValidationEnabled) {
            if (signature == null || signature.isBlank()) {
                log.warn("Provider callback received without X-Provider-Signature header");
                sendUnauthorized(response);
                return;
            }
            byte[] body = StreamUtils.copyToByteArray(request.getInputStream());

            try {
                String expectedSignature = hmacSha256Hex(body, providerSecretKey);
                if (!expectedSignature.equalsIgnoreCase(signature)) {
                    log.warn("Provider callback signature mismatch. Expected: {}, Received: {}", expectedSignature, signature);
                    sendUnauthorized(response);
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to compute provider signature", e);
                sendUnauthorized(response);
                return;
            }

            // Set a sentinel authentication so that security chain passes
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        PROVIDER_PRINCIPAL,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PROVIDER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, body);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            // Signature validation disabled (test profile)
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        PROVIDER_PRINCIPAL,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PROVIDER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        }
    }

    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(this.cachedBody);
            return new jakarta.servlet.ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"code\":\"INVALID_PROVIDER_SIGNATURE\",\"message\":\"Missing or invalid provider signature\"}");
    }

    private String hmacSha256Hex(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] result = mac.doFinal(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
