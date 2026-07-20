import { S } from "./Squaremap.js";
import { registerUICoordinates } from "../react/bridge/coordinatesBridge.js";

class UICoordinates {
    /** @type {boolean} */
    showCoordinates;

    /** @type {boolean} */
    enabled;

    /** @type {string} */
    html;

    /** @type {{ x: number | string, z: number | string }} */
    coords;

    /** @type {(() => void) | null} */
    _onChange = null;

    /**
     * @param {Settings_UI_Coordinates} json
     * @param {boolean} show
     */
    constructor(json, show) {
        this.showCoordinates = show;
        this.enabled = json.enabled !== false;
        this.html = json.html == null ? "undefined" : json.html;
        this.coords = { x: "---", z: "---" };

        S.map.addEventListener("mousemove", (event) => {
            if (S.worldList.curWorld != null) {
                this.update(S.toPoint(event.latlng));
            }
        });

        this.update(null);
        registerUICoordinates(this);
    }

    /**
     * @param {() => void} listener
     */
    onChange(listener) {
        this._onChange = listener;
    }

    /**
     * @param {import("leaflet").Point | null} point
     */
    update(point) {
        this.coords.x = point == null ? "---" : Math.floor(point.x);
        this.coords.z = point == null ? "---" : Math.floor(point.y);
        this._onChange?.();
    }

    getFormattedHtml() {
        return this.html.replace(/{x}/g, String(this.coords.x)).replace(/{z}/g, String(this.coords.z));
    }

    isVisible() {
        return this.enabled && this.showCoordinates;
    }
}

export { UICoordinates };
