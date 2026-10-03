'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowRight, Lock, Mail, User as UserIcon, Phone, ShieldAlert, CheckCircle2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useStore } from '@/context/StoreContext';

export default function RegisterPage() {
  const router = useRouter();
  const { login } = useStore();

  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  });

  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    if (!formData.name.trim() || formData.name.trim().length < 2) {
      setError('Please enter your full name (at least 2 characters).');
      return;
    }

    if (!formData.email.trim()) {
      setError('Please enter a valid email address.');
      return;
    }

    const cleanPhone = formData.phone.replace(/[^0-9]/g, '');
    if (cleanPhone.length < 10) {
      setError('Please enter a valid 10-digit Indian mobile number.');
      return;
    }

    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);

    try {
      const data = await api.register(
        formData.name.trim(),
        formData.email.trim(),
        formData.password,
        formData.phone.trim()
      );

      setSuccessMessage('Registration successful! Welcome to REXXZO. Redirecting to your account...');
      login(data.token, data.user);

      // Redirect after showing success confirmation
      setTimeout(() => {
        router.push('/account');
      }, 1200);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to create account. Please check your information and try again.');
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
            Join REXXZO
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold uppercase tracking-tight text-[#080808]">
            Create Account
          </h1>
          <p className="text-xs text-[#777777]">
            Enjoy curated private drops, express checkout, and seamless order tracking.
          </p>
        </div>

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {successMessage && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center gap-3">
            <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
            <div>
              <p className="font-bold">{successMessage}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleRegister} className="space-y-4">
          <div>
            <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-1.5">
              Full Name *
            </label>
            <div className="relative">
              <input
                type="text"
                name="name"
                required
                value={formData.name}
                onChange={handleChange}
                placeholder="Arjun Mehta"
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2.5 pl-9 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              />
              <UserIcon className="w-4 h-4 text-[#888888] absolute left-3 top-3" />
            </div>
          </div>

          <div>
            <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-1.5">
              Email Address *
            </label>
            <div className="relative">
              <input
                type="email"
                name="email"
                required
                value={formData.email}
                onChange={handleChange}
                placeholder="name@example.com"
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2.5 pl-9 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              />
              <Mail className="w-4 h-4 text-[#888888] absolute left-3 top-3" />
            </div>
          </div>

          <div>
            <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-1.5">
              Mobile Number *
            </label>
            <div className="relative">
              <input
                type="tel"
                name="phone"
                required
                value={formData.phone}
                onChange={handleChange}
                placeholder="+91 98765 43210"
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2.5 pl-9 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              />
              <Phone className="w-4 h-4 text-[#888888] absolute left-3 top-3" />
            </div>
            <span className="text-[10px] text-[#999999] mt-1 block">10-digit Indian mobile number</span>
          </div>

          <div>
            <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-1.5">
              Password *
            </label>
            <div className="relative">
              <input
                type="password"
                name="password"
                required
                value={formData.password}
                onChange={handleChange}
                placeholder="••••••••"
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2.5 pl-9 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              />
              <Lock className="w-4 h-4 text-[#888888] absolute left-3 top-3" />
            </div>
            <span className="text-[10px] text-[#999999] mt-1 block">Must be at least 8 characters</span>
          </div>

          <div>
            <label className="text-[11px] font-bold uppercase tracking-wider text-[#555555] block mb-1.5">
              Confirm Password *
            </label>
            <div className="relative">
              <input
                type="password"
                name="confirmPassword"
                required
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="••••••••"
                className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded px-3 py-2.5 pl-9 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
              />
              <Lock className="w-4 h-4 text-[#888888] absolute left-3 top-3" />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading || !!successMessage}
            className="w-full py-3.5 bg-[#080808] hover:bg-[#222222] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded shadow-md flex items-center justify-center gap-2 transition-all cursor-pointer disabled:opacity-50"
          >
            <span>{loading ? 'Creating Account...' : 'Create Account'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="text-center pt-2 border-t border-[#f0ece5]">
          <p className="text-xs text-[#777777]">
            Already have an account?{' '}
            <Link href="/login" className="font-bold text-[#080808] hover:underline">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
