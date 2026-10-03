import React from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowRight, Compass, Feather, ShieldCheck } from 'lucide-react';

export const metadata = {
  title: 'About Our Studio — REXXZO | Better Everyday',
  description: 'Learn about REXXZO, our commitment to mindful craftsmanship, calm aesthetics, and enduring quality.',
};

export default function AboutPage() {
  return (
    <div className="space-y-16 sm:space-y-24 py-12 sm:py-16">
      {/* Hero section */}
      <section className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-4">
        <span className="text-[11px] font-bold uppercase tracking-[0.3em] text-[#8c7f69]">
          The Studio Manifesto
        </span>
        <h1 className="text-3xl sm:text-5xl font-extrabold uppercase tracking-tight text-[#080808]">
          Better Everyday.
        </h1>
        <p className="text-xs sm:text-sm text-[#666666] max-w-2xl mx-auto leading-relaxed pt-2">
          REXXZO was founded with a singular conviction: that the objects we surround ourselves with directly condition our emotional equilibrium.
        </p>
      </section>

      {/* Image Banner */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="relative aspect-[21/9] rounded-2xl overflow-hidden border border-[#e5e1d8] shadow-lg">
          <Image
            src="/images/hero_banner.jpg"
            alt="REXXZO Atelier and Design Studio"
            fill
            className="object-cover"
            priority
          />
        </div>
      </section>

      {/* Core Principles */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-8 space-y-4 shadow-sm">
            <div className="w-12 h-12 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center">
              <Feather className="w-6 h-6" />
            </div>
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#080808]">
              Tactile Restraint
            </h2>
            <p className="text-xs text-[#666666] leading-relaxed">
              We eliminate superfluous flourishes. Pure geometries, raw unglazed ceramics, and tactile brushed surfaces invite touch and establish domestic serenity.
            </p>
          </div>

          <div className="bg-white rounded-xl border border-[#e5e1d8] p-8 space-y-4 shadow-sm">
            <div className="w-12 h-12 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center">
              <Compass className="w-6 h-6" />
            </div>
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#080808]">
              Enduring Engineering
            </h2>
            <p className="text-xs text-[#666666] leading-relaxed">
              Our products are engineered for daily durability. 18/8 stainless steel, high-density recycled felt, and solid hardwoods are rigorously chosen to last decades.
            </p>
          </div>

          <div className="bg-white rounded-xl border border-[#e5e1d8] p-8 space-y-4 shadow-sm">
            <div className="w-12 h-12 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <h2 className="text-sm font-bold uppercase tracking-wider text-[#080808]">
              Artisanal Integrity
            </h2>
            <p className="text-xs text-[#666666] leading-relaxed">
              We collaborate with master ceramicists, acoustic engineers, and metal turners across India to preserve venerable heritage techniques in contemporary contexts.
            </p>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="max-w-4xl mx-auto px-4 text-center space-y-6">
        <h2 className="text-2xl font-bold uppercase text-[#080808]">Experience the Difference</h2>
        <Link
          href="/shop"
          className="inline-flex items-center gap-2 px-8 py-3.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded hover:bg-[#222222] transition-colors shadow-lg"
        >
          <span>Explore All Collections</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </section>
    </div>
  );
}
