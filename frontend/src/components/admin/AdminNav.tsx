'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  LayoutDashboard,
  ShoppingBag,
  Package,
  Tag,
  Layers,
  RotateCcw,
  CreditCard,
  Star,
  BarChart3,
  Truck,
  Sliders,
  Settings,
  FileText,
  LogOut,
  ExternalLink,
  ShieldCheck,
  User as UserIcon,
} from 'lucide-react';
import { api } from '@/lib/api';

interface AdminNavProps {
  currentTab?: string;
}

export default function AdminNav({ currentTab }: AdminNavProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [adminUser, setAdminUser] = useState<{ email: string; name: string; role: string } | null>(null);

  useEffect(() => {
    const user = api.getAdminUser();
    setAdminUser(user);
  }, []);

  const handleLogout = () => {
    api.adminLogout();
    router.push('/admin/login');
  };

  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, href: '/admin' },
    { id: 'products', label: 'Products', icon: Package, href: '/admin/products' },
    { id: 'categories', label: 'Categories', icon: Tag, href: '/admin/categories' },
    { id: 'inventory', label: 'Inventory', icon: Layers, href: '/admin/inventory' },
    { id: 'orders', label: 'Orders', icon: ShoppingBag, href: '/admin/orders' },
    { id: 'returns', label: 'Returns', icon: RotateCcw, href: '/admin/returns' },
    { id: 'refunds', label: 'Refunds', icon: CreditCard, href: '/admin/refunds' },
    { id: 'reviews', label: 'Reviews', icon: Star, href: '/admin/reviews' },
    { id: 'analytics', label: 'Analytics', icon: BarChart3, href: '/admin/analytics' },
    { id: 'shipments', label: 'Shipments', icon: Truck, href: '/admin/shipments' },
    { id: 'integrations', label: 'Integrations', icon: Sliders, href: '/admin/integrations' },
    { id: 'settings', label: 'Settings', icon: Settings, href: '/admin/settings' },
    { id: 'audit', label: 'Audit Logs', icon: FileText, href: '/admin/audit-logs' },
  ];

  return (
    <header className="bg-white border-b border-[#e5e1d8] sticky top-0 z-40 shadow-xs">
      {/* Top Banner */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/admin" className="flex items-center gap-2">
            <span className="text-xl font-black tracking-tighter text-[#080808] font-serif">REXXZO</span>
            <span className="text-[10px] font-bold uppercase tracking-widest bg-[#080808] text-[#C8BCA7] px-2 py-0.5 rounded">
              CONTROL
            </span>
          </Link>
          <div className="hidden sm:flex items-center gap-1.5 text-[11px] text-[#777777] border-l border-[#e5e1d8] pl-3">
            <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>Live PostgreSQL</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Link
            href="/shop"
            target="_blank"
            rel="noopener noreferrer"
            className="hidden md:flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wider text-[#777777] hover:text-[#080808] transition-colors py-1.5 px-2.5 rounded-lg border border-[#e5e1d8] hover:border-[#080808]"
          >
            <span>Live Storefront</span>
            <ExternalLink className="w-3 h-3" />
          </Link>

          {adminUser && (
            <div className="flex items-center gap-2 text-xs border-l border-[#e5e1d8] pl-3">
              <div className="w-7 h-7 rounded-full bg-[#f4f1ea] border border-[#e5e1d8] flex items-center justify-center font-bold text-[11px] text-[#080808]">
                {adminUser.name?.charAt(0) || 'A'}
              </div>
              <div className="hidden lg:block text-left">
                <div className="text-[11px] font-bold text-[#080808] leading-tight">{adminUser.name}</div>
                <div className="text-[10px] text-[#777777] leading-tight flex items-center gap-1">
                  <ShieldCheck className="w-2.5 h-2.5 text-emerald-600" />
                  {adminUser.role}
                </div>
              </div>
            </div>
          )}

          <button
            onClick={handleLogout}
            title="Sign out of Admin Dashboard"
            className="flex items-center gap-1.5 text-xs text-rose-600 hover:text-rose-700 font-bold uppercase tracking-wider px-2.5 py-1.5 rounded-lg hover:bg-rose-50 border border-transparent hover:border-rose-200 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Logout</span>
          </button>
        </div>
      </div>

      {/* Navigation Tabs Bar */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 overflow-x-auto flex space-x-1 border-t border-[#f0ece5] text-xs scrollbar-none">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isProductsRoute = item.id === 'products' && (pathname.startsWith('/admin/products') || currentTab === 'products');
          const isDashboardRoute = item.id === 'dashboard' && pathname === '/admin' && (!currentTab || currentTab === 'dashboard');
          const isPathMatch = pathname === item.href || (item.id === 'audit' && (pathname === '/admin/audit-logs' || pathname === '/admin/audit'));
          const isOtherTab = currentTab === item.id;
          const isActive = isProductsRoute || isDashboardRoute || isPathMatch || isOtherTab;

          return (
            <Link
              key={item.id}
              href={item.href}
              className={`flex items-center gap-2 py-3 px-3.5 border-b-2 font-bold uppercase tracking-wider text-[11px] whitespace-nowrap transition-colors ${
                isActive
                  ? 'border-[#080808] text-[#080808] bg-[#fcfbfa]'
                  : 'border-transparent text-[#777777] hover:text-[#080808] hover:bg-[#faf9f7]'
              }`}
            >
              <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-[#080808]' : 'text-[#888888]'}`} />
              {item.label}
              {item.id === 'products' && (
                <span className="ml-0.5 text-[9px] px-1.5 py-0.2 bg-[#080808] text-[#C8BCA7] rounded font-mono">
                  CATALOG
                </span>
              )}
            </Link>
          );
        })}
      </div>
    </header>
  );
}
