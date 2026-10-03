'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ArrowRight, Globe, Check } from 'lucide-react';

export default function Footer() {
  const pathname = usePathname();
  const [email, setEmail] = useState('');
  const [subscribed, setSubscribed] = useState(false);

  // Completely separate admin console by not rendering customer footer on /admin routes
  if (pathname?.startsWith('/admin')) {
    return null;
  }

  const handleSubscribe = (e: React.FormEvent) => {
    e.preventDefault();
    if (email) {
      setSubscribed(true);
      setEmail('');
    }
  };

  return (
    <footer className="bg-[#080808] text-white border-t border-[#1c1c1c] pt-16 pb-24 md:pb-16 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-12 pb-14 border-b border-[#222222]">
          
          {/* Brand & Newsletter (2 columns on desktop) */}
          <div className="lg:col-span-2 space-y-6">
            <div>
              <Link href="/" className="inline-block group">
                <span className="text-2xl font-black tracking-[0.2em] uppercase text-white group-hover:text-[#C8BCA7] transition-colors">
                  REXXZO
                </span>
                <span className="block text-[9px] tracking-[0.35em] text-[#C8BCA7] font-semibold uppercase mt-0.5">
                  BETTER EVERYDAY
                </span>
              </Link>
              <p className="mt-4 text-sm text-[#999999] leading-relaxed max-w-sm">
                Mindfully considered objects created to elevate everyday rituals. Balancing tactile warmth, minimal silhouettes, and enduring craftsmanship.
              </p>
            </div>

            {/* Newsletter form */}
            <div className="space-y-3">
              <span className="text-xs uppercase tracking-widest text-[#C8BCA7] font-medium block">
                Stay In Touch
              </span>
              <p className="text-xs text-[#777777]">
                Subscribe for private releases, design stories, and 10% off your inaugural order.
              </p>
              {subscribed ? (
                <div className="flex items-center text-xs text-[#C8BCA7] py-2">
                  <Check className="w-4 h-4 mr-2" /> Thank you for subscribing.
                </div>
              ) : (
                <form onSubmit={handleSubscribe} className="flex max-w-md">
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Enter your email address..."
                    className="bg-[#141414] border border-[#2b2b2b] text-xs text-white placeholder-[#666666] px-4 py-3 rounded-l-md focus:outline-none focus:border-[#C8BCA7] flex-1"
                  />
                  <button
                    type="submit"
                    className="bg-[#C8BCA7] text-[#080808] hover:bg-[#b8ab94] px-4 py-3 rounded-r-md transition-colors text-xs font-bold uppercase tracking-wider flex items-center gap-1 cursor-pointer"
                  >
                    <span>Join</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </form>
              )}
            </div>
          </div>

          {/* Links Column 1: Explore */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-widest text-[#C8BCA7] mb-4">
              Explore
            </h4>
            <ul className="space-y-2.5 text-xs text-[#999999]">
              <li>
                <Link href="/shop" className="hover:text-white transition-colors">
                  All Products
                </Link>
              </li>
              <li>
                <Link href="/categories" className="hover:text-white transition-colors">
                  Shop By Category
                </Link>
              </li>
              <li>
                <Link href="/shop/gift-corner" className="hover:text-white transition-colors flex items-center gap-1.5">
                  <span>Gift Corner</span>
                  <span className="text-[9px] bg-[#C8BCA7]/20 text-[#C8BCA7] px-1.5 py-0.5 rounded uppercase tracking-wider font-semibold">New</span>
                </Link>
              </li>
              <li>
                <Link href="/shop?category=lighting" className="hover:text-white transition-colors">
                  Lighting & Lamps
                </Link>
              </li>
              <li>
                <Link href="/shop?category=kitchen-dining" className="hover:text-white transition-colors">
                  Kitchen & Drinkware
                </Link>
              </li>
              <li>
                <Link href="/shop?category=storage-organizers" className="hover:text-white transition-colors">
                  Desk & Storage
                </Link>
              </li>
            </ul>
          </div>

          {/* Links Column 2: About REXXZO */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-widest text-[#C8BCA7] mb-4">
              Our Brand
            </h4>
            <ul className="space-y-2.5 text-xs text-[#999999]">
              <li>
                <Link href="/about" className="hover:text-white transition-colors">
                  About Our Studio
                </Link>
              </li>
              <li>
                <Link href="/about" className="hover:text-white transition-colors">
                  Design Philosophy
                </Link>
              </li>
              <li>
                <Link href="/contact" className="hover:text-white transition-colors">
                  Contact Us
                </Link>
              </li>
              <li>
                <Link href="/orders" className="hover:text-white transition-colors">
                  Track Your Order
                </Link>
              </li>
              <li>
                <Link href="/account" className="hover:text-white transition-colors">
                  Customer Portal
                </Link>
              </li>
            </ul>
          </div>

          {/* Links Column 3: Customer Care & Legal */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-widest text-[#C8BCA7] mb-4">
              Assistance
            </h4>
            <ul className="space-y-2.5 text-xs text-[#999999]">
              <li>
                <Link href="/contact" className="hover:text-white transition-colors">
                  Shipping & Handling
                </Link>
              </li>
              <li>
                <Link href="/contact" className="hover:text-white transition-colors">
                  Returns & Exchanges
                </Link>
              </li>
              <li>
                <Link href="/contact" className="hover:text-white transition-colors">
                  Care & Maintenance
                </Link>
              </li>
              <li>
                <Link href="/contact" className="hover:text-white transition-colors">
                  Privacy Policy
                </Link>
              </li>
              <li>
                <Link href="/contact" className="hover:text-white transition-colors">
                  Terms of Service
                </Link>
              </li>
            </ul>
          </div>

        </div>

        {/* Bottom copyright & social strip */}
        <div className="pt-8 flex flex-col md:flex-row items-center justify-between text-xs text-[#666666] gap-4">
          <p>© {new Date().getFullYear()} REXXZO. All rights reserved. Better Everyday.</p>
          <div className="flex items-center space-x-6 text-[#999999]">
            <a href="https://instagram.com" target="_blank" rel="noopener noreferrer" className="hover:text-[#C8BCA7] transition-colors" aria-label="Instagram">
              <svg className="w-4 h-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <rect width="20" height="20" x="2" y="2" rx="5" ry="5"></rect>
                <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                <line x1="17.5" x2="17.51" y1="6.5" y2="6.5"></line>
              </svg>
            </a>
            <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" className="hover:text-[#C8BCA7] transition-colors" aria-label="Facebook">
              <svg className="w-4 h-4 fill-none stroke-current stroke-2" viewBox="0 0 24 24">
                <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"></path>
              </svg>
            </a>
            <a href="https://pinterest.com" target="_blank" rel="noopener noreferrer" className="hover:text-[#C8BCA7] transition-colors" aria-label="Global">
              <Globe className="w-4 h-4" />
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
