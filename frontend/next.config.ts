import path from "node:path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Cho phép mọi file SCSS viết `@use "abstracts" as *;` mà không cần đường dẫn tương đối
  sassOptions: {
    loadPaths: [path.join(process.cwd(), "src/styles")],
  },
};

export default nextConfig;
