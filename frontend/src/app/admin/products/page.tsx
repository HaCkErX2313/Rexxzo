'use client';

import React, { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import {
  Package,
  Plus,
  Search,
  Filter,
  Trash2,
  Edit,
  ExternalLink,
  AlertTriangle,
  CheckCircle,
  Eye,
  RefreshCw,
  Tag,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Layers,
  Archive,
  RotateCcw,
  ShieldAlert,
  Info,
} from 'lucide-react';
import AdminNav from '@/components/admin/AdminNav';
import { api } from '@/lib/api';
import { Product, Category } from '@/types';

export default function AdminProductsPage() {
  const router = useRouter();

  // Authentication & Guard
  const [authChecked, setAuthChecked] = useState(false);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Products Data
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);
  const pageSize = 15;

  // Filters & Search
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [stockFilter, setStockFilter] = useState<string>('ALL');
  const [sortBy, setSortBy] = useState<string>('createdAt');
  const [sortDir, setSortDir] = useState<string>('desc');

  // Delete / Archive Modal
  const [deleteModalProduct, setDeleteModalProduct] = useState<Product | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deletionAction, setDeletionAction] = useState<'archive' | 'permanent'>('archive');
  const [permanentConfirmChecked, setPermanentConfirmChecked] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);
  const [canArchiveInstead, setCanArchiveInstead] = useState(false);

  // Verify auth on mount
  useEffect(() => {
    const token = typeof window !== 'undefined'
      ? localStorage.getItem('rexxo_admin_token')
      : null;

    if (!token) {
      router.push('/admin/login');
      return;
    }
    setAuthChecked(true);
  }, [router]);

  // Load categories
  useEffect(() => {
    if (!authChecked) return;
    api.getCategories().then((cats) => {
      setCategories(cats || []);
    }).catch(() => {});
  }, [authChecked]);

  // Load products from PostgreSQL
  const fetchProducts = useCallback(async (isSilent = false) => {
    if (!authChecked) return;
    if (!isSilent) setLoading(true);
    else setRefreshing(true);

    try {
      const res = await api.getAdminProducts({
        search: search.trim() || undefined,
        categoryId: categoryId !== 'ALL' ? Number(categoryId) : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        stockFilter: stockFilter !== 'ALL' ? stockFilter : undefined,
        page,
        size: pageSize,
        sortBy,
        sortDir,
      });

      const list = res?.content || [];
      setProducts(list);
      setTotalElements(res?.totalElements || list.length);
      setTotalPages(res?.totalPages || 1);
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to load products from database' });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [authChecked, search, categoryId, statusFilter, stockFilter, page, sortBy, sortDir]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Quick stats summary
  const totalCount = totalElements;
  const outOfStockCount = products.filter(p => (p.stock !== undefined ? p.stock <= 0 : p.stockQuantity <= 0) || p.status === 'OUT_OF_STOCK').length;
  const lowStockCount = products.filter(p => {
    const stock = p.stock !== undefined ? p.stock : p.stockQuantity;
    const threshold = p.lowStockThreshold || 10;
    return stock > 0 && stock <= threshold;
  }).length;
  const activeCount = products.filter(p => p.status === 'ACTIVE' || (!p.status && p.isActive)).length;

  // Toggle active / draft status
  const handleToggleStatus = async (productId: number) => {
    try {
      const res = await api.toggleAdminProductStatus(productId);
      setMessage({
        type: 'success',
        text: `Product is now ${res.status || (res.isActive ? 'ACTIVE' : 'DRAFT')}. Changes live on storefront.`,
      });
      fetchProducts(true);
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to toggle product status' });
    }
  };

  // Open modal with specific action
  const openRemovalModal = (product: Product, defaultAction: 'archive' | 'permanent' = 'archive') => {
    setDeleteModalProduct(product);
    setDeletionAction(defaultAction);
    setPermanentConfirmChecked(false);
    setModalError(null);
    setCanArchiveInstead(false);
  };

  // Direct archive action
  const handleArchiveProduct = async (product: Product) => {
    try {
      const res = await api.archiveAdminProduct(product.id);
      setMessage({
        type: 'success',
        text: res.message || `Product "${product.name}" safely archived and unlisted from customer storefront.`,
      });
      fetchProducts(true);
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to archive product.' });
    }
  };

  // Direct restore action
  const handleRestoreProduct = async (product: Product) => {
    try {
      const res = await api.restoreAdminProduct(product.id);
      setMessage({
        type: 'success',
        text: res.message || `Product "${product.name}" restored and published back to active storefront.`,
      });
      fetchProducts(true);
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to restore product.' });
    }
  };

  // Execute confirmed modal action
  const executeRemovalAction = async () => {
    if (!deleteModalProduct || deleting) return;
    setDeleting(true);
    setModalError(null);
    setCanArchiveInstead(false);

    try {
      if (deletionAction === 'archive') {
        const res = await api.archiveAdminProduct(deleteModalProduct.id);
        setMessage({
          type: 'success',
          text: res.message || `Product "${deleteModalProduct.name}" archived and removed from storefront catalog.`,
        });
        setDeleteModalProduct(null);
        fetchProducts(true);
      } else {
        const res = await api.deleteAdminProduct(deleteModalProduct.id, true);
        setMessage({
          type: 'success',
          text: res.message || `Product "${deleteModalProduct.name}" permanently deleted from database.`,
        });
        setDeleteModalProduct(null);
        fetchProducts(true);
      }
    } catch (err: any) {
      const errorMsg = err.message || 'Failed to process product removal request.';
      setModalError(errorMsg);
      if (deletionAction === 'permanent' && (errorMsg.includes('historical') || errorMsg.includes('order') || errorMsg.includes('review') || errorMsg.includes('409'))) {
        setCanArchiveInstead(true);
      }
    } finally {
      setDeleting(false);
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
    <div className="min-h-screen bg-[#faf9f6] text-[#080808] pb-24 font-sans">
      <AdminNav currentTab="products" />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 space-y-6">
        {/* Breadcrumb & Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-[#777777] mb-1">
              <Link href="/admin" className="hover:text-[#080808]">Admin</Link>
              <span>/</span>
              <span className="text-[#080808]">Products</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#080808] font-serif">
              Product Listing & Management
            </h1>
            <p className="text-xs text-[#777777] mt-1">
              Primary REXXZO catalog engine powered directly by PostgreSQL. Create, price, publish and control inventory.
            </p>
          </div>

          <div className="flex items-center gap-2.5">
            <button
              onClick={() => fetchProducts(true)}
              disabled={refreshing}
              className="flex items-center gap-1.5 px-3 py-2 bg-white border border-[#e5e1d8] rounded-lg text-xs font-bold uppercase tracking-wider text-[#080808] hover:border-[#080808] transition-colors"
              title="Refresh from PostgreSQL"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
              <span className="hidden sm:inline">Sync</span>
            </button>

            <Link
              href="/admin/products/new"
              className="flex items-center gap-2 px-4 py-2.5 bg-[#080808] text-[#C8BCA7] hover:bg-[#222222] rounded-lg text-xs font-bold uppercase tracking-widest transition-all shadow-sm"
            >
              <Plus className="w-4 h-4" />
              <span>Add Product</span>
            </Link>
          </div>
        </div>

        {/* Global Alert Notification */}
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

        {/* Catalog Metrics Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="bg-white p-4 rounded-xl border border-[#e5e1d8] shadow-xs">
            <div className="text-[10px] font-bold uppercase tracking-widest text-[#777777]">Total Catalog</div>
            <div className="text-xl sm:text-2xl font-black text-[#080808] mt-1">{totalCount}</div>
            <div className="text-[10px] text-emerald-600 mt-0.5 flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
              PostgreSQL Stored
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-[#e5e1d8] shadow-xs">
            <div className="text-[10px] font-bold uppercase tracking-widest text-[#777777]">Active Products</div>
            <div className="text-xl sm:text-2xl font-black text-[#080808] mt-1">{activeCount}</div>
            <div className="text-[10px] text-[#777777] mt-0.5">Live on storefront</div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-[#e5e1d8] shadow-xs">
            <div className="text-[10px] font-bold uppercase tracking-widest text-[#777777]">Low Stock Alert</div>
            <div className="text-xl sm:text-2xl font-black text-amber-600 mt-1">{lowStockCount}</div>
            <div className="text-[10px] text-amber-700 mt-0.5">&lt;= Threshold items</div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-[#e5e1d8] shadow-xs">
            <div className="text-[10px] font-bold uppercase tracking-widest text-[#777777]">Out Of Stock</div>
            <div className="text-xl sm:text-2xl font-black text-rose-600 mt-1">{outOfStockCount}</div>
            <div className="text-[10px] text-rose-700 mt-0.5">Restock required</div>
          </div>
        </div>

        {/* Filter and Search Bar */}
        <div className="bg-white p-4 rounded-xl border border-[#e5e1d8] shadow-xs space-y-3">
          <div className="flex flex-col md:flex-row items-stretch md:items-center gap-3">
            {/* Search Box */}
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-[#888888] absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                placeholder="Search products by name, SKU, or keywords..."
                className="w-full pl-9 pr-3 py-2 bg-[#fcfbfa] border border-[#e5e1d8] rounded-lg text-xs placeholder-[#999999] focus:outline-none focus:border-[#080808] transition-colors"
              />
              {search && (
                <button
                  onClick={() => setSearch('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-[#888888] hover:text-[#080808]"
                >
                  ×
                </button>
              )}
            </div>

            {/* Category Filter */}
            <div className="w-full md:w-48">
              <select
                value={categoryId}
                onChange={(e) => {
                  setCategoryId(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 bg-[#fcfbfa] border border-[#e5e1d8] rounded-lg text-xs text-[#080808] font-medium focus:outline-none focus:border-[#080808]"
              >
                <option value="ALL">All Categories</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Status Filter */}
            <div className="w-full md:w-36">
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 bg-[#fcfbfa] border border-[#e5e1d8] rounded-lg text-xs text-[#080808] font-medium focus:outline-none focus:border-[#080808]"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="DRAFT">Draft</option>
                <option value="OUT_OF_STOCK">Out of Stock</option>
                <option value="ARCHIVED">Archived</option>
              </select>
            </div>

            {/* Stock Filter */}
            <div className="w-full md:w-36">
              <select
                value={stockFilter}
                onChange={(e) => {
                  setStockFilter(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 bg-[#fcfbfa] border border-[#e5e1d8] rounded-lg text-xs text-[#080808] font-medium focus:outline-none focus:border-[#080808]"
              >
                <option value="ALL">All Stock</option>
                <option value="IN_STOCK">In Stock</option>
                <option value="LOW_STOCK">Low Stock (&lt;=10)</option>
                <option value="OUT_OF_STOCK">Out of Stock (0)</option>
              </select>
            </div>

            {/* Sorting */}
            <div className="w-full md:w-44">
              <select
                value={`${sortBy}-${sortDir}`}
                onChange={(e) => {
                  const [field, dir] = e.target.value.split('-');
                  setSortBy(field);
                  setSortDir(dir);
                  setPage(0);
                }}
                className="w-full px-3 py-2 bg-[#fcfbfa] border border-[#e5e1d8] rounded-lg text-xs text-[#080808] font-medium focus:outline-none focus:border-[#080808]"
              >
                <option value="createdAt-desc">Newest First</option>
                <option value="createdAt-asc">Oldest First</option>
                <option value="price-asc">Price: Low to High</option>
                <option value="price-desc">Price: High to Low</option>
                <option value="stock-asc">Stock: Low to High</option>
                <option value="stock-desc">Stock: High to Low</option>
                <option value="name-asc">Name: A to Z</option>
              </select>
            </div>
          </div>
        </div>

        {/* Products Table */}
        <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                  <th className="p-3.5">Product & SKU</th>
                  <th className="p-3.5">Category</th>
                  <th className="p-3.5">Exact Selling Price</th>
                  <th className="p-3.5">MRP / Discount</th>
                  <th className="p-3.5">Stock Level</th>
                  <th className="p-3.5">Publish Status</th>
                  <th className="p-3.5">Created</th>
                  <th className="p-3.5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#f0ece5]">
                {loading ? (
                  <tr>
                    <td colSpan={8} className="p-12 text-center text-[#777777]">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <RefreshCw className="w-5 h-5 animate-spin text-[#080808]" />
                        <span>Querying PostgreSQL products...</span>
                      </div>
                    </td>
                  </tr>
                ) : products.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="p-12 text-center text-[#777777]">
                      <div className="flex flex-col items-center justify-center gap-2 max-w-sm mx-auto">
                        <Package className="w-8 h-8 text-[#cccccc]" />
                        <div className="font-bold text-[#080808]">No Products Found</div>
                        <p className="text-[11px] text-[#777777]">
                          No products match the selected search and filter criteria in PostgreSQL.
                        </p>
                        <Link
                          href="/admin/products/new"
                          className="mt-2 inline-flex items-center gap-1.5 px-3 py-1.5 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase tracking-wider hover:bg-[#222]"
                        >
                          <Plus className="w-3.5 h-3.5" />
                          Add First Product
                        </Link>
                      </div>
                    </td>
                  </tr>
                ) : (
                  products.map((p) => {
                    const primaryImg = p.images && p.images.length > 0
                      ? p.images[0].imageUrl
                      : '/images/hero_banner.jpg';

                    const stock = p.stock !== undefined ? p.stock : (p.stockQuantity !== undefined ? p.stockQuantity : 0);
                    const lowThreshold = p.lowStockThreshold || 10;
                    const isOutOfStock = stock <= 0 || p.status === 'OUT_OF_STOCK';
                    const isLowStock = !isOutOfStock && stock <= lowThreshold;

                    const status = p.status || (isOutOfStock ? 'OUT_OF_STOCK' : (p.isActive ? 'ACTIVE' : 'DRAFT'));
                    const mrp = p.originalPrice || p.compareAtPrice;

                    return (
                      <tr key={p.id} className="hover:bg-[#faf9f6] transition-colors">
                        {/* Image & Product Info */}
                        <td className="p-3.5">
                          <div className="flex items-center gap-3">
                            <div className="relative w-12 h-12 rounded-lg bg-[#f0ece5] overflow-hidden border border-[#e5e1d8] shrink-0">
                              <Image
                                src={primaryImg}
                                alt={p.name}
                                fill
                                sizes="48px"
                                className="object-cover"
                                onError={(e) => {
                                  // Fallback image if broken
                                  (e.target as any).src = '/images/hero_banner.jpg';
                                }}
                              />
                            </div>
                            <div className="min-w-0 max-w-xs sm:max-w-sm">
                              <div className="font-bold text-[#080808] text-[13px] truncate flex items-center gap-1.5">
                                <Link
                                  href={`/admin/products/${p.id}/edit`}
                                  className="hover:underline text-[#080808]"
                                  title={p.name}
                                >
                                  {p.name}
                                </Link>
                                {p.isFeatured && (
                                  <span className="text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.2 bg-[#C8BCA7] text-[#080808] rounded">
                                    Featured
                                  </span>
                                )}
                              </div>
                              <div className="flex items-center gap-2 mt-0.5 text-[11px] text-[#777777]">
                                <span className="font-mono bg-[#f4f1ea] px-1.5 py-0.2 rounded text-[#555555]">
                                  {p.sku || 'NO-SKU'}
                                </span>
                                <span className="truncate">/{p.slug}</span>
                              </div>
                            </div>
                          </div>
                        </td>

                        {/* Category */}
                        <td className="p-3.5">
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-[#f4f1ea] text-[#444444] border border-[#e5e1d8]">
                            <Tag className="w-2.5 h-2.5 text-[#888888]" />
                            {p.categoryName || p.category?.name || 'Uncategorized'}
                          </span>
                        </td>

                        {/* Exact Selling Price */}
                        <td className="p-3.5">
                          <div className="font-black text-[13px] text-[#080808]">
                            ₹{Number(p.price).toLocaleString('en-IN')}
                          </div>
                          <div className="text-[10px] text-[#888888]">Cart & Storefront</div>
                        </td>

                        {/* MRP / Discount */}
                        <td className="p-3.5">
                          {mrp && mrp > p.price ? (
                            <div>
                              <div className="line-through text-[#999999] text-[11px]">
                                ₹{Number(mrp).toLocaleString('en-IN')}
                              </div>
                              <div className="text-[10px] font-bold text-emerald-600">
                                {p.discountPercent
                                  ? `${p.discountPercent}% OFF`
                                  : `${Math.round(((mrp - p.price) / mrp) * 100)}% OFF`}
                              </div>
                            </div>
                          ) : (
                            <span className="text-[#aaaaaa]">—</span>
                          )}
                        </td>

                        {/* Stock */}
                        <td className="p-3.5">
                          <div className="flex items-center gap-1.5">
                            <span
                              className={`w-2 h-2 rounded-full ${
                                isOutOfStock
                                  ? 'bg-rose-500'
                                  : isLowStock
                                  ? 'bg-amber-500 animate-pulse'
                                  : 'bg-emerald-500'
                              }`}
                            ></span>
                            <span className="font-bold text-[12px] text-[#080808]">{stock}</span>
                            <span className="text-[10px] text-[#888888]">units</span>
                          </div>

                          {isOutOfStock ? (
                            <div className="text-[10px] font-bold text-rose-600 uppercase tracking-wider mt-0.5">
                              Out of Stock
                            </div>
                          ) : isLowStock ? (
                            <div className="text-[10px] font-bold text-amber-600 uppercase tracking-wider mt-0.5 flex items-center gap-0.5">
                              <AlertTriangle className="w-2.5 h-2.5" />
                              Low Stock (&lt;={lowThreshold})
                            </div>
                          ) : (
                            <div className="text-[10px] text-emerald-600 mt-0.5">Healthy</div>
                          )}
                        </td>

                        {/* Status */}
                        <td className="p-3.5">
                          <button
                            onClick={() => handleToggleStatus(p.id)}
                            title="Click to toggle status"
                            className={`px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase tracking-wider transition-colors border ${
                              status === 'ACTIVE'
                                ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                                : status === 'DRAFT'
                                ? 'bg-slate-100 text-slate-700 border-slate-300 hover:bg-slate-200'
                                : status === 'OUT_OF_STOCK'
                                ? 'bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100'
                                : 'bg-zinc-100 text-zinc-600 border-zinc-200 hover:bg-zinc-200'
                            }`}
                          >
                            {status}
                          </button>
                        </td>

                        {/* Created Date */}
                        <td className="p-3.5 text-[#777777] text-[11px] whitespace-nowrap">
                          {p.createdAt ? new Date(p.createdAt).toLocaleDateString('en-IN', {
                            day: 'numeric',
                            month: 'short',
                            year: 'numeric'
                          }) : '—'}
                        </td>

                        {/* Actions */}
                        <td className="p-3.5 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {/* Live View on Storefront */}
                            <Link
                              href={`/products/${p.slug || p.id}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              title="View on Storefront"
                              className="p-1.5 text-[#777777] hover:text-[#080808] hover:bg-[#f0ece5] rounded-md transition-colors"
                            >
                              <ExternalLink className="w-3.5 h-3.5" />
                            </Link>

                            {/* Edit Action */}
                            <Link
                              href={`/admin/products/${p.id}/edit`}
                              title="Edit Product"
                              className="p-1.5 text-[#777777] hover:text-[#080808] hover:bg-[#f0ece5] rounded-md transition-colors"
                            >
                              <Edit className="w-3.5 h-3.5" />
                            </Link>

                            {/* Archive / Restore Action */}
                            {status === 'ARCHIVED' ? (
                              <button
                                onClick={() => handleRestoreProduct(p)}
                                title="Restore to Active Storefront"
                                className="flex items-center gap-1 px-2 py-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-md transition-colors"
                              >
                                <RotateCcw className="w-3 h-3" />
                                <span>Restore</span>
                              </button>
                            ) : (
                              <button
                                onClick={() => openRemovalModal(p, 'archive')}
                                title="Archive / Unlist from Storefront"
                                className="flex items-center gap-1 px-2 py-1 text-[11px] font-bold text-[#666666] hover:text-[#080808] bg-zinc-50 hover:bg-zinc-100 border border-zinc-200 rounded-md transition-colors"
                              >
                                <Archive className="w-3 h-3" />
                                <span>Archive</span>
                              </button>
                            )}

                            {/* Delete Action */}
                            <button
                              onClick={() => openRemovalModal(p, 'permanent')}
                              title="Delete Product (Safe / Permanent)"
                              className="p-1.5 text-rose-600 hover:text-rose-700 hover:bg-rose-50 rounded-md transition-colors"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Bar */}
          <div className="p-3.5 bg-[#f9f8f6] border-t border-[#e5e1d8] flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
            <div className="text-[#777777]">
              Showing <span className="font-bold text-[#080808]">{products.length}</span> of{' '}
              <span className="font-bold text-[#080808]">{totalElements}</span> products in PostgreSQL
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                disabled={page === 0 || loading}
                className="flex items-center gap-1 px-3 py-1.5 bg-white border border-[#e5e1d8] rounded-md font-bold uppercase tracking-wider text-[11px] text-[#080808] hover:border-[#080808] disabled:opacity-40 disabled:pointer-events-none transition-colors"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                <span>Prev</span>
              </button>

              <span className="px-2 text-xs font-bold text-[#080808]">
                Page {page + 1} of {Math.max(totalPages, 1)}
              </span>

              <button
                onClick={() => setPage((prev) => Math.min(prev + 1, totalPages - 1))}
                disabled={page >= totalPages - 1 || loading}
                className="flex items-center gap-1 px-3 py-1.5 bg-white border border-[#e5e1d8] rounded-md font-bold uppercase tracking-wider text-[11px] text-[#080808] hover:border-[#080808] disabled:opacity-40 disabled:pointer-events-none transition-colors"
              >
                <span>Next</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      </main>

      {/* Delete / Archive Confirmation Modal */}
      {deleteModalProduct && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-[#e5e1d8] space-y-4 animate-scaleUp">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${
                  deletionAction === 'archive' ? 'bg-amber-100 text-amber-700' : 'bg-rose-100 text-rose-600'
                }`}>
                  {deletionAction === 'archive' ? <Archive className="w-5 h-5" /> : <Trash2 className="w-5 h-5" />}
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-[#080808]">
                    {deletionAction === 'archive' ? 'Archive Product Listing' : 'Permanently Delete Product'}
                  </h3>
                  <p className="text-xs text-[#777777]">PostgreSQL database integrity &amp; storefront visibility</p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setDeleteModalProduct(null)}
                disabled={deleting}
                className="text-[#888888] hover:text-[#080808] text-sm p-1 rounded-md"
              >
                ✕
              </button>
            </div>

            {/* Product Summary Box */}
            <div className="p-3.5 bg-[#faf9f6] rounded-xl border border-[#e5e1d8] text-xs space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="font-extrabold text-sm text-[#080808]">{deleteModalProduct.name}</span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                  deleteModalProduct.status === 'ARCHIVED'
                    ? 'bg-zinc-200 text-zinc-800'
                    : deleteModalProduct.status === 'ACTIVE'
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-slate-100 text-slate-800'
                }`}>
                  {deleteModalProduct.status || (deleteModalProduct.isActive ? 'ACTIVE' : 'DRAFT')}
                </span>
              </div>
              <div className="text-[#666666] flex items-center gap-4 text-[11px]">
                <span>SKU: <strong className="text-[#080808]">{deleteModalProduct.sku || 'N/A'}</strong></span>
                <span>Price: <strong className="text-[#080808]">₹{deleteModalProduct.price?.toLocaleString()}</strong></span>
                <span>Stock: <strong className="text-[#080808]">{deleteModalProduct.stock ?? deleteModalProduct.stockQuantity ?? 0} units</strong></span>
              </div>
            </div>

            {/* Strategy Selection Toggle */}
            <div className="grid grid-cols-2 gap-2 p-1 bg-[#f4f2ee] rounded-xl">
              <button
                type="button"
                onClick={() => {
                  setDeletionAction('archive');
                  setModalError(null);
                  setCanArchiveInstead(false);
                }}
                disabled={deleting}
                className={`flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg text-xs font-bold transition-all ${
                  deletionAction === 'archive'
                    ? 'bg-white text-[#080808] shadow-xs'
                    : 'text-[#666666] hover:text-[#080808]'
                }`}
              >
                <Archive className="w-3.5 h-3.5 text-amber-600" />
                <span>Archive / Unlist</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setDeletionAction('permanent');
                  setPermanentConfirmChecked(false);
                  setModalError(null);
                  setCanArchiveInstead(false);
                }}
                disabled={deleting}
                className={`flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg text-xs font-bold transition-all ${
                  deletionAction === 'permanent'
                    ? 'bg-white text-rose-600 shadow-xs'
                    : 'text-[#666666] hover:text-[#080808]'
                }`}
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Permanent Delete</span>
              </button>
            </div>

            {/* Action Explanation Details */}
            {deletionAction === 'archive' ? (
              <div className="p-3 bg-amber-50/70 border border-amber-200/80 rounded-xl text-xs space-y-1 text-amber-900">
                <div className="font-bold flex items-center gap-1 text-amber-950">
                  <CheckCircle className="w-3.5 h-3.5 text-amber-600" />
                  <span>Safe Removal Strategy (Recommended)</span>
                </div>
                <ul className="list-disc list-inside space-y-0.5 text-[11px] text-amber-800 ml-1">
                  <li>Removes the product from customer storefront, search, and new purchases immediately.</li>
                  <li>Preserves customer past orders, tax invoices, accounting records, and reviews intact.</li>
                  <li>Can be safely restored to the active catalog at any time with one click.</li>
                </ul>
              </div>
            ) : (
              <div className="p-3 bg-rose-50/80 border border-rose-200 rounded-xl text-xs space-y-2 text-rose-950">
                <div className="font-bold flex items-center gap-1 text-rose-800">
                  <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0" />
                  <span>Permanent Database Deletion</span>
                </div>
                <p className="text-[11px] text-rose-800 leading-relaxed">
                  Permitted <strong>only</strong> if this product has no historical orders, invoices, or customer reviews in PostgreSQL. This cannot be undone.
                </p>

                <label className="flex items-start gap-2 pt-1 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={permanentConfirmChecked}
                    onChange={(e) => setPermanentConfirmChecked(e.target.checked)}
                    disabled={deleting}
                    className="mt-0.5 rounded text-rose-600 focus:ring-rose-500"
                  />
                  <span className="text-[11px] text-[#444444] font-medium">
                    I confirm that I want to permanently delete <strong>{deleteModalProduct.name}</strong> from the database.
                  </span>
                </label>
              </div>
            )}

            {/* Error Message & Conflict Resolution */}
            {modalError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs space-y-2 text-rose-900">
                <div className="flex items-start gap-2">
                  <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <span className="leading-relaxed">{modalError}</span>
                </div>
                {canArchiveInstead && (
                  <button
                    type="button"
                    onClick={() => {
                      setDeletionAction('archive');
                      setModalError(null);
                      setCanArchiveInstead(false);
                    }}
                    className="w-full py-1.5 px-3 bg-amber-600 hover:bg-amber-700 text-white font-bold rounded-lg text-xs uppercase tracking-wider transition-colors"
                  >
                    Switch to Safe Archive / Unlist Instead
                  </button>
                )}
              </div>
            )}

            {/* Modal Action Buttons */}
            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-[#f0ece5]">
              <button
                type="button"
                onClick={() => setDeleteModalProduct(null)}
                disabled={deleting}
                className="px-4 py-2 border border-[#e5e1d8] rounded-lg text-xs font-bold uppercase tracking-wider text-[#555] hover:text-[#080808] transition-colors"
              >
                Cancel
              </button>

              <button
                type="button"
                onClick={executeRemovalAction}
                disabled={deleting || (deletionAction === 'permanent' && !permanentConfirmChecked)}
                className={`flex items-center gap-1.5 px-4 py-2 text-white rounded-lg text-xs font-bold uppercase tracking-wider transition-all disabled:opacity-50 disabled:cursor-not-allowed ${
                  deletionAction === 'archive'
                    ? 'bg-[#080808] hover:bg-[#222222]'
                    : 'bg-rose-600 hover:bg-rose-700'
                }`}
              >
                {deleting ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    <span>Processing...</span>
                  </>
                ) : deletionAction === 'archive' ? (
                  <>
                    <Archive className="w-3.5 h-3.5" />
                    <span>Confirm Archive / Unlist</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Permanently Delete</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
