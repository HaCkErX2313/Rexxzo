'use client';

import React, { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';
import { Star, Heart, ShoppingBag, ShieldCheck, Truck, RotateCcw, ArrowRight, Minus, Plus, Check } from 'lucide-react';
import { api, DEMO_PRODUCTS } from '@/lib/api';
import { Product } from '@/types';
import { useStore } from '@/context/StoreContext';
import ProductCard from '@/components/product/ProductCard';

export default function ProductDetailPage() {
  const params = useParams();
  const router = useRouter();
  const idOrSlug = params.id as string;

  const [product, setProduct] = useState<Product | null>(null);
  const [selectedImage, setSelectedImage] = useState<string>('');
  const [quantity, setQuantity] = useState(1);
  const [activeTab, setActiveTab] = useState<'details' | 'specs' | 'shipping' | 'reviews'>('details');
  const [addedNotice, setAddedNotice] = useState(false);

  // Reviews & Rating Distribution state
  const [reviewSummary, setReviewSummary] = useState<import('@/types').ReviewSummary | null>(null);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewTitle, setReviewTitle] = useState('');
  const [reviewComment, setReviewComment] = useState('');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [reviewMessage, setReviewMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const { addToCart, toggleWishlist, isInWishlist } = useStore();

  const loadReviews = async (productId: number) => {
    try {
      const summary = await api.getProductReviewSummary(productId);
      setReviewSummary(summary);
    } catch {
      // non-critical
    }
  };

  useEffect(() => {
    if (idOrSlug) {
      api.getProduct(idOrSlug).then((p) => {
        if (p) {
          setProduct(p);
          setSelectedImage(p.images[0]?.imageUrl || '/images/hero_banner.jpg');
          loadReviews(p.id);
          api.trackAnalyticsEvent('PRODUCT_VIEW', { productId: p.id, productName: p.name });
        }
      });
    }
  }, [idOrSlug]);

  const handleReviewSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!product) return;
    setIsSubmittingReview(true);
    setReviewMessage(null);
    try {
      await api.submitProductReview(product.id, {
        rating: reviewRating,
        title: reviewTitle,
        comment: reviewComment,
      });
      setReviewMessage({
        type: 'success',
        text: 'Thank you! Your verified review has been submitted.',
      });
      setReviewTitle('');
      setReviewComment('');
      setReviewRating(5);
      setShowReviewForm(false);
      loadReviews(product.id);
    } catch (err: any) {
      setReviewMessage({
        type: 'error',
        text: err.message || 'Only verified purchasers can review this product.',
      });
    } finally {
      setIsSubmittingReview(false);
    }
  };

  if (!product) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-24 text-center">
        <div className="w-10 h-10 border-4 border-[#080808] border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-xs uppercase tracking-widest text-[#777777]">Loading Product Details...</p>
      </div>
    );
  }

  const wishlisted = isInWishlist(product.id);
  const discount = product.compareAtPrice && product.compareAtPrice > product.price
    ? Math.round(((product.compareAtPrice - product.price) / product.compareAtPrice) * 100)
    : 0;

  const isOutOfStock = (product.stock !== undefined ? product.stock <= 0 : product.stockQuantity <= 0) || product.status === 'OUT_OF_STOCK';

  const handleAddToCart = async () => {
    if (isOutOfStock) return;
    await addToCart(product.id, quantity);
    setAddedNotice(true);
    setTimeout(() => setAddedNotice(false), 2500);
  };

  const handleBuyNow = async () => {
    if (isOutOfStock) return;
    await addToCart(product.id, quantity);
    router.push('/checkout');
  };

  const related = DEMO_PRODUCTS.filter((p) => p.id !== product.id).slice(0, 4);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 sm:py-16">
      {/* Schema.org Structured Data */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org/',
            '@type': 'Product',
            name: product.name,
            image: product.images?.map((img) => img.imageUrl) || [],
            description: product.description,
            sku: product.sku,
            brand: {
              '@type': 'Brand',
              name: 'REXXZO',
            },
            offers: {
              '@type': 'Offer',
              url: `https://rexxo.in/products/${product.slug || product.id}`,
              priceCurrency: 'INR',
              price: product.price,
              availability:
                product.stockQuantity > 0
                  ? 'https://schema.org/InStock'
                  : 'https://schema.org/OutOfStock',
              itemCondition: 'https://schema.org/NewCondition',
            },
            aggregateRating: {
              '@type': 'AggregateRating',
              ratingValue: (reviewSummary?.averageRating || product.averageRating || 5.0).toFixed(1),
              reviewCount: Math.max(1, reviewSummary?.totalReviews ?? product.reviewCount ?? 1),
            },
          }),
        }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
              {
                '@type': 'ListItem',
                position: 1,
                name: 'Home',
                item: 'https://rexxo.in',
              },
              {
                '@type': 'ListItem',
                position: 2,
                name: 'Shop',
                item: 'https://rexxo.in/shop',
              },
              {
                '@type': 'ListItem',
                position: 3,
                name: product.name,
                item: `https://rexxo.in/products/${product.slug || product.id}`,
              },
            ],
          }),
        }}
      />

      {/* Breadcrumb */}
      <nav className="text-xs text-[#888888] mb-8 flex items-center space-x-2">
        <Link href="/" className="hover:text-[#080808] transition-colors">Home</Link>
        <span>/</span>
        <Link href="/shop" className="hover:text-[#080808] transition-colors">Shop</Link>
        <span>/</span>
        <span className="text-[#080808] font-medium line-clamp-1">{product.name}</span>
      </nav>

      {/* Main Product Showcase */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 lg:gap-16 pb-16">
        
        {/* Gallery (Left) */}
        <div className="space-y-4">
          <div className="relative aspect-[4/5] bg-white rounded-xl border border-[#e5e1d8] overflow-hidden shadow-sm">
            <Image
              src={selectedImage || product.images[0]?.imageUrl || '/images/hero_banner.jpg'}
              alt={product.name}
              fill
              priority
              className="object-cover object-center"
              sizes="(max-width: 1024px) 100vw, 50vw"
            />
            {discount > 0 && (
              <span className="absolute top-4 left-4 bg-[#080808] text-[#C8BCA7] text-[11px] font-bold tracking-wider px-3 py-1 rounded uppercase">
                Save {discount}%
              </span>
            )}
          </div>

          {/* Thumbnails */}
          <div className="flex gap-3 overflow-x-auto pb-2">
            {product.images.map((img, i) => (
              <button
                key={i}
                onClick={() => setSelectedImage(img.imageUrl)}
                className={`relative w-20 h-24 rounded-lg overflow-hidden border-2 flex-shrink-0 transition-colors ${
                  selectedImage === img.imageUrl ? 'border-[#080808]' : 'border-[#e5e1d8]'
                }`}
              >
                <Image src={img.imageUrl} alt={product.name} fill className="object-cover" sizes="80px" />
              </button>
            ))}
          </div>
        </div>

        {/* Product Details & Actions (Right) */}
        <div className="flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <span className="text-xs font-bold uppercase tracking-[0.25em] text-[#8c7f69]">
              {product.categoryName || 'REXXZO Atelier'}
            </span>

            <h1 className="text-2xl sm:text-4xl font-extrabold uppercase tracking-tight text-[#080808]">
              {product.name}
            </h1>

            {/* Ratings & SKU */}
            <div className="flex items-center gap-4 text-xs">
              <div className="flex items-center gap-1.5 text-amber-500">
                <div className="flex">
                  {[...Array(5)].map((_, i) => (
                    <Star
                      key={i}
                      className={`w-3.5 h-3.5 ${
                        i < Math.floor(product.averageRating) ? 'fill-current' : 'text-gray-300'
                      }`}
                    />
                  ))}
                </div>
                <span className="font-bold text-[#080808]">{product.averageRating.toFixed(1)}</span>
                <span className="text-[#888888]">({product.reviewCount} customer reviews)</span>
              </div>
              <span className="text-[#cccccc]">|</span>
              <span className="text-[#888888]">SKU: {product.sku}</span>
            </div>

            {/* Price section */}
            <div className="flex items-baseline gap-3 pt-2">
              <span className="text-2xl sm:text-3xl font-extrabold text-[#080808]">
                ₹{product.price.toLocaleString()}
              </span>
              {product.compareAtPrice && product.compareAtPrice > product.price && (
                <span className="text-base sm:text-lg text-[#888888] line-through">
                  ₹{product.compareAtPrice.toLocaleString()}
                </span>
              )}
              <span className="text-xs text-[#2e7d32] font-semibold bg-green-50 px-2 py-0.5 rounded">
                Inclusive of all taxes
              </span>
            </div>

            {/* Short Description */}
            <p className="text-xs sm:text-sm text-[#555555] leading-relaxed pt-2">
              {product.description}
            </p>

            {/* Stock indicator */}
            {(() => {
              const currentStock = product.stock !== undefined ? product.stock : product.stockQuantity;
              const isOut = currentStock <= 0 || product.status === 'OUT_OF_STOCK';
              return (
                <div className="flex items-center gap-2 text-xs pt-1">
                  <span className={`w-2 h-2 rounded-full ${isOut ? 'bg-rose-500' : currentStock > 5 ? 'bg-emerald-500' : 'bg-amber-500'}`} />
                  <span className={isOut ? 'text-rose-600 font-bold uppercase tracking-wider' : 'text-[#555555]'}>
                    {isOut ? 'Out of Stock' : `In Stock (${currentStock} units available)`}
                  </span>
                </div>
              );
            })()}

            {/* Quantity Selector */}
            <div className="pt-4">
              <label className="text-[11px] font-bold uppercase tracking-wider text-[#080808] block mb-2">
                Quantity
              </label>
              <div className="inline-flex items-center border border-[#d5d0c5] rounded-md bg-white">
                <button
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="px-3 py-2 text-[#666666] hover:text-[#080808] transition-colors"
                  aria-label="Decrease quantity"
                >
                  <Minus className="w-4 h-4" />
                </button>
                <span className="px-4 text-xs font-bold text-[#080808]">{quantity}</span>
                <button
                  onClick={() => setQuantity(Math.min(product.stockQuantity, quantity + 1))}
                  className="px-3 py-2 text-[#666666] hover:text-[#080808] transition-colors"
                  aria-label="Increase quantity"
                >
                  <Plus className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="pt-4 space-y-3">
              <div className="flex gap-3">
                {isOutOfStock ? (
                  <button
                    disabled
                    className="flex-1 py-4 bg-[#f0ece5] text-[#888888] text-xs font-bold uppercase tracking-widest rounded-md flex items-center justify-center gap-2 cursor-not-allowed"
                  >
                    <span>Out of Stock</span>
                  </button>
                ) : (
                  <button
                    onClick={handleAddToCart}
                    className="flex-1 py-4 bg-[#080808] hover:bg-[#222222] text-[#C8BCA7] text-xs font-bold uppercase tracking-widest rounded-md shadow-lg flex items-center justify-center gap-2 transition-all cursor-pointer"
                  >
                    <ShoppingBag className="w-4 h-4" />
                    <span>Add to Cart</span>
                  </button>
                )}

                <button
                  onClick={() => toggleWishlist(product)}
                  className={`p-4 rounded-md border transition-colors flex items-center justify-center cursor-pointer ${
                    wishlisted
                      ? 'bg-[#080808] border-[#080808] text-[#C8BCA7]'
                      : 'bg-white border-[#d5d0c5] text-[#555555] hover:border-[#080808]'
                  }`}
                  aria-label="Toggle wishlist"
                >
                  <Heart className={`w-5 h-5 ${wishlisted ? 'fill-current' : ''}`} />
                </button>
              </div>

              {!isOutOfStock && (
                <button
                  onClick={handleBuyNow}
                  className="w-full py-4 bg-[#C8BCA7] hover:bg-[#b8ab94] text-[#080808] text-xs font-bold uppercase tracking-widest rounded-md transition-all cursor-pointer shadow-md flex items-center justify-center gap-2"
                >
                  <span>Instant Checkout</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              )}

              {addedNotice && (
                <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded flex items-center gap-2 animate-fade-in">
                  <Check className="w-4 h-4 text-emerald-600" />
                  <span>Added {quantity} unit(s) of {product.name} to your selection!</span>
                </div>
              )}
            </div>

            {/* Reassurance strip */}
            <div className="grid grid-cols-3 gap-2 pt-6 border-t border-[#e5e1d8] text-center">
              <div className="p-2 space-y-1">
                <Truck className="w-4 h-4 mx-auto text-[#8c7f69]" />
                <span className="block text-[10px] font-bold uppercase text-[#080808]">Pan-India Express</span>
              </div>
              <div className="p-2 space-y-1">
                <RotateCcw className="w-4 h-4 mx-auto text-[#8c7f69]" />
                <span className="block text-[10px] font-bold uppercase text-[#080808]">7-Day Returns</span>
              </div>
              <div className="p-2 space-y-1">
                <ShieldCheck className="w-4 h-4 mx-auto text-[#8c7f69]" />
                <span className="block text-[10px] font-bold uppercase text-[#080808]">Authentic Craft</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs / Deep Information */}
      <div className="border-t border-[#e5e1d8] pt-12">
        <div className="flex border-b border-[#e5e1d8] space-x-8 mb-8 overflow-x-auto pb-2">
          {(['details', 'specs', 'shipping', 'reviews'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`text-xs font-bold uppercase tracking-widest pb-3 relative transition-colors ${
                activeTab === tab ? 'text-[#080808]' : 'text-[#888888] hover:text-[#080808]'
              }`}
            >
              {tab === 'details' && 'Design Story'}
              {tab === 'specs' && 'Materials & Specs'}
              {tab === 'shipping' && 'Delivery & Returns'}
              {tab === 'reviews' && `Reviews (${product.reviewCount})`}
              {activeTab === tab && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#080808]" />
              )}
            </button>
          ))}
        </div>

        <div className="bg-white rounded-xl border border-[#e5e1d8] p-6 sm:p-8 max-w-4xl">
          {activeTab === 'details' && (
            <div className="space-y-4 text-xs sm:text-sm text-[#555555] leading-relaxed">
              <p>
                The {product.name} embodies the REXXZO pursuit of quiet architectural composure. Designed to seamlessly integrate into minimalist domestic environments, it strips away extraneous visual clutter in favor of pure, timeless geometric geometry.
              </p>
              <p>
                Each component is inspected by hand to ensure precise tolerances and seamless joins. Built to outlive seasonal trends and develop a subtle patina with age.
              </p>
            </div>
          )}

          {activeTab === 'specs' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs text-[#444444]">
              <div className="p-3 bg-[#f8f6f2] rounded">
                <span className="font-bold text-[#080808] block mb-1">Materials:</span>
                <span>{product.materials || 'Hand-finished ceramic, brushed brass, natural stone'}</span>
              </div>
              <div className="p-3 bg-[#f8f6f2] rounded">
                <span className="font-bold text-[#080808] block mb-1">Dimensions:</span>
                <span>{product.dimensions || 'Custom architectural proportion'}</span>
              </div>
              <div className="p-3 bg-[#f8f6f2] rounded">
                <span className="font-bold text-[#080808] block mb-1">Care Guidelines:</span>
                <span>{product.careInstructions || 'Wipe with soft microfiber cloth. Avoid abrasive detergents.'}</span>
              </div>
              <div className="p-3 bg-[#f8f6f2] rounded">
                <span className="font-bold text-[#080808] block mb-1">Country of Origin:</span>
                <span>India (Master Studio Fabricated)</span>
              </div>
            </div>
          )}

          {activeTab === 'shipping' && (
            <div className="space-y-3 text-xs text-[#555555] leading-relaxed">
              <p>
                <strong className="text-[#080808]">Dispatches within 24 hours:</strong> Orders placed before 2:00 PM IST dispatch the same business day in custom protective eco-friendly packaging.
              </p>
              <p>
                <strong className="text-[#080808]">Standard Transit:</strong> 2-4 business days for metropolitan centers; 4-6 business days across tier-2 cities.
              </p>
              <p>
                <strong className="text-[#080808]">7-Day Easy Return:</strong> If you find the object does not suit your space, notify us within 7 days for a hassle-free courier pickup and full refund.
              </p>
            </div>
          )}

          {activeTab === 'reviews' && (
            <div className="space-y-8">
              {/* Review Statistics Header */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pb-6 border-b border-[#f0ece5] items-center">
                <div className="text-center md:text-left space-y-1">
                  <div className="flex items-center justify-center md:justify-start gap-2">
                    <span className="text-4xl font-extrabold text-[#080808]">
                      {(reviewSummary?.averageRating || product.averageRating || 0).toFixed(1)}
                    </span>
                    <div className="flex text-amber-500">
                      {[1, 2, 3, 4, 5].map((s) => (
                        <Star
                          key={s}
                          className={`w-4 h-4 ${
                            s <= Math.round(reviewSummary?.averageRating || product.averageRating || 0)
                              ? 'fill-current'
                              : 'text-gray-300'
                          }`}
                        />
                      ))}
                    </div>
                  </div>
                  <p className="text-xs text-[#777777]">
                    Based on {reviewSummary?.totalReviews ?? product.reviewCount} customer reviews
                  </p>
                </div>

                {/* Rating Distribution Bars */}
                <div className="md:col-span-2 space-y-1.5 text-xs">
                  {[5, 4, 3, 2, 1].map((stars) => {
                    const count = reviewSummary?.ratingDistribution?.[stars] || 0;
                    const total = reviewSummary?.totalReviews || 1;
                    const pct = reviewSummary?.totalReviews ? Math.round((count / total) * 100) : 0;
                    return (
                      <div key={stars} className="flex items-center gap-2">
                        <span className="w-12 text-[#666666] font-medium flex items-center gap-1">
                          {stars} <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                        </span>
                        <div className="flex-1 h-2 bg-[#f0ece5] rounded-full overflow-hidden">
                          <div
                            className="h-full bg-[#080808] rounded-full transition-all duration-500"
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                        <span className="w-10 text-right text-[11px] text-[#888888]">{count}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Write Review Toggle Button */}
              <div className="flex items-center justify-between">
                <h4 className="font-extrabold uppercase tracking-wider text-xs text-[#080808]">
                  Verified Customer Feedback
                </h4>
                <button
                  onClick={() => setShowReviewForm(!showReviewForm)}
                  className="px-4 py-2 bg-[#080808] text-[#C8BCA7] text-xs font-bold uppercase rounded hover:bg-[#222222] transition-colors"
                >
                  {showReviewForm ? 'Close Form' : 'Write a Review'}
                </button>
              </div>

              {/* Review Submission Form */}
              {showReviewForm && (
                <form onSubmit={handleReviewSubmit} className="bg-[#fcfbf9] border border-[#e5e1d8] rounded-xl p-5 sm:p-6 space-y-4">
                  <h5 className="font-bold text-xs uppercase tracking-wider text-[#080808]">
                    Share Your Experience
                  </h5>
                  <p className="text-[11px] text-[#777777]">
                    Only customers who have purchased this product can submit a verified review.
                  </p>

                  {reviewMessage && (
                    <div className={`p-3 rounded text-xs font-medium ${
                      reviewMessage.type === 'success' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-red-50 text-red-800 border border-red-200'
                    }`}>
                      {reviewMessage.text}
                    </div>
                  )}

                  {/* Rating selection */}
                  <div>
                    <label className="block text-[11px] font-bold uppercase tracking-wider text-[#080808] mb-1">
                      Rating *
                    </label>
                    <div className="flex gap-2">
                      {[1, 2, 3, 4, 5].map((s) => (
                        <button
                          key={s}
                          type="button"
                          onClick={() => setReviewRating(s)}
                          className="p-1 focus:outline-none"
                        >
                          <Star
                            className={`w-6 h-6 transition-colors ${
                              s <= reviewRating ? 'fill-amber-400 text-amber-400' : 'text-gray-300'
                            }`}
                          />
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-bold uppercase tracking-wider text-[#080808] mb-1">
                      Review Headline *
                    </label>
                    <input
                      type="text"
                      required
                      value={reviewTitle}
                      onChange={(e) => setReviewTitle(e.target.value)}
                      placeholder="e.g. Masterful proportion and beautiful finish"
                      className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-bold uppercase tracking-wider text-[#080808] mb-1">
                      Detailed Review *
                    </label>
                    <textarea
                      required
                      rows={3}
                      value={reviewComment}
                      onChange={(e) => setReviewComment(e.target.value)}
                      placeholder="Tell us about the craftsmanship, materials, and everyday experience..."
                      className="w-full px-3 py-2 border border-[#d5d0c5] rounded-lg text-xs text-[#080808] focus:outline-none focus:border-[#080808]"
                    />
                  </div>

                  <div className="flex justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setShowReviewForm(false)}
                      className="px-4 py-2 border border-[#e5e1d8] rounded text-xs font-semibold text-[#555555]"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isSubmittingReview}
                      className="px-6 py-2 bg-[#080808] text-[#C8BCA7] rounded text-xs font-bold uppercase tracking-wider hover:bg-[#222222] disabled:opacity-50"
                    >
                      {isSubmittingReview ? 'Submitting...' : 'Submit Review'}
                    </button>
                  </div>
                </form>
              )}

              {/* Reviews List */}
              <div className="space-y-4">
                {reviewSummary?.reviews && reviewSummary.reviews.length > 0 ? (
                  reviewSummary.reviews.map((rev) => (
                    <div key={rev.id} className="space-y-2 border-b border-[#f0ece5] pb-4 last:border-0">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-xs text-[#080808]">{rev.userName}</span>
                          {rev.verifiedPurchase && (
                            <span className="bg-emerald-100 text-emerald-800 text-[10px] font-bold px-2 py-0.5 rounded border border-emerald-300">
                              Verified Purchase
                            </span>
                          )}
                        </div>
                        <span className="text-[10px] text-[#888888]">
                          {new Date(rev.createdAt).toLocaleDateString('en-IN', {
                            day: 'numeric',
                            month: 'short',
                            year: 'numeric'
                          })}
                        </span>
                      </div>
                      <div className="flex text-amber-500">
                        {[1, 2, 3, 4, 5].map((s) => (
                          <Star
                            key={s}
                            className={`w-3.5 h-3.5 ${s <= rev.rating ? 'fill-current' : 'text-gray-200'}`}
                          />
                        ))}
                      </div>
                      <h5 className="text-xs font-bold text-[#080808]">{rev.title}</h5>
                      <p className="text-xs text-[#555555] leading-relaxed">{rev.comment}</p>
                    </div>
                  ))
                ) : (
                  <div className="text-center py-8 space-y-2">
                    <p className="text-xs text-[#777777]">No reviews yet for this object.</p>
                    <p className="text-[11px] text-[#aaaaaa]">Be the first verified customer to share your thoughts.</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Related Products */}
      <div className="mt-20">
        <h2 className="text-xl sm:text-2xl font-extrabold uppercase tracking-tight text-[#080808] mb-8">
          Complementary Objects
        </h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {related.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      </div>
    </div>
  );
}
