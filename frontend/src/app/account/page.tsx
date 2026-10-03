'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { User, Package, Heart, LogOut, MapPin } from 'lucide-react';
import { useStore } from '@/context/StoreContext';

export default function AccountPage() {
  const router = useRouter();
  const { user, logout, wishlist } = useStore();
  const [activeTab, setActiveTab] = useState<'profile' | 'addresses'>('profile');

  if (!user) {
    return (
      <div className="max-w-md mx-auto px-4 py-20 text-center space-y-4">
        <User className="w-12 h-12 text-[#888888] mx-auto" />
        <h2 className="text-xl font-bold uppercase text-[#080808]">Sign In Required</h2>
        <p className="text-xs text-[#777777]">
          Please sign in to view your profile and order history.
        </p>
        <Link
          href="/login"
          className="inline-block mt-2 px-6 py-2.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-wider rounded"
        >
          Sign In Now
        </Link>
      </div>
    );
  }

  const handleLogout = () => {
    logout();
    router.push('/');
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-16">
      <div className="border-b border-[#e5e1d8] pb-6 mb-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold uppercase tracking-[0.25em] text-[#8c7f69] block mb-1">
            Member Sanctuary
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
            Welcome, {user.name}
          </h1>
          <p className="text-xs text-[#777777] mt-1">{user.email}</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleLogout}
            className="px-4 py-2 border border-[#d5d0c5] hover:bg-white text-xs font-bold uppercase tracking-wider text-[#080808] rounded flex items-center gap-1.5 transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Sign Out</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
        
        {/* Navigation Sidebar */}
        <aside className="space-y-2">
          <button
            onClick={() => setActiveTab('profile')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg text-xs font-bold uppercase tracking-wider transition-colors ${
              activeTab === 'profile'
                ? 'bg-[#080808] text-[#C8BCA7]'
                : 'bg-white text-[#555555] hover:bg-[#f4f2ee]'
            }`}
          >
            <User className="w-4 h-4" />
            <span>Profile Overview</span>
          </button>

          <Link
            href="/orders"
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-lg text-xs font-bold uppercase tracking-wider bg-white text-[#555555] hover:bg-[#f4f2ee] transition-colors"
          >
            <Package className="w-4 h-4" />
            <span>My Orders</span>
          </Link>

          <Link
            href="/wishlist"
            className="w-full flex items-center justify-between px-4 py-3 rounded-lg text-xs font-bold uppercase tracking-wider bg-white text-[#555555] hover:bg-[#f4f2ee] transition-colors"
          >
            <div className="flex items-center space-x-3">
              <Heart className="w-4 h-4" />
              <span>Wishlist</span>
            </div>
            {wishlist.length > 0 && (
              <span className="bg-[#C8BCA7] text-[#080808] text-[10px] px-2 py-0.5 rounded-full font-bold">
                {wishlist.length}
              </span>
            )}
          </Link>

          <button
            onClick={() => setActiveTab('addresses')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-lg text-xs font-bold uppercase tracking-wider transition-colors ${
              activeTab === 'addresses'
                ? 'bg-[#080808] text-[#C8BCA7]'
                : 'bg-white text-[#555555] hover:bg-[#f4f2ee]'
            }`}
          >
            <MapPin className="w-4 h-4" />
            <span>Saved Addresses</span>
          </button>
        </aside>

        {/* Tab Content */}
        <main className="md:col-span-3">
          {activeTab === 'profile' && (
            <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 sm:p-8 space-y-6 shadow-sm">
              <h2 className="text-sm font-bold uppercase tracking-widest text-[#080808] border-b border-[#f0ece5] pb-3">
                Account Details
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
                <div>
                  <label className="text-[#888888] uppercase tracking-wider block mb-1">Full Name</label>
                  <p className="font-bold text-[#080808] text-sm">{user.name}</p>
                </div>
                <div>
                  <label className="text-[#888888] uppercase tracking-wider block mb-1">Email Address</label>
                  <p className="font-bold text-[#080808] text-sm">{user.email}</p>
                </div>
                <div>
                  <label className="text-[#888888] uppercase tracking-wider block mb-1">Account Role</label>
                  <span className="inline-block bg-[#f0ece5] text-[#080808] font-bold px-2 py-0.5 rounded uppercase text-[10px]">
                    {user.role}
                  </span>
                </div>
                <div>
                  <label className="text-[#888888] uppercase tracking-wider block mb-1">Membership Status</label>
                  <p className="font-semibold text-emerald-700">Active Member</p>
                </div>
              </div>

              <div className="pt-6 border-t border-[#f0ece5] flex gap-4">
                <Link
                  href="/orders"
                  className="px-5 py-2.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-wider rounded"
                >
                  View Order History
                </Link>
              </div>
            </div>
          )}

          {activeTab === 'addresses' && (
            <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 sm:p-8 space-y-6 shadow-sm">
              <h2 className="text-sm font-bold uppercase tracking-widest text-[#080808] border-b border-[#f0ece5] pb-3">
                Saved Shipping Destinations
              </h2>
              <div className="p-4 rounded-lg border border-[#e5e1d8] bg-[#f9f8f6] space-y-1 text-xs">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-bold text-[#080808]">Primary Residence</span>
                  <span className="bg-[#C8BCA7] text-[#080808] text-[9px] font-bold px-2 py-0.5 rounded uppercase">
                    Default
                  </span>
                </div>
                <p className="text-[#555555]">{user.name}</p>
                <p className="text-[#555555]">Flat 402, Lotus Residency, 12th Main Road</p>
                <p className="text-[#555555]">Indiranagar, Bengaluru, Karnataka 560001</p>
                <p className="text-[#555555]">India</p>
              </div>
            </div>
          )}
        </main>

      </div>
    </div>
  );
}
