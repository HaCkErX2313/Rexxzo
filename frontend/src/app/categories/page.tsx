import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { ArrowRight } from 'lucide-react';
import { DEMO_CATEGORIES } from '@/lib/api';

export const metadata = {
  title: 'Categories — REXXZO | Better Everyday',
  description: 'Explore the full spectrum of REXXZO spatial collections from ambient lighting to acoustic living.',
};

export default function CategoriesPage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      {/* Header */}
      <div className="text-center max-w-2xl mx-auto mb-12 sm:mb-16">
        <span className="text-[11px] font-bold uppercase tracking-[0.3em] text-[#8c7f69] block mb-2">
          Curated Spaces
        </span>
        <h1 className="text-3xl sm:text-5xl font-extrabold uppercase tracking-tight text-[#080808]">
          Collections & Categories
        </h1>
        <p className="text-xs sm:text-sm text-[#777777] mt-3 leading-relaxed">
          Each collection is developed around materials that soothe and silhouettes that bring geometric harmony to contemporary spaces.
        </p>
      </div>

      {/* Categories Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
        {DEMO_CATEGORIES.map((category) => (
          <Link
            key={category.id}
            href={category.slug === 'gift-corner' ? '/shop/gift-corner' : `/shop?category=${category.slug}`}
            className="group relative flex flex-col bg-white rounded-xl border border-[#e5e1d8] overflow-hidden hover:shadow-2xl transition-all duration-300"
          >
            {/* Image Aspect Box */}
            <div className="relative aspect-[16/10] w-full bg-[#181818] overflow-hidden">
              <Image
                src={category.imageUrl || '/images/hero_banner.jpg'}
                alt={category.name}
                fill
                className="object-cover object-center group-hover:scale-106 transition-transform duration-700 ease-out opacity-90 group-hover:opacity-100"
                sizes="(max-width: 768px) 100vw, (max-width: 1024px) 50vw, 33vw"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#080808]/80 via-transparent to-transparent" />
              <div className="absolute top-4 right-4 bg-white/20 backdrop-blur-md px-2.5 py-1 rounded-full text-white text-[10px] font-bold uppercase tracking-wider">
                {category.itemCount} Designs
              </div>
            </div>

            {/* Content Details */}
            <div className="p-6 flex-1 flex flex-col justify-between">
              <div>
                <h2 className="text-lg font-bold uppercase tracking-wide text-[#080808] group-hover:text-[#8c7f69] transition-colors">
                  {category.name}
                </h2>
                <p className="text-xs text-[#666666] mt-2 leading-relaxed">
                  {category.description}
                </p>
              </div>

              <div className="pt-6 mt-4 border-t border-[#f0ece5] flex items-center justify-between text-xs font-bold uppercase tracking-widest text-[#080808] group-hover:text-[#8c7f69] transition-colors">
                <span>View Collection</span>
                <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1.5" />
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
