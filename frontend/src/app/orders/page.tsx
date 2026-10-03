'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { Package, ArrowRight, Truck } from 'lucide-react';
import { api } from '@/lib/api';
import { Order } from '@/types';

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getOrders().then((data) => {
      setOrders(data);
      setLoading(false);
    });
  }, []);

  const getStatusBadge = (status: Order['status']) => {
    switch (status) {
      case 'PAID':
      case 'CONFIRMED':
        return <span className="bg-blue-100 text-blue-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">Confirmed</span>;
      case 'PROCESSING':
      case 'PACKED':
        return <span className="bg-amber-100 text-amber-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">Processing</span>;
      case 'PICKUP_SCHEDULED':
      case 'PICKED_UP':
        return <span className="bg-orange-100 text-orange-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">Pickup</span>;
      case 'SHIPPED':
      case 'IN_TRANSIT':
        return <span className="bg-purple-100 text-purple-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">In Transit</span>;
      case 'OUT_FOR_DELIVERY':
        return <span className="bg-indigo-100 text-indigo-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">Out for Delivery</span>;
      case 'DELIVERED':
        return <span className="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">Delivered</span>;
      case 'CANCELLED':
        return <span className="bg-red-100 text-red-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">Cancelled</span>;
      default:
        return <span className="bg-gray-100 text-gray-800 text-[10px] font-bold px-2.5 py-0.5 rounded uppercase">{status}</span>;
    }
  };

  const orderList: Order[] = Array.isArray(orders) ? orders : [];

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-16">
      <div className="border-b border-[#e5e1d8] pb-6 mb-8 flex justify-between items-end">
        <div>
          <span className="text-[10px] font-bold uppercase tracking-[0.25em] text-[#8c7f69] block mb-1">
            Order Tracking
          </span>
          <h1 className="text-2xl sm:text-4xl font-extrabold uppercase tracking-tight text-[#080808]">
            Order History
          </h1>
        </div>
        <Link
          href="/shop"
          className="text-xs font-bold uppercase tracking-wider text-[#080808] hover:text-[#8c7f69] transition-colors"
        >
          Explore Shop
        </Link>
      </div>

      {loading ? (
        <div className="py-16 text-center text-xs text-[#888888]">Loading your orders...</div>
      ) : orderList.length === 0 ? (
        <div className="bg-white rounded-xl border border-[#e5e1d8] p-12 text-center space-y-4 shadow-sm">
          <Package className="w-12 h-12 text-[#888888] mx-auto" />
          <h3 className="text-base font-bold uppercase text-[#080808]">No Orders Yet</h3>
          <p className="text-xs text-[#777777] max-w-sm mx-auto">
            You haven&apos;t placed any orders yet. Discover timeless pieces to enhance your living environment.
          </p>
          <Link
            href="/shop"
            className="inline-block mt-2 px-6 py-2.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase tracking-wider rounded"
          >
            Start Shopping
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {orderList.map((order: Order) => (
            <div
              key={order.id}
              className="bg-white rounded-xl border border-[#e5e1d8] p-6 space-y-4 shadow-sm"
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-[#f0ece5] gap-3">
                <div className="space-y-1">
                  <div className="flex items-center gap-3 flex-wrap">
                    <span className="text-sm font-bold text-[#080808]">{order.orderNumber}</span>
                    {getStatusBadge(order.status)}
                    {order.shipmentStatus && order.shipmentStatus !== 'Order Placed' && (
                      <span className="text-[10px] text-[#8c7f69] bg-[#f9f8f6] px-2 py-0.5 rounded border border-[#e5e1d8]">
                        {order.shipmentStatus}
                      </span>
                    )}
                  </div>
                  <span className="text-[11px] text-[#888888] block">
                    Placed on {order.createdAt ? new Date(order.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : 'Recently'}
                  </span>
                  {order.awbNumber && (
                    <span className="text-[11px] text-[#777777] flex items-center gap-1">
                      <Truck className="w-3 h-3" />
                      AWB: {order.awbNumber}
                      {order.courierName && ` via ${order.courierName}`}
                    </span>
                  )}
                </div>

                <div className="text-left sm:text-right">
                  <span className="text-xs text-[#777777] block">Total Amount</span>
                  <span className="text-base font-extrabold text-[#080808]">
                    ₹{(order.totalAmount ?? (order as any).total ?? 0).toLocaleString()}
                  </span>
                </div>
              </div>

              {/* Items in order */}
              {order.items && order.items.length > 0 && (
                <div className="space-y-2">
                  {order.items.map((item, idx: number) => (
                    <div key={idx} className="flex justify-between items-center text-xs py-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-[#080808]">{item.productName}</span>
                        <span className="text-[#888888]">× {item.quantity}</span>
                      </div>
                      <span className="font-medium text-[#080808]">₹{((item.price || 0) * (item.quantity || 1)).toLocaleString()}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Shipping & Payment summary */}
              <div className="pt-4 border-t border-[#f0ece5] flex flex-col sm:flex-row justify-between items-start sm:items-center text-[11px] text-[#777777] gap-2">
                <div>
                  <span>Deliver to: </span>
                  <strong className="text-[#080808]">
                    {order.shippingAddress?.city || 'Bengaluru'}, {order.shippingAddress?.state || 'Karnataka'}
                  </strong>
                </div>
                <div className="flex items-center gap-3">
                  <span>
                    Payment: <strong className="text-[#080808]">{order.paymentMethod || 'Online'}</strong>
                  </span>
                  <Link
                    href={`/orders/${order.id}`}
                    className="flex items-center gap-1 px-3 py-1.5 bg-[#080808] text-[#C8BCA7] text-[10px] font-bold uppercase tracking-wider rounded hover:bg-[#222222] transition-colors"
                  >
                    Track Order
                    <ArrowRight className="w-3 h-3" />
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
