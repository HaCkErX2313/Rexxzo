'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Trash2, Plus, Minus, ArrowRight, ShoppingBag, ShieldCheck, Tag, AlertCircle } from 'lucide-react';
import { useStore } from '@/context/StoreContext';

export default function CartPage() {
  const { cart, updateCartItem, removeFromCart } = useStore();
  const [couponCode, setCouponCode] = useState('');
  const [couponApplied, setCouponApplied] = useState(false);
  const [couponError, setCouponError] = useState('');
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null);
  const [cartError, setCartError] = useState<string>('');

  const handleApplyCoupon = (e: React.FormEvent) => {
    e.preventDefault();
    if (!couponCode.trim()) return;
    if (couponCode.trim().toUpperCase() === 'REXXZO10' || couponCode.trim().toUpperCase() === 'REXXO10' || couponCode.trim().toUpperCase() === 'WELCOME10') {
      setCouponApplied(true);
      setCouponError('');
    } else {
      setCouponError('Invalid coupon code. Try REXXZO10 for 10% off.');
    }
  };

  const handleQuantityChange = async (itemId: number, newQty: number, maxStock: number) => {
    if (newQty < 1) return;
    if (newQty > maxStock) {
      setCartError(`Maximum available stock is ${maxStock} unit${maxStock > 1 ? 's' : ''}.`);
      return;
    }
    if (updatingItemId) return; // double-click protection
    setUpdatingItemId(itemId);
    setCartError('');
    try {
      await updateCartItem(itemId, newQty);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setCartError(err.message);
      } else {
        setCartError('Failed to update quantity. Please try again.');
      }
    } finally {
      setUpdatingItemId(null);
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (updatingItemId) return;
    setUpdatingItemId(itemId);
    setCartError('');
    try {
      await removeFromCart(itemId);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setCartError(err.message);
      } else {
        setCartError('Failed to remove item. Please try again.');
      }
    } finally {
      setUpdatingItemId(null);
    }
  };

  const couponDiscount = couponApplied ? Math.round(cart.subtotal * 0.1) : 0;
  const finalTotal = Math.max(0, cart.subtotal + cart.shipping - couponDiscount);

  if (cart.items.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center space-y-6">
        <div className="w-20 h-20 bg-white rounded-full flex items-center justify-center text-[#888888] mx-auto border border-[#e5e1d8] shadow-sm">
          <ShoppingBag className="w-8 h-8" />
        </div>
        <div className="space-y-2">
          <h1 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
            Your Bag is Empty
          </h1>
          <p className="text-xs sm:text-sm text-[#777777] max-w-md mx-auto leading-relaxed">
            Your shopping selection is currently empty. Explore our catalog to find objects designed for daily ritual and spatial calmness.
          </p>
        </div>
        <Link
          href="/shop"
          className="inline-flex items-center gap-2 px-8 py-3.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded hover:bg-[#222222] transition-colors shadow-lg"
        >
          <span>Explore Catalog</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-16">
      <div className="border-b border-[#e5e1d8] pb-6 mb-8">
        <h1 className="text-2xl sm:text-4xl font-extrabold uppercase tracking-tight text-[#080808]">
          Shopping Bag ({cart.items.reduce((s, i) => s + i.quantity, 0)})
        </h1>
      </div>

      {cartError && (
        <div className="mb-6 p-4 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0 text-red-600" />
            <span className="font-medium">{cartError}</span>
          </div>
          <button
            onClick={() => setCartError('')}
            className="text-red-500 hover:text-red-700 font-bold ml-3 text-sm cursor-pointer"
            aria-label="Dismiss error"
          >
            ✕
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        {/* Cart Line Items (2 Cols) */}
        <div className="lg:col-span-2 space-y-4">
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 divide-y divide-[#f0ece5] shadow-sm">
            {cart.items.map((item) => {
              const maxStock = item.stockQuantity || 99;
              const isAtMaxStock = item.quantity >= maxStock;
              const isUpdating = updatingItemId === item.id;

              return (
                <div key={item.id} className="py-6 first:pt-0 last:pb-0 flex flex-col sm:flex-row gap-5 items-start sm:items-center justify-between">
                  
                  <div className="flex gap-4 items-center">
                    <div className="relative w-20 h-24 bg-[#f8f6f2] rounded-md overflow-hidden flex-shrink-0 border border-[#e5e1d8]">
                      <Image
                        src={item.imageUrl || '/images/hero_banner.jpg'}
                        alt={item.productName}
                        fill
                        className="object-cover"
                        sizes="96px"
                      />
                    </div>

                    <div>
                      <Link
                        href={`/products/${item.productSlug || item.productId}`}
                        className="text-sm font-bold text-[#080808] hover:text-[#8c7f69] transition-colors line-clamp-1"
                      >
                        {item.productName}
                      </Link>
                      <p className="text-xs text-[#8c7f69] font-semibold mt-1">
                        ₹{item.price.toLocaleString('en-IN')} each
                      </p>
                      {isAtMaxStock && (
                        <p className="text-[10px] text-amber-700 font-medium mt-1">
                          Max stock reached ({maxStock})
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Modifiers & Subtotal */}
                  <div className="flex items-center justify-between w-full sm:w-auto sm:gap-8">
                    {/* Quantity modifier [-] 1 [+] */}
                    <div className="inline-flex items-center border border-[#d5d0c5] rounded bg-white overflow-hidden shadow-xs">
                      <button
                        onClick={() => handleQuantityChange(item.id, item.quantity - 1, maxStock)}
                        disabled={item.quantity <= 1 || isUpdating}
                        className="px-2.5 py-1.5 text-[#555555] hover:text-[#080808] hover:bg-[#f6f4ee] disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors cursor-pointer"
                        aria-label="Decrease quantity"
                        title={item.quantity <= 1 ? 'Minimum quantity is 1' : 'Decrease quantity'}
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      
                      <span className="px-3 text-xs font-bold text-[#080808] min-w-[32px] text-center select-none">
                        {isUpdating ? (
                          <span className="inline-block animate-pulse text-[#8c7f69]">…</span>
                        ) : (
                          item.quantity
                        )}
                      </span>

                      <button
                        onClick={() => handleQuantityChange(item.id, item.quantity + 1, maxStock)}
                        disabled={isAtMaxStock || isUpdating}
                        className="px-2.5 py-1.5 text-[#555555] hover:text-[#080808] hover:bg-[#f6f4ee] disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors cursor-pointer"
                        aria-label="Increase quantity"
                        title={isAtMaxStock ? `Maximum stock reached (${maxStock})` : 'Increase quantity'}
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    {/* Subtotal */}
                    <div className="text-right min-w-[90px]">
                      <span className="text-sm font-bold text-[#080808] block font-mono">
                        ₹{item.subtotal.toLocaleString('en-IN')}
                      </span>
                    </div>

                    {/* Remove */}
                    <button
                      onClick={() => handleRemoveItem(item.id)}
                      disabled={isUpdating}
                      className="text-[#999999] hover:text-red-500 p-1.5 rounded transition-colors disabled:opacity-30 cursor-pointer"
                      aria-label="Remove item"
                      title="Remove from bag"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                </div>
              );
            })}
          </div>

          <div className="flex justify-between items-center pt-2">
            <Link
              href="/shop"
              className="text-xs font-bold uppercase tracking-wider text-[#080808] hover:text-[#8c7f69] transition-colors"
            >
              ← Continue Shopping
            </Link>
          </div>
        </div>

        {/* Order Summary & Coupon (1 Col) */}
        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-6 shadow-sm">
            <h2 className="text-sm font-bold uppercase tracking-widest text-[#080808] border-b border-[#f0ece5] pb-4">
              Order Summary
            </h2>

            {/* Calculations */}
            <div className="space-y-3 text-xs text-[#555555]">
              <div className="flex justify-between">
                <span>Items Subtotal</span>
                <span className="font-bold text-[#080808] font-mono">₹{cart.subtotal.toLocaleString('en-IN')}</span>
              </div>

              <div className="flex justify-between">
                <span>Estimated Shipping</span>
                <span className="font-medium text-[#080808]">
                  {cart.shipping === 0 ? (
                    <span className="text-emerald-600 font-semibold">Complimentary</span>
                  ) : (
                    `₹${cart.shipping}`
                  )}
                </span>
              </div>

              {couponApplied && (
                <div className="flex justify-between text-emerald-600 font-medium">
                  <span>Coupon Discount (10%)</span>
                  <span>-₹{couponDiscount.toLocaleString('en-IN')}</span>
                </div>
              )}

              <div className="border-t border-[#f0ece5] pt-3 flex justify-between text-sm font-extrabold text-[#080808]">
                <span>Estimated Total</span>
                <span className="text-base text-[#080808] font-mono">₹{finalTotal.toLocaleString('en-IN')}</span>
              </div>
            </div>

            {/* Coupon Code Input */}
            <form onSubmit={handleApplyCoupon} className="pt-2 border-t border-[#f0ece5]">
              <label htmlFor="couponCode" className="block text-[11px] font-bold uppercase tracking-wider text-[#080808] mb-2">
                Promotional Code
              </label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <Tag className="w-3.5 h-3.5 text-[#888888] absolute left-3 top-3 pointer-events-none" />
                  <input
                    id="couponCode"
                    type="text"
                    placeholder="Enter code (e.g. REXXZO10)"
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value)}
                    className="w-full bg-[#f8f6f2] border border-[#d5d0c5] rounded text-xs pl-8 pr-3 py-2 text-[#080808] uppercase placeholder-normal focus:outline-none focus:border-[#080808]"
                  />
                </div>
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-wider rounded hover:bg-[#222222] transition-colors cursor-pointer"
                >
                  Apply
                </button>
              </div>
              {couponError && <p className="text-[11px] text-red-500 mt-1.5">{couponError}</p>}
              {couponApplied && (
                <p className="text-[11px] text-emerald-600 font-medium mt-1.5">
                  Code &quot;{couponCode.toUpperCase()}&quot; applied successfully!
                </p>
              )}
            </form>

            {/* Checkout Action */}
            <div className="pt-2">
              <Link
                href="/checkout"
                className="w-full py-4 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded hover:bg-[#222222] transition-colors flex items-center justify-center gap-2 shadow-lg"
              >
                <span>Proceed to Checkout</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>

            {/* Trust badge */}
            <div className="pt-4 border-t border-[#f0ece5] flex items-center justify-center gap-2 text-[11px] text-[#777777]">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              <span>Secure SSL Encrypted Checkout</span>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
