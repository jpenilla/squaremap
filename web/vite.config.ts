import { defineConfig } from "vite";

export default defineConfig({
    base: "./",
    build: {
        target: "esnext",
        outDir: "../common/build/web",
        sourcemap: true,
    },
});
