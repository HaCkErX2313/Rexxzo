'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Cart, WishlistItem, Product } from '@/types';
import { api } from '@/lib/api';

interface StoreContextType {
  user: User | null;
  cart: Cart;
  wishlist: WishlistItem[];
  isCartOpen: boolean;
  setIsCartOpen: (open: boolean) => void;
  addToCart: (productId: number, quantity?: number) => Promise<void>;
  updateCartItem: (itemId: number, quantity: number) => Promise<void>;
  removeFromCart: (itemId: number) => Promise<void>;
  toggleWishlist: (product: Product) => Promise<void>;
  isInWishlist: (productId: number) => boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  refreshCart: () => Promise<void>;
  refreshWishlist: () => Promise<void>;
}

const StoreContext = createContext<StoreContextType | undefined>(undefined);

export function StoreProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [cart, setCart] = useState<Cart>({ items: [], subtotal: 0, shipping: 0, discount: 0, total: 0 });
  const [wishlist, setWishlist] = useState<WishlistItem[]>([]);
  const [isCartOpen, setIsCartOpen] = useState(false);

  useEffect(() => {
    // Initialize user, cart, wishlist from local storage/api
    const currentUser = api.getCurrentUser();
    if (currentUser) setUser(currentUser);

    api.getCart().then(setCart);
    api.getWishlist().then(setWishlist);
  }, []);

  const refreshCart = async () => {
    const updated = await api.getCart();
    setCart(updated);
  };

  const refreshWishlist = async () => {
    const updated = await api.getWishlist();
    setWishlist(updated);
  };

  const addToCart = async (productId: number, quantity: number = 1) => {
    const updated = await api.addToCart(productId, quantity);
    setCart(updated);
    setIsCartOpen(true);
  };

  const updateCartItem = async (itemId: number, quantity: number) => {
    try {
      const updated = await api.updateCartItem(itemId, quantity);
      setCart(updated);
    } catch (err) {
      try {
        const fresh = await api.getCart();
        setCart(fresh);
      } catch {}
      throw err;
    }
  };

  const removeFromCart = async (itemId: number) => {
    try {
      const updated = await api.removeFromCart(itemId);
      setCart(updated);
    } catch (err) {
      try {
        const fresh = await api.getCart();
        setCart(fresh);
      } catch {}
      throw err;
    }
  };

  const toggleWishlist = async (product: Product) => {
    const updated = await api.toggleWishlist(product);
    setWishlist(updated);
  };

  const isInWishlist = (productId: number) => {
    return wishlist.some(item => item.productId === productId);
  };

  const login = (token: string, loggedInUser: User) => {
    localStorage.setItem('rexxo_token', token);
    localStorage.setItem('rexxo_user', JSON.stringify(loggedInUser));
    setUser(loggedInUser);
    refreshCart();
    refreshWishlist();
  };

  const logout = () => {
    api.logout();
    setUser(null);
    refreshCart();
    refreshWishlist();
  };

  return (
    <StoreContext.Provider
      value={{
        user,
        cart,
        wishlist,
        isCartOpen,
        setIsCartOpen,
        addToCart,
        updateCartItem,
        removeFromCart,
        toggleWishlist,
        isInWishlist,
        login,
        logout,
        refreshCart,
        refreshWishlist,
      }}
    >
      {children}
    </StoreContext.Provider>
  );
}

export function useStore() {
  const context = useContext(StoreContext);
  if (!context) {
    throw new Error('useStore must be used within a StoreProvider');
  }
  return context;
}
