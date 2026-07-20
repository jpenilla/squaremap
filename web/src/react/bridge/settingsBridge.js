import { applyUiSkin, getCurrentUiSkin, UI_SKINS } from "../theme/applyUiSkin.js";

/** @type {boolean} */
let settingsPanelOpen = false;

/** @type {Set<() => void>} */
const listeners = new Set();

function notify() {
    for (const listener of listeners) {
        listener();
    }
}

export function getSettingsBridge() {
    return {
        isSettingsPanelOpen() {
            return settingsPanelOpen;
        },
        toggleSettingsPanel() {
            settingsPanelOpen = !settingsPanelOpen;
            notify();
        },
        collapseSettingsPanel() {
            if (!settingsPanelOpen) {
                return;
            }
            settingsPanelOpen = false;
            notify();
        },
        /**
         * @returns {typeof UI_SKINS}
         */
        getAvailableSkins() {
            return UI_SKINS;
        },
        getCurrentSkin() {
            return getCurrentUiSkin();
        },
        /**
         * @param {string} skinId
         */
        setSkin(skinId) {
            applyUiSkin(skinId);
            notify();
        },
        /**
         * @param {() => void} listener
         * @returns {() => void}
         */
        subscribe(listener) {
            listeners.add(listener);
            return () => listeners.delete(listener);
        },
    };
}
