import { buildWeiranGisLayers, getWeiranGisLayerNames, WEIRAN_GIS_ID_PREFIX } from "./weiranGis.js";

/**
 * @param {unknown} json
 * @returns {any[]}
 */
function normalizeLayerList(json) {
    if (json == null) {
        return [];
    }
    if (Array.isArray(json)) {
        return json;
    }
    if (typeof json === "object" && json !== null && Array.isArray(/** @type {{ layers?: unknown }} */ (json).layers)) {
        return /** @type {{ layers: any[] }} */ (json).layers;
    }
    return [];
}

/**
 * @param {string} url
 * @returns {Promise<any[]>}
 */
async function fetchMarkerLayerList(url) {
    try {
        const res = await fetch(url, { cache: "no-store" });
        if (!res.ok) {
            return [];
        }
        return normalizeLayerList(await res.json());
    } catch {
        return [];
    }
}

/**
 * @param {any[]} layers
 * @returns {any[]}
 */
export function filterSquaremapLayers(layers) {
    const weiranLayerNames = getWeiranGisLayerNames();
    return layers.filter((layer) => {
        if (layer == null || layer.id == null) {
            return false;
        }
        if (String(layer.id).startsWith(WEIRAN_GIS_ID_PREFIX)) {
            return false;
        }
        if (weiranLayerNames.has(layer.name)) {
            return false;
        }
        if (layer.name === "蔚然GIS") {
            return false;
        }
        return true;
    });
}

/** @returns {any[]} */
export function getWeiranGisLayers(worldType) {
    return buildWeiranGisLayers(worldType);
}

/**
 * @param {string} worldName
 * @param {string | null | undefined} worldType
 * @returns {Promise<{ squaremap: any[], weiranGis: any[] }>}
 */
export async function fetchMarkerLayersBySource(worldName, worldType) {
    const serverLayers = filterSquaremapLayers(await fetchMarkerLayerList(`tiles/${worldName}/markers.json`));
    return {
        squaremap: serverLayers,
        weiranGis: getWeiranGisLayers(worldType),
    };
}
