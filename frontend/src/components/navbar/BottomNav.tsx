'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Home, Compass, Heart, ShoppingBag, User } from 'lucide-react';
import { useStore } from '@/context/StoreContext';

export default function BottomNav() {
  const pathname = usePathname();
  const { cart, wishlist, setIsCartOpen, user } = useStore();
  const totalCartCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);

  // Hide on admin routes to allow full screen admin dashboard
  if (pathname.startsWith('/admin')) {
    return null;
  }

  return (
    <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#080808]/95 backdrop-blur-lg border-t border-[#222222] px-3 py-2 flex items-center justify-around shadow-2xl">
      <Link
        href="/"
        className={`flex flex-col items-center justify-center p-1.5 transition-colors ${
          pathname === '/' ? 'text-[#C8BCA7]' : 'text-[#888888]'
        }`}
      >
        <Home className="w-5 h-5" />
        <span className="text-[10px] uppercase tracking-wider mt-1 font-medium">Home</span>
      </Link>

      <Link
        href="/shop"
        className={`flex flex-col items-center justify-center p-1.5 transition-colors ${
          pathname === '/shop' ? 'text-[#C8BCA7]' : 'text-[#888888]'
        }`}
      >
        <Compass className="w-5 h-5" />
        <span className="text-[10px] uppercase tracking-wider mt-1 font-medium">Shop</span>
      </Link>

      <Link
        href="/wishlist"
        className={`flex flex-col items-center justify-center p-1.5 transition-colors relative ${
          pathname === '/wishlist' ? 'text-[#C8BCA7]' : 'text-[#888888]'
        }`}
      >
        <Heart className="w-5 h-5" />
        {wishlist.length > 0 && (
          <span className="absolute top-0 right-2 bg-[#C8BCA7] text-[#080808] text-[9px] font-bold w-3.5 h-3.5 rounded-full flex items-center justify-center">
            {wishlist.length}
          </span>
        )}
        <span className="text-[10px] uppercase tracking-wider mt-1 font-medium">Wishlist</span>
      </Link>

      <button
        onClick={() => setIsCartOpen(true)}
        className="flex flex-col items-center justify-center p-1.5 transition-colors relative text-[#888888] hover:text-[#C8BCA7] cursor-pointer"
      >
        <ShoppingBag className="w-5 h-5" />
        {totalCartCount > 0 && (
          <span className="absolute top-0 right-2 bg-[#C8BCA7] text-[#080808] text-[9px] font-bold w-3.5 h-3.5 rounded-full flex items-center justify-center">
            {totalCartCount}
          </span>
        )}
        <span className="text-[10px] uppercase tracking-wider mt-1 font-medium">Cart</span>
      </button>

      <Link
        href={user ? '/account' : '/login'}
        className={`flex flex-col items-center justify-center p-1.5 transition-colors ${
          pathname === '/account' || pathname === '/login' ? 'text-[#C8BCA7]' : 'text-[#888888]'
        }`}
      >
        <User className="w-5 h-5" />
        <span className="text-[10px] uppercase tracking-wider mt-1 font-medium">Account</span>
      </Link>
    </div>
  );
}
