import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // The e2e run builds into its own folder (NEXT_DIST_DIR=.next-e2e), so it
  // never overwrites or reads the build of `next dev` / `next build`.
  distDir: process.env.NEXT_DIST_DIR || ".next",

  // Hosts that next/image may load and optimise images from. The event images
  // in the database are Unsplash photos (https://images.unsplash.com/photo-…).
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "images.unsplash.com",
        pathname: "/photo-**",
      },
    ],
  },
};

export default nextConfig;
