import { useState } from 'react';
import { Outlet } from 'react-router';
import { Navbar } from './Navbar';
import { Footer } from './Footer';

export function MainLayout() {
  const [selectedCity, setSelectedCity] = useState(() => {
    try {
      return localStorage.getItem('bookngo_selected_city') || 'Vijayawada';
    } catch {
      return 'Vijayawada';
    }
  });

  const handleCityChange = (city) => {
    setSelectedCity(city);
    try {
      localStorage.setItem('bookngo_selected_city', city);
    } catch {
      // ignore
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#0B0F17] text-gray-100 selection:bg-red-500 selection:text-white">
      <Navbar selectedCity={selectedCity} onCityChange={handleCityChange} />
      <main className="flex-1 w-full">
        <Outlet context={{ selectedCity }} />
      </main>
      <Footer />
    </div>
  );
}

export default MainLayout;
