import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target: string = env.VITE_API_BASE_URL || "http://localhost:8765";

  return {
    plugins: [react()],
    server: {
      host: true,
      port: 5173,
      strictPort: true,
      // same paths as nginx in the production image: API, sign-in/out, TV page and price sockets go to the gateway
      proxy: {
        "/oauth2": {
          target,
          changeOrigin: true,
        },
        "/login/oauth2": {
          target,
          changeOrigin: true,
        },
        "/logout": {
          target,
          changeOrigin: true,
        },
        "/api": {
          target,
          changeOrigin: true,
        },
        "/tv.html": {
          target,
          changeOrigin: true,
        },
        "/ws": {
          target,
          changeOrigin: true,
          ws: true,
        },
      },
    },
  };
});