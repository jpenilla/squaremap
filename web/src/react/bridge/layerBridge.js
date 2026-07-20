/** @typedef {import("../../js/GroupedLayerControl.js").GroupedLayerControl} GroupedLayerControl */

/** @typedef {{ source: string, title: string, layers: Array<{ id: string, name: string, checked: boolean, legendHtml: string | null }> }} LayerGroup */

/** @type {GroupedLayerControl | null} */
let groupedLayerControl = null;

/** @type {boolean} */
let controlsVisible = true;

/** @type {boolean} */
let layersPanelOpen = false;

/** @type {Set<() => void>} */
const listeners = new Set();

function notify() {
    for (const listener of listeners) {
        listener();
    }
}

/**
 * @param {GroupedLayerControl} control
 */
export function registerGroupedLayerControl(control) {
    groupedLayerControl = control;
    control.onLayersChange(() => notify());
}

/**
 * @param {boolean} visible
 */
export function setControlsVisible(visible) {
    controlsVisible = visible;
    if (!visible) {
        layersPanelOpen = false;
    }
    notify();
}

export function getLayerBridge() {
    return {
        areControlsVisible() {
            return controlsVisible;
        },
        isLayersPanelExpanded() {
            return layersPanelOpen;
        },
        toggleLayersPanel() {
            layersPanelOpen = !layersPanelOpen;
            notify();
        },
        collapseLayersPanel() {
            if (!layersPanelOpen) {
                return;
            }
            layersPanelOpen = false;
            notify();
        },
        /**
         * @returns {LayerGroup[]}
         */
        getGroupedLayers() {
            return groupedLayerControl?.getGroupedLayers() ?? [];
        },
        /**
         * @param {string} id
         * @param {boolean} checked
         */
        setLayerVisible(id, checked) {
            groupedLayerControl?.setLayerVisible(id, checked);
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
