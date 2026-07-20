/** @typedef {import("../../js/UICoordinates.js").UICoordinates} UICoordinates */

/** @type {UICoordinates | null} */
let uiCoordinates = null;

/** @type {Set<() => void>} */
const listeners = new Set();

function notify() {
    for (const listener of listeners) {
        listener();
    }
}

/**
 * @param {UICoordinates} instance
 */
export function registerUICoordinates(instance) {
    uiCoordinates = instance;
    instance.onChange(() => notify());
    notify();
}

export function getCoordinatesBridge() {
    return {
        isVisible() {
            return uiCoordinates?.isVisible() ?? false;
        },
        getFormattedHtml() {
            return uiCoordinates?.getFormattedHtml() ?? "";
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
