import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "res.cloudinary.com",
      },
    ],
  },
  async redirects() {
    return [
      {
        source: '/gift-corner',
        destination: '/shop/gift-corner',
        permanent: false,
      },
      {
        source: '/signup',
        destination: '/register',
        permanent: false,
      },
      {
        source: '/admin/audit',
        destination: '/admin/audit-logs',
        permanent: false,
      },
    ];
  },
};

export default nextConfig;
