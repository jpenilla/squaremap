/** @typedef {{ id: string, label: string }} UiSkinDefinition */

/** @type {readonly UiSkinDefinition[]} */
export const UI_SKINS = [
    { id: "light", label: "Light 光昼" },
    { id: "gloom", label: "Gloom 静夜" },
    { id: "minecraft", label: "Minecraft 方块" },
    { id: "parchment", label: "Parchment 羊皮纸" },
];

/** @type {string} */
export const DEFAULT_UI_SKIN = "light";

/**
 * @param {string} skinId
 * @returns {UiSkinDefinition | undefined}
 */
export function getUiSkinDefinition(skinId) {
    return UI_SKINS.find((skin) => skin.id === skinId);
}

/**
 * @param {string} [skinId]
 */
export function applyUiSkin(skinId = DEFAULT_UI_SKIN) {
    const root = document.getElementById("react-root");
    if (root == null) {
        return;
    }
    const resolvedSkinId = getUiSkinDefinition(skinId)?.id ?? DEFAULT_UI_SKIN;
    root.dataset.uiSkin = resolvedSkinId;
}

/**
 * @returns {string}
 */
export function getCurrentUiSkin() {
    const root = document.getElementById("react-root");
    return root?.dataset.uiSkin ?? DEFAULT_UI_SKIN;
}
