/** @typedef {import("../../js/GroupedLayerControl.js").GroupedLayerControl} GroupedLayerControl */

/** @typedef {{ source: string, title: string, layers: Array<{ id: string, name: string, checked: boolean, legendHtml: string | null }> }} LayerGroup */

import { getPanelBridge, SIDE_PANEL } from "./panelBridge.js";

/** @type {GroupedLayerControl | null} */
let groupedLayerControl = null;

/** @type {boolean} */
let controlsVisible = true;

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
        getPanelBridge().collapseAll();
    }
    notify();
}

export function getLayerBridge() {
    return {
        areControlsVisible() {
            return controlsVisible;
        },
        isLayersPanelExpanded() {
            return getPanelBridge().isOpen(SIDE_PANEL.LAYERS);
        },
        toggleLayersPanel() {
            getPanelBridge().toggle(SIDE_PANEL.LAYERS);
        },
        collapseLayersPanel() {
            getPanelBridge().collapse(SIDE_PANEL.LAYERS);
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
