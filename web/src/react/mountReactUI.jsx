import { createRoot } from "react-dom/client";
import { App } from "./App.jsx";
import { applyUiSkin } from "./theme/applyUiSkin.js";
import "./styles/skins/light.css";
import "./styles/skins/gloom.css";
import "./styles/skins/minecraft.css";
import "./styles/skins/parchment.css";
import "./styles/react-overlays.css";

/** @type {import("react-dom/client").Root | null} */
let root = null;

export function mountReactUI() {
    const container = document.getElementById("react-root");
    if (!container) {
        console.warn("[react] #react-root not found, skip mount");
        return;
    }

    applyUiSkin();

    if (!root) {
        root = createRoot(container);
    }

    root.render(<App />);
}

export function unmountReactUI() {
    root?.unmount();
    root = null;
}
