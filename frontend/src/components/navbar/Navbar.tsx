'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ShoppingBag, Heart, User as UserIcon, Search, Menu, X } from 'lucide-react';
import { useStore } from '@/context/StoreContext';

export default function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { cart, wishlist, user, setIsCartOpen } = useStore();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);

  // Completely separate admin console by not rendering customer navbar on /admin routes
  if (pathname?.startsWith('/admin')) {
    return null;
  }

  const totalCartCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/shop?q=${encodeURIComponent(searchQuery.trim())}`);
      setSearchOpen(false);
      setMobileMenuOpen(false);
    }
  };

  const [catDropdownOpen, setCatDropdownOpen] = useState(false);

  const navLinks = [
    { name: 'Home', href: '/' },
    { name: 'Shop', href: '/shop' },
    { name: 'Categories', href: '/categories', hasDropdown: true },
    { name: 'Gift Corner', href: '/shop/gift-corner', badge: '🎁' },
    { name: 'About Us', href: '/about' },
    { name: 'Contact', href: '/contact' },
  ];

  const categoryDropdownItems = [
    { name: 'Home Decor', href: '/shop?category=home-decor' },
    { name: 'Kitchen & Dining', href: '/shop?category=kitchen-dining' },
    { name: 'Storage & Organizers', href: '/shop?category=storage-organizers' },
    { name: 'Lighting', href: '/shop?category=lighting' },
    { name: 'Lifestyle', href: '/shop?category=lifestyle' },
    { name: 'Accessories', href: '/shop?category=accessories' },
    { name: '🎁 Gift Corner', href: '/shop/gift-corner', highlight: true },
    { name: 'All Categories →', href: '/categories', all: true },
  ];

  return (
    <>
      {/* Announcement Bar */}
      <div className="bg-[#080808] text-[#C8BCA7] text-[11px] tracking-widest uppercase font-medium py-2 px-4 text-center border-b border-[#222222]">
        Complimentary express delivery on orders over ₹2,000 | Designed for better living
      </div>

      {/* Main Navbar */}
      <header className="sticky top-0 z-40 bg-[#080808]/95 backdrop-blur-md border-b border-[#202020] transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between gap-4">
          
          {/* Mobile Menu Button */}
          <div className="flex items-center lg:hidden">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 text-[#C8BCA7] hover:text-white focus:outline-none"
              aria-label="Toggle Navigation Menu"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>

          {/* Logo & Tagline */}
          <Link href="/" className="flex flex-col items-start group">
            <span className="text-2xl sm:text-3xl font-extrabold tracking-[0.18em] text-white uppercase group-hover:text-[#C8BCA7] transition-colors">
              REXXZO
            </span>
            <span className="text-[9px] tracking-[0.35em] text-[#C8BCA7] font-semibold uppercase mt-0.5">
              BETTER EVERYDAY
            </span>
          </Link>

          {/* Desktop Navigation Links */}
          <nav className="hidden lg:flex items-center space-x-7">
            {navLinks.map((link) => {
              const isActive = pathname === link.href;

              if (link.hasDropdown) {
                return (
                  <div
                    key={link.name}
                    className="relative group py-2"
                    onMouseEnter={() => setCatDropdownOpen(true)}
                    onMouseLeave={() => setCatDropdownOpen(false)}
                  >
                    <Link
                      href={link.href}
                      className={`text-sm tracking-wider uppercase transition-colors relative py-1 flex items-center gap-1 ${
                        isActive ? 'text-[#C8BCA7] font-semibold' : 'text-[#e5e5e5] hover:text-[#C8BCA7]'
                      }`}
                    >
                      <span>{link.name}</span>
                      <span className="text-[10px] text-[#888888] transition-transform group-hover:rotate-180">▾</span>
                      {isActive && (
                        <span className="absolute bottom-0 left-0 right-0 h-[2px] bg-[#C8BCA7] rounded-full" />
                      )}
                    </Link>

                    {/* Category Dropdown */}
                    <div
                      className={`absolute top-full left-0 w-56 bg-[#111111] border border-[#2b2b2b] rounded-xl shadow-2xl py-2 transition-all duration-200 z-50 ${
                        catDropdownOpen ? 'opacity-100 visible translate-y-0' : 'opacity-0 invisible -translate-y-2'
                      }`}
                    >
                      {categoryDropdownItems.map((item) => (
                        <Link
                          key={item.name}
                          href={item.href}
                          className={`block px-4 py-2 text-xs tracking-wider uppercase transition-colors ${
                            item.highlight
                              ? 'text-[#C8BCA7] font-bold bg-[#1a1a1a] hover:bg-[#252525]'
                              : item.all
                              ? 'text-white font-semibold border-t border-[#222222] mt-1 pt-2 hover:text-[#C8BCA7]'
                              : 'text-[#cccccc] hover:text-white hover:bg-[#1a1a1a]'
                          }`}
                        >
                          {item.name}
                        </Link>
                      ))}
                    </div>
                  </div>
                );
              }

              return (
                <Link
                  key={link.name}
                  href={link.href}
                  className={`text-sm tracking-wider uppercase transition-colors relative py-1 flex items-center gap-1.5 ${
                    isActive ? 'text-[#C8BCA7] font-semibold' : 'text-[#e5e5e5] hover:text-[#C8BCA7]'
                  }`}
                >
                  {link.badge && <span>{link.badge}</span>}
                  <span>{link.name}</span>
                  {isActive && (
                    <span className="absolute bottom-0 left-0 right-0 h-[2px] bg-[#C8BCA7] rounded-full" />
                  )}
                </Link>
              );
            })}
          </nav>

          {/* Action Icons */}
          <div className="flex items-center space-x-2 sm:space-x-4">
            {/* Search Input (Desktop) */}
            <form onSubmit={handleSearchSubmit} className="hidden md:flex items-center relative">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products..."
                className="bg-[#181818] border border-[#303030] text-xs text-white placeholder-[#888888] rounded-full pl-9 pr-4 py-2 w-48 focus:w-64 focus:outline-none focus:border-[#C8BCA7] transition-all"
              />
              <Search className="w-4 h-4 text-[#888888] absolute left-3 pointer-events-none" />
            </form>

            {/* Mobile Search Icon Toggle */}
            <button
              onClick={() => setSearchOpen(!searchOpen)}
              className="md:hidden p-2 text-[#C8BCA7] hover:text-white"
              aria-label="Search"
            >
              <Search className="w-5 h-5" />
            </button>

            {/* Wishlist Icon */}
            <Link
              href="/wishlist"
              className="p-2 text-[#C8BCA7] hover:text-white relative transition-colors"
              aria-label="Wishlist"
            >
              <Heart className="w-5 h-5" />
              {wishlist.length > 0 && (
                <span className="absolute top-1 right-1 bg-[#C8BCA7] text-[#080808] text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
                  {wishlist.length}
                </span>
              )}
            </Link>

            <Link
              href={user ? '/account' : '/login'}
              className="p-2 text-[#C8BCA7] hover:text-white transition-colors"
              aria-label="My Account"
            >
              <UserIcon className="w-5 h-5" />
            </Link>

            {/* Cart Icon */}
            <button
              onClick={() => setIsCartOpen(true)}
              className="p-2 text-[#C8BCA7] hover:text-white relative transition-colors cursor-pointer"
              aria-label="Shopping Cart"
            >
              <ShoppingBag className="w-5 h-5" />
              {totalCartCount > 0 && (
                <span className="absolute top-1 right-1 bg-[#C8BCA7] text-[#080808] text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
                  {totalCartCount}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Mobile Search Bar Dropdown */}
        {searchOpen && (
          <div className="md:hidden px-4 pb-4 pt-1 bg-[#080808] border-b border-[#222222]">
            <form onSubmit={handleSearchSubmit} className="relative">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products..."
                autoFocus
                className="w-full bg-[#181818] border border-[#303030] text-sm text-white placeholder-[#888888] rounded-full pl-10 pr-4 py-2.5 focus:outline-none focus:border-[#C8BCA7]"
              />
              <Search className="w-4 h-4 text-[#888888] absolute left-3.5 top-3 pointer-events-none" />
            </form>
          </div>
        )}

        {/* Mobile Navigation Drawer */}
        {mobileMenuOpen && (
          <div className="lg:hidden bg-[#080808] border-b border-[#222222] px-6 py-6 space-y-4">
            <nav className="flex flex-col space-y-3">
              {navLinks.map((link) => (
                <Link
                  key={link.name}
                  href={link.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`text-base tracking-wider uppercase py-2 border-b border-[#1c1c1c] ${
                    pathname === link.href ? 'text-[#C8BCA7] font-bold' : 'text-white'
                  }`}
                >
                  {link.badge && <span className="mr-1.5">{link.badge}</span>}
                  <span>{link.name}</span>
                </Link>
              ))}
            </nav>
            <div className="pt-2 flex flex-col space-y-2">
              {user ? (
                <Link
                  href="/account"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-sm text-[#C8BCA7] flex items-center space-x-2"
                >
                  <UserIcon className="w-4 h-4" />
                  <span>Hello, {user.name}</span>
                </Link>
              ) : (
                <Link
                  href="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-sm text-[#C8BCA7] underline uppercase tracking-wider"
                >
                  Sign In / Register
                </Link>
              )}
            </div>
          </div>
        )}
      </header>
    </>
  );
}
