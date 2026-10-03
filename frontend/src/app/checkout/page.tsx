'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  ShieldCheck,
  CheckCircle2,
  CreditCard,
  Banknote,
  ArrowRight,
  ShoppingBag,
  Loader2,
  AlertCircle,
  MapPin,
  Truck
} from 'lucide-react';
import { useStore } from '@/context/StoreContext';
import { api, shippingApi } from '@/lib/api';
import StateDropdown from '@/components/checkout/StateDropdown';
import { matchIndianState } from '@/constants/indianStates';

interface LocationOption {
  name: string;
  district: string;
  state: string;
}

export default function CheckoutPage() {
  const router = useRouter();
  const { cart, user, refreshCart } = useStore();

  const [formData, setFormData] = useState({
    name: user?.name || '',
    email: user?.email || '',
    phone: user?.phone || '',
    street: '',
    city: '',
    district: '',
    state: '',
    postalCode: '',
    country: 'India',
    paymentMethod: 'ONLINE',
  });

  // Pincode lookup & validation states
  const [pincodeStatus, setPincodeStatus] = useState<'idle' | 'checking' | 'found' | 'not_found' | 'error'>('idle');
  const [pincodeMessage, setPincodeMessage] = useState<string>('');
  const [locationOptions, setLocationOptions] = useState<LocationOption[]>([]);
  const [selectedLocation, setSelectedLocation] = useState<string>('');
  const [pincodeVerifiedState, setPincodeVerifiedState] = useState<string>('');
  const [stateMismatchError, setStateMismatchError] = useState<string>('');

  // Shiprocket live serviceability indicator
  const [serviceability, setServiceability] = useState<{
    checked: boolean;
    serviceable: boolean;
    courierName?: string;
    etd?: string;
  }>({ checked: false, serviceable: true });

  const [isProcessing, setIsProcessing] = useState(false);
  const [submitError, setSubmitError] = useState<string>('');
  const [completedOrderNumber, setCompletedOrderNumber] = useState<string | null>(null);

  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  // Perform backend pincode lookup
  const performPincodeLookup = useCallback(async (code: string) => {
    if (!code || code.length !== 6 || !/^\d{6}$/.test(code)) {
      setPincodeStatus('idle');
      setPincodeMessage('');
      setLocationOptions([]);
      setPincodeVerifiedState('');
      setStateMismatchError('');
      return;
    }

    setPincodeStatus('checking');
    setPincodeMessage('Checking pincode...');
    setStateMismatchError('');

    try {
      const data = await shippingApi.lookupPincode(code);

      if (data.success) {
        setPincodeStatus('found');
        setPincodeMessage('Location found');
        setPincodeVerifiedState(data.state || '');
        setLocationOptions(data.locations || []);

        // Autofill City, District, State
        setFormData((prev) => ({
          ...prev,
          city: data.city || prev.city,
          district: data.district || prev.district,
          state: data.state || prev.state,
        }));

        // Check state mismatch if user had already picked another state
        if (formData.state && !matchIndianState(formData.state, data.state)) {
          setStateMismatchError(
            `Pincode ${code} belongs to ${data.state}, not ${formData.state}.`
          );
        } else {
          setStateMismatchError('');
        }

        // Trigger Shiprocket serviceability check in background
        checkShiprocketServiceability(code);
      } else {
        setPincodeStatus('not_found');
        setPincodeMessage(data.message || 'Pincode not found');
        setLocationOptions([]);
        setPincodeVerifiedState('');
      }
    } catch {
      setPincodeStatus('error');
      setPincodeMessage('Unable to check pincode. Please verify your connection.');
    }
  }, [formData.state]);

  // Handle pincode typing with 350ms debounce
  const handlePincodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawVal = e.target.value.replace(/[^0-9]/g, '').slice(0, 6);
    setFormData((prev) => ({ ...prev, postalCode: rawVal }));

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    if (rawVal.length === 6) {
      debounceTimerRef.current = setTimeout(() => {
        performPincodeLookup(rawVal);
      }, 350);
    } else {
      setPincodeStatus('idle');
      setPincodeMessage('');
      setLocationOptions([]);
      setPincodeVerifiedState('');
      setStateMismatchError('');
    }
  };

  // Handle location/area selection when multiple post offices exist
  const handleLocationSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    setSelectedLocation(val);
    const chosen = locationOptions.find((loc) => loc.name === val);
    if (chosen) {
      setFormData((prev) => ({
        ...prev,
        city: chosen.name,
        district: chosen.district || prev.district,
        state: chosen.state || prev.state,
      }));
    }
  };

  // Handle state change from searchable dropdown
  const handleStateChange = (newState: string) => {
    setFormData((prev) => ({ ...prev, state: newState }));

    // Verify against pincode if 6 digits already entered
    if (formData.postalCode.length === 6 && pincodeVerifiedState) {
      if (!matchIndianState(newState, pincodeVerifiedState)) {
        setStateMismatchError(
          `Pincode ${formData.postalCode} belongs to ${pincodeVerifiedState}, but '${newState}' was selected.`
        );
      } else {
        setStateMismatchError('');
      }
    } else {
      setStateMismatchError('');
    }
  };

  // Check Shiprocket serviceability
  const checkShiprocketServiceability = async (pincode: string) => {
    try {
      const res = await shippingApi.checkServiceability({
        deliveryPincode: pincode,
        orderValue: cart.total,
      });
      if (res && res.couriers && res.couriers.length > 0) {
        const topCourier = res.couriers[0];
        setServiceability({
          checked: true,
          serviceable: res.serviceable,
          courierName: topCourier.courierName,
          etd: topCourier.estimatedDeliveryDays ? `${topCourier.estimatedDeliveryDays} days` : undefined,
        });
      }
    } catch {
      // Fallback silently if Shiprocket credentials are not configured yet
      setServiceability({ checked: false, serviceable: true });
    }
  };

  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError('');

    if (cart.items.length === 0) return;

    // Strict frontend validation before sending
    if (formData.postalCode.length !== 6) {
      setSubmitError('Please enter a valid 6-digit Indian PIN code.');
      return;
    }

    if (pincodeStatus === 'not_found') {
      setSubmitError('Please enter a valid Indian PIN code. The entered PIN code was not found.');
      return;
    }

    if (!formData.state) {
      setSubmitError('Please select a valid State or Union Territory.');
      return;
    }

    if (pincodeVerifiedState && !matchIndianState(formData.state, pincodeVerifiedState)) {
      setSubmitError(
        `State mismatch: PIN Code ${formData.postalCode} belongs to ${pincodeVerifiedState}, but '${formData.state}' is selected.`
      );
      return;
    }

    setIsProcessing(true);

    try {
      const order = await api.placeOrder({
        subtotal: cart.subtotal,
        shippingFee: cart.shipping,
        discountAmount: cart.discount,
        totalAmount: cart.total,
        paymentMethod: formData.paymentMethod === 'ONLINE' ? 'Razorpay / UPI' : 'Cash on Delivery',
        items: cart.items.map((item) => ({
          productId: item.productId,
          productName: item.productName,
          quantity: item.quantity,
          price: item.price,
          imageUrl: item.imageUrl,
        })),
        shippingAddress: {
          street: formData.street,
          city: formData.city,
          state: formData.state,
          postalCode: formData.postalCode,
          country: formData.country,
        },
      });

      await refreshCart();
      setCompletedOrderNumber(order.orderNumber);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setSubmitError(err.message);
      } else {
        setSubmitError('Order placement encountered an error. Please try again.');
      }
    } finally {
      setIsProcessing(false);
    }
  };

  if (completedOrderNumber) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center space-y-6">
        <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto">
          <CheckCircle2 className="w-10 h-10" />
        </div>
        <div className="space-y-2">
          <span className="text-xs font-bold uppercase tracking-widest text-[#8c7f69]">
            Order Confirmed
          </span>
          <h1 className="text-3xl font-extrabold uppercase text-[#080808]">
            Thank You for Your Order
          </h1>
          <p className="text-sm text-[#555555]">
            Your order number is <strong className="text-[#080808]">{completedOrderNumber}</strong>. We have sent a confirmation email with tracking updates.
          </p>
        </div>

        <div className="pt-4 flex flex-col sm:flex-row justify-center gap-4">
          <Link
            href="/orders"
            className="px-6 py-3 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded hover:bg-[#222222] transition-colors"
          >
            Track Order Status
          </Link>
          <Link
            href="/shop"
            className="px-6 py-3 border border-[#d5d0c5] text-[#080808] text-xs font-bold uppercase tracking-widest rounded hover:bg-white transition-colors"
          >
            Continue Shopping
          </Link>
        </div>
      </div>
    );
  }

  if (cart.items.length === 0) {
    return (
      <div className="max-w-md mx-auto px-4 py-20 text-center space-y-4">
        <ShoppingBag className="w-12 h-12 text-[#888888] mx-auto" />
        <h2 className="text-xl font-bold uppercase text-[#080808]">Your cart is empty</h2>
        <p className="text-xs text-[#777777]">Add products to your cart before proceeding to checkout.</p>
        <Link
          href="/shop"
          className="inline-block mt-2 px-6 py-2.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase rounded"
        >
          Return to Shop
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-16">
      <div className="border-b border-[#e5e1d8] pb-6 mb-8">
        <h1 className="text-2xl sm:text-4xl font-extrabold uppercase tracking-tight text-[#080808]">
          Express Checkout
        </h1>
      </div>

      {submitError && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{submitError}</span>
        </div>
      )}

      <form onSubmit={handlePlaceOrder} className="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        {/* Checkout Form (2 Cols) */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* 1. Customer Information */}
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4 shadow-sm">
            <h2 className="text-xs font-bold uppercase tracking-widest text-[#080808] border-b border-[#f0ece5] pb-3">
              1. Contact Information
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                  Full Name *
                </label>
                <input
                  type="text"
                  name="name"
                  required
                  value={formData.name}
                  onChange={handleChange}
                  placeholder="e.g. Arjun Mehta"
                  className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                  Email Address *
                </label>
                <input
                  type="email"
                  name="email"
                  required
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="name@domain.com"
                  className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                  Phone Number (for Courier Tracking SMS) *
                </label>
                <input
                  type="tel"
                  name="phone"
                  required
                  value={formData.phone}
                  onChange={handleChange}
                  placeholder="+91 98765 43210"
                  className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                />
              </div>
            </div>
          </div>

          {/* 2. Shipping Address */}
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4 shadow-sm">
            <div className="flex items-center justify-between border-b border-[#f0ece5] pb-3">
              <h2 className="text-xs font-bold uppercase tracking-widest text-[#080808]">
                2. Shipping Destination
              </h2>
              <span className="text-[11px] text-[#8c7f69] font-medium flex items-center gap-1">
                <MapPin className="w-3.5 h-3.5" /> India Delivery
              </span>
            </div>

            <div className="space-y-4">
              
              {/* PIN Code Lookup first for auto-fill */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="text-[11px] font-bold uppercase text-[#555555]">
                      PIN Code (6 Digits) *
                    </label>
                    {pincodeStatus === 'checking' && (
                      <span className="text-[10px] text-[#8c7f69] font-semibold flex items-center gap-1">
                        <Loader2 className="w-3 h-3 animate-spin" /> Checking pincode...
                      </span>
                    )}
                    {pincodeStatus === 'found' && (
                      <span className="text-[10px] text-emerald-700 font-bold flex items-center gap-1">
                        <CheckCircle2 className="w-3 h-3" /> Location found
                      </span>
                    )}
                    {pincodeStatus === 'not_found' && (
                      <span className="text-[10px] text-red-600 font-bold flex items-center gap-1">
                        <AlertCircle className="w-3 h-3" /> Pincode not found
                      </span>
                    )}
                  </div>
                  <div className="relative">
                    <input
                      type="text"
                      name="postalCode"
                      required
                      maxLength={6}
                      value={formData.postalCode}
                      onChange={handlePincodeChange}
                      placeholder="e.g. 560001"
                      className={`w-full bg-[#f9f8f6] border rounded p-2.5 text-xs text-[#080808] font-mono tracking-wider focus:outline-none ${
                        pincodeStatus === 'not_found'
                          ? 'border-red-400 focus:border-red-500'
                          : pincodeStatus === 'found'
                          ? 'border-emerald-400 focus:border-emerald-500'
                          : 'border-[#e5e1d8] focus:border-[#C8BCA7]'
                      }`}
                    />
                  </div>
                  <span className="text-[10px] text-[#999999] mt-1 block">
                    City, District and State will be auto-filled upon entering 6 digits
                  </span>
                </div>

                {/* Multiple Localities / Post Offices selector if available */}
                {locationOptions.length > 1 && (
                  <div>
                    <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                      Select Area / Locality ({locationOptions.length} found)
                    </label>
                    <select
                      value={selectedLocation}
                      onChange={handleLocationSelect}
                      className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                    >
                      <option value="">Choose Area/Post Office...</option>
                      {locationOptions.map((loc) => (
                        <option key={loc.name} value={loc.name}>
                          {loc.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              {/* Street Address */}
              <div>
                <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                  Street Address & Apartment / Suite *
                </label>
                <input
                  type="text"
                  name="street"
                  required
                  value={formData.street}
                  onChange={handleChange}
                  placeholder="e.g. Flat 402, Lotus Residency, 12th Main"
                  className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                />
              </div>

              {/* City, District, State Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                    City / Town *
                  </label>
                  <input
                    type="text"
                    name="city"
                    required
                    value={formData.city}
                    onChange={handleChange}
                    placeholder="Bengaluru"
                    className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                    District
                  </label>
                  <input
                    type="text"
                    name="district"
                    value={formData.district}
                    onChange={handleChange}
                    placeholder="Bengaluru Urban"
                    className="w-full bg-[#f9f8f6] border border-[#e5e1d8] rounded p-2.5 text-xs text-[#080808] focus:outline-none focus:border-[#C8BCA7]"
                  />
                </div>

                <div>
                  <label className="text-[11px] font-bold uppercase text-[#555555] block mb-1.5">
                    State / UT *
                  </label>
                  <StateDropdown
                    value={formData.state}
                    onChange={handleStateChange}
                    hasError={Boolean(stateMismatchError)}
                  />
                </div>
              </div>

              {/* State & Pincode Mismatch Warning */}
              {stateMismatchError && (
                <div className="p-2.5 bg-amber-50 border border-amber-200 text-amber-800 text-xs rounded flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0 text-amber-600" />
                  <span>{stateMismatchError}</span>
                </div>
              )}

              {/* Shiprocket Serviceability Banner */}
              {serviceability.checked && (
                <div className="p-3 bg-[#fbf9f5] border border-[#e5e1d8] rounded-lg text-xs flex items-center justify-between">
                  <span className="flex items-center gap-2 text-[#080808] font-medium">
                    <Truck className="w-4 h-4 text-[#8c7f69]" />
                    Delivery Serviceable via Shiprocket ({serviceability.courierName || 'Standard Express'})
                  </span>
                  {serviceability.etd && (
                    <span className="text-[10px] uppercase font-bold text-[#8c7f69] bg-white px-2 py-0.5 rounded border border-[#e5e1d8]">
                      Est. {serviceability.etd}
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* 3. Payment Method */}
          <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4 shadow-sm">
            <h2 className="text-xs font-bold uppercase tracking-widest text-[#080808] border-b border-[#f0ece5] pb-3">
              3. Payment Selection
            </h2>
            <div className="space-y-3">
              <label className={`flex items-center justify-between p-4 border rounded-lg cursor-pointer transition-all ${
                formData.paymentMethod === 'ONLINE'
                  ? 'bg-[#faf9f7] border-[#080808]'
                  : 'bg-white border-[#e5e1d8] hover:border-[#C8BCA7]'
              }`}>
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="ONLINE"
                    checked={formData.paymentMethod === 'ONLINE'}
                    onChange={handleChange}
                    className="accent-[#080808]"
                  />
                  <div>
                    <div className="text-xs font-bold text-[#080808] flex items-center gap-2">
                      <span>Online Payment (UPI, Credit/Debit Card, Netbanking)</span>
                      <span className="text-[9px] bg-emerald-100 text-emerald-800 px-1.5 py-0.5 rounded uppercase font-extrabold tracking-wider">
                        Fastest
                      </span>
                    </div>
                    <div className="text-[11px] text-[#777777]">
                      Secured via Razorpay 256-bit encryption. Supports Google Pay, PhonePe, Paytm & cards.
                    </div>
                  </div>
                </div>
                <CreditCard className="w-5 h-5 text-[#888888]" />
              </label>

              <label className={`flex items-center justify-between p-4 border rounded-lg cursor-pointer transition-all ${
                formData.paymentMethod === 'COD'
                  ? 'bg-[#faf9f7] border-[#080808]'
                  : 'bg-white border-[#e5e1d8] hover:border-[#C8BCA7]'
              }`}>
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="COD"
                    checked={formData.paymentMethod === 'COD'}
                    onChange={handleChange}
                    className="accent-[#080808]"
                  />
                  <div>
                    <div className="text-xs font-bold text-[#080808]">Cash on Delivery (COD)</div>
                    <div className="text-[11px] text-[#777777]">
                      Pay in cash or UPI to the courier delivery partner upon parcel arrival.
                    </div>
                  </div>
                </div>
                <Banknote className="w-5 h-5 text-[#888888]" />
              </label>
            </div>
          </div>

        </div>

        {/* Order Summary Sidebar (1 Col) */}
        <div className="lg:col-span-1">
          <div className="bg-[#f9f8f6] border border-[#e5e1d8] rounded-xl p-6 space-y-6 sticky top-24">
            <h2 className="text-xs font-bold uppercase tracking-widest text-[#080808] border-b border-[#e5e1d8] pb-3">
              Order Summary ({cart.items.reduce((sum, item) => sum + item.quantity, 0)} Items)
            </h2>

            <div className="space-y-3 max-h-64 overflow-y-auto pr-1">
              {cart.items.map((item) => (
                <div key={item.productId} className="flex justify-between items-center text-xs">
                  <div className="flex-1 pr-2 truncate">
                    <span className="font-bold text-[#080808] block truncate">{item.productName}</span>
                    <span className="text-[10px] text-[#888888]">Qty: {item.quantity}</span>
                  </div>
                  <span className="font-mono text-[#080808] font-semibold">
                    ₹{(item.price * item.quantity).toLocaleString('en-IN')}
                  </span>
                </div>
              ))}
            </div>

            <div className="border-t border-[#e5e1d8] pt-4 space-y-2 text-xs">
              <div className="flex justify-between text-[#555555]">
                <span>Subtotal</span>
                <span className="font-mono">₹{cart.subtotal.toLocaleString('en-IN')}</span>
              </div>
              <div className="flex justify-between text-[#555555]">
                <span>Shipping</span>
                <span className="font-mono">
                  {cart.shipping === 0 ? (
                    <span className="text-emerald-700 font-bold uppercase">Free</span>
                  ) : (
                    `₹${cart.shipping}`
                  )}
                </span>
              </div>
              {cart.discount > 0 && (
                <div className="flex justify-between text-emerald-700 font-medium">
                  <span>Discount</span>
                  <span className="font-mono">-₹{cart.discount.toLocaleString('en-IN')}</span>
                </div>
              )}
              <div className="border-t border-[#e5e1d8] pt-3 flex justify-between items-baseline">
                <span className="text-sm font-bold uppercase text-[#080808]">Total</span>
                <span className="text-lg font-bold font-mono text-[#080808]">
                  ₹{cart.total.toLocaleString('en-IN')}
                </span>
              </div>
            </div>

            <button
              type="submit"
              disabled={isProcessing || Boolean(stateMismatchError)}
              className="w-full py-4 bg-[#080808] hover:bg-[#222222] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded-lg shadow-lg flex items-center justify-center gap-2 transition-all cursor-pointer disabled:opacity-50"
            >
              <span>{isProcessing ? 'Processing Order...' : 'Place Order'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            <div className="flex items-center justify-center gap-2 text-[10px] text-[#888888]">
              <ShieldCheck className="w-3.5 h-3.5 text-[#8c7f69]" />
              <span>Verified Indian Delivery & Secured Checkout</span>
            </div>
          </div>
        </div>

      </form>
    </div>
  );
}
