/** @typedef {{ ticks: number[], segmentWidthsPx: number[], totalWidthPx: number }} ScaleBarState */

/** @type {ScaleBarState | null} */
let scaleState = null;

/** @type {Set<() => void>} */
const listeners = new Set();

function notify() {
    for (const listener of listeners) {
        listener();
    }
}

/**
 * @param {ScaleBarState | null} state
 */
export function publishScaleBarState(state) {
    scaleState = state;
    notify();
}

export function getScaleBarBridge() {
    return {
        /**
         * @returns {ScaleBarState | null}
         */
        getScale() {
            return scaleState;
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
