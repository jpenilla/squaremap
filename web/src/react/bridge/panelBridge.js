/** @typedef {'layers' | 'search' | 'org' | 'settings'} SidePanelId */

export const SIDE_PANEL = {
    LAYERS: "layers",
    SEARCH: "search",
    ORG: "org",
    SETTINGS: "settings",
};

/** @type {SidePanelId | null} */
let activePanel = null;

/** @type {Set<() => void>} */
const listeners = new Set();

function notify() {
    for (const listener of listeners) {
        listener();
    }
}

export function getPanelBridge() {
    return {
        /**
         * @returns {SidePanelId | null}
         */
        getActivePanel() {
            return activePanel;
        },
        /**
         * @param {SidePanelId} panelId
         */
        isOpen(panelId) {
            return activePanel === panelId;
        },
        /**
         * @param {SidePanelId} panelId
         */
        toggle(panelId) {
            activePanel = activePanel === panelId ? null : panelId;
            notify();
        },
        /**
         * @param {SidePanelId} panelId
         */
        collapse(panelId) {
            if (activePanel === panelId) {
                activePanel = null;
                notify();
            }
        },
        collapseAll() {
            if (activePanel === null) {
                return;
            }
            activePanel = null;
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

/**
 * @param {EventTarget | null} target
 */
export function shouldDismissSidePanels(target) {
    if (!(target instanceof HTMLElement)) {
        return true;
    }
    return (
        target.closest(".map-floating-controls") == null && target.closest(".map-side-panel") == null
    );
}
