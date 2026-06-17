import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), "");
    const mapServerUrl = env.MAP_SERVER_URL || "http://localhost:8080"; // 8080 为 squaremap 上游默认；Magic Flower 远程开发见 .env.development

    return {
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
                "/tiles": {
                    target: mapServerUrl,
                    changeOrigin: true,
                },
                "/images": {
                    target: mapServerUrl,
                    changeOrigin: true,
                },
                "/favicon.ico": {
                    target: mapServerUrl,
                    changeOrigin: true,
                },
            },
        },
    };
});
