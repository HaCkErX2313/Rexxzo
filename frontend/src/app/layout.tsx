import type { Metadata } from 'next';
import './globals.css';
import { StoreProvider } from '@/context/StoreContext';
import Navbar from '@/components/navbar/Navbar';
import BottomNav from '@/components/navbar/BottomNav';
import Footer from '@/components/footer/Footer';
import CartDrawer from '@/components/cart/CartDrawer';

export const metadata: Metadata = {
  metadataBase: new URL('https://rexxo.in'),
  title: 'REXXZO — Better Everyday | Premium Home & Lifestyle Products',
  description:
    'Discover thoughtfully designed home decor, acoustic audio, ambient lighting, and lifestyle products from REXXZO. Better products for a simpler, more beautiful everyday.',
  keywords: [
    'REXXZO',
    'Better Everyday',
    'Home Decor',
    'Minimalist Lifestyle',
    'Ceramic Lamps',
    'Premium Audio',
    'Modern Homeware',
  ],
  openGraph: {
    title: 'REXXZO — Better Everyday | Premium Home & Lifestyle Products',
    description:
      'Thoughtfully designed objects for mindful living. Balancing tactile warmth, minimal silhouettes, and enduring craftsmanship.',
    url: 'https://rexxo.in',
    siteName: 'REXXZO',
    images: [
      {
        url: '/images/hero_banner.jpg',
        width: 1200,
        height: 630,
        alt: 'REXXZO Curated Lifestyle',
      },
    ],
    locale: 'en_IN',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'REXXZO — Better Everyday',
    description: 'Elevate your daily living with mindfully designed lifestyle essentials.',
    images: ['/images/hero_banner.jpg'],
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full">
      <body className="min-h-full flex flex-col bg-[#F7F5F1] text-[#080808] selection:bg-[#C8BCA7] selection:text-[#080808]">
        <StoreProvider>
          <Navbar />
          <CartDrawer />
          <main className="flex-1 pb-16 md:pb-0">{children}</main>
          <BottomNav />
          <Footer />
        </StoreProvider>
      </body>
    </html>
  );
}
