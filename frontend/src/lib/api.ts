import { Product, Category, Cart, CartItem, WishlistItem, Order, User, Review, Coupon } from '@/types';

const isClient = typeof window !== 'undefined';
const isLocalhost = isClient && (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');

const API_BASE_URL = 
  process.env.NEXT_PUBLIC_API_URL || 
  (isLocalhost ? 'http://localhost:8080/api' : 'https://rexxzo-backend.onrender.com/api');

// Initial curated demo products showcasing REXXZO aesthetics
export const DEMO_CATEGORIES: Category[] = [
  { id: 1, name: 'Home Decor', slug: 'home-decor', description: 'Handcrafted ceramic vessels and sculptural essentials', itemCount: 12, imageUrl: '/images/desk_lamp.jpg' },
  { id: 2, name: 'Kitchen & Dining', slug: 'kitchen-dining', description: 'Minimalist drinkware and artisanal dining ware', itemCount: 8, imageUrl: '/images/water_bottle.jpg' },
  { id: 3, name: 'Storage & Organizers', slug: 'storage-organizers', description: 'Functional architectural organization for calm spaces', itemCount: 6, imageUrl: '/images/storage_box.jpg' },
  { id: 4, name: 'Lighting', slug: 'lighting', description: 'Atmospheric dimmable lighting with tactile materiality', itemCount: 5, imageUrl: '/images/desk_lamp.jpg' },
  { id: 5, name: 'Lifestyle', slug: 'lifestyle', description: 'Acoustic fidelity wrapped in soft tactile aluminum', itemCount: 7, imageUrl: '/images/headphones.jpg' },
  { id: 6, name: 'Accessories', slug: 'accessories', description: 'Refined daily essentials for your workspace and home', itemCount: 9, imageUrl: '/images/hero_banner.jpg' },
  { id: 7, name: 'Gift Corner', slug: 'gift-corner', description: 'Thoughtful gifts for every occasion. Curated design objects, sculptural lighting, and artisanal lifestyle essentials.', itemCount: 10, imageUrl: '/images/hero_banner.jpg' },
];

export const DEMO_PRODUCTS: Product[] = [
  {
    id: 1,
    name: 'Aura Minimalist Ceramic Desk Lamp',
    slug: 'aura-minimalist-ceramic-desk-lamp',
    description: 'Designed with architectural proportion, the Aura Desk Lamp features a textured stone ceramic base with brushed brass accents. Emanates a warm, ambient glow with 3-step touch dimming.',
    price: 3499,
    compareAtPrice: 4299,
    sku: 'REX-LMP-01',
    stockQuantity: 18,
    category: DEMO_CATEGORIES[3],
    categoryId: 4,
    categoryName: 'Lighting',
    images: [{ imageUrl: '/images/desk_lamp.jpg', isPrimary: true, altText: 'Aura Minimalist Ceramic Desk Lamp' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.9,
    reviewCount: 42,
    materials: 'Natural terracotta ceramic, brushed brass, warm LED 2700K',
    dimensions: 'H 38cm × W 22cm × D 22cm',
    careInstructions: 'Wipe gently with a dry microfiber cloth.',
  },
  {
    id: 2,
    name: 'Kanso Studio Acoustic Wireless Headphones',
    slug: 'kanso-studio-acoustic-wireless-headphones',
    description: 'Mastercrafted sound with high-res planar acoustic drivers. Encased in bead-blasted anodized aluminum with memory-foam ear cushions wrapped in vegan leather.',
    price: 7999,
    compareAtPrice: 9499,
    sku: 'REX-AUD-02',
    stockQuantity: 14,
    category: DEMO_CATEGORIES[4],
    categoryId: 5,
    categoryName: 'Lifestyle',
    images: [{ imageUrl: '/images/headphones.jpg', isPrimary: true, altText: 'Kanso Studio Acoustic Wireless Headphones' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.8,
    reviewCount: 68,
    materials: 'Matte aluminum, vegan protein leather, high-fidelity dynamic drivers',
    dimensions: '190 × 165 × 80 mm',
    careInstructions: 'Store in protective case when not in use. Avoid moisture.',
  },
  {
    id: 3,
    name: 'Terra Ergonomic Insulated Water Bottle',
    slug: 'terra-ergonomic-insulated-water-bottle',
    description: 'Triple-wall vacuum insulated stainless steel canteen with a tactile powder finish. Keeps liquids cold for 36 hours or hot for 18 hours. Leakproof silicone-sealed cap.',
    price: 1499,
    compareAtPrice: 1899,
    sku: 'REX-BTL-03',
    stockQuantity: 45,
    category: DEMO_CATEGORIES[1],
    categoryId: 2,
    categoryName: 'Kitchen & Dining',
    images: [{ imageUrl: '/images/water_bottle.jpg', isPrimary: true, altText: 'Terra Ergonomic Insulated Water Bottle' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.9,
    reviewCount: 112,
    materials: '18/8 Pro-grade stainless steel, BPA-free silicone seal',
    dimensions: 'Capacity 750ml, Height 26cm, Base diameter 7.5cm',
    careInstructions: 'Hand wash recommended for powder coat longevity.',
  },
  {
    id: 4,
    name: 'Modula Architectural Felt Storage Box',
    slug: 'modula-architectural-felt-storage-box',
    description: 'Structured storage created from recycled compressed PET felt with solid beech wood handles. Stacks seamlessly to organize desk tools, textiles, and accessories.',
    price: 1899,
    compareAtPrice: 2299,
    sku: 'REX-STR-04',
    stockQuantity: 28,
    category: DEMO_CATEGORIES[2],
    categoryId: 3,
    categoryName: 'Storage & Organizers',
    images: [{ imageUrl: '/images/storage_box.jpg', isPrimary: true, altText: 'Modula Architectural Felt Storage Box' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.7,
    reviewCount: 39,
    materials: 'High-density recycled felt, sustainably sourced solid beech wood',
    dimensions: 'L 36cm × W 28cm × H 18cm',
    careInstructions: 'Spot clean with mild damp cloth. Do not soak.',
  },
  {
    id: 5,
    name: 'Sora Sculptural Stoneware Pitcher',
    slug: 'sora-sculptural-stoneware-pitcher',
    description: 'An elegant statement silhouette that serves beverage or elevates fresh botanicals. Hand-thrown stoneware finished in a velvety reactive matte off-white glaze.',
    price: 2499,
    compareAtPrice: 2999,
    sku: 'REX-HOM-05',
    stockQuantity: 16,
    category: DEMO_CATEGORIES[0],
    categoryId: 1,
    categoryName: 'Home Decor',
    images: [{ imageUrl: '/images/hero_banner.jpg', isPrimary: true, altText: 'Sora Sculptural Stoneware Pitcher' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.9,
    reviewCount: 27,
    materials: 'Artisanal stoneware ceramic, non-porous food-safe glaze',
    dimensions: 'Height 24cm, Capacity 1.2L',
    careInstructions: 'Dishwasher safe; gentle handwash recommended.',
  },
  {
    id: 6,
    name: 'Equinox Brass & Marble Incense Burner',
    slug: 'equinox-brass-marble-incense-burner',
    description: 'A meditative balance of polished green Udaipur marble and solid spun raw brass. Catches ash with graceful geometric composure.',
    price: 1299,
    compareAtPrice: 1599,
    sku: 'REX-ACC-06',
    stockQuantity: 32,
    category: DEMO_CATEGORIES[5],
    categoryId: 6,
    categoryName: 'Accessories',
    images: [{ imageUrl: '/images/desk_lamp.jpg', isPrimary: true, altText: 'Equinox Brass & Marble Incense Burner' }],
    isFeatured: false,
    isActive: true,
    averageRating: 4.6,
    reviewCount: 19,
    materials: 'Natural polished marble, brushed solid brass',
    dimensions: 'Diameter 12cm, Height 3cm',
    careInstructions: 'Wipe with soft dry cloth.',
  },
  {
    id: 7,
    name: 'Aura Ceramic Ambient Table Lamp',
    slug: 'aura-ceramic-ambient-table-lamp',
    description: 'Handcrafted stone ceramic lamp with brushed brass details. A comforting ambient glow for modern homes and thoughtful housewarming gifts.',
    price: 3499,
    compareAtPrice: 4299,
    sku: 'REX-GFT-01',
    stockQuantity: 25,
    category: DEMO_CATEGORIES[6],
    categoryId: 7,
    categoryName: 'Gift Corner',
    images: [{ imageUrl: '/images/desk_lamp.jpg', isPrimary: true, altText: 'Aura Ceramic Ambient Table Lamp' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.9,
    reviewCount: 48,
    materials: 'Natural terracotta ceramic, brushed brass, warm LED 2700K',
    dimensions: 'H 38cm × W 22cm × D 22cm',
    careInstructions: 'Wipe gently with a dry microfiber cloth.',
  },
  {
    id: 8,
    name: 'Sora Hand-Thrown Stoneware Vessel',
    slug: 'sora-hand-thrown-stoneware-vessel',
    description: 'An artisanal statement piece finished in reactive matte glaze. Perfect for celebratory florals or standalone sculptural presence.',
    price: 2499,
    compareAtPrice: 2999,
    sku: 'REX-GFT-02',
    stockQuantity: 18,
    category: DEMO_CATEGORIES[6],
    categoryId: 7,
    categoryName: 'Gift Corner',
    images: [{ imageUrl: '/images/hero_banner.jpg', isPrimary: true, altText: 'Sora Hand-Thrown Stoneware Vessel' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.85,
    reviewCount: 34,
    materials: 'Artisanal stoneware ceramic, non-porous food-safe glaze',
    dimensions: 'Height 24cm, Capacity 1.2L',
    careInstructions: 'Dishwasher safe; gentle handwash recommended.',
  },
  {
    id: 9,
    name: 'Equinox Brass & Marble Meditation Set',
    slug: 'equinox-brass-marble-meditation-set',
    description: 'Turned green marble base paired with spun solid brass. An exquisite tactile gift for mindful daily living.',
    price: 1299,
    compareAtPrice: 1599,
    sku: 'REX-GFT-03',
    stockQuantity: 40,
    category: DEMO_CATEGORIES[6],
    categoryId: 7,
    categoryName: 'Gift Corner',
    images: [{ imageUrl: '/images/desk_lamp.jpg', isPrimary: true, altText: 'Equinox Brass & Marble Meditation Set' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.95,
    reviewCount: 62,
    materials: 'Natural polished marble, brushed solid brass',
    dimensions: 'Diameter 12cm, Height 3cm',
    careInstructions: 'Wipe with soft dry cloth.',
  },
  {
    id: 10,
    name: 'Kanso Acoustic Headphone Gift Edition',
    slug: 'kanso-acoustic-headphone-gift-edition',
    description: 'Studio fidelity encased in bead-blasted anodized aluminum with vegan leather memory ear cushions. Complete in bespoke gift packaging.',
    price: 7999,
    compareAtPrice: 9499,
    sku: 'REX-GFT-04',
    stockQuantity: 12,
    category: DEMO_CATEGORIES[6],
    categoryId: 7,
    categoryName: 'Gift Corner',
    images: [{ imageUrl: '/images/headphones.jpg', isPrimary: true, altText: 'Kanso Acoustic Headphone Gift Edition' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.9,
    reviewCount: 89,
    materials: 'Matte aluminum, vegan protein leather, high-fidelity dynamic drivers',
    dimensions: '190 × 165 × 80 mm',
    careInstructions: 'Store in protective case when not in use. Avoid moisture.',
  },
  {
    id: 11,
    name: 'Terra Matte Travel Canteen Gift Box',
    slug: 'terra-matte-travel-canteen-gift-box',
    description: 'Triple-wall insulated stainless steel flask with powder-coated tactile grip. Keeps drinks cold for 36 hours. Delivered in premium gift packaging.',
    price: 1499,
    compareAtPrice: 1899,
    sku: 'REX-GFT-05',
    stockQuantity: 30,
    category: DEMO_CATEGORIES[6],
    categoryId: 7,
    categoryName: 'Gift Corner',
    images: [{ imageUrl: '/images/water_bottle.jpg', isPrimary: true, altText: 'Terra Matte Travel Canteen Gift Box' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.75,
    reviewCount: 29,
    materials: '18/8 Pro-grade stainless steel, BPA-free silicone seal',
    dimensions: 'Capacity 750ml, Height 26cm, Base diameter 7.5cm',
    careInstructions: 'Hand wash recommended for powder coat longevity.',
  },
  {
    id: 12,
    name: 'Architectural Desk Organizer Trio',
    slug: 'architectural-desk-organizer-trio',
    description: 'Compressed sustainable PET felt with solid beech wood handles. Elegantly clears workspace clutter for the modern creative.',
    price: 1899,
    compareAtPrice: 2299,
    sku: 'REX-GFT-06',
    stockQuantity: 22,
    category: DEMO_CATEGORIES[6],
    categoryId: 7,
    categoryName: 'Gift Corner',
    images: [{ imageUrl: '/images/storage_box.jpg', isPrimary: true, altText: 'Architectural Desk Organizer Trio' }],
    isFeatured: true,
    isActive: true,
    averageRating: 4.8,
    reviewCount: 41,
    materials: 'High-density recycled felt, sustainably sourced solid beech wood',
    dimensions: 'L 36cm × W 28cm × H 18cm',
    careInstructions: 'Spot clean with mild damp cloth. Do not soak.',
  },
];

function getAuthHeader(): Record<string, string> {
  if (typeof window === 'undefined') return {};
  const token = localStorage.getItem('rexxo_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export function getAdminAuthHeader(): Record<string, string> {
  if (typeof window === 'undefined') return {};
  const token = localStorage.getItem('rexxo_admin_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function handleAdminResponse<T = any>(res: Response, fallbackError: string): Promise<T> {
  if (res.status === 401 || res.status === 403) {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('rexxo_admin_token');
      localStorage.removeItem('rexxo_admin_user');
      window.location.href = '/admin/login';
    }
    throw new Error('Your admin session has expired or is invalid. Please log in again.');
  }
  if (res.status >= 500) {
    throw new Error('Unable to load this information. Please try again.');
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || err.error || fallbackError);
  }
  return res.json();
}

// Cart DTO formatter
function formatCartDto(data: any): Cart {
  if (!data) return { items: [], subtotal: 0, shipping: 0, discount: 0, total: 0 };
  const items: CartItem[] = (data.items || []).map((i: any) => {
    const unitPrice = Number(i.price ?? i.unitPrice ?? 0);
    const qty = Number(i.quantity ?? 1);
    const sub = Number(i.subtotal ?? unitPrice * qty);
    return {
      id: Number(i.id),
      productId: Number(i.productId),
      productName: i.productName || i.name || 'Product',
      productSlug: i.productSlug || i.slug || '',
      price: unitPrice,
      quantity: qty,
      imageUrl: i.imageUrl || '/images/hero_banner.jpg',
      stockQuantity: Number(i.stockQuantity ?? 99),
      subtotal: sub,
    };
  });
  const subtotal = Number(data.subtotal ?? items.reduce((sum, it) => sum + it.subtotal, 0));
  const shipping = Number(data.shipping ?? (subtotal > 2000 || subtotal === 0 ? 0 : 150));
  const discount = Number(data.discount ?? 0);
  const total = Number(data.total ?? Math.max(0, subtotal + shipping - discount));
  return {
    id: data.id ? Number(data.id) : undefined,
    items,
    subtotal,
    shipping,
    discount,
    total,
    couponCode: data.couponCode,
  };
}

// REST Client connecting to Spring Boot backend with robust fallback
export const api = {
  // Products
  async getProducts(params?: { category?: string; query?: string; sort?: string }): Promise<Product[]> {
    try {
      const queryParams = new URLSearchParams();
      if (params?.category) queryParams.set('category', params.category);
      if (params?.query) queryParams.set('search', params.query);
      if (params?.sort) {
        if (params.sort === 'price_asc') {
          queryParams.set('sortBy', 'price');
          queryParams.set('sortDir', 'asc');
        } else if (params.sort === 'price_desc') {
          queryParams.set('sortBy', 'price');
          queryParams.set('sortDir', 'desc');
        } else if (params.sort === 'rating') {
          queryParams.set('sortBy', 'avgRating');
          queryParams.set('sortDir', 'desc');
        }
      }
      queryParams.set('size', '100');
      
      const res = await fetch(`${API_BASE_URL}/products?${queryParams.toString()}`, {
        headers: { 'Content-Type': 'application/json' },
      });
      if (res.ok) {
        const data = await res.json();
        const list = Array.isArray(data) ? data : (data?.content || []);
        if (list.length > 0) {
          return list.map((item: any) => ({
            id: item.id,
            name: item.name,
            slug: item.slug,
            description: item.description || item.shortDescription || '',
            price: Number(item.price),
            compareAtPrice: item.originalPrice ? Number(item.originalPrice) : undefined,
            originalPrice: item.originalPrice ? Number(item.originalPrice) : undefined,
            discountPercent: item.discountPercent ? Number(item.discountPercent) : undefined,
            sku: item.sku || '',
            stockQuantity: item.stock !== undefined ? Number(item.stock) : 0,
            stock: item.stock !== undefined ? Number(item.stock) : 0,
            status: item.status || (item.stock > 0 ? 'ACTIVE' : 'OUT_OF_STOCK'),
            lowStockThreshold: item.lowStockThreshold || 10,
            categoryName: item.categoryName || (item.category?.name),
            category: item.category || {
              id: item.categoryId || 0,
              name: item.categoryName || '',
              slug: item.categorySlug || '',
            },
            images: item.images && item.images.length > 0 
              ? (typeof item.images[0] === 'string' ? item.images.map((url: string) => ({ imageUrl: url })) : item.images)
              : (item.primaryImage ? [{ imageUrl: item.primaryImage, isPrimary: true }] : [{ imageUrl: '/images/hero_banner.jpg', isPrimary: true }]),
            averageRating: item.avgRating ? Number(item.avgRating) : 5,
            reviewCount: item.reviewCount || 0,
            isFeatured: !!item.isFeatured,
            isActive: item.isActive !== undefined ? item.isActive : true,
          }));
        }
      }
    } catch {
      // Backend offline or unreachable: gracefully use local curated catalog
    }
    
    // Apply in-memory filtering for demo catalog
    let list = [...DEMO_PRODUCTS];
    const catFilter = params?.category;
    if (catFilter && catFilter !== 'all') {
      list = list.filter(p => p.category?.slug === catFilter || p.categoryName?.toLowerCase() === catFilter.toLowerCase());
    }
    if (params?.query) {
      const q = params.query.toLowerCase();
      list = list.filter(p => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q));
    }
    if (params?.sort === 'price_asc') list.sort((a, b) => a.price - b.price);
    if (params?.sort === 'price_desc') list.sort((a, b) => b.price - a.price);
    if (params?.sort === 'rating') list.sort((a, b) => b.averageRating - a.averageRating);
    return list;
  },

  async getProduct(idOrSlug: string | number): Promise<Product | null> {
    try {
      const res = await fetch(`${API_BASE_URL}/products/${idOrSlug}`, {
        headers: { 'Content-Type': 'application/json' },
      });
      if (res.ok) {
        const item = await res.json();
        return {
          id: item.id,
          name: item.name,
          slug: item.slug,
          description: item.description || item.shortDescription || '',
          price: Number(item.price),
          compareAtPrice: item.originalPrice ? Number(item.originalPrice) : undefined,
          originalPrice: item.originalPrice ? Number(item.originalPrice) : undefined,
          discountPercent: item.discountPercent ? Number(item.discountPercent) : undefined,
          sku: item.sku || '',
          stockQuantity: item.stock !== undefined ? Number(item.stock) : 0,
          stock: item.stock !== undefined ? Number(item.stock) : 0,
          status: item.status || (item.stock > 0 ? 'ACTIVE' : 'OUT_OF_STOCK'),
          lowStockThreshold: item.lowStockThreshold || 10,
          categoryName: item.categoryName || (item.category?.name),
          category: item.category || {
            id: item.categoryId || 0,
            name: item.categoryName || '',
            slug: item.categorySlug || '',
          },
          images: item.images && item.images.length > 0
            ? (typeof item.images[0] === 'string' ? item.images.map((url: string) => ({ imageUrl: url })) : item.images)
            : [{ imageUrl: '/images/hero_banner.jpg', isPrimary: true }],
          averageRating: item.avgRating ? Number(item.avgRating) : 5,
          reviewCount: item.reviewCount || 0,
          isFeatured: !!item.isFeatured,
          isActive: true,
          materials: item.material,
          dimensions: item.dimensions,
          weightGrams: item.weightGrams,
        };
      }
    } catch {
      // Backend offline
    }
    return DEMO_PRODUCTS.find(p => p.id === Number(idOrSlug) || p.slug === idOrSlug) || DEMO_PRODUCTS[0];
  },

  // Categories
  async getCategories(): Promise<Category[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/categories`);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) return data;
      }
    } catch {
      // fallback
    }
    return DEMO_CATEGORIES;
  },

  // Cart (supports both backend endpoints and offline sync)
  async getCart(): Promise<Cart> {
    const authHeaders = getAuthHeader();
    if (authHeaders.Authorization) {
      try {
        const res = await fetch(`${API_BASE_URL}/cart`, {
          headers: { ...authHeaders, 'Content-Type': 'application/json' },
        });
        if (res.ok) {
          const raw = await res.json();
          return formatCartDto(raw);
        }
        if (res.status === 401) {
          localStorage.removeItem('rexxo_token');
          localStorage.removeItem('rexxo_user');
        }
      } catch {
        // fallback
      }
    }
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('rexxo_cart');
      if (stored) {
        try {
          return formatCartDto(JSON.parse(stored));
        } catch {}
      }
    }
    return { items: [], subtotal: 0, shipping: 0, discount: 0, total: 0 };
  },

  async addToCart(productId: number, quantity: number = 1): Promise<Cart> {
    const authHeaders = getAuthHeader();
    if (authHeaders.Authorization) {
      const res = await fetch(`${API_BASE_URL}/cart/items`, {
        method: 'POST',
        headers: { ...authHeaders, 'Content-Type': 'application/json' },
        body: JSON.stringify({ productId, quantity }),
      });
      if (res.ok) {
        const raw = await res.json();
        return formatCartDto(raw);
      }
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to add item to cart');
    }

    // Offline / guest local cart updater
    const current = await this.getCart();
    let product = await this.getProduct(productId);
    if (!product) {
      product = DEMO_PRODUCTS.find(p => p.id === productId) || null;
    }
    const stockLimit = product ? (product.stock !== undefined ? product.stock : product.stockQuantity) : 99;
    const existing = current.items.find(i => i.productId === productId);
    if (existing) {
      if (existing.quantity + quantity > stockLimit) {
        throw new Error(`Insufficient stock. Only ${stockLimit} units available.`);
      }
      existing.quantity += quantity;
      existing.subtotal = existing.quantity * existing.price;
    } else if (product) {
      if (quantity > stockLimit) {
        throw new Error(`Insufficient stock. Only ${stockLimit} units available.`);
      }
      current.items.push({
        id: Date.now(),
        productId: product.id,
        productName: product.name,
        productSlug: product.slug,
        price: product.price,
        quantity,
        imageUrl: product.images && product.images.length > 0 ? product.images[0].imageUrl : '/images/hero_banner.jpg',
        stockQuantity: stockLimit,
        subtotal: product.price * quantity,
      });
    }
    const subtotal = current.items.reduce((sum, item) => sum + item.subtotal, 0);
    const shipping = subtotal > 2000 || subtotal === 0 ? 0 : 150;
    const discount = current.discount || 0;
    const total = Math.max(0, subtotal + shipping - discount);
    const updatedCart: Cart = { ...current, subtotal, shipping, discount, total };
    if (typeof window !== 'undefined') {
      localStorage.setItem('rexxo_cart', JSON.stringify(updatedCart));
    }
    return updatedCart;
  },

  async updateCartItem(itemId: number, quantity: number): Promise<Cart> {
    const authHeaders = getAuthHeader();
    if (authHeaders.Authorization) {
      const res = await fetch(`${API_BASE_URL}/cart/items/${itemId}`, {
        method: 'PUT',
        headers: { ...authHeaders, 'Content-Type': 'application/json' },
        body: JSON.stringify({ quantity }),
      });
      if (res.ok) {
        const raw = await res.json();
        return formatCartDto(raw);
      }
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to update item quantity');
    }

    // Guest cart updater
    const current = await this.getCart();
    if (quantity <= 0) {
      current.items = current.items.filter(i => i.id !== itemId);
    } else {
      const item = current.items.find(i => i.id === itemId);
      if (item) {
        if (quantity > (item.stockQuantity || 99)) {
          throw new Error(`Insufficient stock. Only ${item.stockQuantity} units available.`);
        }
        item.quantity = quantity;
        item.subtotal = item.price * quantity;
      }
    }
    const subtotal = current.items.reduce((sum, item) => sum + item.subtotal, 0);
    const shipping = subtotal > 2000 || subtotal === 0 ? 0 : 150;
    const discount = current.discount || 0;
    const total = Math.max(0, subtotal + shipping - discount);
    const updatedCart: Cart = { ...current, subtotal, shipping, discount, total };
    if (typeof window !== 'undefined') {
      localStorage.setItem('rexxo_cart', JSON.stringify(updatedCart));
    }
    return updatedCart;
  },

  async removeFromCart(itemId: number): Promise<Cart> {
    const authHeaders = getAuthHeader();
    if (authHeaders.Authorization) {
      try {
        const res = await fetch(`${API_BASE_URL}/cart/items/${itemId}`, {
          method: 'DELETE',
          headers: { ...authHeaders, 'Content-Type': 'application/json' },
        });
        if (res.ok) {
          const raw = await res.json();
          return formatCartDto(raw);
        }
      } catch {
        // fallback
      }
    }
    return this.updateCartItem(itemId, 0);
  },

  // Wishlist
  async getWishlist(): Promise<WishlistItem[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/wishlist`, {
        headers: { ...getAuthHeader() },
      });
      if (res.ok) return await res.json();
    } catch {
      // fallback
    }
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('rexxo_wishlist');
      if (stored) return JSON.parse(stored);
    }
    return [];
  },

  async toggleWishlist(product: Product): Promise<WishlistItem[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/wishlist/${product.id}`, {
        method: 'POST',
        headers: { ...getAuthHeader() },
      });
      if (res.ok) return await res.json();
    } catch {
      // fallback
    }
    const current = await this.getWishlist();
    const idx = current.findIndex(w => w.productId === product.id);
    let updated: WishlistItem[];
    if (idx >= 0) {
      updated = current.filter(w => w.productId !== product.id);
    } else {
      updated = [...current, {
        id: Date.now(),
        productId: product.id,
        productName: product.name,
        productSlug: product.slug,
        price: product.price,
        imageUrl: product.images[0]?.imageUrl,
        stockQuantity: product.stockQuantity,
        averageRating: product.averageRating,
      }];
    }
    if (typeof window !== 'undefined') {
      localStorage.setItem('rexxo_wishlist', JSON.stringify(updated));
    }
    return updated;
  },

  // Orders
  async placeOrder(orderData: Partial<Order>): Promise<Order> {
    try {
      const res = await fetch(`${API_BASE_URL}/orders`, {
        method: 'POST',
        headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify(orderData),
      });
      if (res.ok) return await res.json();
    } catch {
      // fallback
    }
    const newOrder: Order = {
      id: Math.floor(Math.random() * 90000) + 10000,
      orderNumber: `REX-${Date.now().toString().slice(-6)}`,
      status: 'CONFIRMED',
      subtotal: orderData.subtotal || 0,
      shippingFee: orderData.shippingFee || 0,
      discountAmount: orderData.discountAmount || 0,
      totalAmount: orderData.totalAmount || 0,
      items: orderData.items || [],
      shippingAddress: orderData.shippingAddress || {
        street: '123 Minimalist Avenue',
        city: 'Bengaluru',
        state: 'Karnataka',
        postalCode: '560001',
        country: 'India',
      },
      paymentMethod: orderData.paymentMethod || 'Online (Razorpay / UPI)',
      paymentStatus: 'COMPLETED',
      createdAt: new Date().toISOString(),
    };
    if (typeof window !== 'undefined') {
      const existing = JSON.parse(localStorage.getItem('rexxo_orders') || '[]');
      localStorage.setItem('rexxo_orders', JSON.stringify([newOrder, ...existing]));
      localStorage.removeItem('rexxo_cart');
    }
    return newOrder;
  },

  async getOrders(): Promise<Order[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/orders`, {
        headers: { ...getAuthHeader() },
      });
      if (res.ok) {
        const data = await res.json();
        const rawList = Array.isArray(data) ? data : (data && Array.isArray(data.content) ? data.content : []);
        return rawList.map((o: any) => ({
          id: o.id,
          orderNumber: o.orderNumber || `REX-${o.id}`,
          status: o.status || 'CONFIRMED',
          subtotal: Number(o.subtotal ?? o.total ?? 0),
          shippingFee: Number(o.shippingFee ?? 0),
          discountAmount: Number(o.discountAmount ?? 0),
          totalAmount: Number(o.totalAmount ?? o.total ?? 0),
          items: Array.isArray(o.items)
            ? o.items.map((i: any) => ({
                id: i.id,
                productId: i.productId || i.id,
                productName: i.productName || 'Curated Item',
                quantity: i.quantity || 1,
                price: Number(i.price ?? i.unitPrice ?? 0),
                imageUrl: i.imageUrl || i.productImageUrl || '/images/hero_banner.jpg',
              }))
            : [],
          shippingAddress: {
            street: o.shippingAddress?.street || o.shippingAddress || '',
            city: o.shippingAddress?.city || o.shippingCity || 'Bengaluru',
            state: o.shippingAddress?.state || o.shippingState || 'Karnataka',
            postalCode: o.shippingAddress?.postalCode || o.shippingPincode || '560001',
            country: 'India',
          },
          paymentMethod: o.paymentMethod || 'Online (Razorpay / UPI)',
          paymentStatus: o.paymentStatus || 'COMPLETED',
          createdAt: o.createdAt || new Date().toISOString(),
        }));
      }
    } catch {
      // fallback
    }
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('rexxo_orders');
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed)) return parsed;
        } catch {
          // ignore corrupted storage
        }
      }
    }
    return [];
  },

  // Auth - strictly communicates with Spring Boot /api/auth
  async login(email: string, password: string): Promise<{ token: string; user: User }> {
    let res: Response;
    try {
      res = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
    } catch {
      throw new Error('Unable to connect to authentication server. Please verify backend is running.');
    }

    if (!res.ok) {
      let message = 'Invalid email or password.';
      try {
        const err = await res.json();
        if (err && err.message) {
          message = err.message;
        }
      } catch {
        // fallback to default
      }
      throw new Error(message);
    }

    const data = await res.json();
    if (typeof window !== 'undefined') {
      localStorage.setItem('rexxo_token', data.token);
      localStorage.setItem('rexxo_user', JSON.stringify(data.user));
    }
    return data;
  },

  async initiateSignup(fullName: string, email: string, phone: string, password: string, confirmPassword: string): Promise<{ success: boolean; message: string; verificationId: string; maskedEmail: string; maskedPhone: string }> {
    let res: Response;
    try {
      res = await fetch(`${API_BASE_URL}/auth/signup/initiate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fullName, email, phone, password, confirmPassword }),
      });
    } catch {
      throw new Error('Unable to connect to authentication server. Please verify backend is running.');
    }

    if (!res.ok) {
      let message = 'Failed to initiate signup verification.';
      try {
        const err = await res.json();
        if (err && err.message) message = err.message;
      } catch {
        // fallback
      }
      throw new Error(message);
    }

    return await res.json();
  },

  async verifySignup(verificationId: string, emailOtp: string, phoneOtp: string): Promise<{ token: string; user: User }> {
    let res: Response;
    try {
      res = await fetch(`${API_BASE_URL}/auth/signup/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ verificationId, emailOtp, phoneOtp }),
      });
    } catch {
      throw new Error('Unable to connect to authentication server. Please verify backend is running.');
    }

    if (!res.ok) {
      let message = 'Verification failed. Please check your codes.';
      try {
        const err = await res.json();
        if (err && err.message) message = err.message;
      } catch {
        // fallback
      }
      throw new Error(message);
    }

    const data = await res.json();
    if (typeof window !== 'undefined') {
      localStorage.setItem('rexxo_token', data.token);
      localStorage.setItem('rexxo_user', JSON.stringify(data.user));
    }
    return data;
  },

  async resendEmailOtp(verificationId: string): Promise<{ success: boolean; message: string }> {
    let res: Response;
    try {
      res = await fetch(`${API_BASE_URL}/auth/signup/resend-email-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ verificationId }),
      });
    } catch {
      throw new Error('Unable to connect to authentication server.');
    }

    if (!res.ok) {
      let message = 'Failed to resend email code.';
      try {
        const err = await res.json();
        if (err && err.message) message = err.message;
      } catch {
        // fallback
      }
      throw new Error(message);
    }

    return await res.json();
  },

  async resendPhoneOtp(verificationId: string): Promise<{ success: boolean; message: string }> {
    let res: Response;
    try {
      res = await fetch(`${API_BASE_URL}/auth/signup/resend-phone-otp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ verificationId }),
      });
    } catch {
      throw new Error('Unable to connect to authentication server.');
    }

    if (!res.ok) {
      let message = 'Failed to resend mobile code.';
      try {
        const err = await res.json();
        if (err && err.message) message = err.message;
      } catch {
        // fallback
      }
      throw new Error(message);
    }

    return await res.json();
  },

  async register(name: string, email: string, password: string, phone?: string): Promise<{ token: string; user: User }> {
    let res: Response;
    try {
      res = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fullName: name,
          name: name,
          email,
          password,
          phone: phone || '',
        }),
      });
    } catch {
      throw new Error('Unable to connect to authentication server.');
    }

    if (!res.ok) {
      let message = 'Registration failed. Please check your information.';
      try {
        const err = await res.json();
        if (err && err.message) message = err.message;
        else if (err && err.fieldErrors) {
          const firstErr = Object.values(err.fieldErrors)[0];
          if (firstErr) message = String(firstErr);
        }
      } catch {
        // fallback
      }
      throw new Error(message);
    }

    const data = await res.json();
    if (data.token) {
      localStorage.setItem('rexxo_token', data.token);
      localStorage.setItem('rexxo_user', JSON.stringify(data.user));
    }
    return data;
  },

  getCurrentUser(): User | null {
    if (typeof window === 'undefined') return null;
    const stored = localStorage.getItem('rexxo_user');
    return stored ? JSON.parse(stored) : null;
  },

  logout(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('rexxo_token');
      localStorage.removeItem('rexxo_user');
    }
  },

  // Admin APIs
  async getAdminStats() {
    try {
      const res = await fetch(`${API_BASE_URL}/admin/metrics`, {
        headers: { ...getAuthHeader() },
      });
      if (res.ok) return await res.json();
    } catch {
      // fallback
    }
    return {
      totalRevenue: 248900,
      totalOrders: 84,
      totalProducts: DEMO_PRODUCTS.length,
      totalCustomers: 62,
    };
  },

  async getAdminProducts(params?: {
    search?: string;
    categoryId?: number;
    status?: string;
    stockFilter?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<{ content: Product[]; totalElements: number; totalPages: number; number: number; size: number }> {
    const q = new URLSearchParams();
    if (params?.search) q.set('search', params.search);
    if (params?.categoryId) q.set('categoryId', String(params.categoryId));
    if (params?.status && params.status !== 'ALL') q.set('status', params.status);
    if (params?.stockFilter && params.stockFilter !== 'ALL') q.set('stockFilter', params.stockFilter);
    if (params?.page !== undefined) q.set('page', String(params.page));
    if (params?.size !== undefined) q.set('size', String(params.size));
    if (params?.sortBy) q.set('sortBy', params.sortBy);
    if (params?.sortDir) q.set('sortDir', params.sortDir);

    const res = await fetch(`${API_BASE_URL}/admin/products?${q.toString()}`, {
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
    });
    return handleAdminResponse(res, 'Failed to fetch admin products');
  },

  async getAdminProduct(id: number): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/products/${id}`, {
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
    });
    return handleAdminResponse(res, 'Failed to fetch product details');
  },

  async createAdminProduct(payload: any): Promise<Product> {
    const res = await fetch(`${API_BASE_URL}/admin/products`, {
      method: 'POST',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return handleAdminResponse(res, 'Failed to create product');
  },

  async updateAdminProduct(id: number, payload: any): Promise<Product> {
    const res = await fetch(`${API_BASE_URL}/admin/products/${id}`, {
      method: 'PUT',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return handleAdminResponse(res, 'Failed to update product');
  },

  async deleteAdminProduct(id: number): Promise<{ success: boolean; message: string; action: string }> {
    const res = await fetch(`${API_BASE_URL}/admin/products/${id}`, {
      method: 'DELETE',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
    });
    return handleAdminResponse(res, 'Failed to delete product');
  },

  async toggleAdminProductStatus(id: number): Promise<{ success: boolean; isActive: boolean; status: string }> {
    const res = await fetch(`${API_BASE_URL}/admin/products/${id}/toggle`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
    });
    return handleAdminResponse(res, 'Failed to toggle product status');
  },

  async uploadAdminProductImage(file: File): Promise<{ url: string; filename: string }> {
    const formData = new FormData();
    formData.append('file', file);
    const headers = getAdminAuthHeader();
    const res = await fetch(`${API_BASE_URL}/admin/products/upload-image`, {
      method: 'POST',
      headers: { ...headers },
      body: formData,
    });
    return handleAdminResponse(res, 'Failed to upload product image');
  },

  async createProduct(productData: Partial<Product>): Promise<Product> {
    return this.createAdminProduct(productData);
  },

  async deleteProduct(id: number): Promise<boolean> {
    await this.deleteAdminProduct(id);
    return true;
  },

  // ─── ORDER CANCELLATION ──────────────────────────────────────────────────
  async cancelOrder(orderId: number, reason: string): Promise<{ success: boolean; message: string }> {
    const res = await fetch(`${API_BASE_URL}/orders/${orderId}/cancel`, {
      method: 'PATCH',
      headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to cancel order');
    }
    return res.json();
  },

  // ─── RETURNS & REFUNDS ───────────────────────────────────────────────────
  async getReturnEligibility(orderId: number): Promise<import('@/types').ReturnEligibility> {
    const res = await fetch(`${API_BASE_URL}/returns/orders/${orderId}/eligibility`, {
      headers: { ...getAuthHeader() },
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to check return eligibility');
    }
    return res.json();
  },

  async requestReturn(data: {
    orderId: number;
    reason: string;
    description?: string;
    items: Array<{ orderItemId: number; quantity: number }>;
  }): Promise<import('@/types').ReturnRequest> {
    const res = await fetch(`${API_BASE_URL}/returns`, {
      method: 'POST',
      headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to submit return request');
    }
    return res.json();
  },

  async getMyReturns(page = 0, size = 10): Promise<{ content: import('@/types').ReturnRequest[]; totalElements: number; totalPages: number }> {
    const res = await fetch(`${API_BASE_URL}/returns/my?page=${page}&size=${size}`, {
      headers: { ...getAuthHeader() },
    });
    if (!res.ok) {
      throw new Error('Failed to fetch returns');
    }
    return res.json();
  },

  async getReturnById(returnId: number): Promise<import('@/types').ReturnRequest> {
    const res = await fetch(`${API_BASE_URL}/returns/${returnId}`, {
      headers: { ...getAuthHeader() },
    });
    if (!res.ok) {
      throw new Error('Return request not found');
    }
    return res.json();
  },

  async cancelReturn(returnId: number): Promise<import('@/types').ReturnRequest> {
    const res = await fetch(`${API_BASE_URL}/returns/${returnId}/cancel`, {
      method: 'PATCH',
      headers: { ...getAuthHeader() },
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to cancel return request');
    }
    return res.json();
  },

  // ─── REVIEWS ─────────────────────────────────────────────────────────────
  async getProductReviewSummary(productId: number): Promise<import('@/types').ReviewSummary> {
    const res = await fetch(`${API_BASE_URL}/reviews/product/${productId}/summary`);
    if (!res.ok) {
      return { averageRating: 0, totalReviews: 0, ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }, reviews: [] };
    }
    return res.json();
  },

  async submitProductReview(productId: number, data: { rating: number; title: string; comment: string }): Promise<Review> {
    const res = await fetch(`${API_BASE_URL}/reviews/product/${productId}`, {
      method: 'POST',
      headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ rating: data.rating, title: data.title, body: data.comment }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to submit review');
    }
    return res.json();
  },

  // ─── ANALYTICS ───────────────────────────────────────────────────────────
  async trackAnalyticsEvent(eventType: string, eventData: Record<string, unknown> = {}): Promise<void> {
    try {
      await fetch(`${API_BASE_URL}/analytics/events`, {
        method: 'POST',
        headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify({
          eventType,
          productId: eventData.productId,
          orderId: eventData.orderId,
          pageUrl: typeof window !== 'undefined' ? window.location.pathname : undefined,
          metadata: JSON.stringify(eventData),
        }),
      });
    } catch {
      // non-blocking
    }
  },

  // ─── ADMIN AUTHENTICATION & ACCESS CONTROL ─────────────────────────────
  async getAdminSetupStatus(): Promise<{ setupRequired: boolean; message: string }> {
    const res = await fetch(`${API_BASE_URL}/admin/auth/setup-status`);
    if (!res.ok) throw new Error('Failed to verify admin setup status');
    return res.json();
  },

  async adminSetup(data: { name: string; email: string; password: string }): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/auth/setup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || err.error || 'Administrator setup failed');
    }
    const result = await res.json();
    if (result.token && typeof window !== 'undefined') {
      localStorage.setItem('rexxo_admin_token', result.token);
      localStorage.setItem('rexxo_admin_user', JSON.stringify({ email: result.email, name: result.name, role: result.role }));
    }
    return result;
  },

  async adminLogin(data: { email: string; password: string }): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'You entered an incorrect email or password.');
    }
    const result = await res.json();
    if (result.token && typeof window !== 'undefined') {
      localStorage.setItem('rexxo_admin_token', result.token);
      localStorage.setItem('rexxo_admin_user', JSON.stringify({ email: result.email, name: result.name, role: result.role }));
    }
    return result;
  },

  adminLogout(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('rexxo_admin_token');
      localStorage.removeItem('rexxo_admin_user');
      window.location.href = '/admin/login';
    }
  },

  getAdminUser(): { email: string; name: string; role: string } | null {
    if (typeof window === 'undefined') return null;
    const raw = localStorage.getItem('rexxo_admin_user');
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },

  async createAdmin(data: { name: string; email: string; password: string }): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/users/create-admin`, {
      method: 'POST',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleAdminResponse(res, 'Failed to create administrator account');
  },

  // ─── ADMIN DASHBOARD & MODULES ───────────────────────────────────────────
  async getAdminDashboard(): Promise<import('@/types').AdminDashboardData> {
    const res = await fetch(`${API_BASE_URL}/admin/dashboard`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load admin dashboard');
  },

  async getAdminOrders(params: { status?: string; search?: string; page?: number; size?: number } = {}): Promise<any> {
    const query = new URLSearchParams();
    if (params.status && params.status !== 'ALL') query.set('status', params.status);
    if (params.search) query.set('search', params.search);
    query.set('page', String(params.page || 0));
    query.set('size', String(params.size || 20));

    const res = await fetch(`${API_BASE_URL}/admin/orders?${query.toString()}`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load orders');
  },

  async updateAdminOrderStatus(orderId: number, status: string): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
    return handleAdminResponse(res, 'Failed to update order status');
  },



  async toggleAdminProduct(productId: number): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/products/${productId}/toggle`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to toggle product status');
  },

  async getAdminInventory(page = 0, size = 30): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/inventory?page=${page}&size=${size}`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load inventory');
  },

  async adjustAdminStock(productId: number, stock: number): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/inventory/${productId}/stock`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ stock }),
    });
    return handleAdminResponse(res, 'Failed to adjust stock');
  },

  async getAdminReturns(status?: string, page = 0, size = 20): Promise<any> {
    const query = new URLSearchParams();
    if (status && status !== 'ALL') query.set('status', status);
    query.set('page', String(page));
    query.set('size', String(size));

    const res = await fetch(`${API_BASE_URL}/admin/returns?${query.toString()}`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load returns');
  },

  async approveAdminReturn(returnId: number, adminNotes?: string): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/returns/${returnId}/approve`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ adminNotes }),
    });
    return handleAdminResponse(res, 'Failed to approve return');
  },

  async rejectAdminReturn(returnId: number, rejectionReason: string): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/returns/${returnId}/reject`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ rejectionReason }),
    });
    return handleAdminResponse(res, 'Failed to reject return');
  },

  async scheduleAdminReturnPickup(returnId: number, courier: string, awb: string): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/returns/${returnId}/pickup`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ courier, awb }),
    });
    return handleAdminResponse(res, 'Failed to schedule pickup');
  },

  async receiveAdminReturn(returnId: number): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/returns/${returnId}/receive`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to mark return received');
  },

  async approveAdminRefund(returnId: number): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/returns/${returnId}/refund`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to approve refund');
  },

  async getAdminRefunds(page = 0, size = 20): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/refunds?page=${page}&size=${size}`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load refunds');
  },

  async getAdminReviews(status?: string, page = 0, size = 20): Promise<any> {
    const query = new URLSearchParams();
    if (status && status !== 'ALL') query.set('status', status);
    query.set('page', String(page));
    query.set('size', String(size));

    const res = await fetch(`${API_BASE_URL}/admin/reviews?${query.toString()}`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load reviews');
  },

  async moderateAdminReview(reviewId: number, status: 'APPROVED' | 'REJECTED'): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/reviews/${reviewId}/moderate`, {
      method: 'PATCH',
      headers: { ...getAdminAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
    return handleAdminResponse(res, 'Failed to moderate review');
  },

  async getAdminAnalytics(range = '30d'): Promise<import('@/types').AnalyticsSummary> {
    const res = await fetch(`${API_BASE_URL}/admin/analytics?range=${range}`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load analytics');
  },

  async getAdminIntegrations(): Promise<import('@/types').AdminIntegrationsStatus> {
    const res = await fetch(`${API_BASE_URL}/admin/integrations`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load integrations');
  },

  async getAdminAuditLogs(page = 0, size = 30): Promise<any> {
    const res = await fetch(`${API_BASE_URL}/admin/audit-logs?page=${page}&size=${size}`, {
      headers: { ...getAdminAuthHeader() },
    });
    return handleAdminResponse(res, 'Failed to load audit logs');
  },

  /**
   * Fetch all categories for admin (including inactive)
   */
  async getAdminCategories(): Promise<Category[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/categories/admin/all`, {
        headers: getAdminAuthHeader(),
      });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) return data;
      }
    } catch (e) {
      console.warn('Backend admin categories unavailable, using mock categories', e);
    }
    return DEMO_CATEGORIES;
  },

  /**
   * Toggle category active state
   */
  async toggleCategory(id: number): Promise<{ success: boolean; isActive: boolean }> {
    try {
      const res = await fetch(`${API_BASE_URL}/categories/${id}/toggle`, {
        method: 'PATCH',
        headers: getAdminAuthHeader(),
      });
      if (res.ok) return res.json();
    } catch (e) {
      console.warn('Failed to toggle category on backend', e);
    }
    return { success: true, isActive: true };
  },

  /**
   * Update product category
   */
  async updateProductCategory(productId: number, categoryId: number): Promise<Product> {
    const res = await fetch(`${API_BASE_URL}/products/${productId}/category?categoryId=${categoryId}`, {
      method: 'PATCH',
      headers: getAdminAuthHeader(),
    });
    return handleAdminResponse(res, 'Failed to update product category');
  },
};

// ─── Shipping API ──────────────────────────────────────────────────────────────
// All calls go to Spring Boot — Shiprocket credentials NEVER reach the browser.

export const shippingApi = {
  /**
   * Real-time Indian pincode lookup with auto-fill (City, District, State).
   * Calls Spring Boot backend, never calls external API directly.
   */
  async lookupPincode(pincode: string): Promise<{
    success: boolean;
    pincode: string;
    state: string;
    district: string;
    city: string;
    locations: Array<{ name: string; district: string; state: string }>;
    message: string;
  }> {
    const res = await fetch(`${API_BASE_URL}/shipping/pincode/${pincode}`);
    if (!res.ok) {
      throw new Error('Pincode lookup failed');
    }
    return res.json();
  },

  /**
   * Check if delivery is available to the given pincode.
   * Called during checkout after address entry.
   */
  async checkServiceability(params: {
    deliveryPincode: string;
    weightGrams?: number;
    cod?: boolean;
    orderValue?: number;
  }): Promise<import('@/types').ServiceabilityResponse> {
    const res = await fetch(`${API_BASE_URL}/shipping/serviceability`, {
      method: 'POST',
      headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
    if (!res.ok) {
      throw new Error('Failed to check serviceability');
    }
    return res.json();
  },

  /**
   * Get order tracking information from the backend.
   * Backend syncs with Shiprocket automatically.
   */
  async getOrderTracking(orderId: number): Promise<import('@/types').ShipmentTracking | null> {
    try {
      const res = await fetch(`${API_BASE_URL}/shipping/track/${orderId}`, {
        headers: { ...getAuthHeader() },
      });
      if (res.ok) return res.json();
      return null;
    } catch {
      return null;
    }
  },

  /**
   * Create a Razorpay payment order for the given REXXZO order.
   * Returns razorpayOrderId and key for the Razorpay checkout modal.
   */
  async createRazorpayOrder(orderId: number): Promise<{
    razorpayOrderId: string;
    amountPaisa: number;
    currency: string;
    keyId: string;
  }> {
    const res = await fetch(`${API_BASE_URL}/payments/create-order`, {
      method: 'POST',
      headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ orderId }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to initialize payment');
    }
    return res.json();
  },

  /**
   * Verify Razorpay payment signature on the backend.
   * This is the ONLY way an order gets marked as PAID.
   */
  async verifyPayment(params: {
    razorpayOrderId: string;
    razorpayPaymentId: string;
    razorpaySignature: string;
  }): Promise<{ success: boolean; orderNumber: string; message: string }> {
    const res = await fetch(`${API_BASE_URL}/payments/verify`, {
      method: 'POST',
      headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Payment verification failed');
    }
    return res.json();
  },

  /**
   * Confirm a COD order. No payment collected — courier collects on delivery.
   */
  async confirmCodOrder(orderId: number): Promise<{ success: boolean; orderNumber: string; message: string }> {
    const res = await fetch(`${API_BASE_URL}/payments/confirm-cod`, {
      method: 'POST',
      headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ orderId }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'COD confirmation failed');
    }
    return res.json();
  },
};


