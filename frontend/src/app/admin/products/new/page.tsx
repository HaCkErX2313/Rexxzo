'use client';

import React, { useState, useEffect, useId } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import {
  ArrowLeft,
  Upload,
  Plus,
  Trash2,
  CheckCircle,
  AlertTriangle,
  RefreshCw,
  Sparkles,
  Info,
  DollarSign,
  Layers,
  Truck,
  Image as ImageIcon,
  Save,
  FileText,
} from 'lucide-react';
import AdminNav from '@/components/admin/AdminNav';
import { api } from '@/lib/api';
import { Category } from '@/types';

export default function AddProductPage() {
  const router = useRouter();

  // Authentication Guard
  const [authChecked, setAuthChecked] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Form Fields
  const [name, setName] = useState('');
  const [sku, setSku] = useState('');
  const [description, setDescription] = useState('');
  const [shortDescription, setShortDescription] = useState('');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [isFeatured, setIsFeatured] = useState(false);

  // Pricing
  const [price, setPrice] = useState<string>('');
  const [mrp, setMrp] = useState<string>('');

  // Inventory
  const [stockQuantity, setStockQuantity] = useState<string>('10');
  const [lowStockThreshold, setLowStockThreshold] = useState<string>('10');

  // Images
  const [images, setImages] = useState<string[]>([]);
  const [customImageUrl, setCustomImageUrl] = useState('');

  // Shipping
  const [weightGrams, setWeightGrams] = useState<string>('');
  const [lengthCm, setLengthCm] = useState<string>('');
  const [widthCm, setWidthCm] = useState<string>('');
  const [heightCm, setHeightCm] = useState<string>('');
  const [materials, setMaterials] = useState('');
  const [dimensions, setDimensions] = useState('');

  // Status
  const [status, setStatus] = useState<'ACTIVE' | 'DRAFT' | 'OUT_OF_STOCK'>('ACTIVE');

  // Check auth & load categories
  useEffect(() => {
    const token = typeof window !== 'undefined'
      ? localStorage.getItem('rexxo_admin_token')
      : null;

    if (!token) {
      router.push('/admin/login');
      return;
    }
    setAuthChecked(true);

    api.getCategories().then((cats) => {
      setCategories(cats || []);
      if (cats && cats.length > 0) {
        setCategoryId(cats[0].id);
      }
    }).catch(() => {});
  }, [router]);

  // Auto-generate unique SKU
  const generateSku = () => {
    const prefix = name ? name.substring(0, 3).toUpperCase().replace(/[^A-Z]/g, 'REX') : 'REX';
    const rand = Math.floor(1000 + Math.random() * 9000);
    setSku(`${prefix}-${rand}`);
  };

  // Dynamic discount calculation
  const numericPrice = parseFloat(price) || 0;
  const numericMrp = parseFloat(mrp) || 0;
  const discountPercent = (numericMrp > numericPrice && numericPrice > 0)
    ? Math.round(((numericMrp - numericPrice) / numericMrp) * 100)
    : 0;

  // Image Upload handler
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploadingImage(true);
    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const res = await api.uploadAdminProductImage(file);
        if (res.url) {
          setImages((prev) => [...prev, res.url]);
        }
      }
      setMessage({ type: 'success', text: 'Image uploaded successfully.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Image upload failed' });
    } finally {
      setUploadingImage(false);
      e.target.value = '';
    }
  };

  const handleAddImageUrl = () => {
    if (!customImageUrl.trim()) return;
    setImages((prev) => [...prev, customImageUrl.trim()]);
    setCustomImageUrl('');
  };

  const handleRemoveImage = (index: number) => {
    setImages((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleSetPrimaryImage = (index: number) => {
    setImages((prev) => {
      const selected = prev[index];
      const rest = prev.filter((_, idx) => idx !== index);
      return [selected, ...rest];
    });
  };

  // Submit Handler
  const handleSubmit = async (targetStatus: 'ACTIVE' | 'DRAFT') => {
    setMessage(null);

    // Frontend validations
    if (!name.trim()) {
      setMessage({ type: 'error', text: 'Product name is required.' });
      return;
    }
    if (!sku.trim()) {
      setMessage({ type: 'error', text: 'SKU is required.' });
      return;
    }
    if (!description.trim()) {
      setMessage({ type: 'error', text: 'Product description is required.' });
      return;
    }
    if (!categoryId) {
      setMessage({ type: 'error', text: 'Please select a valid Category.' });
      return;
    }
    if (!price || isNaN(numericPrice) || numericPrice < 0) {
      setMessage({ type: 'error', text: 'Please enter a valid selling price (>= 0).' });
      return;
    }
    if (numericMrp > 0 && numericPrice > numericMrp) {
      setMessage({ type: 'error', text: `Selling price (₹${numericPrice}) cannot exceed MRP (₹${numericMrp}).` });
      return;
    }
    if (images.length === 0) {
      setMessage({ type: 'error', text: 'Please add at least one product image.' });
      return;
    }

    setLoading(true);
    try {
      const finalStatus = targetStatus === 'DRAFT' ? 'DRAFT' : status;

      const payload = {
        name: name.trim(),
        sku: sku.trim().toUpperCase(),
        description: description.trim(),
        shortDescription: shortDescription.trim() || undefined,
        categoryId: Number(categoryId),
        price: numericPrice,
        compareAtPrice: numericMrp > 0 ? numericMrp : undefined,
        stockQuantity: parseInt(stockQuantity, 10) || 0,
        lowStockThreshold: parseInt(lowStockThreshold, 10) || 10,
        status: finalStatus,
        isActive: finalStatus !== 'DRAFT',
        isFeatured,
        imageUrls: images,
        weightGrams: weightGrams ? parseInt(weightGrams, 10) : undefined,
        lengthCm: lengthCm ? parseFloat(lengthCm) : undefined,
        widthCm: widthCm ? parseFloat(widthCm) : undefined,
        heightCm: heightCm ? parseFloat(heightCm) : undefined,
        materials: materials.trim() || undefined,
        dimensions: dimensions.trim() || undefined,
      };

      const created = await api.createAdminProduct(payload);

      setMessage({
        type: 'success',
        text: `Product "${created.name}" saved with exact price ₹${numericPrice.toLocaleString('en-IN')}. Redirecting to catalog...`,
      });

      setTimeout(() => {
        router.push('/admin/products');
      }, 1200);
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to create product in PostgreSQL' });
      setLoading(false);
    }
  };

  if (!authChecked) {
    return (
      <div className="min-h-screen bg-[#faf9f6] flex items-center justify-center">
        <div className="flex items-center gap-3 text-xs font-bold uppercase tracking-widest text-[#777777]">
          <RefreshCw className="w-4 h-4 animate-spin text-[#080808]" />
          Verifying Admin Access...
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#faf9f6] text-[#080808] pb-28 font-sans">
      <AdminNav currentTab="products" />

      <main className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 space-y-6">
        {/* Breadcrumb & Navigation */}
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-[#777777] mb-1">
              <Link href="/admin" className="hover:text-[#080808]">Admin</Link>
              <span>/</span>
              <Link href="/admin/products" className="hover:text-[#080808]">Products</Link>
              <span>/</span>
              <span className="text-[#080808]">Add Product</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#080808] font-serif">
              Add New Product
            </h1>
            <p className="text-xs text-[#777777] mt-0.5">
              Publish direct to REXXZO storefront and store in PostgreSQL.
            </p>
          </div>

          <Link
            href="/admin/products"
            className="flex items-center gap-1.5 px-3 py-2 border border-[#e5e1d8] rounded-lg text-xs font-bold uppercase tracking-wider text-[#555] hover:text-[#080808] hover:border-[#080808] bg-white transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Cancel</span>
          </Link>
        </div>

        {/* Global Feedback Banner */}
        {message && (
          <div
            className={`p-3.5 rounded-xl border flex items-center justify-between text-xs font-medium animate-fadeIn ${
              message.type === 'success'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                : 'bg-rose-50 border-rose-200 text-rose-800'
            }`}
          >
            <div className="flex items-center gap-2">
              {message.type === 'success' ? (
                <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" />
              ) : (
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
              )}
              <span>{message.text}</span>
            </div>
            <button
              onClick={() => setMessage(null)}
              className="text-xs font-bold uppercase tracking-wider underline hover:opacity-80 ml-4"
            >
              Dismiss
            </button>
          </div>
        )}

        <div className="space-y-6">
          {/* SECTION 1: PRODUCT INFORMATION */}
          <div className="bg-white rounded-2xl border border-[#e5e1d8] p-6 shadow-xs space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-[#f0ece5]">
              <div className="w-7 h-7 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center font-bold text-xs">
                1
              </div>
              <div>
                <h2 className="text-sm font-extrabold uppercase tracking-wider text-[#080808]">
                  Product Information
                </h2>
                <p className="text-[11px] text-[#777777]">Essential naming, identification, and category placement</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Product Name */}
              <div className="md:col-span-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Product Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Premium Ceramic Sculptural Vase"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808] transition-colors"
                />
              </div>

              {/* SKU */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-bold uppercase tracking-wider text-[#333333]">
                    SKU (Stock Keeping Unit) <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={generateSku}
                    className="text-[10px] font-bold text-[#777777] hover:text-[#080808] underline flex items-center gap-1"
                  >
                    <Sparkles className="w-2.5 h-2.5" />
                    Auto-generate
                  </button>
                </div>
                <input
                  type="text"
                  value={sku}
                  onChange={(e) => setSku(e.target.value.toUpperCase())}
                  placeholder="e.g. REX-VASE-001"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs font-mono uppercase text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              {/* Category */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Category <span className="text-rose-500">*</span>
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs font-semibold text-[#080808] focus:outline-none focus:border-[#080808]"
                >
                  <option value="" disabled>Select Category</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name} {cat.slug === 'gift-corner' ? '🎁' : ''}
                    </option>
                  ))}
                </select>
              </div>

              {/* Short Description */}
              <div className="md:col-span-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Short Tagline / Subtitle
                </label>
                <input
                  type="text"
                  value={shortDescription}
                  onChange={(e) => setShortDescription(e.target.value)}
                  placeholder="e.g. Hand-thrown artisanal vase finished in soft reactive stone glaze"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              {/* Full Description */}
              <div className="md:col-span-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Full Description <span className="text-rose-500">*</span>
                </label>
                <textarea
                  rows={4}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Provide comprehensive details about craftsmanship, aesthetic profile, materials, and styling tips..."
                  className="w-full p-3.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              {/* Featured toggle */}
              <div className="md:col-span-2 flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="featuredToggle"
                  checked={isFeatured}
                  onChange={(e) => setIsFeatured(e.target.checked)}
                  className="w-4 h-4 rounded text-[#080808] border-[#ccc] focus:ring-0 cursor-pointer"
                />
                <label htmlFor="featuredToggle" className="text-xs font-bold text-[#080808] cursor-pointer">
                  Feature this product on homepage curations
                </label>
              </div>
            </div>
          </div>

          {/* SECTION 2: PRICING (EXACT PRICE ENGINE) */}
          <div className="bg-white rounded-2xl border border-[#e5e1d8] p-6 shadow-xs space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-[#f0ece5]">
              <div className="w-7 h-7 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center font-bold text-xs">
                2
              </div>
              <div>
                <h2 className="text-sm font-extrabold uppercase tracking-wider text-[#080808]">
                  Exact Pricing &amp; Discounts
                </h2>
                <p className="text-[11px] text-[#777777]">
                  Backend strictly uses PostgreSQL BigDecimal. Frontend never determines final total.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-start">
              {/* Selling Price */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Selling Price (₹) <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 font-bold text-xs text-[#555]">₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    placeholder="1499"
                    className="w-full pl-8 pr-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs font-bold text-[#080808] focus:outline-none focus:border-[#080808]"
                  />
                </div>
                <p className="text-[10px] text-[#888888] mt-1">Exact customer price charged at checkout</p>
              </div>

              {/* MRP */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  MRP / Original Price (₹)
                </label>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 font-bold text-xs text-[#555]">₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={mrp}
                    onChange={(e) => setMrp(e.target.value)}
                    placeholder="1999"
                    className="w-full pl-8 pr-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                  />
                </div>
                <p className="text-[10px] text-[#888888] mt-1">Strikethrough reference price</p>
              </div>

              {/* Calculated Discount Display */}
              <div className="bg-[#faf9f6] p-4 rounded-xl border border-[#e5e1d8]">
                <div className="text-[10px] font-bold uppercase tracking-widest text-[#777777]">
                  Calculated Storefront Display
                </div>
                <div className="flex items-baseline gap-2 mt-1">
                  <span className="text-lg font-black text-[#080808]">
                    ₹{numericPrice > 0 ? numericPrice.toLocaleString('en-IN') : '0'}
                  </span>
                  {numericMrp > numericPrice && (
                    <span className="text-xs line-through text-[#999999]">
                      ₹{numericMrp.toLocaleString('en-IN')}
                    </span>
                  )}
                </div>
                {discountPercent > 0 && (
                  <div className="text-xs font-bold text-emerald-600 mt-1">
                    {discountPercent}% OFF (calculated safely on backend)
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* SECTION 3: INVENTORY */}
          <div className="bg-white rounded-2xl border border-[#e5e1d8] p-6 shadow-xs space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-[#f0ece5]">
              <div className="w-7 h-7 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center font-bold text-xs">
                3
              </div>
              <div>
                <h2 className="text-sm font-extrabold uppercase tracking-wider text-[#080808]">
                  Inventory &amp; Thresholds
                </h2>
                <p className="text-[11px] text-[#777777]">Integrated with REXXZO inventory tracking</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Stock Quantity <span className="text-rose-500">*</span>
                </label>
                <input
                  type="number"
                  min="0"
                  value={stockQuantity}
                  onChange={(e) => setStockQuantity(e.target.value)}
                  placeholder="20"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs font-bold text-[#080808] focus:outline-none focus:border-[#080808]"
                />
                <p className="text-[10px] text-[#888888] mt-1">
                  Reduced automatically upon checkout. Cannot drop below 0.
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Low Stock Threshold
                </label>
                <input
                  type="number"
                  min="0"
                  value={lowStockThreshold}
                  onChange={(e) => setLowStockThreshold(e.target.value)}
                  placeholder="10"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
                <p className="text-[10px] text-[#888888] mt-1">
                  Triggers LOW STOCK warning badge in Admin when inventory reaches this level
                </p>
              </div>
            </div>
          </div>

          {/* SECTION 4: PRODUCT IMAGES */}
          <div className="bg-white rounded-2xl border border-[#e5e1d8] p-6 shadow-xs space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-[#f0ece5]">
              <div className="w-7 h-7 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center font-bold text-xs">
                4
              </div>
              <div>
                <h2 className="text-sm font-extrabold uppercase tracking-wider text-[#080808]">
                  Product Imagery <span className="text-rose-500">*</span>
                </h2>
                <p className="text-[11px] text-[#777777]">
                  First image becomes Primary Cover. Drag/click to reorder or upload multiple.
                </p>
              </div>
            </div>

            {/* Upload & URL Box */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* File Upload Button */}
              <div className="border-2 border-dashed border-[#e5e1d8] rounded-xl p-5 text-center flex flex-col items-center justify-center hover:border-[#080808] transition-colors bg-[#fcfbfa]">
                <Upload className="w-6 h-6 text-[#777777] mb-2" />
                <label className="cursor-pointer">
                  <span className="text-xs font-bold text-[#080808] hover:underline block">
                    {uploadingImage ? 'Uploading Image...' : 'Click to Upload from Device'}
                  </span>
                  <span className="text-[10px] text-[#888888] block mt-0.5">
                    JPG, PNG, WebP supported
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    disabled={uploadingImage}
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </label>
              </div>

              {/* Image URL Input */}
              <div className="border border-[#e5e1d8] rounded-xl p-4 flex flex-col justify-between bg-[#fcfbfa]">
                <div>
                  <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                    Or Enter Image URL
                  </label>
                  <input
                    type="text"
                    value={customImageUrl}
                    onChange={(e) => setCustomImageUrl(e.target.value)}
                    placeholder="/images/hero_banner.jpg or https://..."
                    className="w-full px-3 py-2 bg-white border border-[#e5e1d8] rounded-lg text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                  />
                </div>
                <button
                  type="button"
                  onClick={handleAddImageUrl}
                  className="mt-3 px-3 py-1.5 bg-[#080808] text-[#C8BCA7] rounded-lg text-xs font-bold uppercase tracking-wider hover:bg-[#222] self-end"
                >
                  Add Image URL
                </button>
              </div>
            </div>

            {/* Uploaded Images Grid */}
            {images.length > 0 && (
              <div className="space-y-2 pt-2">
                <div className="text-xs font-bold uppercase tracking-wider text-[#777777]">
                  Attached Images ({images.length})
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3">
                  {images.map((imgUrl, idx) => (
                    <div
                      key={idx}
                      className="group relative aspect-square rounded-xl overflow-hidden border border-[#e5e1d8] bg-[#f0ece5]"
                    >
                      <Image
                        src={imgUrl}
                        alt={`Product image ${idx + 1}`}
                        fill
                        sizes="120px"
                        className="object-cover"
                        onError={(e) => {
                          (e.target as any).src = '/images/hero_banner.jpg';
                        }}
                      />
                      {idx === 0 && (
                        <div className="absolute top-1.5 left-1.5 bg-[#080808] text-[#C8BCA7] text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded shadow">
                          Primary
                        </div>
                      )}
                      <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                        {idx !== 0 && (
                          <button
                            type="button"
                            onClick={() => handleSetPrimaryImage(idx)}
                            title="Make Primary"
                            className="p-1.5 bg-white text-[#080808] rounded-md text-[10px] font-bold hover:bg-[#f0ece5]"
                          >
                            Main
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleRemoveImage(idx)}
                          title="Remove image"
                          className="p-1.5 bg-rose-600 text-white rounded-md hover:bg-rose-700"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* SECTION 5: SHIPPING & SPECIFICATIONS */}
          <div className="bg-white rounded-2xl border border-[#e5e1d8] p-6 shadow-xs space-y-5">
            <div className="flex items-center gap-2.5 pb-3 border-b border-[#f0ece5]">
              <div className="w-7 h-7 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center font-bold text-xs">
                5
              </div>
              <div>
                <h2 className="text-sm font-extrabold uppercase tracking-wider text-[#080808]">
                  Shipping Dimensions &amp; Specifications
                </h2>
                <p className="text-[11px] text-[#777777]">
                  Used for Shiprocket dispatch and customer product spec sheets
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Weight (Grams)
                </label>
                <input
                  type="number"
                  min="0"
                  value={weightGrams}
                  onChange={(e) => setWeightGrams(e.target.value)}
                  placeholder="500"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Length (cm)
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  value={lengthCm}
                  onChange={(e) => setLengthCm(e.target.value)}
                  placeholder="15.0"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Width (cm)
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  value={widthCm}
                  onChange={(e) => setWidthCm(e.target.value)}
                  placeholder="15.0"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Height (cm)
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  value={heightCm}
                  onChange={(e) => setHeightCm(e.target.value)}
                  placeholder="24.0"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              <div className="col-span-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Materials / Craftsmanship
                </label>
                <input
                  type="text"
                  value={materials}
                  onChange={(e) => setMaterials(e.target.value)}
                  placeholder="e.g. Artisanal stoneware ceramic, non-porous food-safe glaze"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>

              <div className="col-span-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-[#333333] mb-1.5">
                  Dimensions Description
                </label>
                <input
                  type="text"
                  value={dimensions}
                  onChange={(e) => setDimensions(e.target.value)}
                  placeholder="e.g. Height 24cm, Capacity 1.2L"
                  className="w-full px-3.5 py-2.5 bg-[#fcfbfa] border border-[#e5e1d8] rounded-xl text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                />
              </div>
            </div>
          </div>

          {/* SECTION 6: PUBLISHING STATUS */}
          <div className="bg-white rounded-2xl border border-[#e5e1d8] p-6 shadow-xs space-y-4">
            <div className="flex items-center gap-2.5 pb-3 border-b border-[#f0ece5]">
              <div className="w-7 h-7 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center font-bold text-xs">
                6
              </div>
              <div>
                <h2 className="text-sm font-extrabold uppercase tracking-wider text-[#080808]">
                  Publishing &amp; Storefront Visibility
                </h2>
                <p className="text-[11px] text-[#777777]">Select how this product enters the catalog</p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <label
                onClick={() => setStatus('ACTIVE')}
                className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  status === 'ACTIVE'
                    ? 'border-[#080808] bg-[#fcfbfa]'
                    : 'border-[#e5e1d8] hover:border-[#cccccc]'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-extrabold uppercase tracking-wider text-[#080808]">
                    Active
                  </span>
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
                </div>
                <p className="text-[11px] text-[#777777]">
                  Immediately visible and purchasable on REXXZO storefront and category pages.
                </p>
              </label>

              <label
                onClick={() => setStatus('DRAFT')}
                className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  status === 'DRAFT'
                    ? 'border-[#080808] bg-[#fcfbfa]'
                    : 'border-[#e5e1d8] hover:border-[#cccccc]'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-extrabold uppercase tracking-wider text-[#080808]">
                    Draft
                  </span>
                  <span className="w-2.5 h-2.5 rounded-full bg-slate-400"></span>
                </div>
                <p className="text-[11px] text-[#777777]">
                  Hidden from public storefront. Visible only to Admins for editing.
                </p>
              </label>

              <label
                onClick={() => setStatus('OUT_OF_STOCK')}
                className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                  status === 'OUT_OF_STOCK'
                    ? 'border-[#080808] bg-[#fcfbfa]'
                    : 'border-[#e5e1d8] hover:border-[#cccccc]'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-extrabold uppercase tracking-wider text-[#080808]">
                    Out of Stock
                  </span>
                  <span className="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
                </div>
                <p className="text-[11px] text-[#777777]">
                  Visible on storefront with &quot;Out of Stock&quot; badge, preventing purchases.
                </p>
              </label>
            </div>
          </div>

          {/* ACTION BUTTONS */}
          <div className="flex flex-col sm:flex-row items-center justify-end gap-3 pt-4 border-t border-[#e5e1d8]">
            <Link
              href="/admin/products"
              className="w-full sm:w-auto px-5 py-3 border border-[#e5e1d8] rounded-xl text-xs font-bold uppercase tracking-wider text-[#555] hover:text-[#080808] text-center bg-white transition-colors"
            >
              Cancel
            </Link>

            <button
              type="button"
              disabled={loading}
              onClick={() => handleSubmit('DRAFT')}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-6 py-3 bg-[#f0ece5] hover:bg-[#e5e1d8] text-[#080808] rounded-xl text-xs font-bold uppercase tracking-widest transition-all disabled:opacity-50"
            >
              <FileText className="w-4 h-4" />
              <span>Save as Draft</span>
            </button>

            <button
              type="button"
              disabled={loading}
              onClick={() => handleSubmit('ACTIVE')}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-8 py-3 bg-[#080808] hover:bg-[#222222] text-[#C8BCA7] rounded-xl text-xs font-bold uppercase tracking-widest transition-all shadow-md disabled:opacity-50"
            >
              {loading ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Saving to PostgreSQL...</span>
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  <span>Save Product</span>
                </>
              )}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
