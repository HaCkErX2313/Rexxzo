'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import {
  LayoutDashboard,
  ShoppingBag,
  Package,
  Users,
  RotateCcw,
  CreditCard,
  Star,
  BarChart3,
  Truck,
  Settings,
  ShieldCheck,
  Search,
  Plus,
  RefreshCw,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Clock,
  ArrowRight,
  Sliders,
  DollarSign,
  Layers,
  FileText,
  Tag,
  LogOut,
  UserPlus,
  Shield
} from 'lucide-react';
import { api, DEMO_CATEGORIES } from '@/lib/api';
import { AdminDashboardData, AnalyticsSummary, AdminIntegrationsStatus, ReturnRequest, Refund, Review, Order, Product, Category } from '@/types';
import { useStore } from '@/context/StoreContext';

type AdminTab =
  | 'dashboard'
  | 'products'
  | 'categories'
  | 'inventory'
  | 'orders'
  | 'returns'
  | 'refunds'
  | 'reviews'
  | 'analytics'
  | 'shipments'
  | 'integrations'
  | 'settings'
  | 'audit';

export default function AdminPage({ initialTab }: { initialTab?: AdminTab } = {}) {
  const router = useRouter();
  const { user } = useStore();
  const [adminUser, setAdminUser] = useState<{ email: string; name: string; role: string } | null>(null);
  const [authChecked, setAuthChecked] = useState(false);
  const [activeTab, setActiveTab] = useState<AdminTab>(initialTab || 'dashboard');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Data states
  const [dashboardData, setDashboardData] = useState<AdminDashboardData | null>(null);
  const [orders, setOrders] = useState<any[]>([]);
  const [orderFilter, setOrderFilter] = useState('ALL');
  const [orderSearch, setOrderSearch] = useState('');
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [inventory, setInventory] = useState<any>(null);
  const [returns, setReturns] = useState<ReturnRequest[]>([]);
  const [returnFilter, setReturnFilter] = useState('ALL');
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewFilter, setReviewFilter] = useState('ALL');
  const [analytics, setAnalytics] = useState<AnalyticsSummary | null>(null);
  const [analyticsRange, setAnalyticsRange] = useState('30d');
  const [integrations, setIntegrations] = useState<AdminIntegrationsStatus | null>(null);
  const [auditLogs, setAuditLogs] = useState<any[]>([]);

  // Modals & Action Forms
  const [isProductModalOpen, setIsProductModalOpen] = useState(false);
  const [isCreateAdminModalOpen, setIsCreateAdminModalOpen] = useState(false);
  const [newAdmin, setNewAdmin] = useState({ name: '', email: '', password: '' });
  const [creatingAdmin, setCreatingAdmin] = useState(false);
  const [newProduct, setNewProduct] = useState({
    name: '',
    description: '',
    price: '',
    compareAtPrice: '',
    categoryName: 'Home Decor',
    stockQuantity: '20',
    sku: '',
    imageUrl: '',
    weightGrams: 500,
    materials: '',
    dimensions: '',
    isFeatured: false,
  });

  const [selectedOrder, setSelectedOrder] = useState<any | null>(null);
  const [selectedReturn, setSelectedReturn] = useState<ReturnRequest | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [pickupCourier, setPickupCourier] = useState('Shiprocket Partner');
  const [pickupAwb, setPickupAwb] = useState('');
  const [stockEditId, setStockEditId] = useState<number | null>(null);
  const [newStockVal, setNewStockVal] = useState<number>(0);

  // Authentication check on mount
  useEffect(() => {
    // Strictly verify admin token; never fall back to customer token
    const token = typeof window !== 'undefined'
      ? localStorage.getItem('rexxo_admin_token')
      : null;

    if (!token) {
      router.push('/admin/login');
      return;
    }

    const currentAdmin = api.getAdminUser();
    setAdminUser(currentAdmin);
    setAuthChecked(true);

    if (initialTab) {
      setActiveTab(initialTab);
      return;
    }

    const pathname = window.location.pathname;
    const searchParams = new URLSearchParams(window.location.search);
    const tabParam = searchParams.get('tab');

    if (tabParam === 'products' || pathname === '/admin/products') {
      router.push('/admin/products');
    } else if (tabParam) {
      setActiveTab(tabParam as AdminTab);
    } else if (pathname.includes('/categories')) {
      setActiveTab('categories');
    } else if (pathname.includes('/inventory')) {
      setActiveTab('inventory');
    } else if (pathname.includes('/orders')) {
      setActiveTab('orders');
    } else if (pathname.includes('/returns')) {
      setActiveTab('returns');
    } else if (pathname.includes('/refunds')) {
      setActiveTab('refunds');
    } else if (pathname.includes('/reviews')) {
      setActiveTab('reviews');
    } else if (pathname.includes('/analytics')) {
      setActiveTab('analytics');
    } else if (pathname.includes('/shipments')) {
      setActiveTab('shipments');
    } else if (pathname.includes('/integrations')) {
      setActiveTab('integrations');
    } else if (pathname.includes('/settings')) {
      setActiveTab('settings');
    } else if (pathname.includes('/audit')) {
      setActiveTab('audit');
    }
  }, [router, initialTab]);

  // Load active tab data
  const loadTabData = useCallback(async () => {
    setLoading(true);
    try {
      if (activeTab === 'dashboard') {
        const d = await api.getAdminDashboard();
        setDashboardData(d);
      } else if (activeTab === 'orders' || activeTab === 'shipments') {
        const ords = await api.getAdminOrders({ status: orderFilter, search: orderSearch });
        setOrders(ords.content || []);
      } else if (activeTab === 'products') {
        const prods = await api.getAdminProducts();
        setProducts(prods.content || []);
      } else if (activeTab === 'categories') {
        const cats = await api.getAdminCategories();
        setCategories(cats);
      } else if (activeTab === 'inventory') {
        const inv = await api.getAdminInventory();
        setInventory(inv);
      } else if (activeTab === 'returns') {
        const rets = await api.getAdminReturns(returnFilter);
        setReturns(rets.content || []);
      } else if (activeTab === 'refunds') {
        const refs = await api.getAdminRefunds();
        setRefunds(refs.content || []);
      } else if (activeTab === 'reviews') {
        const revs = await api.getAdminReviews(reviewFilter);
        setReviews(revs.content || []);
      } else if (activeTab === 'analytics') {
        const an = await api.getAdminAnalytics(analyticsRange);
        setAnalytics(an);
      } else if (activeTab === 'integrations') {
        const intg = await api.getAdminIntegrations();
        setIntegrations(intg);
      } else if (activeTab === 'audit') {
        const logs = await api.getAdminAuditLogs();
        setAuditLogs(logs.content || []);
      }
    } catch (err: any) {
      const errorMsg = err.message || 'Failed to load module data';
      setMessage({ type: 'error', text: errorMsg });
      if (errorMsg.includes('expired') || errorMsg.includes('log in again')) {
        setTimeout(() => router.push('/admin/login'), 1200);
      }
    } finally {
      setLoading(false);
    }
  }, [activeTab, orderFilter, orderSearch, returnFilter, reviewFilter, analyticsRange, router]);

  useEffect(() => {
    if (authChecked) {
      loadTabData();
    }
  }, [loadTabData, authChecked]);

  // Actions
  const handleToggleProduct = async (id: number) => {
    try {
      await api.toggleAdminProduct(id);
      loadTabData();
      setMessage({ type: 'success', text: 'Product active status updated.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Action failed' });
    }
  };

  const handleCreateSubAdmin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newAdmin.name || !newAdmin.email || !newAdmin.password) return;
    setCreatingAdmin(true);
    try {
      await api.createAdmin(newAdmin);
      setIsCreateAdminModalOpen(false);
      setNewAdmin({ name: '', email: '', password: '' });
      setMessage({ type: 'success', text: 'New administrator provisioned successfully.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to create administrator' });
    } finally {
      setCreatingAdmin(false);
    }
  };

  const handleToggleCategory = async (id: number) => {
    try {
      await api.toggleCategory(id);
      loadTabData();
      setMessage({ type: 'success', text: 'Category active status updated successfully.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Action failed' });
    }
  };

  const handleUpdateProductCategory = async (productId: number, newCatId: number) => {
    try {
      await api.updateProductCategory(productId, newCatId);
      loadTabData();
      setMessage({ type: 'success', text: 'Product category reassigned successfully.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to update product category' });
    }
  };

  const handleAdjustStock = async (id: number) => {
    try {
      await api.adjustAdminStock(id, newStockVal);
      setStockEditId(null);
      loadTabData();
      setMessage({ type: 'success', text: 'Inventory stock adjusted successfully.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to adjust stock' });
    }
  };

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProduct.name || !newProduct.price) return;
    try {
      await api.createProduct({
        name: newProduct.name,
        description: newProduct.description,
        price: Number(newProduct.price),
        compareAtPrice: newProduct.compareAtPrice ? Number(newProduct.compareAtPrice) : undefined,
        categoryName: newProduct.categoryName,
        stockQuantity: Number(newProduct.stockQuantity),
        sku: newProduct.sku || `REX-${Math.floor(Math.random() * 900) + 100}`,
        materials: newProduct.materials,
        dimensions: newProduct.dimensions,
        isFeatured: newProduct.isFeatured,
        images: [{ imageUrl: newProduct.imageUrl || '/images/hero_banner.jpg', isPrimary: true }],
      });
      setIsProductModalOpen(false);
      loadTabData();
      setMessage({ type: 'success', text: 'Product created successfully.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to create product' });
    }
  };

  const handleApproveReturn = async (returnId: number) => {
    try {
      await api.approveAdminReturn(returnId, 'Approved by Store Administrator');
      loadTabData();
      setSelectedReturn(null);
      setMessage({ type: 'success', text: 'Return request approved. Ready for pickup.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to approve return' });
    }
  };

  const handleRejectReturn = async (returnId: number) => {
    if (!rejectionReason.trim()) {
      setMessage({ type: 'error', text: 'Please enter a rejection reason.' });
      return;
    }
    try {
      await api.rejectAdminReturn(returnId, rejectionReason);
      loadTabData();
      setSelectedReturn(null);
      setRejectionReason('');
      setMessage({ type: 'success', text: 'Return request rejected.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to reject return' });
    }
  };

  const handleSchedulePickup = async (returnId: number) => {
    if (!pickupAwb.trim()) {
      setMessage({ type: 'error', text: 'Please enter an AWB number.' });
      return;
    }
    try {
      await api.scheduleAdminReturnPickup(returnId, pickupCourier, pickupAwb);
      loadTabData();
      setSelectedReturn(null);
      setPickupAwb('');
      setMessage({ type: 'success', text: 'Return pickup scheduled.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to schedule pickup' });
    }
  };

  const handleReceiveReturn = async (returnId: number) => {
    try {
      await api.receiveAdminReturn(returnId);
      loadTabData();
      setSelectedReturn(null);
      setMessage({ type: 'success', text: 'Return marked as received. Pending refund approval.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to receive return' });
    }
  };

  const handleApproveRefund = async (returnId: number) => {
    try {
      await api.approveAdminRefund(returnId);
      loadTabData();
      setSelectedReturn(null);
      setMessage({ type: 'success', text: 'Refund approved and recorded in pending queue.' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to approve refund' });
    }
  };

  const handleModerateReview = async (reviewId: number, status: 'APPROVED' | 'REJECTED') => {
    try {
      await api.moderateAdminReview(reviewId, status);
      loadTabData();
      setMessage({ type: 'success', text: `Review marked as ${status}.` });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to moderate review' });
    }
  };

  const handleUpdateOrderStatus = async (orderId: number, status: string) => {
    try {
      await api.updateAdminOrderStatus(orderId, status);
      loadTabData();
      setMessage({ type: 'success', text: `Order #${orderId} status updated to ${status}.` });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.message || 'Failed to update order status' });
    }
  };

  return (
    <div className="min-h-screen bg-[#fcfbf9] text-[#080808]">
      {/* Top Header */}
      <div className="bg-white border-b border-[#e5e1d8] sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-[#080808] text-[#C8BCA7] flex items-center justify-center font-bold text-sm tracking-wider">
              R
            </div>
            <div>
              <h1 className="text-sm font-extrabold uppercase tracking-widest text-[#080808]">
                REXXZO Operational Console
              </h1>
              <span className="text-[10px] text-[#8c7f69] uppercase tracking-wider block">
                Production Administration & Security
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Authenticated Admin Badge */}
            <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 bg-[#FAF9F7] border border-[#e5e1d8] rounded text-xs">
              <Shield className="w-3.5 h-3.5 text-[#8c7f69]" />
              <span className="font-semibold text-[#080808] max-w-[150px] truncate">
                {adminUser?.email || 'admin@rexxzo.in'}
              </span>
              <span className="text-[9px] bg-[#080808] text-[#C8BCA7] px-1.5 py-0.5 rounded font-bold uppercase tracking-wider">
                ADMIN
              </span>
            </div>

            <button
              onClick={() => setIsCreateAdminModalOpen(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-white border border-[#e5e1d8] rounded text-xs font-semibold text-[#080808] hover:border-[#080808] transition-colors"
              title="Create another administrator"
            >
              <UserPlus className="w-3.5 h-3.5 text-[#8c7f69]" />
              <span className="hidden md:inline">Add Admin</span>
            </button>

            <button
              onClick={() => loadTabData()}
              className="flex items-center gap-1.5 px-3 py-1.5 border border-[#e5e1d8] rounded text-xs font-semibold text-[#555555] hover:border-[#080808] transition-colors"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span className="hidden sm:inline">Refresh</span>
            </button>

            <Link
              href="/shop"
              className="px-3 py-1.5 bg-white border border-[#e5e1d8] text-[#080808] rounded text-xs font-bold uppercase tracking-wider hover:border-[#080808] transition-colors"
            >
              Storefront
            </Link>

            <button
              onClick={() => api.adminLogout()}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-[#080808] text-[#C8BCA7] hover:bg-[#222222] rounded text-xs font-bold uppercase tracking-wider transition-colors cursor-pointer"
              title="Sign out of Admin Console"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Logout</span>
            </button>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 overflow-x-auto flex space-x-1 border-t border-[#f0ece5] text-xs">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
            { id: 'orders', label: 'Orders', icon: ShoppingBag },
            { id: 'products', label: 'Products', icon: Package },
            { id: 'categories', label: 'Categories', icon: Tag },
            { id: 'inventory', label: 'Inventory', icon: Layers },
            { id: 'returns', label: 'Returns', icon: RotateCcw },
            { id: 'refunds', label: 'Refunds', icon: CreditCard },
            { id: 'reviews', label: 'Reviews', icon: Star },
            { id: 'analytics', label: 'Analytics', icon: BarChart3 },
            { id: 'shipments', label: 'Shipments', icon: Truck },
            { id: 'integrations', label: 'Integrations', icon: Sliders },
            { id: 'settings', label: 'Settings', icon: Settings },
            { id: 'audit', label: 'Audit Logs', icon: FileText },
          ].map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => {
                  if (tab.id === 'products') {
                    router.push('/admin/products');
                  } else {
                    setActiveTab(tab.id as AdminTab);
                    setMessage(null);
                  }
                }}
                className={`flex items-center gap-2 py-3 px-3.5 border-b-2 font-bold uppercase tracking-wider text-[11px] whitespace-nowrap transition-colors ${
                  isActive
                    ? 'border-[#080808] text-[#080808]'
                    : 'border-transparent text-[#777777] hover:text-[#080808]'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Global Notification Banner */}
      {message && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4">
          <div
            className={`p-3 rounded-xl border flex items-center justify-between text-xs font-medium ${
              message.type === 'success'
                ? 'bg-emerald-50 text-emerald-900 border-emerald-200'
                : 'bg-red-50 text-red-900 border-red-200'
            }`}
          >
            <div className="flex items-center gap-2">
              {message.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              ) : (
                <AlertCircle className="w-4 h-4 text-red-600" />
              )}
              <span>{message.text}</span>
            </div>
            <button onClick={() => setMessage(null)} className="text-gray-400 hover:text-gray-700">
              <XCircle className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Main Content Area */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* ─── 1. DASHBOARD OVERVIEW ────────────────────────────────────────── */}
        {activeTab === 'dashboard' && dashboardData && (
          <div className="space-y-8">
            {/* KPI Cards Grid */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 shadow-sm space-y-2">
                <span className="text-[10px] uppercase tracking-widest text-[#8c7f69] font-bold block">
                  Total Revenue
                </span>
                <p className="text-2xl font-extrabold text-[#080808]">
                  ₹{(dashboardData.totalRevenue ?? 0).toLocaleString()}
                </p>
                <span className="text-[10px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded font-bold inline-block">
                  From paid PostgreSQL orders
                </span>
              </div>

              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 shadow-sm space-y-2">
                <span className="text-[10px] uppercase tracking-widest text-[#8c7f69] font-bold block">
                  Total Orders
                </span>
                <p className="text-2xl font-extrabold text-[#080808]">
                  {dashboardData.totalOrders ?? 0}
                </p>
                <div className="flex gap-2 text-[10px] text-[#777777]">
                  <span>Pending: {dashboardData.pendingOrders ?? 0}</span>
                  <span>·</span>
                  <span>Delivered: {dashboardData.deliveredOrders ?? 0}</span>
                </div>
              </div>

              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 shadow-sm space-y-2">
                <span className="text-[10px] uppercase tracking-widest text-[#8c7f69] font-bold block">
                  Returns & Refunds
                </span>
                <p className="text-2xl font-extrabold text-amber-900">
                  {dashboardData.returnRequests ?? 0}
                </p>
                <span className="text-[10px] text-amber-700 bg-amber-50 px-2 py-0.5 rounded font-bold inline-block">
                  Refunds: ₹{(dashboardData.refundAmount ?? 0).toLocaleString()}
                </span>
              </div>

              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 shadow-sm space-y-2">
                <span className="text-[10px] uppercase tracking-widest text-[#8c7f69] font-bold block">
                  Stock Alerts
                </span>
                <p className="text-2xl font-extrabold text-red-700">
                  {dashboardData.outOfStockProducts ?? 0} Out
                </p>
                <span className="text-[10px] text-red-700 bg-red-50 px-2 py-0.5 rounded font-bold inline-block">
                  {dashboardData.lowStockProducts ?? 0} Low Stock Items
                </span>
              </div>
            </div>

            {/* Recent Orders Overview */}
            <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#080808]">
                  Recent Activity & Orders
                </h3>
                <button
                  onClick={() => setActiveTab('orders')}
                  className="text-xs text-[#8c7f69] hover:text-[#080808] font-bold uppercase tracking-wider flex items-center gap-1"
                >
                  View All Orders <ArrowRight className="w-3 h-3" />
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-[#f0ece5] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                      <th className="py-2.5">Order</th>
                      <th>Customer</th>
                      <th>Status</th>
                      <th>Payment</th>
                      <th>Courier / AWB</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#f7f5f0]">
                    {dashboardData.recentOrders?.map((ord) => (
                      <tr key={ord.id} className="hover:bg-[#faf9f6]">
                        <td className="py-3 font-bold text-[#080808]">{ord.orderNumber}</td>
                        <td className="text-[#555555]">
                          {(ord as any).customerName || (ord as any).user?.name || 'Customer'}
                        </td>
                        <td>
                          <span className="bg-gray-100 text-gray-800 text-[10px] font-bold px-2 py-0.5 rounded">
                            {ord.status}
                          </span>
                        </td>
                        <td>
                          <span className="text-[11px] text-[#555555]">
                            {ord.paymentStatus || 'PENDING'} ({ord.paymentMethod || 'Online'})
                          </span>
                        </td>
                        <td className="text-[#777777]">
                          {ord.awbNumber ? `${ord.courierName || 'Courier'} (${ord.awbNumber})` : '—'}
                        </td>
                        <td className="font-extrabold text-[#080808]">
                          ₹{(ord.totalAmount || (ord as any).total || 0).toLocaleString()}
                        </td>
                      </tr>
                    ))}
                    {(!dashboardData.recentOrders || dashboardData.recentOrders.length === 0) && (
                      <tr>
                        <td colSpan={6} className="py-8 text-center text-[#888888]">
                          No orders yet
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* ─── 2. ORDERS MANAGEMENT ────────────────────────────────────────── */}
        {(activeTab === 'orders' || activeTab === 'shipments') && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                  {activeTab === 'shipments' ? 'Shipments & Tracking' : 'Orders Management'}
                </h2>
                <p className="text-xs text-[#777777]">
                  Monitor real-time fulfillment, AWB tracking, and order states.
                </p>
              </div>

              <div className="flex items-center gap-3">
                <div className="relative">
                  <Search className="w-3.5 h-3.5 text-[#888888] absolute left-3 top-2.5" />
                  <input
                    type="text"
                    placeholder="Search by order or phone..."
                    value={orderSearch}
                    onChange={(e) => setOrderSearch(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && loadTabData()}
                    className="pl-8 pr-3 py-1.5 border border-[#d5d0c5] rounded text-xs bg-white focus:outline-none focus:border-[#080808]"
                  />
                </div>

                <select
                  value={orderFilter}
                  onChange={(e) => setOrderFilter(e.target.value)}
                  className="px-3 py-1.5 border border-[#d5d0c5] rounded text-xs bg-white font-semibold"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="PENDING">Pending</option>
                  <option value="PAID">Paid</option>
                  <option value="PROCESSING">Processing</option>
                  <option value="SHIPPED">Shipped</option>
                  <option value="DELIVERED">Delivered</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            </div>

            <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                    <th className="p-3">Order #</th>
                    <th className="p-3">Customer</th>
                    <th className="p-3">Status</th>
                    <th className="p-3">Payment</th>
                    <th className="p-3">Shipment / AWB</th>
                    <th className="p-3">Total</th>
                    <th className="p-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0ece5]">
                  {orders.map((ord) => (
                    <tr key={ord.id} className="hover:bg-[#faf9f6]">
                      <td className="p-3 font-bold text-[#080808]">{ord.orderNumber}</td>
                      <td className="p-3">
                        <div className="font-semibold text-[#080808]">{ord.customerName}</div>
                        <div className="text-[11px] text-[#777777]">{ord.customerPhone}</div>
                      </td>
                      <td className="p-3">
                        <span className="bg-blue-50 text-blue-900 border border-blue-200 text-[10px] font-bold px-2 py-0.5 rounded">
                          {ord.status}
                        </span>
                      </td>
                      <td className="p-3 text-[11px]">
                        <span className="font-bold">{ord.paymentStatus}</span> ({ord.paymentMethod})
                      </td>
                      <td className="p-3 text-[11px] text-[#666666]">
                        {ord.awb ? (
                          <div>
                            <span className="font-bold text-[#080808]">{ord.courier}</span>
                            <span className="block text-[#888888]">AWB: {ord.awb}</span>
                          </div>
                        ) : (
                          <span className="text-[#aaaaaa]">Pending fulfillment</span>
                        )}
                      </td>
                      <td className="p-3 font-extrabold text-[#080808]">
                        ₹{(ord.total || ord.totalAmount || 0).toLocaleString()}
                      </td>
                      <td className="p-3 text-right space-x-2">
                        <select
                          value={ord.status}
                          onChange={(e) => handleUpdateOrderStatus(ord.id, e.target.value)}
                          className="px-2 py-1 border border-[#ccc] rounded text-[11px] bg-white"
                        >
                          <option value="PENDING">PENDING</option>
                          <option value="PAID">PAID</option>
                          <option value="PROCESSING">PROCESSING</option>
                          <option value="PACKED">PACKED</option>
                          <option value="SHIPPED">SHIPPED</option>
                          <option value="DELIVERED">DELIVERED</option>
                          <option value="CANCELLED">CANCELLED</option>
                        </select>
                      </td>
                    </tr>
                  ))}
                  {orders.length === 0 && (
                    <tr>
                      <td colSpan={7} className="p-8 text-center text-[#888888]">
                        No orders matching the selected filter.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ─── 3. PRODUCTS & CATALOG ───────────────────────────────────────── */}
        {activeTab === 'products' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                  Product Catalog
                </h2>
                <p className="text-xs text-[#777777]">
                  Manage items, prices, active status, and catalog visibility.
                </p>
              </div>
              <button
                onClick={() => setIsProductModalOpen(true)}
                className="flex items-center gap-1.5 px-4 py-2 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase tracking-wider hover:bg-[#222222]"
              >
                <Plus className="w-3.5 h-3.5" />
                Add Product
              </button>
            </div>

            <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                    <th className="p-3">Object</th>
                    <th className="p-3">SKU</th>
                    <th className="p-3">Category</th>
                    <th className="p-3">Price</th>
                    <th className="p-3">Stock</th>
                    <th className="p-3">Status</th>
                    <th className="p-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0ece5]">
                  {products.map((p) => (
                    <tr key={p.id} className="hover:bg-[#faf9f6]">
                      <td className="p-3">
                        <div className="font-bold text-[#080808]">{p.name}</div>
                        <div className="text-[11px] text-[#777777] line-clamp-1">{p.description}</div>
                      </td>
                      <td className="p-3 text-[11px] text-[#555555]">{p.sku}</td>
                      <td className="p-3 text-[11px]">
                        <select
                          value={p.category?.id || p.categoryId || ''}
                          onChange={(e) => handleUpdateProductCategory(p.id, Number(e.target.value))}
                          className="px-2 py-1 bg-white border border-[#d5d0c5] rounded text-[11px] font-medium text-[#080808] hover:border-[#080808] focus:outline-none"
                          title="Assign or reassign product category"
                        >
                          {DEMO_CATEGORIES.map((c) => (
                            <option key={c.id} value={c.id}>
                              {c.name}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="p-3 font-extrabold text-[#080808]">
                        ₹{(p.price ?? 0).toLocaleString()}
                        {p.compareAtPrice && p.compareAtPrice > p.price && (
                          <span className="block text-[10px] text-[#888888] line-through font-normal">
                            ₹{(p.compareAtPrice ?? 0).toLocaleString()}
                          </span>
                        )}
                      </td>
                      <td className="p-3 font-semibold">
                        <span className={p.stockQuantity < 10 ? 'text-red-700 font-bold' : 'text-[#080808]'}>
                          {p.stockQuantity} in stock
                        </span>
                      </td>
                      <td className="p-3">
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                            p.isActive
                              ? 'bg-emerald-50 text-emerald-900 border border-emerald-200'
                              : 'bg-gray-100 text-gray-700'
                          }`}
                        >
                          {p.isActive ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                      </td>
                      <td className="p-3 text-right">
                        <button
                          onClick={() => handleToggleProduct(p.id)}
                          className="px-2.5 py-1 border border-[#ccc] rounded text-[11px] font-semibold text-[#555555] hover:border-[#080808]"
                        >
                          {p.isActive ? 'Deactivate' : 'Activate'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ─── CATEGORIES MANAGEMENT ───────────────────────────────────────── */}
        {activeTab === 'categories' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                  Category Architecture & Navigation
                </h2>
                <p className="text-xs text-[#777777]">
                  Manage store categories including Gift Corner, active status, and catalog hierarchy.
                </p>
              </div>
            </div>

            <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                    <th className="p-3">Category Name</th>
                    <th className="p-3">Slug</th>
                    <th className="p-3">URL Path</th>
                    <th className="p-3">Status</th>
                    <th className="p-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0ece5]">
                  {(categories.length > 0 ? categories : DEMO_CATEGORIES).map((cat) => (
                    <tr key={cat.id} className="hover:bg-[#faf9f6]">
                      <td className="p-3">
                        <div className="font-bold text-[#080808] flex items-center gap-2">
                          {cat.slug === 'gift-corner' && <span>🎁</span>}
                          <span>{cat.name}</span>
                          {cat.slug === 'gift-corner' && (
                            <span className="bg-[#C8BCA7]/20 text-[#8c7f69] text-[9px] font-bold px-1.5 py-0.5 rounded uppercase">
                              Curated
                            </span>
                          )}
                        </div>
                        {cat.description && (
                          <div className="text-[11px] text-[#777777] mt-0.5">{cat.description}</div>
                        )}
                      </td>
                      <td className="p-3 font-mono text-[11px] text-[#555555]">{cat.slug}</td>
                      <td className="p-3 text-[11px]">
                        <Link
                          href={cat.slug === 'gift-corner' ? '/shop/gift-corner' : `/shop?category=${cat.slug}`}
                          target="_blank"
                          className="text-[#8c7f69] hover:underline font-mono text-[11px]"
                        >
                          {cat.slug === 'gift-corner' ? '/shop/gift-corner' : `/shop?category=${cat.slug}`}
                        </Link>
                      </td>
                      <td className="p-3">
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                            cat.isActive !== false
                              ? 'bg-emerald-50 text-emerald-900 border border-emerald-200'
                              : 'bg-gray-100 text-gray-700'
                          }`}
                        >
                          {cat.isActive !== false ? 'ACTIVE' : 'INACTIVE'}
                        </span>
                      </td>
                      <td className="p-3 text-right">
                        <button
                          onClick={() => handleToggleCategory(cat.id)}
                          className="px-2.5 py-1 border border-[#ccc] rounded text-[11px] font-semibold text-[#555555] hover:border-[#080808]"
                        >
                          {cat.isActive !== false ? 'Disable' : 'Enable'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ─── 4. INVENTORY MANAGEMENT ─────────────────────────────────────── */}
        {activeTab === 'inventory' && inventory && (
          <div className="space-y-6">
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-4 text-center">
                <span className="text-[10px] uppercase tracking-wider text-[#8c7f69] font-bold">Total Stock</span>
                <p className="text-2xl font-extrabold text-[#080808] mt-1">{inventory.totalStock || 0}</p>
              </div>
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-4 text-center">
                <span className="text-[10px] uppercase tracking-wider text-amber-800 font-bold">Low Stock (&lt; 10)</span>
                <p className="text-2xl font-extrabold text-amber-700 mt-1">{inventory.lowStockCount || 0}</p>
              </div>
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-4 text-center">
                <span className="text-[10px] uppercase tracking-wider text-red-800 font-bold">Out of Stock</span>
                <p className="text-2xl font-extrabold text-red-700 mt-1">{inventory.outOfStockCount || 0}</p>
              </div>
            </div>

            <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                    <th className="p-3">Product</th>
                    <th className="p-3">SKU</th>
                    <th className="p-3">Current Stock</th>
                    <th className="p-3">Status</th>
                    <th className="p-3 text-right">Adjustment</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0ece5]">
                  {inventory.products?.content?.map((prod: any) => (
                    <tr key={prod.id} className="hover:bg-[#faf9f6]">
                      <td className="p-3 font-bold text-[#080808]">{prod.name}</td>
                      <td className="p-3 text-[11px] text-[#777777]">{prod.sku}</td>
                      <td className="p-3 font-bold text-[#080808]">
                        {stockEditId === prod.id ? (
                          <input
                            type="number"
                            min="0"
                            value={newStockVal}
                            onChange={(e) => setNewStockVal(Number(e.target.value))}
                            className="w-20 px-2 py-1 border border-[#080808] rounded text-xs"
                          />
                        ) : (
                          `${prod.stock} units`
                        )}
                      </td>
                      <td className="p-3">
                        {prod.stock <= 0 ? (
                          <span className="bg-red-50 text-red-800 border border-red-200 text-[10px] font-bold px-2 py-0.5 rounded">
                            Out of Stock
                          </span>
                        ) : prod.stock < 10 ? (
                          <span className="bg-amber-50 text-amber-800 border border-amber-200 text-[10px] font-bold px-2 py-0.5 rounded">
                            Low Stock
                          </span>
                        ) : (
                          <span className="bg-emerald-50 text-emerald-800 border border-emerald-200 text-[10px] font-bold px-2 py-0.5 rounded">
                            Adequate
                          </span>
                        )}
                      </td>
                      <td className="p-3 text-right">
                        {stockEditId === prod.id ? (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => handleAdjustStock(prod.id)}
                              className="px-3 py-1 bg-[#080808] text-[#C8BCA7] rounded text-[11px] font-bold uppercase"
                            >
                              Save
                            </button>
                            <button
                              onClick={() => setStockEditId(null)}
                              className="px-2 py-1 text-[#888888] text-[11px]"
                            >
                              Cancel
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => {
                              setStockEditId(prod.id);
                              setNewStockVal(prod.stock);
                            }}
                            className="px-3 py-1 border border-[#ccc] rounded text-[11px] font-semibold text-[#555555] hover:border-[#080808]"
                          >
                            Adjust Stock
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ─── 5. RETURNS MANAGEMENT ───────────────────────────────────────── */}
        {activeTab === 'returns' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                  Returns & Reverse Logistics
                </h2>
                <p className="text-xs text-[#777777]">
                  Approve/reject returns, schedule pickups, mark items received, and process refunds.
                </p>
              </div>

              <select
                value={returnFilter}
                onChange={(e) => setReturnFilter(e.target.value)}
                className="px-3 py-1.5 border border-[#d5d0c5] rounded text-xs bg-white font-semibold"
              >
                <option value="ALL">All Return Statuses</option>
                <option value="RETURN_REQUESTED">Requested</option>
                <option value="RETURN_APPROVED">Approved</option>
                <option value="PICKUP_SCHEDULED">Pickup Scheduled</option>
                <option value="RECEIVED">Received</option>
                <option value="REFUND_PENDING">Refund Pending</option>
                <option value="REFUNDED">Refunded</option>
              </select>
            </div>

            <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                    <th className="p-3">Return ID</th>
                    <th className="p-3">Order</th>
                    <th className="p-3">Status</th>
                    <th className="p-3">Reason</th>
                    <th className="p-3">Refund Amount</th>
                    <th className="p-3">Pickup AWB</th>
                    <th className="p-3 text-right">Workflow Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0ece5]">
                  {returns.map((ret) => (
                    <tr key={ret.id} className="hover:bg-[#faf9f6]">
                      <td className="p-3 font-bold text-[#080808]">{ret.returnNumber}</td>
                      <td className="p-3 text-[#555555]">#{ret.orderId}</td>
                      <td className="p-3">
                        <span className="bg-amber-50 text-amber-900 border border-amber-300 text-[10px] font-bold px-2 py-0.5 rounded">
                          {ret.status}
                        </span>
                      </td>
                      <td className="p-3 text-[#555555] max-w-xs truncate">{ret.reason}</td>
                      <td className="p-3 font-extrabold text-[#080808]">
                        ₹{(ret.refundAmount || 0).toLocaleString()}
                      </td>
                      <td className="p-3 text-[11px] text-[#777777]">
                        {ret.pickupAwb ? `${ret.pickupCourier} (${ret.pickupAwb})` : '—'}
                      </td>
                      <td className="p-3 text-right space-x-1.5">
                        {ret.status === 'RETURN_REQUESTED' && (
                          <>
                            <button
                              onClick={() => handleApproveReturn(ret.id)}
                              className="px-2 py-1 bg-[#080808] text-[#C8BCA7] rounded text-[10px] font-bold uppercase"
                            >
                              Approve
                            </button>
                            <button
                              onClick={() => {
                                setSelectedReturn(ret);
                                setRejectionReason('');
                              }}
                              className="px-2 py-1 border border-red-300 text-red-700 bg-red-50 rounded text-[10px] font-bold uppercase"
                            >
                              Reject
                            </button>
                          </>
                        )}
                        {ret.status === 'RETURN_APPROVED' && (
                          <button
                            onClick={() => {
                              setSelectedReturn(ret);
                              setPickupAwb('');
                            }}
                            className="px-2 py-1 bg-purple-700 text-white rounded text-[10px] font-bold uppercase"
                          >
                            Schedule Pickup
                          </button>
                        )}
                        {['PICKUP_SCHEDULED', 'PICKED_UP'].includes(ret.status) && (
                          <button
                            onClick={() => handleReceiveReturn(ret.id)}
                            className="px-2 py-1 bg-teal-700 text-white rounded text-[10px] font-bold uppercase"
                          >
                            Mark Received
                          </button>
                        )}
                        {['RECEIVED', 'QUALITY_CHECK'].includes(ret.status) && (
                          <button
                            onClick={() => handleApproveRefund(ret.id)}
                            className="px-2 py-1 bg-emerald-700 text-white rounded text-[10px] font-bold uppercase"
                          >
                            Approve Refund
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {returns.length === 0 && (
                    <tr>
                      <td colSpan={7} className="p-8 text-center text-[#888888]">
                        No return requests found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Modal for Reject or Schedule Pickup */}
            {selectedReturn && (
              <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 backdrop-blur-sm">
                <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 border border-[#e5e1d8] shadow-2xl">
                  <h3 className="text-sm font-extrabold uppercase text-[#080808]">
                    Manage Return #{selectedReturn.returnNumber}
                  </h3>

                  {selectedReturn.status === 'RETURN_REQUESTED' ? (
                    <div className="space-y-3 text-xs">
                      <p className="text-[#666666]">
                        Provide a clear reason for rejecting this customer return request:
                      </p>
                      <textarea
                        rows={3}
                        value={rejectionReason}
                        onChange={(e) => setRejectionReason(e.target.value)}
                        placeholder="e.g. Return window expired, item damaged by customer..."
                        className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs"
                      />
                      <div className="flex justify-end gap-2 pt-2">
                        <button
                          type="button"
                          onClick={() => setSelectedReturn(null)}
                          className="px-3 py-1.5 border border-[#e5e1d8] rounded text-xs"
                        >
                          Cancel
                        </button>
                        <button
                          type="button"
                          onClick={() => handleRejectReturn(selectedReturn.id)}
                          className="px-4 py-1.5 bg-red-600 text-white rounded text-xs font-bold uppercase"
                        >
                          Confirm Rejection
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-3 text-xs">
                      <p className="text-[#666666]">
                        Assign reverse logistics courier and tracking AWB for pickup:
                      </p>
                      <div>
                        <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">
                          Courier Partner
                        </label>
                        <input
                          type="text"
                          value={pickupCourier}
                          onChange={(e) => setPickupCourier(e.target.value)}
                          className="w-full px-3 py-2 border border-[#d5d0c5] rounded text-xs"
                        />
                      </div>
                      <div>
                        <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">
                          Pickup AWB Number *
                        </label>
                        <input
                          type="text"
                          value={pickupAwb}
                          onChange={(e) => setPickupAwb(e.target.value)}
                          placeholder="e.g. SR-RET-789012"
                          className="w-full px-3 py-2 border border-[#d5d0c5] rounded text-xs"
                        />
                      </div>
                      <div className="flex justify-end gap-2 pt-2">
                        <button
                          type="button"
                          onClick={() => setSelectedReturn(null)}
                          className="px-3 py-1.5 border border-[#e5e1d8] rounded text-xs"
                        >
                          Cancel
                        </button>
                        <button
                          type="button"
                          onClick={() => handleSchedulePickup(selectedReturn.id)}
                          className="px-4 py-1.5 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase"
                        >
                          Schedule Pickup
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        {/* ─── 6. REFUNDS MANAGEMENT ───────────────────────────────────────── */}
        {activeTab === 'refunds' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                Refunds Ledger & Razorpay Ready
              </h2>
              <p className="text-xs text-[#777777]">
                Refund ledger architecture designed for Razorpay webhook and payout integration.
              </p>
            </div>

            <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                    <th className="p-3">Refund ID</th>
                    <th className="p-3">Order ID</th>
                    <th className="p-3">Amount</th>
                    <th className="p-3">Status</th>
                    <th className="p-3">Provider</th>
                    <th className="p-3">Provider Ref ID</th>
                    <th className="p-3">Created</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0ece5]">
                  {refunds.map((ref) => (
                    <tr key={ref.id} className="hover:bg-[#faf9f6]">
                      <td className="p-3 font-bold text-[#080808]">{ref.refundId}</td>
                      <td className="p-3 text-[#555555]">#{ref.orderId}</td>
                      <td className="p-3 font-extrabold text-[#080808]">₹{(ref.amount ?? 0).toLocaleString()}</td>
                      <td className="p-3">
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                            ref.status === 'REFUNDED'
                              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                              : 'bg-orange-50 text-orange-800 border border-orange-200'
                          }`}
                        >
                          {ref.status}
                        </span>
                      </td>
                      <td className="p-3 font-semibold">{ref.provider}</td>
                      <td className="p-3 text-[#777777] font-mono text-[11px]">
                        {ref.providerRefundId || 'Awaiting Razorpay API config'}
                      </td>
                      <td className="p-3 text-[#888888]">
                        {new Date(ref.createdAt).toLocaleDateString('en-IN')}
                      </td>
                    </tr>
                  ))}
                  {refunds.length === 0 && (
                    <tr>
                      <td colSpan={7} className="p-8 text-center text-[#888888]">
                        No refunds processed yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ─── 7. REVIEWS MODERATION ───────────────────────────────────────── */}
        {activeTab === 'reviews' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                  Product Reviews & Moderation
                </h2>
                <p className="text-xs text-[#777777]">
                  Verify purchases, prevent duplicate spam, and moderate published ratings.
                </p>
              </div>

              <select
                value={reviewFilter}
                onChange={(e) => setReviewFilter(e.target.value)}
                className="px-3 py-1.5 border border-[#d5d0c5] rounded text-xs bg-white font-semibold"
              >
                <option value="ALL">All Reviews</option>
                <option value="PENDING">Pending Moderation</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </div>

            <div className="space-y-4">
              {reviews.map((rev) => (
                <div key={rev.id} className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-3">
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <div className="flex items-center gap-3">
                      <span className="font-bold text-xs text-[#080808]">{rev.userName}</span>
                      {rev.verifiedPurchase && (
                        <span className="bg-emerald-50 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded border border-emerald-200">
                          Verified Purchase
                        </span>
                      )}
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-gray-100 text-gray-800">
                        {rev.status || 'APPROVED'}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleModerateReview(rev.id, 'APPROVED')}
                        className="px-3 py-1 bg-emerald-700 text-white rounded text-[11px] font-bold uppercase hover:bg-emerald-800"
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => handleModerateReview(rev.id, 'REJECTED')}
                        className="px-3 py-1 border border-red-300 text-red-700 bg-red-50 rounded text-[11px] font-bold uppercase hover:bg-red-100"
                      >
                        Reject
                      </button>
                    </div>
                  </div>

                  <div className="flex text-amber-500">
                    {[1, 2, 3, 4, 5].map((s) => (
                      <Star
                        key={s}
                        className={`w-3.5 h-3.5 ${s <= rev.rating ? 'fill-current' : 'text-gray-200'}`}
                      />
                    ))}
                  </div>

                  <h4 className="font-bold text-xs text-[#080808]">{rev.title}</h4>
                  <p className="text-xs text-[#555555] leading-relaxed">{rev.comment}</p>
                </div>
              ))}
              {reviews.length === 0 && (
                <div className="bg-white rounded-xl border border-[#e5e1d8] p-8 text-center text-[#888888] text-xs">
                  No reviews matching moderation filter.
                </div>
              )}
            </div>
          </div>
        )}

        {/* ─── 8. BUSINESS ANALYTICS ───────────────────────────────────────── */}
        {activeTab === 'analytics' && analytics && (
          <div className="space-y-8">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                  PostgreSQL Business Analytics
                </h2>
                <p className="text-xs text-[#777777]">
                  Real transactional metrics calculated directly from database records.
                </p>
              </div>

              <div className="flex gap-1 bg-[#eae6df] p-1 rounded-lg">
                {['today', '7d', '30d', '90d'].map((r) => (
                  <button
                    key={r}
                    onClick={() => setAnalyticsRange(r)}
                    className={`px-3 py-1 rounded text-xs font-bold uppercase tracking-wider transition-colors ${
                      analyticsRange === r ? 'bg-white text-[#080808] shadow-sm' : 'text-[#666666]'
                    }`}
                  >
                    {r}
                  </button>
                ))}
              </div>
            </div>

            {/* Metrics Grid */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 sm:gap-6">
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-1">
                <span className="text-[10px] uppercase tracking-wider text-[#8c7f69] font-bold">Revenue</span>
                <p className="text-2xl font-extrabold text-[#080808]">₹{(analytics.totalRevenue ?? 0).toLocaleString()}</p>
              </div>
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-1">
                <span className="text-[10px] uppercase tracking-wider text-[#8c7f69] font-bold">Orders</span>
                <p className="text-2xl font-extrabold text-[#080808]">{analytics.totalOrders ?? 0}</p>
              </div>
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-1">
                <span className="text-[10px] uppercase tracking-wider text-[#8c7f69] font-bold">Avg Order Value</span>
                <p className="text-2xl font-extrabold text-[#080808]">₹{(analytics.averageOrderValue ?? 0).toLocaleString()}</p>
              </div>
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-1">
                <span className="text-[10px] uppercase tracking-wider text-[#8c7f69] font-bold">Return Rate</span>
                <p className="text-2xl font-extrabold text-amber-800">{analytics.returnRate ?? 0}%</p>
              </div>
            </div>

            {/* Top Products & Categories */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-4">
                <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#080808]">Top Products</h3>
                <div className="space-y-3">
                  {analytics.topProducts?.map((tp) => (
                    <div key={tp.productId} className="flex justify-between items-center text-xs py-1 border-b border-[#f0ece5] last:border-0">
                      <div>
                        <p className="font-bold text-[#080808]">{tp.productName || tp.name || 'Product'}</p>
                        <p className="text-[11px] text-[#777777]">{tp.unitsSold ?? 0} units sold</p>
                      </div>
                      <span className="font-extrabold text-[#080808]">₹{(tp.revenue ?? tp.totalRevenue ?? 0).toLocaleString()}</span>
                    </div>
                  ))}
                  {(!analytics.topProducts || analytics.topProducts.length === 0) && (
                    <p className="text-xs text-[#888888] text-center py-4">No sales yet</p>
                  )}
                </div>
              </div>

              <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-4">
                <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#080808]">Top Categories</h3>
                <div className="space-y-3">
                  {analytics.topCategories?.map((tc, idx) => (
                    <div key={idx} className="flex justify-between items-center text-xs py-1 border-b border-[#f0ece5] last:border-0">
                      <span className="font-bold text-[#080808]">{tc.categoryName}</span>
                      <span className="font-extrabold text-[#080808]">₹{(tc.revenue ?? 0).toLocaleString()}</span>
                    </div>
                  ))}
                  {(!analytics.topCategories || analytics.topCategories.length === 0) && (
                    <p className="text-xs text-[#888888] text-center py-4">No sales yet</p>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ─── 9. INTEGRATIONS & SECRETS STATUS ─────────────────────────────── */}
        {activeTab === 'integrations' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                Production Integrations & Credentials Status
              </h2>
              <p className="text-xs text-[#777777]">
                Backend security audit status. Secrets remain server-side and are NEVER exposed to the browser.
              </p>
            </div>

            {loading && !integrations ? (
              <div className="flex items-center justify-center p-12 bg-white rounded-xl border border-[#e5e1d8]">
                <RefreshCw className="w-5 h-5 text-[#8c7f69] animate-spin" />
                <span className="ml-2 text-xs text-[#666666]">Checking integration configurations...</span>
              </div>
            ) : integrations ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* Razorpay */}
                <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-xs uppercase tracking-wider text-[#080808]">Razorpay Payments</h3>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        integrations.razorpay?.configured
                          ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border border-amber-200'
                      }`}
                    >
                      {integrations.razorpay?.configured ? 'CONFIGURED' : 'NOT CONFIGURED'}
                    </span>
                  </div>
                  <p className="text-xs text-[#666666] leading-relaxed">
                    {integrations.razorpay?.details || 'Payment gateway and automated refund architecture.'}
                  </p>
                  <div className="pt-2 border-t border-[#f0ece5] flex justify-between items-center text-[11px] text-[#777777]">
                    <span>Provider: {integrations.razorpay?.provider || 'Razorpay'}</span>
                    <span>Key: {integrations.razorpay?.keyId || 'Not set'}</span>
                  </div>
                </div>

                {/* Shiprocket */}
                <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-xs uppercase tracking-wider text-[#080808]">Shiprocket Logistics</h3>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        integrations.shiprocket?.configured
                          ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border border-amber-200'
                      }`}
                    >
                      {integrations.shiprocket?.configured ? 'CONFIGURED' : 'NOT CONFIGURED'}
                    </span>
                  </div>
                  <p className="text-xs text-[#666666] leading-relaxed">
                    {integrations.shiprocket?.details || 'Pincode serviceability & tracking engine with automated reverse pickup and AWB generation.'}
                  </p>
                  <div className="pt-2 border-t border-[#f0ece5] flex justify-between items-center text-[11px] text-[#777777]">
                    <span>Provider: {integrations.shiprocket?.provider || 'Shiprocket'}</span>
                  </div>
                </div>

                {/* Cloudinary */}
                <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-xs uppercase tracking-wider text-[#080808]">Cloudinary Media</h3>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        integrations.cloudinary?.configured
                          ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border border-amber-200'
                      }`}
                    >
                      {integrations.cloudinary?.configured ? 'CONFIGURED' : 'NOT CONFIGURED'}
                    </span>
                  </div>
                  <p className="text-xs text-[#666666] leading-relaxed">
                    {integrations.cloudinary?.details || 'Cloud media CDN and dynamic image transformation.'}
                  </p>
                  <div className="pt-2 border-t border-[#f0ece5] flex justify-between items-center text-[11px] text-[#777777]">
                    <span>Provider: {integrations.cloudinary?.provider || 'Cloudinary'}</span>
                  </div>
                </div>

                {/* Email Delivery */}
                <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-xs uppercase tracking-wider text-[#080808]">Email Delivery (SMTP)</h3>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        integrations.email?.configured
                          ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border border-amber-200'
                      }`}
                    >
                      {integrations.email?.configured ? 'CONFIGURED' : 'NOT CONFIGURED'}
                    </span>
                  </div>
                  <p className="text-xs text-[#666666] leading-relaxed">
                    {integrations.email?.details || 'Transactional order receipts, confirmations, and security notices.'}
                  </p>
                  <div className="pt-2 border-t border-[#f0ece5] flex justify-between items-center text-[11px] text-[#777777]">
                    <span>Provider: {integrations.email?.provider || 'SMTP'}</span>
                  </div>
                </div>

                {/* SMS Delivery */}
                <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-xs uppercase tracking-wider text-[#080808]">SMS Delivery</h3>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        integrations.sms?.configured
                          ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border border-amber-200'
                      }`}
                    >
                      {integrations.sms?.configured ? 'CONFIGURED' : 'NOT CONFIGURED'}
                    </span>
                  </div>
                  <p className="text-xs text-[#666666] leading-relaxed">
                    {integrations.sms?.details || 'Real-time SMS dispatch alerts and order tracking.'}
                  </p>
                  <div className="pt-2 border-t border-[#f0ece5] flex justify-between items-center text-[11px] text-[#777777]">
                    <span>Provider: {integrations.sms?.provider || 'Fast2SMS'}</span>
                  </div>
                </div>

                {/* WhatsApp */}
                <div className="bg-white rounded-xl border border-[#e5e1d8] p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="font-bold text-xs uppercase tracking-wider text-[#080808]">WhatsApp Notifications</h3>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        integrations.whatsapp?.configured
                          ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          : 'bg-amber-50 text-amber-800 border border-amber-200'
                      }`}
                    >
                      {integrations.whatsapp?.configured ? 'CONFIGURED' : 'NOT CONFIGURED'}
                    </span>
                  </div>
                  <p className="text-xs text-[#666666] leading-relaxed">
                    {integrations.whatsapp?.details || 'Non-blocking async event notifications for order lifecycle.'}
                  </p>
                  <div className="pt-2 border-t border-[#f0ece5] flex justify-between items-center text-[11px] text-[#777777]">
                    <span>Provider: {integrations.whatsapp?.provider || 'Meta Cloud API'}</span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="p-8 text-center text-xs text-[#888888] bg-white rounded-xl border border-[#e5e1d8]">
                Unable to load integration status. Click refresh to try again.
              </div>
            )}
          </div>
        )}

        {/* ─── 10. AUDIT LOGS ──────────────────────────────────────────────── */}
        {activeTab === 'audit' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                Admin Operational Audit Trail
              </h2>
              <p className="text-xs text-[#777777]">
                Immutable records of administrative actions, stock adjustments, and order state transitions.
              </p>
            </div>

            <div className="bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="bg-[#f9f8f6] border-b border-[#e5e1d8] text-[#8c7f69] uppercase tracking-wider text-[10px]">
                    <th className="p-3">Timestamp</th>
                    <th className="p-3">Admin</th>
                    <th className="p-3">Action</th>
                    <th className="p-3">Entity</th>
                    <th className="p-3">Details</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#f0ece5]">
                  {auditLogs.map((log) => (
                    <tr key={log.id} className="hover:bg-[#faf9f6]">
                      <td className="p-3 text-[#888888]">
                        {new Date(log.createdAt).toLocaleString('en-IN')}
                      </td>
                      <td className="p-3 font-bold text-[#080808]">{log.adminEmail}</td>
                      <td className="p-3">
                        <span className="font-mono text-[10px] bg-gray-100 px-2 py-0.5 rounded font-bold">
                          {log.action}
                        </span>
                      </td>
                      <td className="p-3 text-[#555555]">
                        {log.entityType} #{log.entityId}
                      </td>
                      <td className="p-3 text-[#555555] max-w-sm truncate">{log.details}</td>
                    </tr>
                  ))}
                  {auditLogs.length === 0 && (
                    <tr>
                      <td colSpan={5} className="p-8 text-center text-[#888888]">
                        No audit records captured yet.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ─── 11. SETTINGS & ACCESS CONTROL ─────────────────────────────── */}
        {activeTab === 'settings' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                Platform Security & Administrator Management
              </h2>
              <p className="text-xs text-[#777777]">
                Configure cryptographic safeguards, session policies, and administrative personnel.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Active Session & Admin Card */}
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4 shadow-sm">
                <div className="flex items-center justify-between pb-3 border-b border-[#f0ece5]">
                  <div className="flex items-center gap-2 font-bold text-xs uppercase tracking-wider text-[#080808]">
                    <ShieldCheck className="w-4 h-4 text-emerald-600" />
                    <span>Current Active Session</span>
                  </div>
                  <span className="text-[10px] bg-emerald-50 text-emerald-800 font-bold px-2 py-0.5 rounded">
                    AUTHENTICATED
                  </span>
                </div>

                <div className="space-y-2 text-xs">
                  <div className="flex justify-between py-1 border-b border-[#faf9f6]">
                    <span className="text-[#666666]">Admin Email:</span>
                    <span className="font-bold text-[#080808]">{adminUser?.email || 'admin@rexxzo.in'}</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-[#faf9f6]">
                    <span className="text-[#666666]">Security Role:</span>
                    <span className="font-mono font-bold text-emerald-800">ROLE_ADMIN</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-[#faf9f6]">
                    <span className="text-[#666666]">Password Hashing:</span>
                    <span className="font-mono text-[#080808]">BCrypt (Strength 12)</span>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-[#666666]">Session Token:</span>
                    <span className="font-mono text-[10px] text-[#777777]">Stateless JWT (HMAC-SHA256)</span>
                  </div>
                </div>

                <div className="pt-2">
                  <button
                    onClick={() => api.adminLogout()}
                    className="w-full py-2.5 px-3 border border-red-200 text-red-700 bg-red-50 hover:bg-red-100 rounded text-xs font-bold uppercase tracking-wider transition-colors flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Terminate Session & Logout</span>
                  </button>
                </div>
              </div>

              {/* Sub-Administrator Provisioning Card */}
              <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4 shadow-sm flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between pb-3 border-b border-[#f0ece5]">
                    <div className="flex items-center gap-2 font-bold text-xs uppercase tracking-wider text-[#080808]">
                      <UserPlus className="w-4 h-4 text-[#8c7f69]" />
                      <span>Administrator Provisioning</span>
                    </div>
                  </div>

                  <p className="text-xs text-[#666666] leading-relaxed mt-4">
                    Only authenticated master administrators can provision new administrator accounts. Unrestricted public registration is permanently disabled.
                  </p>
                </div>

                <div className="pt-4">
                  <button
                    onClick={() => setIsCreateAdminModalOpen(true)}
                    className="w-full py-3 px-4 bg-[#080808] text-[#C8BCA7] hover:bg-[#222222] rounded-lg text-xs font-bold uppercase tracking-widest transition-colors flex items-center justify-center gap-2 shadow-sm cursor-pointer"
                  >
                    <UserPlus className="w-4 h-4" />
                    <span>Provision New Administrator</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Create Administrator Modal */}
      {isCreateAdminModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 border border-[#e5e1d8] shadow-2xl">
            <div className="flex items-center justify-between border-b border-[#f0ece5] pb-3">
              <div className="flex items-center gap-2">
                <UserPlus className="w-4 h-4 text-[#8c7f69]" />
                <h3 className="text-sm font-extrabold uppercase text-[#080808]">Provision Administrator</h3>
              </div>
              <button onClick={() => setIsCreateAdminModalOpen(false)} className="text-[#888888] hover:text-[#080808]">
                <XCircle className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSubAdmin} className="space-y-4 text-xs">
              <div>
                <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Full Name *</label>
                <input
                  type="text"
                  required
                  value={newAdmin.name}
                  onChange={(e) => setNewAdmin({ ...newAdmin, name: e.target.value })}
                  placeholder="e.g. Operations Manager"
                  className="w-full px-3 py-2.5 bg-[#FAF9F7] border border-[#d5d0c5] rounded-lg text-xs text-[#080808]"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Admin Email *</label>
                <input
                  type="email"
                  required
                  value={newAdmin.email}
                  onChange={(e) => setNewAdmin({ ...newAdmin, email: e.target.value })}
                  placeholder="e.g. ops@rexxzo.com"
                  className="w-full px-3 py-2.5 bg-[#FAF9F7] border border-[#d5d0c5] rounded-lg text-xs text-[#080808]"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Temporary Password *</label>
                <input
                  type="password"
                  required
                  value={newAdmin.password}
                  onChange={(e) => setNewAdmin({ ...newAdmin, password: e.target.value })}
                  placeholder="••••••••••••"
                  className="w-full px-3 py-2.5 bg-[#FAF9F7] border border-[#d5d0c5] rounded-lg text-xs text-[#080808]"
                />
                <span className="text-[10px] text-[#888888] mt-1 block">Minimum 6 characters. Will be securely hashed with BCrypt.</span>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[#f0ece5]">
                <button
                  type="button"
                  onClick={() => setIsCreateAdminModalOpen(false)}
                  className="px-4 py-2 border border-[#e5e1d8] rounded text-xs font-semibold text-[#555555]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creatingAdmin}
                  className="px-5 py-2 bg-[#080808] text-[#C8BCA7] hover:bg-[#222222] disabled:opacity-50 rounded text-xs font-bold uppercase tracking-wider flex items-center gap-1.5"
                >
                  {creatingAdmin && <div className="w-3 h-3 border-2 border-[#C8BCA7] border-t-transparent rounded-full animate-spin" />}
                  <span>Save Administrator</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Product Modal */}
      {isProductModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-4 max-h-[90vh] overflow-y-auto border border-[#e5e1d8] shadow-2xl">
            <div className="flex items-center justify-between border-b border-[#f0ece5] pb-3">
              <h3 className="text-base font-extrabold uppercase text-[#080808]">Create Architectural Object</h3>
              <button onClick={() => setIsProductModalOpen(false)} className="text-[#888888]">
                <XCircle className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateProduct} className="space-y-4 text-xs">
              <div>
                <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Product Name *</label>
                <input
                  type="text"
                  required
                  value={newProduct.name}
                  onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
                  placeholder="e.g. Sculptural Ceramic Canteen"
                  className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs"
                />
              </div>

              <div>
                <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Description</label>
                <textarea
                  rows={2}
                  value={newProduct.description}
                  onChange={(e) => setNewProduct({ ...newProduct, description: e.target.value })}
                  className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Price (₹) *</label>
                  <input
                    type="number"
                    required
                    value={newProduct.price}
                    onChange={(e) => setNewProduct({ ...newProduct, price: e.target.value })}
                    className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Compare Price (₹)</label>
                  <input
                    type="number"
                    value={newProduct.compareAtPrice}
                    onChange={(e) => setNewProduct({ ...newProduct, compareAtPrice: e.target.value })}
                    className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Category</label>
                  <select
                    value={newProduct.categoryName}
                    onChange={(e) => setNewProduct({ ...newProduct, categoryName: e.target.value })}
                    className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs bg-white"
                  >
                    {DEMO_CATEGORIES.map((c) => (
                      <option key={c.id} value={c.name}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-[10px] font-bold uppercase text-[#080808] mb-1">Stock Quantity</label>
                  <input
                    type="number"
                    value={newProduct.stockQuantity}
                    onChange={(e) => setNewProduct({ ...newProduct, stockQuantity: e.target.value })}
                    className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-[#f0ece5]">
                <button
                  type="button"
                  onClick={() => setIsProductModalOpen(false)}
                  className="px-4 py-2 border border-[#e5e1d8] rounded text-xs font-semibold text-[#555555]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-6 py-2 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase tracking-wider"
                >
                  Create Object
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
