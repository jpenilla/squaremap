import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import http from "node:http";
import https from "node:https";

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), "");
    const mapServerUrl = env.MAP_SERVER_URL || "http://localhost:8080"; // 8080 为 squaremap 上游默认；Magic Flower 远程开发见 .env.development
    const proxyMaxSockets = Number(env.PROXY_MAX_SOCKETS) || 3;
    const proxyTimeoutMs = Number(env.PROXY_TIMEOUT_MS) || 60000;
    const useHttps = mapServerUrl.startsWith("https://");
    const proxyAgent = useHttps
        ? new https.Agent({
              keepAlive: true,
              maxSockets: proxyMaxSockets,
              maxFreeSockets: proxyMaxSockets,
          })
        : new http.Agent({
              keepAlive: true,
              maxSockets: proxyMaxSockets,
              maxFreeSockets: proxyMaxSockets,
          });

    /** @type {import("vite").ProxyOptions} */
    const proxyOptions = {
        target: mapServerUrl,
        changeOrigin: true,
        agent: proxyAgent,
        timeout: proxyTimeoutMs,
        proxyTimeout: proxyTimeoutMs,
    };

    return {
        plugins: [react()],
        base: "./",
        build: {
            target: "esnext",
            outDir: "../common/build/web",
            sourcemap: true,
        },
        server: {
            host: "localhost",
            port: 5173,
            strictPort: true,
            proxy: {
                "/tiles": proxyOptions,
                "/images": proxyOptions,
                "/favicon.ico": proxyOptions,
            },
        },
    };
});
