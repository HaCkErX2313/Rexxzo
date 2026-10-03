'use client';

import React, { useState, useEffect, useMemo, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import {
  Gift,
  Search,
  SlidersHorizontal,
  RotateCcw,
  Sparkles,
  Heart,
  Package,
  Check,
  ChevronRight,
  ShieldCheck,
  ArrowRight
} from 'lucide-react';
import ProductCard from '@/components/product/ProductCard';
import { api, DEMO_PRODUCTS, DEMO_CATEGORIES } from '@/lib/api';
import { Product } from '@/types';

function GiftCornerInner() {
  const searchParams = useSearchParams();
  const initialQuery = searchParams.get('q') || '';

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState<string>(initialQuery);
  const [sortBy, setSortBy] = useState<string>('featured');
  const [priceTier, setPriceTier] = useState<string>('all');
  const [maxPrice, setMaxPrice] = useState<number>(10000);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  useEffect(() => {
    setLoading(true);
    // Fetch products belonging to gift-corner category
    api.getProducts({ category: 'gift-corner' })
      .then((data) => {
        // Filter specifically for gift-corner items
        const giftItems = data.filter(
          (p) =>
            p.category?.slug === 'gift-corner' ||
            p.categoryName?.toLowerCase() === 'gift corner' ||
            p.sku?.startsWith('REX-GFT')
        );
        if (giftItems.length > 0) {
          setProducts(giftItems);
        } else {
          // Fallback to curated demo gift products
          const fallback = DEMO_PRODUCTS.filter(
            (p) =>
              p.category?.slug === 'gift-corner' ||
              p.categoryName?.toLowerCase() === 'gift corner'
          );
          setProducts(fallback.length > 0 ? fallback : DEMO_PRODUCTS);
        }
      })
      .catch(() => {
        const fallback = DEMO_PRODUCTS.filter(
          (p) =>
            p.category?.slug === 'gift-corner' ||
            p.categoryName?.toLowerCase() === 'gift corner'
        );
        setProducts(fallback.length > 0 ? fallback : DEMO_PRODUCTS);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  // Filter & sort logic
  const filteredProducts = useMemo(() => {
    return products
      .filter((p) => {
        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchName = p.name.toLowerCase().includes(q);
          const matchDesc = p.description.toLowerCase().includes(q);
          if (!matchName && !matchDesc) return false;
        }

        // Price Tier filter
        if (priceTier === 'under-2000' && p.price > 2000) return false;
        if (priceTier === '2000-4000' && (p.price < 2000 || p.price > 4000)) return false;
        if (priceTier === 'luxury' && p.price < 4000) return false;

        // Custom price slider
        if (p.price > maxPrice) return false;

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'price_asc') return a.price - b.price;
        if (sortBy === 'price_desc') return b.price - a.price;
        if (sortBy === 'rating') return b.averageRating - a.averageRating;
        return (b.isFeatured ? 1 : 0) - (a.isFeatured ? 1 : 0);
      });
  }, [products, searchQuery, sortBy, priceTier, maxPrice]);

  const resetFilters = () => {
    setSearchQuery('');
    setSortBy('featured');
    setPriceTier('all');
    setMaxPrice(10000);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
      {/* Breadcrumb Navigation */}
      <nav className="flex items-center space-x-2 text-xs text-[#888888] mb-6">
        <Link href="/" className="hover:text-[#080808] transition-colors">
          Home
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-[#aaaaaa]" />
        <Link href="/shop" className="hover:text-[#080808] transition-colors">
          Shop
        </Link>
        <ChevronRight className="w-3.5 h-3.5 text-[#aaaaaa]" />
        <span className="font-semibold text-[#080808]">Gift Corner</span>
      </nav>

      {/* Hero Category Banner */}
      <div className="relative rounded-2xl bg-[#080808] text-white overflow-hidden p-8 sm:p-12 mb-10 border border-[#222222] shadow-sm">
        <div className="relative z-10 max-w-2xl space-y-3 sm:space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-[#1a1a1a] border border-[#333333] rounded-full text-[#C8BCA7] text-[11px] font-bold uppercase tracking-widest">
            <Gift className="w-3.5 h-3.5" />
            <span>Curated Giving</span>
          </div>

          <h1 className="text-3xl sm:text-5xl font-extrabold uppercase tracking-tight text-white leading-tight">
            Gift Corner
          </h1>

          <p className="text-base sm:text-lg font-serif italic text-[#C8BCA7]">
            Thoughtful gifts for every occasion.
          </p>

          <p className="text-xs sm:text-sm text-[#a3a3a3] leading-relaxed max-w-xl pt-1">
            Discover mindfully designed objects developed to endure. From sculptural stoneware to high-fidelity acoustic design, every piece arrives ready to delight and inspire.
          </p>
        </div>

        {/* Gifting Guarantee Pillars */}
        <div className="mt-8 pt-6 border-t border-[#222222] grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs text-[#d5d0c5]">
          <div className="flex items-center gap-2.5">
            <Package className="w-4 h-4 text-[#C8BCA7] flex-shrink-0" />
            <span>Bespoke Luxury Gift Packaging</span>
          </div>
          <div className="flex items-center gap-2.5">
            <Heart className="w-4 h-4 text-[#C8BCA7] flex-shrink-0" />
            <span>Complimentary Handwritten Note</span>
          </div>
          <div className="flex items-center gap-2.5">
            <ShieldCheck className="w-4 h-4 text-[#C8BCA7] flex-shrink-0" />
            <span>Insured Express Delivery Across India</span>
          </div>
        </div>
      </div>

      {/* Filter & Sorting Controls Header */}
      <div className="border-b border-[#e5e1d8] pb-6 mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Quick Price Tier Filter Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          <span className="text-xs font-bold uppercase tracking-wider text-[#666666] mr-1 hidden sm:inline">
            Budget:
          </span>
          {[
            { id: 'all', label: 'All Gifts' },
            { id: 'under-2000', label: 'Under ₹2,000' },
            { id: '2000-4000', label: '₹2,000 – ₹4,000' },
            { id: 'luxury', label: 'Luxury Drops (₹4,000+)' },
          ].map((tier) => (
            <button
              key={tier.id}
              onClick={() => setPriceTier(tier.id)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold tracking-wider transition-colors whitespace-nowrap cursor-pointer ${
                priceTier === tier.id
                  ? 'bg-[#080808] text-[#C8BCA7]'
                  : 'bg-white text-[#555555] border border-[#e5e1d8] hover:text-[#080808]'
              }`}
            >
              {tier.label}
            </button>
          ))}
        </div>

        {/* Right side: Search & Sort controls */}
        <div className="flex items-center gap-3">
          {/* Search within Gift Corner */}
          <div className="relative flex-1 sm:w-56">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search gifts..."
              className="w-full bg-white border border-[#e5e1d8] rounded-full pl-8 pr-3 py-1.5 text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#C8BCA7]"
            />
            <Search className="w-3.5 h-3.5 text-[#888888] absolute left-3 top-2.5" />
          </div>

          {/* Sort By Dropdown */}
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="bg-white border border-[#e5e1d8] rounded-lg px-3 py-1.5 text-xs text-[#080808] font-medium focus:outline-none focus:border-[#C8BCA7]"
          >
            <option value="featured">Featured First</option>
            <option value="price_asc">Price: Low to High</option>
            <option value="price_desc">Price: High to Low</option>
            <option value="rating">Highest Rated</option>
          </select>
        </div>
      </div>

      {/* Showing count indicator & reset button */}
      <div className="flex items-center justify-between text-xs text-[#777777] mb-6">
        <p>
          Showing <strong className="text-[#080808]">{filteredProducts.length}</strong> curated gifts
        </p>

        {(searchQuery || priceTier !== 'all' || maxPrice < 10000 || sortBy !== 'featured') && (
          <button
            onClick={resetFilters}
            className="inline-flex items-center gap-1.5 text-[#8c7f69] hover:text-[#080808] font-semibold cursor-pointer transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset filters</span>
          </button>
        )}
      </div>

      {/* Product Grid */}
      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="animate-pulse bg-white rounded-xl border border-[#e5e1d8] overflow-hidden">
              <div className="aspect-[4/3] bg-[#eae6df]" />
              <div className="p-4 space-y-3">
                <div className="h-3 bg-[#eae6df] rounded w-2/3" />
                <div className="h-4 bg-[#eae6df] rounded w-4/5" />
                <div className="h-4 bg-[#eae6df] rounded w-1/3" />
              </div>
            </div>
          ))}
        </div>
      ) : filteredProducts.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-2xl border border-[#e5e1d8] p-8 space-y-4 max-w-md mx-auto">
          <Gift className="w-12 h-12 text-[#8c7f69] mx-auto opacity-70" />
          <h3 className="text-lg font-bold uppercase text-[#080808]">No gifts found</h3>
          <p className="text-xs text-[#777777] leading-relaxed">
            We couldn’t find any items matching your selected filters. Try resetting your filters to explore our full gifting collection.
          </p>
          <button
            onClick={resetFilters}
            className="px-5 py-2.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-wider rounded hover:bg-[#222222] transition-colors cursor-pointer"
          >
            Clear All Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
          {filteredProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {/* Bottom Category Cross-Link Section */}
      <div className="mt-16 pt-10 border-t border-[#e5e1d8] text-center">
        <h3 className="text-xs font-bold uppercase tracking-[0.25em] text-[#8c7f69] mb-2">
          Explore More Collections
        </h3>
        <p className="text-sm text-[#555555] max-w-md mx-auto mb-6">
          Looking for specific spaces? Browse our core collections.
        </p>
        <div className="flex flex-wrap items-center justify-center gap-2">
          {DEMO_CATEGORIES.filter((c) => c.slug !== 'gift-corner').map((c) => (
            <Link
              key={c.id}
              href={`/shop?category=${c.slug}`}
              className="px-4 py-2 bg-white border border-[#e5e1d8] rounded-full text-xs font-medium text-[#444444] hover:text-[#080808] hover:border-[#080808] transition-colors"
            >
              {c.name}
            </Link>
          ))}
          <Link
            href="/categories"
            className="px-4 py-2 bg-[#080808] text-[#C8BCA7] rounded-full text-xs font-bold uppercase tracking-wider hover:bg-[#222222] transition-colors"
          >
            All Categories &rarr;
          </Link>
        </div>
      </div>
    </div>
  );
}

export default function GiftCornerClient() {
  return (
    <Suspense
      fallback={
        <div className="max-w-7xl mx-auto px-4 py-20 text-center text-xs text-[#888888]">
          Loading Gift Corner...
        </div>
      }
    >
      <GiftCornerInner />
    </Suspense>
  );
}
