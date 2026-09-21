package com.bookngo.paymentservice.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Simulates payment provider behaviour when PAYMENT_SIMULATION_ENABLED=true.
 *
 * The simulator exercises the real payment lifecycle state transitions and
 * Booking Service integration — it does NOT bypass them. It only replaces the
 * real network call to a payment gateway.
 *
 * Simulation controls (providerName values):
 *   SIM_SUCCESS   → SUCCESS
 *   SIM_FAILURE   → FAILURE
 *   SIM_CANCELLED → CANCELLED
 *   SIM_UNKNOWN   → UNKNOWN (requires reconciliation)
 *   (default)     → SUCCESS
 */
@Slf4j
@Service
public class PaymentSimulatorService {

    private final boolean simulationEnabled;

    public PaymentSimulatorService(
            @Value("${app.payment.simulation-enabled:true}") boolean simulationEnabled) {
        this.simulationEnabled = simulationEnabled;
    }

    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    /**
     * Simulates sending the payment to the provider and returns a simulated provider reference.
     */
    public String simulateProviderReference(String providerName) {
        return "SIM-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Determines the simulated provider outcome based on providerName.
     * @return "SUCCESS", "FAILURE", "CANCELLED", or "UNKNOWN"
     */
    public String simulateOutcome(String providerName) {
        if (providerName == null) {
            return "SUCCESS";
        }
        return switch (providerName.toUpperCase()) {
            case "SIM_FAILURE" -> "FAILURE";
            case "SIM_CANCELLED" -> "CANCELLED";
            case "SIM_UNKNOWN" -> "UNKNOWN";
            case "ASYNC", "WEBHOOK", "SIM_ASYNC" -> "PENDING";
            default -> "SUCCESS";
        };
    }

    /**
     * Simulates reconciliation check for UNKNOWN/TIMEOUT payments.
     * For simulation, SIM_UNKNOWN resolves to SUCCESS after reconciliation.
     * This exercises the real reconciliation state machine.
     */
    public String simulateReconciliationOutcome(String providerName, String providerReference) {
        log.info("Reconciling payment with providerName={}, ref={}", providerName, providerReference);
        // In simulation, UNKNOWN reconciles to SUCCESS to exercise the full lifecycle
        if (providerName != null && "SIM_UNKNOWN".equalsIgnoreCase(providerName)) {
            return "SUCCESS";
        }
        return "SUCCESS";
    }
}
