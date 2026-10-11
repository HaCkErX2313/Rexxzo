import { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',
        allow: '/',
        disallow: ['/admin', '/admin/', '/admin/*', '/account', '/checkout', '/orders/'],
      },
    ],
    sitemap: 'https://rexxo.com/sitemap.xml',
  };
}
