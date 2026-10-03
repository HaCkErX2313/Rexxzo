'use client';

import React, { useState, useEffect, useMemo, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { SlidersHorizontal, Search, RotateCcw, X } from 'lucide-react';
import ProductCard from '@/components/product/ProductCard';
import { api, DEMO_CATEGORIES } from '@/lib/api';
import { Product } from '@/types';

function ShopContent() {
  const searchParams = useSearchParams();
  const initialCategory = searchParams.get('category') || 'all';
  const initialQuery = searchParams.get('q') || '';

  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>(initialCategory);
  const [searchQuery, setSearchQuery] = useState<string>(initialQuery);
  const [sortBy, setSortBy] = useState<string>('featured');
  const [priceRange, setPriceRange] = useState<number>(10000);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  useEffect(() => {
    if (initialCategory) setSelectedCategory(initialCategory);
    if (initialQuery) setSearchQuery(initialQuery);
  }, [initialCategory, initialQuery]);

  useEffect(() => {
    setLoading(true);
    api.getProducts().then((data) => {
      setProducts(data);
      setLoading(false);
    });
  }, []);

  // Filter & sort logic
  const filteredProducts = useMemo(() => {
    return products
      .filter((p) => {
        // Category filter
        if (selectedCategory !== 'all') {
          const matchSlug = p.category?.slug === selectedCategory;
          const matchName = p.categoryName?.toLowerCase().replace(/\s+/g, '-') === selectedCategory;
          if (!matchSlug && !matchName) return false;
        }
        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchName = p.name.toLowerCase().includes(q);
          const matchDesc = p.description.toLowerCase().includes(q);
          if (!matchName && !matchDesc) return false;
        }
        // Price limit
        if (p.price > priceRange) return false;

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'price_asc') return a.price - b.price;
        if (sortBy === 'price_desc') return b.price - a.price;
        if (sortBy === 'rating') return b.averageRating - a.averageRating;
        return (b.isFeatured ? 1 : 0) - (a.isFeatured ? 1 : 0);
      });
  }, [products, selectedCategory, searchQuery, sortBy, priceRange]);

  const resetFilters = () => {
    setSelectedCategory('all');
    setSearchQuery('');
    setSortBy('featured');
    setPriceRange(10000);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-14">
      {/* Page Title & Breadcrumb */}
      <div className="border-b border-[#e5e1d8] pb-8 mb-8">
        <span className="text-[11px] font-bold uppercase tracking-[0.25em] text-[#8c7f69] block mb-2">
          Curated Catalog
        </span>
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold uppercase tracking-tight text-[#080808]">
              {selectedCategory === 'all'
                ? 'All Products'
                : DEMO_CATEGORIES.find((c) => c.slug === selectedCategory)?.name || 'Collection'}
            </h1>
            <p className="text-xs sm:text-sm text-[#777777] mt-1.5 max-w-xl">
              Thoughtfully engineered essentials for modern spaces. Showing{' '}
              <strong className="text-[#080808]">{filteredProducts.length}</strong> available pieces.
            </p>
          </div>

          {/* Quick Category Pills on Desktop */}
          <div className="hidden lg:flex items-center gap-2 overflow-x-auto pb-1">
            <button
              onClick={() => setSelectedCategory('all')}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold tracking-wider uppercase transition-colors ${
                selectedCategory === 'all'
                  ? 'bg-[#080808] text-[#C8BCA7]'
                  : 'bg-white text-[#666666] border border-[#e5e1d8] hover:text-[#080808]'
              }`}
            >
              All
            </button>
            {DEMO_CATEGORIES.map((cat) => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.slug)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-semibold tracking-wider uppercase transition-colors whitespace-nowrap ${
                  selectedCategory === cat.slug
                    ? 'bg-[#080808] text-[#C8BCA7]'
                    : 'bg-white text-[#666666] border border-[#e5e1d8] hover:text-[#080808]'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Mobile Filter & Sort Bar */}
      <div className="lg:hidden flex items-center justify-between gap-3 mb-6">
        <button
          onClick={() => setMobileFilterOpen(true)}
          className="flex-1 py-2.5 px-4 bg-white border border-[#e5e1d8] rounded-md text-xs font-bold uppercase tracking-wider text-[#080808] flex items-center justify-center gap-2 shadow-sm"
        >
          <SlidersHorizontal className="w-4 h-4 text-[#8c7f69]" />
          <span>Filters</span>
        </button>

        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
          className="py-2.5 px-4 bg-white border border-[#e5e1d8] rounded-md text-xs font-bold uppercase tracking-wider text-[#080808] shadow-sm"
        >
          <option value="featured">Featured</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
          <option value="rating">Top Rated</option>
        </select>
      </div>

      {/* Main Content Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        
        {/* Desktop Sidebar Filters */}
        <aside className="hidden lg:block space-y-6">
          <div className="bg-white rounded-lg border border-[#e5e1d8] p-6 space-y-6">
            <div className="flex items-center justify-between pb-4 border-b border-[#f0ece5]">
              <h3 className="text-xs font-bold uppercase tracking-widest text-[#080808] flex items-center gap-2">
                <SlidersHorizontal className="w-4 h-4 text-[#8c7f69]" />
                <span>Filters</span>
              </h3>
              <button
                onClick={resetFilters}
                className="text-[11px] text-[#888888] hover:text-[#080808] flex items-center gap-1 transition-colors"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Reset</span>
              </button>
            </div>

            {/* Keyword Search */}
            <div>
              <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-2">
                Search
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="e.g. ceramic, lamp..."
                  className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2 text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#C8BCA7]"
                />
                <Search className="w-3.5 h-3.5 text-[#888888] absolute right-3 top-2.5" />
              </div>
            </div>

            {/* Category selection */}
            <div>
              <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-2">
                Categories
              </label>
              <div className="space-y-1.5">
                <button
                  onClick={() => setSelectedCategory('all')}
                  className={`w-full text-left px-2 py-1.5 rounded text-xs transition-colors flex justify-between ${
                    selectedCategory === 'all'
                      ? 'bg-[#f4f1ea] font-bold text-[#080808]'
                      : 'text-[#666666] hover:bg-[#faf9f7]'
                  }`}
                >
                  <span>All Categories</span>
                  <span className="text-[10px] text-[#999999]">{products.length}</span>
                </button>
                {DEMO_CATEGORIES.map((cat) => (
                  <button
                    key={cat.id}
                    onClick={() => setSelectedCategory(cat.slug)}
                    className={`w-full text-left px-2 py-1.5 rounded text-xs transition-colors flex justify-between ${
                      selectedCategory === cat.slug
                        ? 'bg-[#f4f1ea] font-bold text-[#080808]'
                        : 'text-[#666666] hover:bg-[#faf9f7]'
                    }`}
                  >
                    <span>{cat.name}</span>
                    <span className="text-[10px] text-[#999999]">
                      {products.filter((p) => p.category?.slug === cat.slug || p.categoryName === cat.name).length}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {/* Price Filter Slider */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555]">
                  Max Price
                </label>
                <span className="text-xs font-bold text-[#080808]">₹{priceRange.toLocaleString()}</span>
              </div>
              <input
                type="range"
                min={1000}
                max={10000}
                step={500}
                value={priceRange}
                onChange={(e) => setPriceRange(Number(e.target.value))}
                className="w-full accent-[#080808] cursor-pointer"
              />
              <div className="flex justify-between text-[10px] text-[#888888] mt-1">
                <span>₹1,000</span>
                <span>₹10,000</span>
              </div>
            </div>

            {/* Sort Option (Desktop) */}
            <div>
              <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-2">
                Sort Order
              </label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              >
                <option value="featured">Featured Curations</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="rating">Top Rated</option>
              </select>
            </div>
          </div>
        </aside>

        {/* Product Grid Area */}
        <main className="lg:col-span-3">
          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="animate-pulse bg-white rounded-lg p-4 border border-[#e5e1d8] space-y-4">
                  <div className="aspect-[4/5] bg-[#eae6df] rounded" />
                  <div className="h-4 bg-[#eae6df] rounded w-3/4" />
                  <div className="h-4 bg-[#eae6df] rounded w-1/2" />
                </div>
              ))}
            </div>
          ) : filteredProducts.length === 0 ? (
            <div className="bg-white rounded-lg border border-[#e5e1d8] p-12 text-center space-y-4">
              <div className="w-12 h-12 rounded-full bg-[#f4f2ee] text-[#888888] flex items-center justify-center mx-auto">
                <Search className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-[#080808]">No Products Found</h3>
              <p className="text-xs text-[#777777] max-w-sm mx-auto">
                We couldn&apos;t find any pieces matching your current filters. Try resetting the filters or searching with different keywords.
              </p>
              <button
                onClick={resetFilters}
                className="px-5 py-2.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-wider rounded hover:bg-[#222222] transition-colors"
              >
                Reset All Filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
              {filteredProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </main>
      </div>

      {/* Mobile Filters Bottom Sheet */}
      {mobileFilterOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setMobileFilterOpen(false)} />
          <div className="fixed inset-x-0 bottom-0 max-h-[85vh] bg-white rounded-t-2xl p-6 overflow-y-auto space-y-6 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-[#e5e1d8]">
              <h3 className="text-sm font-bold uppercase tracking-wider text-[#080808]">Filter Products</h3>
              <button onClick={() => setMobileFilterOpen(false)} className="p-1">
                <X className="w-5 h-5 text-[#888888]" />
              </button>
            </div>

            {/* Mobile Categories */}
            <div>
              <span className="text-xs font-bold uppercase text-[#555555] block mb-2">Category</span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setSelectedCategory('all')}
                  className={`p-2 rounded text-xs text-left ${
                    selectedCategory === 'all' ? 'bg-[#080808] text-white font-bold' : 'bg-[#f4f2ee]'
                  }`}
                >
                  All Categories
                </button>
                {DEMO_CATEGORIES.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => setSelectedCategory(c.slug)}
                    className={`p-2 rounded text-xs text-left ${
                      selectedCategory === c.slug ? 'bg-[#080808] text-white font-bold' : 'bg-[#f4f2ee]'
                    }`}
                  >
                    {c.name}
                  </button>
                ))}
              </div>
            </div>

            {/* Mobile Price Slider */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-bold uppercase text-[#555555]">Max Price</span>
                <span className="text-xs font-bold text-[#080808]">₹{priceRange.toLocaleString()}</span>
              </div>
              <input
                type="range"
                min={1000}
                max={10000}
                step={500}
                value={priceRange}
                onChange={(e) => setPriceRange(Number(e.target.value))}
                className="w-full accent-[#080808]"
              />
            </div>

            {/* Apply button */}
            <div className="pt-2 flex gap-3">
              <button
                onClick={resetFilters}
                className="flex-1 py-3 border border-[#e5e1d8] rounded text-xs font-bold uppercase tracking-wider text-[#666666]"
              >
                Reset
              </button>
              <button
                onClick={() => setMobileFilterOpen(false)}
                className="flex-1 py-3 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase tracking-wider"
              >
                Show Results
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function ShopPage() {
  return (
    <Suspense fallback={<div className="max-w-7xl mx-auto px-4 py-16 text-center text-sm">Loading catalog...</div>}>
      <ShopContent />
    </Suspense>
  );
}
