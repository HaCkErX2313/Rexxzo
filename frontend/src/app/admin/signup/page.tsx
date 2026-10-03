'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ShieldAlert, ShieldCheck, Lock, Mail, User as UserIcon, ArrowRight, AlertCircle, CheckCircle2 } from 'lucide-react';
import { api } from '@/lib/api';

export default function AdminSetupPage() {
  const router = useRouter();
  const [checking, setChecking] = useState(true);
  const [setupRequired, setSetupRequired] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    api.getAdminSetupStatus()
      .then((res) => {
        setSetupRequired(res.setupRequired);
      })
      .catch(() => {
        setSetupRequired(false);
      })
      .finally(() => {
        setChecking(false);
      });
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (password.length < 6) {
      setErrorMessage('Password must be at least 6 characters.');
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await api.adminSetup({
        name: name.trim(),
        email: email.trim(),
        password: password,
      });

      router.push('/admin');
    } catch (err: any) {
      setErrorMessage(err.message || 'Administrator initial setup failed.');
    } finally {
      setLoading(false);
    }
  };

  if (checking) {
    return (
      <div className="min-h-screen bg-[#F7F5F1] text-[#080808] flex items-center justify-center">
        <div className="flex items-center gap-3 text-xs uppercase tracking-widest text-[#8c7f69] font-bold">
          <div className="w-4 h-4 border-2 border-[#8c7f69] border-t-transparent rounded-full animate-spin" />
          <span>Verifying System Setup State...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F7F5F1] text-[#080808] flex flex-col justify-center items-center px-4 sm:px-6 lg:px-8 py-12">
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
            <span>Master Administrator Provisioning</span>
          </div>
        </div>

        {/* LOCKED STATE: When Setup is already completed */}
        {!setupRequired ? (
          <div className="bg-white rounded-2xl border border-[#E5E1D8] shadow-sm p-8 text-center space-y-5">
            <div className="w-12 h-12 rounded-full bg-emerald-50 text-emerald-700 mx-auto flex items-center justify-center">
              <CheckCircle2 className="w-6 h-6" />
            </div>

            <div className="space-y-2">
              <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                Admin Setup Already Completed
              </h2>
              <p className="text-xs text-[#666666] leading-relaxed max-w-sm mx-auto">
                The master administrator account for this REXXZO deployment is already provisioned. Public registration is locked to protect platform security.
              </p>
            </div>

            <div className="pt-2">
              <Link
                href="/admin/login"
                className="w-full py-3 px-4 bg-[#080808] text-[#C8BCA7] hover:bg-[#222222] rounded-lg font-bold text-xs uppercase tracking-widest transition-colors flex items-center justify-center gap-2 shadow-sm"
              >
                <span>Proceed to Admin Login</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            <p className="text-[11px] text-[#888888] pt-2">
              Need additional administrators? Existing administrators can create new sub-accounts from the authenticated admin console.
            </p>
          </div>
        ) : (
          /* ACTIVE INITIAL SETUP FORM */
          <div className="bg-white rounded-2xl border border-[#E5E1D8] shadow-sm p-6 sm:p-8 space-y-6">
            <div className="space-y-1">
              <h2 className="text-sm font-extrabold uppercase tracking-tight text-[#080808]">
                Initial Administrator Account
              </h2>
              <p className="text-xs text-[#666666]">
                Configure the primary administrative credentials. Once saved, this setup endpoint will lock permanently.
              </p>
            </div>

            {errorMessage && (
              <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-900 text-xs flex items-start gap-2.5">
                <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Full Name */}
              <div className="space-y-1.5">
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#333333]">
                  Administrator Full Name
                </label>
                <div className="relative">
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Master Administrator"
                    className="w-full pl-10 pr-4 py-2.5 bg-[#FAF9F7] border border-[#DDD8CF] rounded-lg text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#080808] focus:bg-white"
                  />
                  <UserIcon className="w-4 h-4 text-[#888888] absolute left-3.5 top-3" />
                </div>
              </div>

              {/* Email */}
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
                    className="w-full pl-10 pr-4 py-2.5 bg-[#FAF9F7] border border-[#DDD8CF] rounded-lg text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#080808] focus:bg-white"
                  />
                  <Mail className="w-4 h-4 text-[#888888] absolute left-3.5 top-3" />
                </div>
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#333333]">
                  Master Password
                </label>
                <div className="relative">
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••••••"
                    className="w-full pl-10 pr-4 py-2.5 bg-[#FAF9F7] border border-[#DDD8CF] rounded-lg text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#080808] focus:bg-white"
                  />
                  <Lock className="w-4 h-4 text-[#888888] absolute left-3.5 top-3" />
                </div>
                <span className="text-[10px] text-[#888888] block">Minimum 6 characters. Will be BCrypt hashed.</span>
              </div>

              {/* Confirm Password */}
              <div className="space-y-1.5">
                <label className="block text-[11px] font-bold uppercase tracking-wider text-[#333333]">
                  Confirm Password
                </label>
                <div className="relative">
                  <input
                    type="password"
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="••••••••••••"
                    className="w-full pl-10 pr-4 py-2.5 bg-[#FAF9F7] border border-[#DDD8CF] rounded-lg text-xs text-[#080808] placeholder-[#999999] focus:outline-none focus:border-[#080808] focus:bg-white"
                  />
                  <Lock className="w-4 h-4 text-[#888888] absolute left-3.5 top-3" />
                </div>
              </div>

              {/* Submit */}
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 px-4 bg-[#080808] text-[#C8BCA7] hover:bg-[#222222] disabled:opacity-50 disabled:cursor-not-allowed rounded-lg font-bold text-xs uppercase tracking-widest transition-colors flex items-center justify-center gap-2 shadow-sm cursor-pointer mt-2"
              >
                {loading ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-[#C8BCA7] border-t-transparent rounded-full animate-spin" />
                    <span>Provisioning Master Admin...</span>
                  </>
                ) : (
                  <>
                    <span>Initialize Administrator</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </>
                )}
              </button>
            </form>
          </div>
        )}

        <div className="text-center">
          <Link
            href="/admin/login"
            className="text-xs text-[#666666] hover:text-[#080808] transition-colors inline-flex items-center gap-1 font-medium"
          >
            ← Return to Admin Login
          </Link>
        </div>

      </div>
    </div>
  );
}
