/**
 * 主题预览色（仅用于设置面板内的小预览，不受当前生效皮肤影响）。
 * 与 skins/*.css 保持同步。
 */
/** @type {Record<string, { bg: string, border: string, text: string, textMuted: string, primary: string, shadow: string }>} */
export const SKIN_PREVIEW_TOKENS = {
    light: {
        bg: "rgba(255, 255, 255, 0.94)",
        border: "#d9d9d9",
        text: "rgba(0, 0, 0, 0.88)",
        textMuted: "rgba(0, 0, 0, 0.45)",
        primary: "#1677ff",
        shadow: "0 1px 4px rgba(0, 0, 0, 0.12)",
    },
    gloom: {
        bg: "rgba(0, 0, 0, 0.72)",
        border: "rgba(255, 255, 255, 0.18)",
        text: "rgba(255, 255, 255, 0.88)",
        textMuted: "rgba(255, 255, 255, 0.55)",
        primary: "#1677ff",
        shadow: "0 1px 5px rgba(0, 0, 0, 0.45)",
    },
    minecraft: {
        bg: "#c6c6c6",
        border: "#000000",
        text: "#3f3f3f",
        textMuted: "#6b6b6b",
        primary: "#5b8731",
        shadow: "inset 1px 1px 0 #ffffff, inset -1px -1px 0 #555555",
    },
    parchment: {
        bg: "rgba(243, 233, 210, 0.96)",
        border: "#6b5344",
        text: "#4a3728",
        textMuted: "rgba(74, 55, 40, 0.62)",
        primary: "#b8860b",
        shadow:
            "0 1px 3px rgba(74, 55, 40, 0.12), inset 0 0 0 1px rgba(74, 55, 40, 0.42), inset 0 0 0 3px rgba(217, 201, 168, 0.65)",
    },
};
