import React from 'react';
import { Truck, ShieldCheck, RefreshCw, Headphones } from 'lucide-react';

export default function TrustStrip() {
  const perks = [
    {
      icon: Truck,
      title: 'Complimentary Shipping',
      desc: 'On all orders above ₹2,000 pan-India',
    },
    {
      icon: ShieldCheck,
      title: 'Verified Secure Checkout',
      desc: 'Encrypted via 256-bit bank grade gateway',
    },
    {
      icon: RefreshCw,
      title: '7-Day Effortless Returns',
      desc: 'Doorstep pickup with no questions asked',
    },
    {
      icon: Headphones,
      title: 'Dedicated Concierge',
      desc: '7-day design consultation and support',
    },
  ];

  return (
    <div className="bg-[#0e0e0e] border-y border-[#202020] text-white py-8 sm:py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-6 sm:gap-8">
          {perks.map((perk, index) => {
            const Icon = perk.icon;
            return (
              <div key={index} className="flex items-start space-x-3.5 group">
                <div className="p-2.5 rounded-lg bg-[#1a1a1a] text-[#C8BCA7] border border-[#282828] group-hover:border-[#C8BCA7] transition-colors flex-shrink-0">
                  <Icon className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-xs sm:text-sm font-bold uppercase tracking-wider text-white group-hover:text-[#C8BCA7] transition-colors">
                    {perk.title}
                  </h4>
                  <p className="text-[11px] sm:text-xs text-[#888888] mt-0.5 leading-snug">
                    {perk.desc}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
