'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { 
  Package, Truck, CheckCircle2, Clock, MapPin, ArrowLeft, 
  RefreshCw, ExternalLink, RotateCcw, AlertCircle, XCircle, Check, HelpCircle
} from 'lucide-react';
import { shippingApi, api } from '@/lib/api';
import { ShipmentTracking, ReturnEligibility, ReturnRequest } from '@/types';

// Terminal statuses — stop polling when reached
const TERMINAL_STATUSES = ['Delivered', 'Cancelled', 'Returned', 'Return to Origin'];

const STATUS_STEPS = [
  { key: 'Order Placed', label: 'Order Placed', icon: Package },
  { key: 'Processing', label: 'Processing', icon: Clock },
  { key: 'Pickup Scheduled', label: 'Pickup Scheduled', icon: MapPin },
  { key: 'Picked Up', label: 'Picked Up', icon: Truck },
  { key: 'In Transit', label: 'In Transit', icon: Truck },
  { key: 'Out for Delivery', label: 'Out for Delivery', icon: Truck },
  { key: 'Delivered', label: 'Delivered', icon: CheckCircle2 },
];

const RETURN_REASONS = [
  { value: 'DEFECTIVE_OR_DAMAGED', label: 'Item arrived damaged or defective' },
  { value: 'WRONG_ITEM_RECEIVED', label: 'Received wrong product or variant' },
  { value: 'NOT_AS_DESCRIBED', label: 'Product does not match website description' },
  { value: 'QUALITY_NOT_EXPECTED', label: 'Material quality not as expected' },
  { value: 'SIZE_OR_FIT_ISSUE', label: 'Size or dimensions unsuitable' },
  { value: 'CHANGED_MIND', label: 'No longer needed / Changed mind' },
];

function getStepIndex(status: string): number {
  const s = status?.toLowerCase() || '';
  if (s.includes('delivered')) return 6;
  if (s.includes('out for delivery')) return 5;
  if (s.includes('transit')) return 4;
  if (s.includes('picked up')) return 3;
  if (s.includes('pickup scheduled')) return 2;
  if (s.includes('processing') || s.includes('awb') || s.includes('packed')) return 1;
  return 0;
}

export default function OrderDetailPage() {
  const params = useParams();
  const orderId = Number(params.id);

  const [tracking, setTracking] = useState<ShipmentTracking | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Return & Cancellation state
  const [eligibility, setEligibility] = useState<ReturnEligibility | null>(null);
  const [orderReturns, setOrderReturns] = useState<ReturnRequest[]>([]);
  const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionMessage, setActionMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Return request form state
  const [selectedReason, setSelectedReason] = useState(RETURN_REASONS[0].value);
  const [returnDescription, setReturnDescription] = useState('');
  const [itemQuantities, setItemQuantities] = useState<Record<number, number>>({});

  const fetchTracking = useCallback(async () => {
    try {
      const data = await shippingApi.getOrderTracking(orderId);
      setTracking(data);
      setLastUpdated(new Date());
    } catch {
      // Network error — keep showing cached data
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  }, [orderId]);

  const loadReturnData = useCallback(async () => {
    try {
      const [eligRes, returnsRes] = await Promise.allSettled([
        api.getReturnEligibility(orderId),
        api.getMyReturns()
      ]);

      if (eligRes.status === 'fulfilled') {
        setEligibility(eligRes.value);
        // initialize default quantities
        const initialQtys: Record<number, number> = {};
        eligRes.value.items?.forEach((item: import('@/types').ReturnEligibilityItem) => {
          if (item.availableReturnQuantity > 0) {
            initialQtys[item.orderItemId] = item.availableReturnQuantity;
          }
        });
        setItemQuantities(initialQtys);
      }

      if (returnsRes.status === 'fulfilled' && returnsRes.value.content) {
        const matching = returnsRes.value.content.filter((r: import('@/types').ReturnRequest) => r.orderId === orderId);
        setOrderReturns(matching);
      }
    } catch {
      // Non-critical
    }
  }, [orderId]);

  useEffect(() => {
    fetchTracking();
    loadReturnData();
  }, [fetchTracking, loadReturnData]);

  // Live polling every 45 seconds — stops on terminal status or unmount
  useEffect(() => {
    if (!tracking) return;
    const isTerminal = TERMINAL_STATUSES.some(
      t => tracking.shipmentStatus?.toLowerCase().includes(t.toLowerCase())
    );
    if (isTerminal) return;

    const interval = setInterval(fetchTracking, 45_000);
    return () => clearInterval(interval);
  }, [tracking, fetchTracking]);

  const handleManualRefresh = () => {
    setIsRefreshing(true);
    fetchTracking();
    loadReturnData();
  };

  const handleCancelOrder = async () => {
    if (!cancelReason.trim()) {
      setActionMessage({ type: 'error', text: 'Please provide a cancellation reason.' });
      return;
    }
    setIsSubmitting(true);
    setActionMessage(null);
    try {
      await api.cancelOrder(orderId, cancelReason);
      setActionMessage({ type: 'success', text: 'Order cancelled successfully. Restocked inventory.' });
      setIsCancelModalOpen(false);
      fetchTracking();
    } catch (err: any) {
      setActionMessage({ type: 'error', text: err.message || 'Failed to cancel order.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!eligibility) return;

    const selectedItems = Object.entries(itemQuantities)
      .filter(([_, qty]) => qty > 0)
      .map(([orderItemId, quantity]) => ({
        orderItemId: Number(orderItemId),
        quantity
      }));

    if (selectedItems.length === 0) {
      setActionMessage({ type: 'error', text: 'Please select at least one item to return.' });
      return;
    }

    setIsSubmitting(true);
    setActionMessage(null);
    try {
      await api.requestReturn({
        orderId,
        reason: selectedReason,
        description: returnDescription,
        items: selectedItems
      });
      setActionMessage({ type: 'success', text: 'Return request submitted successfully. Our team will review it shortly.' });
      setIsReturnModalOpen(false);
      loadReturnData();
    } catch (err: any) {
      setActionMessage({ type: 'error', text: err.message || 'Failed to submit return request.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancelReturnRequest = async (returnId: number) => {
    if (!confirm('Are you sure you want to cancel this return request?')) return;
    try {
      await api.cancelReturn(returnId);
      setActionMessage({ type: 'success', text: 'Return request cancelled.' });
      loadReturnData();
    } catch (err: any) {
      setActionMessage({ type: 'error', text: err.message || 'Failed to cancel return.' });
    }
  };

  const getReturnStatusBadge = (status: ReturnRequest['status']) => {
    switch (status) {
      case 'RETURN_REQUESTED':
        return <span className="bg-amber-100 text-amber-900 border border-amber-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Return Requested</span>;
      case 'RETURN_APPROVED':
        return <span className="bg-blue-100 text-blue-900 border border-blue-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Return Approved</span>;
      case 'RETURN_REJECTED':
        return <span className="bg-red-100 text-red-900 border border-red-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Rejected</span>;
      case 'PICKUP_SCHEDULED':
        return <span className="bg-purple-100 text-purple-900 border border-purple-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Pickup Scheduled</span>;
      case 'PICKED_UP':
        return <span className="bg-indigo-100 text-indigo-900 border border-indigo-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Picked Up</span>;
      case 'RECEIVED':
      case 'QUALITY_CHECK':
        return <span className="bg-teal-100 text-teal-900 border border-teal-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">{status.replace('_', ' ')}</span>;
      case 'REFUND_PENDING':
        return <span className="bg-orange-100 text-orange-900 border border-orange-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Refund Pending</span>;
      case 'REFUNDED':
        return <span className="bg-emerald-100 text-emerald-900 border border-emerald-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Refund Completed</span>;
      case 'RETURN_CANCELLED':
        return <span className="bg-gray-100 text-gray-700 border border-gray-300 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Cancelled</span>;
      default:
        return <span className="bg-gray-100 text-gray-800 text-[10px] font-bold px-2 py-0.5 rounded uppercase">{status}</span>;
    }
  };

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="w-8 h-8 border-2 border-[#080808] border-t-transparent rounded-full animate-spin mx-auto" />
        <p className="mt-4 text-xs text-[#777777]">Loading order details...</p>
      </div>
    );
  }

  if (!tracking) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center space-y-4">
        <Package className="w-12 h-12 text-[#888888] mx-auto" />
        <h2 className="text-lg font-bold uppercase text-[#080808]">Order Not Found</h2>
        <p className="text-xs text-[#777777]">
          We could not find tracking information for this order. Please verify your order number.
        </p>
        <Link href="/orders" className="inline-block mt-2 px-6 py-2.5 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase rounded">
          Back to Orders
        </Link>
      </div>
    );
  }

  const currentStepIndex = getStepIndex(tracking.shipmentStatus || '');
  const isTerminal = TERMINAL_STATUSES.some(
    t => tracking.shipmentStatus?.toLowerCase().includes(t.toLowerCase())
  );
  const isDelivered = tracking.shipmentStatus?.toLowerCase().includes('delivered');
  const isCancellable = ['placed', 'processing', 'packed', 'confirmed', 'paid'].some(
    s => tracking.shipmentStatus?.toLowerCase().includes(s)
  );

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-10 sm:py-16 space-y-8">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-[#e5e1d8] pb-6 flex-wrap gap-4">
        <div>
          <Link href="/orders" className="flex items-center gap-1.5 text-[11px] text-[#8c7f69] uppercase tracking-wider mb-2 hover:text-[#080808] transition-colors">
            <ArrowLeft className="w-3.5 h-3.5" />
            Order History
          </Link>
          <h1 className="text-2xl font-extrabold uppercase text-[#080808]">
            Order #{tracking.orderNumber}
          </h1>
          <p className="text-xs text-[#888888] mt-1">Order tracking and lifecycle management</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleManualRefresh}
            disabled={isRefreshing}
            className="flex items-center gap-2 px-3 py-2 border border-[#e5e1d8] rounded text-xs text-[#555555] hover:border-[#080808] transition-colors disabled:opacity-50 cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
            Refresh
          </button>

          {isCancellable && (
            <button
              onClick={() => setIsCancelModalOpen(true)}
              className="px-3 py-2 border border-red-300 text-red-700 bg-red-50 hover:bg-red-100 rounded text-xs font-bold uppercase transition-colors"
            >
              Cancel Order
            </button>
          )}

          {isDelivered && eligibility?.eligible && (
            <button
              onClick={() => setIsReturnModalOpen(true)}
              className="flex items-center gap-1.5 px-4 py-2 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase hover:bg-[#222222] transition-colors shadow-sm"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Request Return
            </button>
          )}
        </div>
      </div>

      {/* Action Notification Banner */}
      {actionMessage && (
        <div className={`p-4 rounded-xl border flex items-center justify-between text-xs font-medium ${
          actionMessage.type === 'success' 
            ? 'bg-emerald-50 text-emerald-900 border-emerald-200' 
            : 'bg-red-50 text-red-900 border-red-200'
        }`}>
          <div className="flex items-center gap-2">
            {actionMessage.type === 'success' ? <Check className="w-4 h-4 text-emerald-600" /> : <AlertCircle className="w-4 h-4 text-red-600" />}
            <span>{actionMessage.text}</span>
          </div>
          <button onClick={() => setActionMessage(null)} className="text-gray-400 hover:text-gray-700">
            <XCircle className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Return Requests History for this Order */}
      {orderReturns.length > 0 && (
        <div className="bg-[#fcfbf9] rounded-xl border border-[#e5e1d8] p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-extrabold uppercase tracking-widest text-[#080808] flex items-center gap-2">
              <RotateCcw className="w-4 h-4 text-[#8c7f69]" />
              Return Requests ({orderReturns.length})
            </h3>
          </div>

          <div className="space-y-4">
            {orderReturns.map(ret => (
              <div key={ret.id} className="bg-white rounded-lg border border-[#e5e1d8] p-4 text-xs space-y-3">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-[#080808]">{ret.returnNumber}</span>
                    {getReturnStatusBadge(ret.status)}
                  </div>
                  <span className="text-[#888888]">
                    Requested on {new Date(ret.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                  </span>
                </div>

                <div className="text-[#555555]">
                  <p><strong className="text-[#080808]">Reason:</strong> {ret.reason.replace(/_/g, ' ')}</p>
                  {ret.description && <p className="mt-0.5"><strong className="text-[#080808]">Notes:</strong> {ret.description}</p>}
                  {ret.rejectionReason && (
                    <p className="mt-1 text-red-700 bg-red-50 p-2 rounded border border-red-200">
                      <strong>Rejection Reason:</strong> {ret.rejectionReason}
                    </p>
                  )}
                  {ret.pickupAwb && (
                    <p className="mt-1 text-purple-800 bg-purple-50 p-2 rounded border border-purple-200 flex items-center gap-2">
                      <Truck className="w-4 h-4" />
                      <strong>Pickup Courier:</strong> {ret.pickupCourier} · <strong>AWB:</strong> {ret.pickupAwb}
                    </p>
                  )}
                  {ret.refundAmount && (
                    <p className="mt-1 text-emerald-800 font-bold">
                      Refund Amount: ₹{ret.refundAmount.toLocaleString()}
                    </p>
                  )}
                </div>

                {/* Items in Return */}
                <div className="pt-2 border-t border-[#f0ece5] flex items-center justify-between">
                  <div className="space-y-1">
                    {ret.items?.map((it, idx) => (
                      <span key={idx} className="inline-block bg-[#f4f2ee] px-2 py-0.5 rounded text-[11px] text-[#444444] mr-2">
                        {it.productName} (Qty: {it.quantity})
                      </span>
                    ))}
                  </div>

                  {ret.status === 'RETURN_REQUESTED' && (
                    <button
                      onClick={() => handleCancelReturnRequest(ret.id)}
                      className="text-[11px] font-bold uppercase text-red-600 hover:text-red-800 underline ml-auto"
                    >
                      Cancel Return
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Return Eligibility Notice */}
      {isDelivered && (
        <div className="bg-[#f9f8f6] rounded-xl border border-[#e5e1d8] p-4 text-xs flex items-start gap-3">
          <HelpCircle className="w-5 h-5 text-[#8c7f69] flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h4 className="font-bold text-[#080808] uppercase tracking-wider text-[11px]">7-Day Return Policy</h4>
            {eligibility?.eligible ? (
              <p className="text-[#666666] mt-0.5">
                This order is eligible for return. You have <strong className="text-[#080808]">{eligibility.daysRemaining} days remaining</strong> to request a return or exchange.
              </p>
            ) : (
              <p className="text-[#777777] mt-0.5">
                {eligibility?.reason || 'Returns are no longer available for this order.'}
              </p>
            )}
          </div>
        </div>
      )}

      {/* Current Status Banner */}
      <div className="bg-[#f9f8f6] rounded-xl border border-[#e5e1d8] p-6">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div>
            <span className="text-[10px] uppercase tracking-widest text-[#8c7f69] block mb-1">
              Current Status
            </span>
            <h2 className="text-xl font-extrabold text-[#080808]">
              {tracking.shipmentStatus || 'Order Placed'}
            </h2>
            {tracking.courierName && (
              <p className="text-xs text-[#777777] mt-1">via {tracking.courierName}</p>
            )}
          </div>
          <div className="text-right space-y-1">
            {tracking.awbNumber && (
              <div>
                <span className="text-[10px] text-[#8c7f69] uppercase tracking-wider block">AWB</span>
                <span className="text-sm font-bold text-[#080808]">{tracking.awbNumber}</span>
              </div>
            )}
            {tracking.estimatedDeliveryDate && (
              <div>
                <span className="text-[10px] text-[#8c7f69] uppercase tracking-wider block">Est. Delivery</span>
                <span className="text-xs font-semibold text-[#080808]">
                  {new Date(tracking.estimatedDeliveryDate).toLocaleDateString('en-IN', {
                    day: 'numeric', month: 'short', year: 'numeric'
                  })}
                </span>
              </div>
            )}
          </div>
        </div>

        {tracking.trackingUrl && (
          <a
            href={tracking.trackingUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-4 inline-flex items-center gap-1.5 text-[11px] text-[#8c7f69] hover:text-[#080808] transition-colors"
          >
            <ExternalLink className="w-3.5 h-3.5" />
            Track on courier website
          </a>
        )}
      </div>

      {/* Progress Steps */}
      {!['Cancelled', 'Return to Origin', 'Returned'].some(
        s => tracking.shipmentStatus?.toLowerCase().includes(s.toLowerCase())
      ) && (
        <div className="bg-white rounded-xl border border-[#e5e1d8] p-6">
          <h3 className="text-[10px] uppercase tracking-widest text-[#8c7f69] mb-6">
            Shipment Progress
          </h3>
          <div className="relative">
            <div className="absolute left-4 top-4 bottom-4 w-0.5 bg-[#e5e1d8]" />
            <div
              className="absolute left-4 top-4 w-0.5 bg-[#080808] transition-all duration-700"
              style={{ height: `${(currentStepIndex / (STATUS_STEPS.length - 1)) * 100}%` }}
            />

            <div className="space-y-6">
              {STATUS_STEPS.map((step, index) => {
                const StepIcon = step.icon;
                const isDone = index <= currentStepIndex;
                const isCurrent = index === currentStepIndex;
                return (
                  <div key={step.key} className="relative flex items-center gap-4 pl-12">
                    <div className={`absolute left-0 w-8 h-8 rounded-full flex items-center justify-center border-2 transition-all duration-300 ${
                      isDone
                        ? 'bg-[#080808] border-[#080808] text-white'
                        : 'bg-white border-[#e5e1d8] text-[#cccccc]'
                    } ${isCurrent ? 'scale-110' : ''}`}>
                      <StepIcon className="w-4 h-4" />
                    </div>
                    <div>
                      <p className={`text-xs font-bold uppercase tracking-wider ${
                        isDone ? 'text-[#080808]' : 'text-[#aaaaaa]'
                      }`}>
                        {step.label}
                      </p>
                      {isCurrent && tracking.latestEvent && (
                        <p className="text-[11px] text-[#777777] mt-0.5">
                          {tracking.latestEvent.location && `${tracking.latestEvent.location} — `}
                          {tracking.latestEvent.description}
                        </p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Tracking History */}
      {tracking.events && tracking.events.length > 0 && (
        <div className="bg-white rounded-xl border border-[#e5e1d8] p-6">
          <h3 className="text-[10px] uppercase tracking-widest text-[#8c7f69] mb-6">
            Tracking History
          </h3>
          <div className="space-y-4">
            {[...tracking.events].reverse().map((event, index) => (
              <div key={index} className="flex gap-4 pb-4 border-b border-[#f0ece5] last:border-0 last:pb-0">
                <div className={`w-2 h-2 rounded-full mt-1.5 flex-shrink-0 ${
                  index === 0 ? 'bg-[#080808]' : 'bg-[#d5d0c5]'
                }`} />
                <div className="flex-1 min-w-0">
                  <p className={`text-xs font-semibold ${index === 0 ? 'text-[#080808]' : 'text-[#444444]'}`}>
                    {event.description || event.rawStatus}
                  </p>
                  {event.location && (
                    <p className="text-[11px] text-[#888888] mt-0.5">{event.location}</p>
                  )}
                  {event.eventTimestamp && (
                    <p className="text-[11px] text-[#aaaaaa] mt-0.5">
                      {new Date(event.eventTimestamp).toLocaleString('en-IN', {
                        day: 'numeric', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit'
                      })}
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Return Request Modal */}
      {isReturnModalOpen && eligibility && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 sm:p-8 space-y-6 max-h-[90vh] overflow-y-auto border border-[#e5e1d8] shadow-2xl">
            <div className="flex items-center justify-between border-b border-[#f0ece5] pb-4">
              <div>
                <h3 className="text-base font-extrabold uppercase tracking-tight text-[#080808]">
                  Request Return / Refund
                </h3>
                <p className="text-xs text-[#888888] mt-0.5">Order #{tracking.orderNumber}</p>
              </div>
              <button
                onClick={() => setIsReturnModalOpen(false)}
                className="text-[#888888] hover:text-[#080808] p-1"
              >
                <XCircle className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmitReturn} className="space-y-5 text-xs">
              {/* Product Selection */}
              <div>
                <label className="font-bold text-[#080808] block mb-2 uppercase tracking-wider text-[11px]">
                  Select Items & Quantity
                </label>
                <div className="space-y-3">
                  {eligibility.items?.map(item => {
                    const maxQty = item.availableReturnQuantity;
                    const selectedQty = itemQuantities[item.orderItemId] || 0;
                    return (
                      <div key={item.orderItemId} className="flex items-center justify-between p-3 border border-[#e5e1d8] rounded-lg bg-[#faf9f6]">
                        <div>
                          <p className="font-bold text-[#080808]">{item.productName}</p>
                          <p className="text-[11px] text-[#777777]">
                            ₹{item.unitPrice.toLocaleString()} each · Purchased: {item.purchasedQuantity} (Available: {maxQty})
                          </p>
                        </div>
                        {maxQty > 0 ? (
                          <div className="flex items-center gap-2">
                            <span className="text-[11px] text-[#888888]">Qty:</span>
                            <select
                              value={selectedQty}
                              onChange={e => setItemQuantities({
                                ...itemQuantities,
                                [item.orderItemId]: Number(e.target.value)
                              })}
                              className="px-2 py-1 border border-[#ccc] rounded bg-white font-semibold"
                            >
                              {Array.from({ length: maxQty + 1 }, (_, i) => (
                                <option key={i} value={i}>{i}</option>
                              ))}
                            </select>
                          </div>
                        ) : (
                          <span className="text-[10px] text-red-600 bg-red-50 px-2 py-0.5 rounded font-semibold">
                            Already Returned
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Reason */}
              <div>
                <label className="font-bold text-[#080808] block mb-1 uppercase tracking-wider text-[11px]">
                  Return Reason *
                </label>
                <select
                  value={selectedReason}
                  onChange={e => setSelectedReason(e.target.value)}
                  className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg bg-white text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                >
                  {RETURN_REASONS.map(r => (
                    <option key={r.value} value={r.value}>{r.label}</option>
                  ))}
                </select>
              </div>

              {/* Description */}
              <div>
                <label className="font-bold text-[#080808] block mb-1 uppercase tracking-wider text-[11px]">
                  Detailed Description (Optional)
                </label>
                <textarea
                  rows={3}
                  value={returnDescription}
                  onChange={e => setReturnDescription(e.target.value)}
                  placeholder="Provide specific details about the issue to expedite approval..."
                  className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs focus:outline-none focus:border-[#080808]"
                />
              </div>

              {/* Notice */}
              <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-[11px] text-blue-900 space-y-1">
                <p className="font-semibold">Refund Architecture:</p>
                <p>Upon admin inspection and item pickup, refund is initiated back to your original payment method or bank account.</p>
              </div>

              {/* Modal Actions */}
              <div className="flex items-center justify-end gap-3 pt-3 border-t border-[#f0ece5]">
                <button
                  type="button"
                  onClick={() => setIsReturnModalOpen(false)}
                  className="px-4 py-2 border border-[#e5e1d8] rounded text-xs font-semibold text-[#555555] hover:border-[#080808]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-6 py-2 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase tracking-wider hover:bg-[#222222] disabled:opacity-50"
                >
                  {isSubmitting ? 'Submitting...' : 'Submit Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Cancel Order Modal */}
      {isCancelModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-5 border border-[#e5e1d8] shadow-2xl">
            <div className="flex items-center justify-between border-b border-[#f0ece5] pb-3">
              <h3 className="text-base font-extrabold uppercase text-[#080808]">
                Cancel Order #{tracking.orderNumber}
              </h3>
              <button onClick={() => setIsCancelModalOpen(false)} className="text-[#888888] hover:text-[#080808]">
                <XCircle className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <p className="text-[#666666]">
                Are you sure you want to cancel this order? Any reserved stock will be immediately restocked.
              </p>

              <div>
                <label className="font-bold text-[#080808] block mb-1 uppercase tracking-wider text-[11px]">
                  Reason for Cancellation *
                </label>
                <input
                  type="text"
                  value={cancelReason}
                  onChange={e => setCancelReason(e.target.value)}
                  placeholder="e.g. Changed mind, placed duplicate order..."
                  className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs focus:outline-none focus:border-[#080808]"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-[#f0ece5]">
                <button
                  type="button"
                  onClick={() => setIsCancelModalOpen(false)}
                  className="px-4 py-2 border border-[#e5e1d8] rounded text-xs font-semibold text-[#555555]"
                >
                  Keep Order
                </button>
                <button
                  type="button"
                  onClick={handleCancelOrder}
                  disabled={isSubmitting}
                  className="px-5 py-2 bg-red-600 text-white rounded text-xs font-bold uppercase hover:bg-red-700 disabled:opacity-50"
                >
                  {isSubmitting ? 'Cancelling...' : 'Confirm Cancellation'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Last Updated */}
      {lastUpdated && (
        <p className="text-center text-[11px] text-[#aaaaaa]">
          {isTerminal
            ? 'Tracking complete — no further updates expected'
            : `Last updated: ${lastUpdated.toLocaleTimeString('en-IN')} · Auto-refreshes every 45s`
          }
        </p>
      )}

    </div>
  );
}
