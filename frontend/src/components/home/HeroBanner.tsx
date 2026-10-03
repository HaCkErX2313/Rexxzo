'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { ArrowRight } from 'lucide-react';

export default function HeroBanner() {
  return (
    <section className="relative w-full h-[85vh] min-h-[580px] max-h-[820px] bg-[#080808] overflow-hidden flex items-center">
      {/* Background Image */}
      <Image
        src="/images/hero_banner.jpg"
        alt="REXXZO Curated Lifestyle Interior"
        fill
        priority
        className="object-cover object-center opacity-65 scale-102 transition-transform duration-1000 ease-out"
        sizes="100vw"
      />

      {/* Dark Vignette and Gradient Overlay */}
      <div className="absolute inset-0 bg-gradient-to-r from-[#080808]/95 via-[#080808]/65 to-transparent" />
      <div className="absolute inset-0 bg-gradient-to-t from-[#080808] via-transparent to-black/30" />

      {/* Hero Content Container */}
      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full py-12">
        <div className="max-w-2xl space-y-6">
          
          {/* Eyebrow */}
          <div className="inline-flex items-center space-x-2 bg-white/10 backdrop-blur-md border border-white/15 px-3.5 py-1.5 rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-[#C8BCA7] animate-pulse" />
            <span className="text-[10px] sm:text-xs font-semibold uppercase tracking-[0.25em] text-[#C8BCA7]">
              PREMIUM LIFESTYLE OBJECTS
            </span>
          </div>

          {/* Headline */}
          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white uppercase leading-[1.05]">
            Better <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#C8BCA7] via-[#e5ded4] to-white font-serif italic normal-case tracking-normal">
              Everyday
            </span>
          </h1>

          {/* Subcopy */}
          <p className="text-sm sm:text-base text-[#cfcfcf] leading-relaxed max-w-lg font-light">
            Thoughtfully designed objects for serene living. Discover elevated ceramics, atmospheric lighting, acoustic sound, and architectural storage.
          </p>

          {/* CTAs */}
          <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
            <Link
              href="/shop"
              className="inline-flex items-center justify-center gap-2 px-8 py-4 bg-[#C8BCA7] hover:bg-[#b8ab94] text-[#080808] text-xs sm:text-sm font-bold uppercase tracking-widest rounded transition-all duration-200 shadow-xl cursor-pointer"
            >
              <span>Shop Collection</span>
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link
              href="/categories"
              className="inline-flex items-center justify-center gap-2 px-8 py-4 bg-white/10 hover:bg-white/20 text-white border border-white/20 text-xs sm:text-sm font-semibold uppercase tracking-widest rounded backdrop-blur-sm transition-all duration-200 cursor-pointer"
            >
              <span>Explore Categories</span>
            </Link>
          </div>

          {/* Micro Feature highlights */}
          <div className="pt-6 flex items-center space-x-8 text-[#a0a0a0] text-xs border-t border-white/10">
            <div>
              <span className="block text-white font-bold text-base sm:text-lg">100%</span>
              <span className="text-[10px] tracking-wider uppercase">Artisanal Materials</span>
            </div>
            <div className="w-px h-8 bg-white/10" />
            <div>
              <span className="block text-white font-bold text-base sm:text-lg">24h</span>
              <span className="text-[10px] tracking-wider uppercase">Dispatch Guarantee</span>
            </div>
            <div className="w-px h-8 bg-white/10" />
            <div>
              <span className="block text-white font-bold text-base sm:text-lg">4.9★</span>
              <span className="text-[10px] tracking-wider uppercase">Customer Rating</span>
            </div>
          </div>

        </div>
      </div>
    </section>
  );
}
