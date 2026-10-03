'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { ArrowUpRight } from 'lucide-react';
import { Category } from '@/types';

export default function CategoryCard({ category, subtitle }: { category: Category; subtitle?: string }) {
  const isGiftCorner = category.slug === 'gift-corner' || category.name.toLowerCase() === 'gift corner';
  const targetHref = isGiftCorner ? '/shop/gift-corner' : `/shop?category=${category.slug}`;
  const displaySubtitle = subtitle || (isGiftCorner ? 'Find something special' : `${category.itemCount || 10}+ Items`);

  return (
    <Link
      href={targetHref}
      className="group relative block overflow-hidden rounded-lg bg-[#181818] aspect-[4/3] sm:aspect-[1/1] border border-[#e8e4dc] shadow-sm hover:shadow-xl transition-all duration-300"
    >
      {/* Background Image */}
      <Image
        src={category.imageUrl || '/images/hero_banner.jpg'}
        alt={category.name}
        fill
        sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 16vw"
        className="object-cover object-center group-hover:scale-108 transition-transform duration-700 ease-out opacity-85 group-hover:opacity-100"
      />

      {/* Gradient Overlay */}
      <div className="absolute inset-0 bg-gradient-to-t from-[#080808]/90 via-[#080808]/30 to-transparent" />

      {/* Content */}
      <div className="absolute inset-0 p-4 sm:p-5 flex flex-col justify-end">
        <div className="flex items-end justify-between">
          <div>
            <span className="text-[10px] tracking-widest text-[#C8BCA7] font-semibold uppercase block mb-1">
              {displaySubtitle}
            </span>
            <h3 className="text-sm sm:text-base font-bold text-white tracking-wide group-hover:text-[#C8BCA7] transition-colors">
              {category.name}
            </h3>
          </div>

          <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md group-hover:bg-[#C8BCA7] text-white group-hover:text-[#080808] flex items-center justify-center transition-all duration-300">
            <ArrowUpRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
          </div>
        </div>
      </div>
    </Link>
  );
}
