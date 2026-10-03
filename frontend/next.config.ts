import type { NextConfig } from "next";

const nextConfig: NextConfig = {
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
