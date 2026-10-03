import React from 'react';
import { Metadata } from 'next';
import GiftCornerClient from './GiftCornerClient';

export const metadata: Metadata = {
  title: 'Gift Corner | REXXZO',
  description: 'Discover thoughtful and stylish gifts for every occasion at REXXZO.',
  alternates: {
    canonical: 'https://rexxo.in/shop/gift-corner',
  },
  openGraph: {
    title: 'Gift Corner | REXXZO',
    description: 'Discover thoughtful and stylish gifts for every occasion at REXXZO.',
    url: 'https://rexxo.in/shop/gift-corner',
    siteName: 'REXXZO',
    images: [
      {
        url: '/images/hero_banner.jpg',
        width: 1200,
        height: 630,
        alt: 'REXXZO Gift Corner — Thoughtful gifts for every occasion',
      },
    ],
    locale: 'en_IN',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Gift Corner | REXXZO',
    description: 'Discover thoughtful and stylish gifts for every occasion at REXXZO.',
    images: ['/images/hero_banner.jpg'],
  },
};

export default function GiftCornerPage() {
  return <GiftCornerClient />;
}
