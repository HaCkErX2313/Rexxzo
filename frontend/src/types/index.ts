export interface User {
  id: number;
  email: string;
  name: string;
  phone?: string;
  role: 'CUSTOMER' | 'ADMIN';
  createdAt?: string;
}

export interface Address {
  id?: number;
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault?: boolean;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  itemCount?: number;
  isActive?: boolean;
}

export interface ProductImage {
  id?: number;
  imageUrl: string;
  altText?: string;
  isPrimary?: boolean;
  displayOrder?: number;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  description: string;
  price: number;
  compareAtPrice?: number;
  originalPrice?: number;
  discountPercent?: number;
  sku: string;
  stockQuantity: number;
  stock?: number;
  lowStockThreshold?: number;
  status?: 'ACTIVE' | 'DRAFT' | 'OUT_OF_STOCK' | 'ARCHIVED' | string;
  category?: Category;
  categoryId?: number;
  categoryName?: string;
  images: ProductImage[];
  isFeatured?: boolean;
  isActive?: boolean;
  averageRating: number;
  avgRating?: number;
  reviewCount: number;
  materials?: string;
  dimensions?: string;
  weightGrams?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  careInstructions?: string;
  createdAt?: string;
  updatedAt?: string;
}


export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  productSlug: string;
  price: number;
  quantity: number;
  imageUrl?: string;
  stockQuantity: number;
  subtotal: number;
}

export interface Cart {
  id?: number;
  items: CartItem[];
  subtotal: number;
  shipping: number;
  discount: number;
  total: number;
  couponCode?: string;
}

export interface WishlistItem {
  id: number;
  productId: number;
  productName: string;
  productSlug: string;
  price: number;
  imageUrl?: string;
  stockQuantity: number;
  averageRating?: number;
}

export interface OrderItem {
  id?: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
  imageUrl?: string;
}

export interface Order {
  id: number;
  orderNumber: string;
  status: 'PENDING' | 'PENDING_PAYMENT' | 'PAID' | 'CONFIRMED' | 'PROCESSING' | 'PACKED'
        | 'PICKUP_SCHEDULED' | 'PICKED_UP' | 'SHIPPED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY'
        | 'DELIVERED' | 'CANCELLED' | 'REFUNDED' | 'RETURN_REQUESTED' | 'RETURN_IN_TRANSIT'
        | 'RTO' | 'RETURNED';
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  items: OrderItem[];
  shippingAddress: Address;
  paymentMethod: string;
  paymentStatus: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';
  createdAt: string;
  /** Shipment tracking info — populated when available */
  shipmentStatus?: string;
  awbNumber?: string;
  courierName?: string;
  trackingUrl?: string;
  estimatedDeliveryDate?: string;
}

export interface CourierOption {
  courierId: string;
  courierName: string;
  rate: number;
  estimatedDeliveryDays: number;
  codAvailable: boolean;
  codCharges?: number;
}

export interface ServiceabilityResponse {
  serviceable: boolean;
  message: string;
  couriers: CourierOption[];
}

export interface TrackingEvent {
  status: string;
  rawStatus?: string;
  description: string;
  location: string;
  eventTimestamp: string;
}

export interface ShipmentTracking {
  orderNumber: string;
  shipmentStatus: string;
  awbNumber?: string;
  courierName?: string;
  estimatedDeliveryDate?: string;
  trackingUrl?: string;
  latestEvent?: TrackingEvent;
  events: TrackingEvent[];
}


export interface Review {
  id: number;
  productId: number;
  userId: number;
  userName: string;
  rating: number;
  title: string;
  comment: string;
  verifiedPurchase: boolean;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
}

export interface ReviewSummary {
  averageRating: number;
  totalReviews: number;
  ratingDistribution: Record<number, number>;
  reviews: Review[];
}

export interface ReturnItem {
  id: number;
  orderItemId: number;
  productId: number;
  productName: string;
  productImageUrl?: string;
  quantity: number;
  refundAmount: number;
}

export interface ReturnRequest {
  id: number;
  returnNumber: string;
  orderId: number;
  orderNumber: string;
  status: 'RETURN_REQUESTED' | 'RETURN_APPROVED' | 'RETURN_REJECTED' | 'PICKUP_SCHEDULED' | 'PICKED_UP' | 'RECEIVED' | 'QUALITY_CHECK' | 'REFUND_PENDING' | 'REFUNDED' | 'RETURN_CANCELLED';
  reason: string;
  description?: string;
  rejectionReason?: string;
  pickupAwb?: string;
  pickupCourier?: string;
  refundAmount: number;
  adminNotes?: string;
  items: ReturnItem[];
  createdAt: string;
  updatedAt?: string;
}

export interface ReturnEligibilityItem {
  orderItemId: number;
  productId: number;
  productName: string;
  productImageUrl?: string;
  unitPrice: number;
  purchasedQuantity: number;
  previouslyReturnedQuantity: number;
  availableReturnQuantity: number;
}

export interface ReturnEligibility {
  eligible: boolean;
  reason?: string;
  daysRemaining?: number;
  items: ReturnEligibilityItem[];
}

export interface Refund {
  id: number;
  refundId: string;
  orderId: number;
  paymentId?: number;
  amount: number;
  status: 'REFUND_PENDING' | 'REFUND_PROCESSING' | 'REFUNDED' | 'REFUND_FAILED';
  reason?: string;
  provider: string;
  providerRefundId?: string;
  createdAt: string;
}

export interface Coupon {
  id: number;
  code: string;
  discountPercent: number;
  maxDiscount?: number;
  minOrderAmount?: number;
  expiryDate?: string;
  isActive: boolean;
}

export interface TopProductSummary {
  productId: number;
  productName: string;
  name?: string;
  unitsSold: number;
  revenue: number;
  totalRevenue?: number;
}

export interface TopCategorySummary {
  categoryName: string;
  unitsSold?: number;
  revenue: number;
}

export interface AdminDashboardData {
  totalRevenue: number;
  totalOrders: number;
  totalProducts?: number;
  totalCustomers?: number;
  pendingOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  returnRequests: number;
  refundAmount: number;
  lowStockProducts: number;
  outOfStockProducts: number;
  recentOrders: Order[];
  topProducts?: TopProductSummary[];
}

export interface AnalyticsSummary {
  range?: string;
  dateRange?: string;
  totalRevenue: number;
  totalOrders: number;
  averageOrderValue: number;
  pendingOrders?: number;
  deliveredOrders?: number;
  cancelledOrders?: number;
  cancellationRate: number;
  returnRequestsCount?: number;
  returnRate: number;
  totalRefundAmount: number;
  lowStockCount?: number;
  outOfStockCount?: number;
  topProducts: TopProductSummary[];
  topCategories: TopCategorySummary[];
  eventCounts?: Record<string, number>;
}

export interface IntegrationDetail {
  configured: boolean;
  status: 'CONFIGURED' | 'NOT_CONFIGURED' | string;
  provider: string;
  details?: string;
  keyId?: string;
}

export interface AdminIntegrationsStatus {
  shiprocket?: IntegrationDetail;
  razorpay?: IntegrationDetail;
  cloudinary?: IntegrationDetail;
  email?: IntegrationDetail;
  sms?: IntegrationDetail;
  whatsapp?: IntegrationDetail;
  [key: string]: IntegrationDetail | undefined;
}
