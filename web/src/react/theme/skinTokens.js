import { theme } from "antd";
import { DEFAULT_UI_SKIN, getCurrentUiSkin } from "./applyUiSkin.js";

/** Ant Design token 与 CSS 皮肤变量的对应关系 */
const ANT_TOKEN_VAR_MAP = {
    colorPrimary: "--weiran-ui-color-primary",
    colorBgContainer: "--weiran-ui-bg-container",
    colorBorder: "--weiran-ui-border",
    colorText: "--weiran-ui-color-text",
    colorTextSecondary: "--weiran-ui-color-text-muted",
    colorBgTextHover: "--weiran-ui-bg-hover",
};

/** Light 光昼 — Ant Design token 回退值 */
const LIGHT_FALLBACK_ANT_TOKENS = {
    colorPrimary: "#1677ff",
    colorBgContainer: "rgba(255, 255, 255, 0.94)",
    colorBorder: "#d9d9d9",
    colorText: "rgba(0, 0, 0, 0.88)",
    colorTextSecondary: "rgba(0, 0, 0, 0.45)",
    colorBgTextHover: "rgba(0, 0, 0, 0.04)",
    borderRadius: 8,
};

/** Gloom 静夜 — Ant Design token 回退值 */
const GLOOM_FALLBACK_ANT_TOKENS = {
    colorPrimary: "#1677ff",
    colorBgContainer: "rgba(0, 0, 0, 0.72)",
    colorBorder: "rgba(255, 255, 255, 0.18)",
    colorText: "rgba(255, 255, 255, 0.88)",
    colorTextSecondary: "rgba(255, 255, 255, 0.55)",
    colorBgTextHover: "rgba(255, 255, 255, 0.12)",
    borderRadius: 8,
};

/** Minecraft 方块 — Ant Design token 回退值 */
const MINECRAFT_FALLBACK_ANT_TOKENS = {
    colorPrimary: "#5b8731",
    colorBgContainer: "#c6c6c6",
    colorBorder: "#555555",
    colorText: "#1f1f1f",
    colorTextSecondary: "#454545",
    colorBgTextHover: "#b0b0b0",
    borderRadius: 0,
    fontWeightStrong: 700,
};

/** Parchment 羊皮纸 — Ant Design token 回退值 */
const PARCHMENT_FALLBACK_ANT_TOKENS = {
    colorPrimary: "#b8860b",
    colorBgContainer: "rgba(243, 233, 210, 0.96)",
    colorBorder: "#d9c9a8",
    colorText: "#4a3728",
    colorTextSecondary: "rgba(74, 55, 40, 0.62)",
    colorBgTextHover: "rgba(74, 55, 40, 0.06)",
    borderRadius: 10,
};

/** @type {Record<string, typeof theme.defaultAlgorithm>} */
const SKIN_ALGORITHMS = {
    light: theme.defaultAlgorithm,
    gloom: theme.darkAlgorithm,
    minecraft: theme.defaultAlgorithm,
    parchment: theme.defaultAlgorithm,
};

/** @type {Record<string, Record<string, string | number>>} */
const SKIN_FALLBACK_ANT_TOKENS = {
    light: LIGHT_FALLBACK_ANT_TOKENS,
    gloom: GLOOM_FALLBACK_ANT_TOKENS,
    minecraft: MINECRAFT_FALLBACK_ANT_TOKENS,
    parchment: PARCHMENT_FALLBACK_ANT_TOKENS,
};

/**
 * @param {string} [skinId]
 * @returns {typeof theme.defaultAlgorithm}
 */
export function getAntdAlgorithmForSkin(skinId = getCurrentUiSkin()) {
    return SKIN_ALGORITHMS[skinId] ?? SKIN_ALGORITHMS[DEFAULT_UI_SKIN];
}

/**
 * @param {string} [skinId]
 * @returns {Record<string, string | number>}
 */
function getFallbackAntTokens(skinId = getCurrentUiSkin()) {
    const fallback = SKIN_FALLBACK_ANT_TOKENS[skinId] ?? SKIN_FALLBACK_ANT_TOKENS[DEFAULT_UI_SKIN];
    return { ...fallback };
}

/**
 * @param {HTMLElement | null | undefined} root
 * @returns {Record<string, string | number>}
 */
export function readAntdTokensFromSkin(root = document.getElementById("react-root")) {
    const skinId = root?.dataset.uiSkin ?? DEFAULT_UI_SKIN;
    const fallback = getFallbackAntTokens(skinId);

    if (root == null) {
        return fallback;
    }

    const computed = getComputedStyle(root);
    /** @type {Record<string, string | number>} */
    const tokens = { ...fallback };

    for (const [tokenName, cssVar] of Object.entries(ANT_TOKEN_VAR_MAP)) {
        const value = computed.getPropertyValue(cssVar).trim();
        if (value !== "") {
            tokens[tokenName] = value;
        }
    }

    const radiusRaw = computed.getPropertyValue("--weiran-ui-radius").trim();
    if (radiusRaw !== "") {
        const radius = Number.parseFloat(radiusRaw);
        if (!Number.isNaN(radius)) {
            tokens.borderRadius = radius;
        }
    }

    return tokens;
}

/**
 * @param {HTMLElement | null | undefined} [root]
 */
export function buildAntdSkinTheme(root = document.getElementById("react-root")) {
    const skinId = root?.dataset.uiSkin ?? DEFAULT_UI_SKIN;
    return {
        algorithm: getAntdAlgorithmForSkin(skinId),
        token: readAntdTokensFromSkin(root),
    };
}

/**
 * @param {string} cssVar
 * @param {number} fallback
 * @param {HTMLElement | null | undefined} [root]
 */
export function readSkinPxVar(
    cssVar,
    fallback,
    root = document.getElementById("react-root"),
) {
    if (root == null) {
        return fallback;
    }
    const raw = getComputedStyle(root).getPropertyValue(cssVar).trim();
    const value = Number.parseFloat(raw);
    return Number.isNaN(value) ? fallback : value;
}
