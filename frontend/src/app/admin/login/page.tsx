'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ShieldCheck, Eye, EyeOff, Lock, Mail, ArrowRight, AlertCircle, Sparkles } from 'lucide-react';
import { api } from '@/lib/api';

export default function AdminLoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [setupRequired, setSetupRequired] = useState<boolean | null>(null);

  useEffect(() => {
    // Check if initial admin setup is required
    api.getAdminSetupStatus()
      .then((status) => {
        setSetupRequired(status.setupRequired);
      })
      .catch(() => {
        setSetupRequired(false);
      });
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setLoading(true);

    try {
      await api.adminLogin({
        email: email.trim(),
        password: password,
      });

      // Successful login
      router.push('/admin');
    } catch (err: any) {
      setErrorMessage(err.message || 'You entered an incorrect email or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F5F1] text-[#080808] flex flex-col justify-center items-center px-4 sm:px-6 lg:px-8 py-12">
      {/* Container */}
      <div className="w-full max-w-md space-y-8">
        
        {/* Brand Header */}
        <div className="text-center space-y-3">
          <Link href="/" className="inline-block group">
            <span className="text-2xl sm:text-3xl font-black tracking-[0.25em] uppercase text-[#080808] group-hover:text-[#8c7f69] transition-colors">
              REXXZO
            </span>
            <span className="block text-[10px] tracking-[0.35em] text-[#8c7f69] font-bold uppercase mt-0.5">
              OPERATIONAL CONSOLE
            </span>
          </Link>
          
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#EFECE6] border border-[#DDD8CF] text-[11px] font-bold uppercase tracking-wider text-[#555047]">
            <ShieldCheck className="w-3.5 h-3.5 text-[#8c7f69]" />
            <span>Admin Portal</span>
          </div>

          <p className="text-xs text-[#666666] max-w-xs mx-auto">
            Authorized administrator access for catalog operations, orders, logistics, and store settings.
          </p>
        </div>

        {/* Setup Banner (Only shown if initial setup is pending) */}
        {setupRequired && (
          <div className="bg-[#FAF8F5] border border-[#C8BCA7] rounded-xl p-4 text-xs space-y-2 shadow-sm">
            <div className="flex items-center gap-2 font-bold text-[#080808]">
              <Sparkles className="w-4 h-4 text-[#8c7f69]" />
              <span>Initial Setup Required</span>
            </div>
            <p className="text-[#666666] text-[11px] leading-relaxed">
              No administrator account has been provisioned yet. Set up your master credentials now.
            </p>
            <Link
              href="/admin/signup"
              className="inline-flex items-center gap-1.5 text-[#080808] font-bold underline hover:text-[#8c7f69] text-xs pt-1"
            >
              Configure Master Administrator <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        )}

        {/* Login Card */}
        <div className="bg-white rounded-2xl border border-[#E5E1D8] shadow-sm p-6 sm:p-8 space-y-6">
          
          {/* Error Alert */}
          {errorMessage && (
            <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-900 text-xs flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Email Field */}
            <div className="space-y-1.5">
              <label className="block text-[11px] font-bold uppercase tracking-wider text-[#333333]">
                Admin Email Address
              </label>
              <div className="relative">
                <input
                  type="email"
                  required
                  autoComplete="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@rexxzo.in"
                  className="w-full pl-10 pr-4 py-2.5 bg-[#FAF9F7] border border-[#DDD8CF] rounded-lg text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#080808] focus:bg-white transition-colors"
                />
                <Mail className="w-4 h-4 text-[#888888] absolute left-3.5 top-3" />
              </div>
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#333333]">
                  Admin Password
                </label>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-10 pr-10 py-2.5 bg-[#FAF9F7] border border-[#DDD8CF] rounded-lg text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#080808] focus:bg-white transition-colors"
                />
                <Lock className="w-4 h-4 text-[#888888] absolute left-3.5 top-3" />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3 text-[#888888] hover:text-[#080808] transition-colors"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 bg-[#080808] text-[#C8BCA7] hover:bg-[#222222] disabled:opacity-50 disabled:cursor-not-allowed rounded-lg font-bold text-xs uppercase tracking-widest transition-colors flex items-center justify-center gap-2 shadow-sm cursor-pointer"
            >
              {loading ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-[#C8BCA7] border-t-transparent rounded-full animate-spin" />
                  <span>Verifying Credentials...</span>
                </>
              ) : (
                <>
                  <span>Authenticate Session</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </form>

          {/* Footer note */}
          <div className="pt-4 border-t border-[#F0ECE5] text-center">
            <span className="text-[11px] text-[#888888]">
              Restricted console. Sessions are cryptographically signed and audited.
            </span>
          </div>
        </div>

        {/* Back to Storefront Link */}
        <div className="text-center">
          <Link
            href="/"
            className="text-xs text-[#666666] hover:text-[#080808] transition-colors inline-flex items-center gap-1 font-medium"
          >
            ← Return to REXXZO Storefront
          </Link>
        </div>

      </div>
    </div>
  );
}
