'use client';

import React, { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { api } from '@/lib/api';

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const [isAuthorized, setIsAuthorized] = useState<boolean>(false);
  const [isChecking, setIsChecking] = useState<boolean>(true);

  useEffect(() => {
    // 1. Allow public access to standalone admin auth pages
    if (pathname === '/admin/login' || pathname === '/admin/signup') {
      const token = typeof window !== 'undefined' ? localStorage.getItem('rexxo_admin_token') : null;
      const adminUser = api.getAdminUser();

      // If already logged in as ADMIN and visiting login, go directly to admin console
      if (token && adminUser?.role === 'ADMIN' && pathname === '/admin/login') {
        router.replace('/admin');
        return;
      }

      setIsAuthorized(true);
      setIsChecking(false);
      return;
    }

    // 2. Strict Route Guard for all other /admin/* routes
    if (typeof window === 'undefined') return;

    const adminToken = localStorage.getItem('rexxo_admin_token');
    const adminUser = api.getAdminUser();
    const customerToken = localStorage.getItem('rexxo_token');
    const customerUser = api.getCurrentUser();

    // If no admin token or role is not ADMIN
    if (!adminToken || adminUser?.role !== 'ADMIN') {
      setIsAuthorized(false);
      setIsChecking(false);

      // If user is a logged-in normal customer attempting to access /admin, redirect to storefront home
      if (customerToken || customerUser) {
        router.replace('/');
      } else {
        // If unauthenticated, redirect to admin login
        router.replace('/admin/login');
      }
      return;
    }

    // Authorized Admin
    setIsAuthorized(true);
    setIsChecking(false);
  }, [pathname, router]);

  // While verifying authorization, render a neutral loading screen (zero admin console leak)
  if (isChecking || !isAuthorized) {
    // For login and signup, don't show the loading screen if authorized
    if (pathname === '/admin/login' || pathname === '/admin/signup') {
      return <>{children}</>;
    }

    return (
      <div className="min-h-screen bg-[#F7F5F1] flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="w-8 h-8 border-2 border-[#080808] border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-xs uppercase tracking-widest text-[#777777] font-semibold">
            Authenticating Console Access...
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
