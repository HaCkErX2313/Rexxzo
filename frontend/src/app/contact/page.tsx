'use client';

import React, { useState } from 'react';
import { Mail, Phone, MapPin, Send, Check } from 'lucide-react';

export default function ContactPage() {
  const [submitted, setSubmitted] = useState(false);
  const [formData, setFormData] = useState({ name: '', email: '', subject: '', message: '' });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setFormData({ name: '', email: '', subject: '', message: '' });
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <div className="text-center max-w-2xl mx-auto mb-12 sm:mb-16 space-y-2">
        <span className="text-[10px] font-bold uppercase tracking-[0.3em] text-[#8c7f69]">
          Client Concierge
        </span>
        <h1 className="text-3xl sm:text-4xl font-extrabold uppercase tracking-tight text-[#080808]">
          Connect With REXXZO
        </h1>
        <p className="text-xs sm:text-sm text-[#777777]">
          We are here to assist with product inquiries, spatial curation, or order status.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        {/* Info Column */}
        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4 shadow-sm">
            <h2 className="text-xs font-bold uppercase tracking-widest text-[#080808] border-b border-[#f0ece5] pb-3">
              Studio & Offices
            </h2>
            <div className="space-y-4 text-xs text-[#555555]">
              <div className="flex items-start gap-3">
                <MapPin className="w-4 h-4 text-[#8c7f69] flex-shrink-0 mt-0.5" />
                <div>
                  <strong className="text-[#080808] block mb-0.5">Design Atelier:</strong>
                  <span>REXXZO Living Spaces, 100 Feet Road, Indiranagar, Bengaluru 560038, India</span>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Mail className="w-4 h-4 text-[#8c7f69] flex-shrink-0" />
                <div>
                  <strong className="text-[#080808] block mb-0.5">Email Inquiries:</strong>
                  <span className="font-mono">concierge@rexxzo.in</span>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Phone className="w-4 h-4 text-[#8c7f69] flex-shrink-0" />
                <div>
                  <strong className="text-[#080808] block mb-0.5">Telephone Assistance:</strong>
                  <span>+91 (080) 4123-REXXZ (Mon–Sat, 10am–7pm IST)</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Form Column */}
        <div className="lg:col-span-2">
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-8 shadow-sm">
            <h2 className="text-sm font-bold uppercase tracking-widest text-[#080808] border-b border-[#f0ece5] pb-4 mb-6">
              Send a Message
            </h2>

            {submitted ? (
              <div className="p-6 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-lg text-center space-y-2">
                <Check className="w-8 h-8 text-emerald-600 mx-auto" />
                <h3 className="font-bold text-sm uppercase">Message Transmitted</h3>
                <p className="text-xs">
                  Thank you for reaching out. Our concierge will review your message and respond within 24 business hours.
                </p>
                <button
                  onClick={() => setSubmitted(false)}
                  className="mt-3 px-4 py-2 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-wider rounded"
                >
                  Send Another Inquiry
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                      Your Name *
                    </label>
                    <input
                      type="text"
                      required
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      placeholder="e.g. Maya Rao"
                      className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                    />
                  </div>

                  <div>
                    <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                      Your Email *
                    </label>
                    <input
                      type="email"
                      required
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      placeholder="maya@example.com"
                      className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                    Subject *
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.subject}
                    onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                    placeholder="Product inquiry / Order support"
                    className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                    Message *
                  </label>
                  <textarea
                    required
                    rows={5}
                    value={formData.message}
                    onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                    placeholder="Please specify how we may assist you..."
                    className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                  />
                </div>

                <button
                  type="submit"
                  className="px-8 py-3.5 bg-[#080808] hover:bg-[#222222] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded shadow-md flex items-center justify-center gap-2 transition-all cursor-pointer"
                >
                  <span>Transmit Message</span>
                  <Send className="w-4 h-4" />
                </button>
              </form>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
