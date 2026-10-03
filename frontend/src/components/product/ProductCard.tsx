'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Heart, Star, ShoppingBag } from 'lucide-react';
import { Product } from '@/types';
import { useStore } from '@/context/StoreContext';

export default function ProductCard({ product }: { product: Product }) {
  const { addToCart, toggleWishlist, isInWishlist } = useStore();
  const wishlisted = isInWishlist(product.id);

  const discountPercent = product.compareAtPrice && product.compareAtPrice > product.price
    ? Math.round(((product.compareAtPrice - product.price) / product.compareAtPrice) * 100)
    : 0;

  const primaryImage = product.images?.[0]?.imageUrl || '/images/hero_banner.jpg';

  return (
    <div className="group relative flex flex-col bg-white rounded-lg border border-[#eae6df] overflow-hidden hover:shadow-xl transition-all duration-300">
      {/* Image Container */}
      <div className="relative aspect-[4/5] w-full bg-[#f4f2ee] overflow-hidden">
        <Link href={`/products/${product.slug || product.id}`} className="relative block w-full h-full">
          <Image
            src={primaryImage}
            alt={product.name}
            fill
            sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
            className="object-cover object-center group-hover:scale-105 transition-transform duration-500 ease-out"
          />
        </Link>

        {/* Badges */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5 pointer-events-none">
          {((product.stock !== undefined && product.stock <= 0) || (product.stockQuantity !== undefined && product.stockQuantity <= 0) || product.status === 'OUT_OF_STOCK') && (
            <span className="bg-rose-600 text-white text-[10px] font-bold tracking-wider px-2 py-0.5 rounded uppercase shadow-xs">
              Out of Stock
            </span>
          )}
          {discountPercent > 0 && !(product.stock !== undefined && product.stock <= 0) && (
            <span className="bg-[#080808] text-[#C8BCA7] text-[10px] font-bold tracking-wider px-2 py-0.5 rounded uppercase">
              Save {discountPercent}%
            </span>
          )}
          {product.isFeatured && discountPercent === 0 && !(product.stock !== undefined && product.stock <= 0) && (
            <span className="bg-[#C8BCA7] text-[#080808] text-[10px] font-bold tracking-wider px-2 py-0.5 rounded uppercase">
              Featured
            </span>
          )}
        </div>

        {/* Wishlist Button */}
        <button
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            toggleWishlist(product);
          }}
          className={`absolute top-3 right-3 p-2 rounded-full backdrop-blur-md transition-all duration-200 cursor-pointer ${
            wishlisted
              ? 'bg-[#080808] text-[#C8BCA7]'
              : 'bg-white/80 hover:bg-white text-[#444444] hover:text-[#080808] shadow-sm'
          }`}
          aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
        >
          <Heart className={`w-4 h-4 ${wishlisted ? 'fill-[#C8BCA7]' : ''}`} />
        </button>

        {/* Quick Add Overlay on Desktop Hover */}
        <div className="absolute inset-x-3 bottom-3 opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 hidden sm:block">
          {((product.stock !== undefined && product.stock <= 0) || (product.stockQuantity !== undefined && product.stockQuantity <= 0) || product.status === 'OUT_OF_STOCK') ? (
            <div className="w-full bg-[#333333]/90 text-[#aaaaaa] backdrop-blur-sm text-[11px] font-bold uppercase tracking-wider py-2.5 rounded shadow-lg text-center">
              Out of Stock
            </div>
          ) : (
            <button
              onClick={() => addToCart(product.id, 1)}
              className="w-full bg-[#080808]/90 hover:bg-[#080808] text-white backdrop-blur-sm text-[11px] font-bold uppercase tracking-wider py-2.5 rounded shadow-lg flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
            >
              <ShoppingBag className="w-3.5 h-3.5 text-[#C8BCA7]" />
              <span>Quick Add</span>
            </button>
          )}
        </div>
      </div>

      {/* Details Container */}
      <div className="p-4 flex flex-col flex-1 justify-between bg-white">
        <div>
          {/* Category */}
          <span className="text-[10px] tracking-widest text-[#888888] uppercase font-medium block mb-1">
            {product.categoryName || 'Lifestyle'}
          </span>

          {/* Product Name */}
          <Link
            href={`/products/${product.slug || product.id}`}
            className="text-xs sm:text-sm font-semibold text-[#111111] hover:text-[#8c7f69] transition-colors line-clamp-1 block mb-1.5"
          >
            {product.name}
          </Link>

          {/* Rating */}
          <div className="flex items-center gap-1 mb-2">
            <div className="flex items-center text-amber-500">
              <Star className="w-3 h-3 fill-current" />
            </div>
            <span className="text-[11px] font-bold text-[#222222]">
              {product.averageRating.toFixed(1)}
            </span>
            <span className="text-[10px] text-[#888888]">
              ({product.reviewCount})
            </span>
          </div>
        </div>

        {/* Price & Mobile Add button */}
        <div className="flex items-center justify-between pt-2 border-t border-[#f0ece5]">
          <div className="flex items-baseline gap-2">
            <span className="text-sm sm:text-base font-bold text-[#080808]">
              ₹{product.price.toLocaleString()}
            </span>
            {product.compareAtPrice && product.compareAtPrice > product.price && (
              <span className="text-xs text-[#999999] line-through">
                ₹{product.compareAtPrice.toLocaleString()}
              </span>
            )}
          </div>

          <button
            onClick={() => addToCart(product.id, 1)}
            className="sm:hidden p-2 bg-[#080808] text-[#C8BCA7] rounded hover:bg-[#222222] transition-colors cursor-pointer"
            aria-label="Add to cart"
          >
            <ShoppingBag className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
