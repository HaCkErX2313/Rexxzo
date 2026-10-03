import { MetadataRoute } from 'next';
import { DEMO_CATEGORIES, DEMO_PRODUCTS } from '@/lib/api';

export default function sitemap(): MetadataRoute.Sitemap {
  const baseUrl = 'https://rexxo.in';

  // Core static pages
  const staticRoutes: MetadataRoute.Sitemap = [
    {
      url: baseUrl,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 1.0,
    },
    {
      url: `${baseUrl}/shop`,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 0.9,
    },
    {
      url: `${baseUrl}/shop/gift-corner`,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 0.9,
    },
    {
      url: `${baseUrl}/categories`,
      lastModified: new Date(),
      changeFrequency: 'weekly',
      priority: 0.8,
    },
    {
      url: `${baseUrl}/about`,
      lastModified: new Date(),
      changeFrequency: 'monthly',
      priority: 0.5,
    },
    {
      url: `${baseUrl}/contact`,
      lastModified: new Date(),
      changeFrequency: 'monthly',
      priority: 0.5,
    },
  ];

  // Category pages
  const categoryRoutes: MetadataRoute.Sitemap = DEMO_CATEGORIES.map((c) => ({
    url: `${baseUrl}/shop?category=${encodeURIComponent(c.name)}`,
    lastModified: new Date(),
    changeFrequency: 'weekly',
    priority: 0.8,
  }));

  // Product pages (SEO-friendly slugs)
  const productRoutes: MetadataRoute.Sitemap = DEMO_PRODUCTS.map((p) => ({
    url: `${baseUrl}/products/${p.slug || p.id}`,
    lastModified: new Date(),
    changeFrequency: 'daily',
    priority: 0.8,
  }));

  return [...staticRoutes, ...categoryRoutes, ...productRoutes];
}
