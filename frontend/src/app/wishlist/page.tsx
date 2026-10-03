'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Heart, Trash2, ShoppingBag, ArrowRight } from 'lucide-react';
import { useStore } from '@/context/StoreContext';
import { DEMO_PRODUCTS } from '@/lib/api';

export default function WishlistPage() {
  const { wishlist, toggleWishlist, addToCart } = useStore();

  if (wishlist.length === 0) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-24 text-center space-y-6">
        <div className="w-20 h-20 bg-white rounded-full flex items-center justify-center text-[#888888] mx-auto border border-[#e5e1d8] shadow-sm">
          <Heart className="w-8 h-8 text-[#C8BCA7]" />
        </div>
        <div className="space-y-2">
          <span className="text-[10px] font-bold uppercase tracking-[0.3em] text-[#8c7f69]">
            Saved Objects
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
            Your Wishlist is Empty
          </h1>
          <p className="text-xs sm:text-sm text-[#777777] max-w-md mx-auto leading-relaxed">
            Your wishlist is waiting. Save the pieces you love and find them here later.
          </p>
        </div>
        <Link
          href="/shop"
          className="inline-flex items-center gap-2 px-8 py-3.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded hover:bg-[#222222] transition-colors shadow-lg"
        >
          <span>Discover Products</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-16">
      <div className="border-b border-[#e5e1d8] pb-6 mb-8 flex justify-between items-end">
        <div>
          <span className="text-[10px] font-bold uppercase tracking-[0.25em] text-[#8c7f69] block mb-1">
            Personal Collection
          </span>
          <h1 className="text-2xl sm:text-4xl font-extrabold uppercase tracking-tight text-[#080808]">
            My Wishlist ({wishlist.length})
          </h1>
        </div>
        <Link
          href="/shop"
          className="text-xs font-bold uppercase tracking-wider text-[#080808] hover:text-[#8c7f69] transition-colors"
        >
          Browse More
        </Link>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {wishlist.map((item) => {
          const product = DEMO_PRODUCTS.find((p) => p.id === item.productId) || {
            id: item.productId,
            name: item.productName,
            slug: item.productSlug,
            description: '',
            price: item.price,
            sku: 'REX',
            stockQuantity: item.stockQuantity || 10,
            images: [{ imageUrl: item.imageUrl || '/images/hero_banner.jpg' }],
            averageRating: item.averageRating || 4.8,
            reviewCount: 12,
          };

          return (
            <div
              key={item.id}
              className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden flex flex-col justify-between shadow-sm group hover:shadow-lg transition-all"
            >
              <div className="relative aspect-[4/5] bg-[#f8f6f2] overflow-hidden">
                <Link href={`/products/${item.productSlug || item.productId}`} className="relative block w-full h-full">
                  <Image
                    src={item.imageUrl || '/images/hero_banner.jpg'}
                    alt={item.productName}
                    fill
                    className="object-cover group-hover:scale-105 transition-transform duration-500"
                    sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
                  />
                </Link>

                <button
                  onClick={() => toggleWishlist(product)}
                  className="absolute top-3 right-3 p-2 bg-white/80 hover:bg-white text-[#777777] hover:text-red-500 rounded-full backdrop-blur-sm transition-colors cursor-pointer shadow"
                  aria-label="Remove from wishlist"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>

              <div className="p-4 space-y-3">
                <Link
                  href={`/products/${item.productSlug || item.productId}`}
                  className="text-xs sm:text-sm font-bold text-[#080808] hover:text-[#8c7f69] transition-colors line-clamp-1"
                >
                  {item.productName}
                </Link>

                <p className="text-sm font-extrabold text-[#080808]">
                  ₹{item.price.toLocaleString()}
                </p>

                <button
                  onClick={() => addToCart(item.productId, 1)}
                  className="w-full py-2.5 bg-[#080808] hover:bg-[#222222] text-[#C8BCA7] text-[11px] font-bold uppercase tracking-wider rounded flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow"
                >
                  <ShoppingBag className="w-3.5 h-3.5" />
                  <span>Move to Cart</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
