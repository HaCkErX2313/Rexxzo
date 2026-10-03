'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { X, Trash2, Plus, Minus, ArrowRight, ShoppingBag, AlertCircle } from 'lucide-react';
import { useStore } from '@/context/StoreContext';

export default function CartDrawer() {
  const { cart, isCartOpen, setIsCartOpen, updateCartItem, removeFromCart } = useStore();
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null);
  const [drawerError, setDrawerError] = useState<string>('');

  if (!isCartOpen) return null;

  const freeShippingThreshold = 2000;
  const remainingForFreeShipping = Math.max(0, freeShippingThreshold - cart.subtotal);
  const freeShippingPercent = Math.min(100, (cart.subtotal / freeShippingThreshold) * 100);

  const handleQuantityChange = async (itemId: number, newQty: number, maxStock: number) => {
    if (newQty < 1) return;
    if (newQty > maxStock) {
      setDrawerError(`Maximum available stock is ${maxStock} unit${maxStock > 1 ? 's' : ''}.`);
      return;
    }
    if (updatingItemId) return;
    setUpdatingItemId(itemId);
    setDrawerError('');
    try {
      await updateCartItem(itemId, newQty);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setDrawerError(err.message);
      } else {
        setDrawerError('Failed to update quantity.');
      }
    } finally {
      setUpdatingItemId(null);
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (updatingItemId) return;
    setUpdatingItemId(itemId);
    setDrawerError('');
    try {
      await removeFromCart(itemId);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setDrawerError(err.message);
      } else {
        setDrawerError('Failed to remove item.');
      }
    } finally {
      setUpdatingItemId(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={() => setIsCartOpen(false)}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-[#080808] border-l border-[#222222] text-white flex flex-col shadow-2xl">
          
          {/* Header */}
          <div className="px-6 py-5 border-b border-[#1c1c1c] flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <ShoppingBag className="w-5 h-5 text-[#C8BCA7]" />
              <h2 className="text-sm uppercase tracking-widest font-bold text-white">
                Your Selection ({cart.items.reduce((s, i) => s + i.quantity, 0)})
              </h2>
            </div>
            <button
              onClick={() => setIsCartOpen(false)}
              className="p-1.5 text-[#888888] hover:text-white transition-colors cursor-pointer"
              aria-label="Close cart drawer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Drawer Error Banner */}
          {drawerError && (
            <div className="px-6 py-2.5 bg-red-950/80 border-b border-red-900/60 text-red-300 text-xs flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-3.5 h-3.5 flex-shrink-0 text-red-400" />
                <span>{drawerError}</span>
              </div>
              <button
                onClick={() => setDrawerError('')}
                className="text-red-400 hover:text-white ml-2 text-xs font-bold cursor-pointer"
                aria-label="Dismiss error"
              >
                ✕
              </button>
            </div>
          )}

          {/* Free Shipping Progress */}
          <div className="bg-[#121212] px-6 py-3 border-b border-[#202020]">
            <p className="text-xs text-[#999999] mb-1.5">
              {remainingForFreeShipping === 0 ? (
                <span className="text-[#C8BCA7] font-semibold">You unlocked Complimentary Shipping!</span>
              ) : (
                <span>Add <strong className="text-white">₹{remainingForFreeShipping.toLocaleString('en-IN')}</strong> more for Free Shipping</span>
              )}
            </p>
            <div className="w-full bg-[#222222] h-1.5 rounded-full overflow-hidden">
              <div
                className="bg-[#C8BCA7] h-full transition-all duration-300 rounded-full"
                style={{ width: `${freeShippingPercent}%` }}
              />
            </div>
          </div>

          {/* Cart Items List */}
          <div className="flex-1 overflow-y-auto px-6 py-4 divide-y divide-[#1c1c1c]">
            {cart.items.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center py-16 space-y-4">
                <div className="w-16 h-16 rounded-full bg-[#181818] flex items-center justify-center text-[#888888]">
                  <ShoppingBag className="w-7 h-7" />
                </div>
                <div className="space-y-1">
                  <h3 className="text-base font-semibold text-white">Your Cart is Empty</h3>
                  <p className="text-xs text-[#888888] max-w-xs">
                    Discover objects that bring quiet elegance and utility into your day.
                  </p>
                </div>
                <button
                  onClick={() => setIsCartOpen(false)}
                  className="mt-4 px-6 py-2.5 bg-[#C8BCA7] text-[#080808] text-xs uppercase tracking-wider font-bold rounded hover:bg-[#b8ab94] transition-colors cursor-pointer"
                >
                  Explore Collection
                </button>
              </div>
            ) : (
              cart.items.map((item) => {
                const maxStock = item.stockQuantity || 99;
                const isAtMaxStock = item.quantity >= maxStock;
                const isUpdating = updatingItemId === item.id;

                return (
                  <div key={item.id} className="py-4 flex gap-4 items-start">
                    <div className="w-20 h-20 bg-[#161616] rounded-md overflow-hidden relative flex-shrink-0 border border-[#222222]">
                      <Image
                        src={item.imageUrl || '/images/hero_banner.jpg'}
                        alt={item.productName}
                        fill
                        className="object-cover"
                        sizes="80px"
                      />
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <Link
                        href={`/products/${item.productSlug || item.productId}`}
                        onClick={() => setIsCartOpen(false)}
                        className="text-xs font-semibold text-white hover:text-[#C8BCA7] transition-colors line-clamp-1"
                      >
                        {item.productName}
                      </Link>
                      <p className="text-xs text-[#C8BCA7] mt-1 font-mono">
                        ₹{item.price.toLocaleString('en-IN')}
                      </p>
                      {isAtMaxStock && (
                        <p className="text-[10px] text-amber-400/90 font-medium mt-0.5">
                          Max stock reached ({maxStock})
                        </p>
                      )}

                      {/* Quantity controls & remove */}
                      <div className="flex items-center justify-between mt-3">
                        {/* Quantity modifier [-] 1 [+] */}
                        <div className="flex items-center border border-[#333333] rounded bg-[#111111] overflow-hidden">
                          <button
                            onClick={() => handleQuantityChange(item.id, item.quantity - 1, maxStock)}
                            disabled={item.quantity <= 1 || isUpdating}
                            className="p-1.5 hover:text-[#C8BCA7] text-[#888888] disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                            aria-label="Decrease quantity"
                            title={item.quantity <= 1 ? 'Minimum quantity is 1' : 'Decrease quantity'}
                          >
                            <Minus className="w-3.5 h-3.5" />
                          </button>

                          <span className="px-2.5 text-xs font-semibold text-white min-w-[28px] text-center select-none">
                            {isUpdating ? (
                              <span className="inline-block animate-pulse text-[#C8BCA7]">…</span>
                            ) : (
                              item.quantity
                            )}
                          </span>

                          <button
                            onClick={() => handleQuantityChange(item.id, item.quantity + 1, maxStock)}
                            disabled={isAtMaxStock || isUpdating}
                            className="p-1.5 hover:text-[#C8BCA7] text-[#888888] disabled:opacity-30 disabled:cursor-not-allowed transition-colors cursor-pointer"
                            aria-label="Increase quantity"
                            title={isAtMaxStock ? `Maximum stock reached (${maxStock})` : 'Increase quantity'}
                          >
                            <Plus className="w-3.5 h-3.5" />
                          </button>
                        </div>

                        {/* Subtotal & Remove */}
                        <div className="flex items-center gap-3">
                          <span className="text-xs font-bold text-white font-mono">
                            ₹{item.subtotal.toLocaleString('en-IN')}
                          </span>

                          <button
                            onClick={() => handleRemoveItem(item.id)}
                            disabled={isUpdating}
                            className="text-[#777777] hover:text-red-400 p-1 transition-colors disabled:opacity-30 cursor-pointer"
                            aria-label="Remove item"
                            title="Remove item"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Footer & Checkout */}
          {cart.items.length > 0 && (
            <div className="border-t border-[#1c1c1c] px-6 py-5 bg-[#0d0d0d] space-y-4">
              <div className="space-y-1.5 text-xs">
                <div className="flex justify-between text-[#999999]">
                  <span>Subtotal</span>
                  <span className="text-white font-medium font-mono">₹{cart.subtotal.toLocaleString('en-IN')}</span>
                </div>
                <div className="flex justify-between text-[#999999]">
                  <span>Estimated Shipping</span>
                  <span className="text-white font-medium">
                    {cart.shipping === 0 ? 'Complimentary' : `₹${cart.shipping}`}
                  </span>
                </div>
                <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-[#222222]">
                  <span>Total</span>
                  <span className="text-[#C8BCA7] font-mono">₹{cart.total.toLocaleString('en-IN')}</span>
                </div>
              </div>

              <div className="flex flex-col gap-2">
                <Link
                  href="/checkout"
                  onClick={() => setIsCartOpen(false)}
                  className="w-full bg-[#C8BCA7] hover:bg-[#b8ab94] text-[#080808] font-bold text-xs uppercase tracking-wider py-3.5 rounded flex items-center justify-center gap-2 transition-colors cursor-pointer shadow-lg"
                >
                  <span>Proceed to Checkout</span>
                  <ArrowRight className="w-4 h-4" />
                </Link>
                <Link
                  href="/cart"
                  onClick={() => setIsCartOpen(false)}
                  className="w-full text-center text-xs text-[#999999] hover:text-white uppercase tracking-wider py-2 transition-colors"
                >
                  View Full Cart & Apply Coupon
                </Link>
              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
