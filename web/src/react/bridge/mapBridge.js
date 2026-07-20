/** @typedef {import("../../js/Squaremap.js").SquaremapMap} SquaremapMap */

/** @type {SquaremapMap | null} */
let squaremap = null;

/**
 * @param {SquaremapMap} instance
 */
export function registerSquaremap(instance) {
    squaremap = instance;
}

/**
 * React 侧访问 Leaflet 地图的桥接 API（方案 B：岛屿架构）。
 * 在 squaremap 完成初始化前调用时，方法会安全 no-op。
 */
export function getMapBridge() {
    return {
        /** @returns {import("leaflet").Map | null} */
        getMap() {
            return squaremap?.map ?? null;
        },
        /**
         * @param {number | string} x
         * @param {number | string} z
         * @param {number | string} [zoom]
         */
        flyTo(x, z, zoom) {
            if (!squaremap) {
                return;
            }
            const parsedX = Number(x);
            const parsedZ = Number(z);
            const parsedZoom = zoom === undefined ? squaremap.map.getZoom() : Number(zoom);
            squaremap.centerOn(parsedX, parsedZ, parsedZoom);
        },
        zoomIn() {
            squaremap?.map?.zoomIn();
        },
        zoomOut() {
            squaremap?.map?.zoomOut();
        },
    };
}
