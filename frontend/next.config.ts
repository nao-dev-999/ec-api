import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // ECS Fargateコンテナ用に.next/standaloneへ最小限のランタイムを出力する(Dockerfile参照)
  output: "standalone",
};

export default nextConfig;
