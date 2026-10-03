import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { ArrowRight, Sparkles } from 'lucide-react';
import HeroBanner from '@/components/home/HeroBanner';
import TrustStrip from '@/components/home/TrustStrip';
import CategoryCard from '@/components/category/CategoryCard';
import ProductCard from '@/components/product/ProductCard';
import { api } from '@/lib/api';

export default async function HomePage() {
  const [categories, products] = await Promise.all([
    api.getCategories(),
    api.getProducts(),
  ]);

  const featuredProducts = products.filter(p => p.isFeatured).slice(0, 4);
  const trendingProducts = products.slice(2, 6);

  return (
    <div className="space-y-16 sm:space-y-24">
      {/* 1. Cinematic Hero */}
      <HeroBanner />

      {/* 2. Trust Strip */}
      <TrustStrip />

      {/* 3. Shop by Category */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
          <div>
            <div className="flex items-center gap-2 text-[#8c7f69] text-xs font-bold uppercase tracking-widest mb-1.5">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Spatial Collections</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
              Shop By Category
            </h2>
          </div>
          <Link
            href="/categories"
            className="inline-flex items-center text-xs font-bold uppercase tracking-widest text-[#080808] hover:text-[#8c7f69] transition-colors group"
          >
            <span>View All Categories</span>
            <ArrowRight className="w-4 h-4 ml-1.5 transition-transform group-hover:translate-x-1" />
          </Link>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-3 sm:gap-4">
          {categories.slice(0, 7).map((category) => (
            <CategoryCard key={category.id} category={category} />
          ))}
        </div>
      </section>

      {/* 4. Featured Products */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
          <div>
            <span className="text-[11px] font-bold uppercase tracking-[0.2em] text-[#8c7f69] block mb-1.5">
              Hand-Picked Selection
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
              Featured Products
            </h2>
          </div>
          <Link
            href="/shop"
            className="inline-flex items-center text-xs font-bold uppercase tracking-widest text-[#080808] hover:text-[#8c7f69] transition-colors group"
          >
            <span>Explore All Pieces</span>
            <ArrowRight className="w-4 h-4 ml-1.5 transition-transform group-hover:translate-x-1" />
          </Link>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {featuredProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>

      {/* 5. Editorial Spotlight Banner */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="relative rounded-2xl bg-[#080808] text-white overflow-hidden p-8 sm:p-12 lg:p-16 border border-[#222222]">
          <div className="absolute right-0 top-0 bottom-0 w-full lg:w-1/2 opacity-35 lg:opacity-60 overflow-hidden">
            <Image
              src="/images/desk_lamp.jpg"
              alt="Design detail"
              fill
              className="object-cover object-center"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-[#080808] via-[#080808]/70 to-transparent" />
          </div>

          <div className="relative z-10 max-w-xl space-y-6">
            <span className="text-xs uppercase tracking-[0.25em] text-[#C8BCA7] font-semibold">
              The REXXZO Philosophy
            </span>
            <h3 className="text-2xl sm:text-4xl font-extrabold uppercase tracking-tight leading-tight">
              Calmness by Design, <br />
              <span className="font-serif italic font-normal text-[#C8BCA7] normal-case">
                Longevity by Nature.
              </span>
            </h3>
            <p className="text-sm text-[#a3a3a3] leading-relaxed">
              We reject transient novelties. Every REXXZO object is developed with intentional restraint — engineered from honest stone, matte ceramics, turned wood, and brushed metals that acquire character over years of daily contact.
            </p>
            <div className="pt-2">
              <Link
                href="/about"
                className="inline-flex items-center gap-2 px-6 py-3 bg-[#C8BCA7] text-[#080808] font-bold text-xs uppercase tracking-widest rounded hover:bg-[#b8ab94] transition-colors"
              >
                <span>Read Our Journal</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 6. Trending / Essential Curations */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-12">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between mb-8 gap-4">
          <div>
            <span className="text-[11px] font-bold uppercase tracking-[0.2em] text-[#8c7f69] block mb-1.5">
              Daily Essentials
            </span>
            <h2 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
              Trending This Season
            </h2>
          </div>
          <Link
            href="/shop"
            className="inline-flex items-center text-xs font-bold uppercase tracking-widest text-[#080808] hover:text-[#8c7f69] transition-colors group"
          >
            <span>See Catalog</span>
            <ArrowRight className="w-4 h-4 ml-1.5 transition-transform group-hover:translate-x-1" />
          </Link>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {trendingProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>
    </div>
  );
}
