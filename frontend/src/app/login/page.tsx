'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowRight, Lock, Mail, ShieldAlert } from 'lucide-react';
import { api } from '@/lib/api';
import { useStore } from '@/context/StoreContext';

export default function LoginPage() {
  const router = useRouter();
  const { login } = useStore();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const data = await api.login(email, password);
      login(data.token, data.user);
      if (data.user.role === 'ADMIN') {
        router.push('/admin');
      } else {
        router.push('/account');
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Invalid email or password.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto px-4 py-16 sm:py-24">
      <div className="bg-white rounded-2xl border border-[#e5e1d8] p-8 shadow-xl space-y-6">
        <div className="text-center space-y-2">
          <span className="text-[10px] font-bold uppercase tracking-[0.3em] text-[#8c7f69]">
            Member Access
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
            Sign In to REXXZO
          </h1>
          <p className="text-xs text-[#777777]">
            Access your order history, curations, and bespoke member privileges.
          </p>
        </div>

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-1.5">
              Email Address
            </label>
            <div className="relative">
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@example.com"
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2.5 pl-9 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              />
              <Mail className="w-4 h-4 text-[#888888] absolute left-3 top-3" />
            </div>
          </div>

          <div>
            <div className="flex justify-between items-center mb-1.5">
              <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555]">
                Password
              </label>
              <a href="#" className="text-[10px] text-[#8c7f69] hover:underline">
                Forgot password?
              </a>
            </div>
            <div className="relative">
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2.5 pl-9 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              />
              <Lock className="w-4 h-4 text-[#888888] absolute left-3 top-3" />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 bg-[#080808] hover:bg-[#222222] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded shadow-md flex items-center justify-center gap-2 transition-all cursor-pointer disabled:opacity-50"
          >
            <span>{loading ? 'Authenticating...' : 'Sign In'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="text-center pt-2 border-t border-[#f0ece5]">
          <p className="text-xs text-[#777777]">
            Don&apos;t have an account yet?{' '}
            <Link href="/register" className="font-bold text-[#080808] hover:underline">
              Create an account
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
