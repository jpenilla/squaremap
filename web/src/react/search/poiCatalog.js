import instanceCatalog from "../../../data/weiran-gis/instances.json";
import layerCatalog from "../../../data/weiran-gis/layers.json";
import { getInstanceDimension, gisDimensionMatchesWorld } from "../../js/util/gisDimension.js";
import { WEIRAN_GIS_ID_PREFIX } from "../../js/util/weiranGis.js";

/** @typedef {import('../../js/util/gisDimension.js').GisDimension} GisDimension */

/** @typedef {{ id: string, name: string, layerKey: string, layerName: string, layerId: string, dimension: GisDimension, x: number, z: number, minZoom: number | null, maxZoom: number | null }} SearchPoi */

/** @type {SearchPoi[] | null} */
let cachedPois = null;

/**
 * @returns {SearchPoi[]}
 */
export function getSearchablePois() {
    if (cachedPois != null) {
        return cachedPois;
    }

    /** @type {Map<string, number | null>} */
    const layerMinZoom = new Map(
        Object.entries(layerCatalog.layers ?? {}).map(([key, layer]) => [
            key,
            layer.minZoom == null ? null : Number(layer.minZoom),
        ]),
    );

    /** @type {Map<string, string>} */
    const layerNames = new Map(
        Object.entries(layerCatalog.layers ?? {}).map(([key, layer]) => [key, String(layer.name ?? key)]),
    );

    cachedPois = (instanceCatalog.instances ?? []).map((instance) => {
        const layerKey = String(instance.layer);
        return {
            id: String(instance.id),
            name: String(instance.text ?? instance.id),
            layerKey,
            layerName: layerNames.get(layerKey) ?? layerKey,
            layerId: `${WEIRAN_GIS_ID_PREFIX}-${layerKey}`,
            dimension: getInstanceDimension(instance),
            x: Number(instance.point?.x),
            z: Number(instance.point?.z),
            minZoom: layerMinZoom.get(layerKey) ?? null,
            maxZoom: null,
        };
    });

    return cachedPois;
}

/**
 * @param {SearchPoi[]} pois
 * @param {string | null | undefined} worldType
 */
export function filterPoisByWorld(pois, worldType) {
    if (worldType == null) {
        return pois;
    }
    return pois.filter((poi) => gisDimensionMatchesWorld(poi.dimension, worldType));
}

/**
 * @param {string} query
 * @param {SearchPoi[]} pois
 * @param {number} [limit]
 * @returns {SearchPoi[]}
 */
export function searchPois(query, pois, limit = 50) {
    const normalized = query.trim().toLowerCase();
    if (normalized === "") {
        return [];
    }

    /** @type {Array<{ poi: SearchPoi, rank: number }>} */
    const matched = [];

    for (const poi of pois) {
        const name = poi.name.toLowerCase();
        const layerName = poi.layerName.toLowerCase();
        let rank = -1;

        if (name === normalized) {
            rank = 0;
        } else if (name.startsWith(normalized)) {
            rank = 1;
        } else if (name.includes(normalized)) {
            rank = 2;
        } else if (layerName.includes(normalized)) {
            rank = 3;
        }

        if (rank >= 0) {
            matched.push({ poi, rank });
        }
    }

    matched.sort((a, b) => {
        if (a.rank !== b.rank) {
            return a.rank - b.rank;
        }
        return a.poi.name.localeCompare(b.poi.name, "zh-Hans");
    });

    return matched.slice(0, limit).map((entry) => entry.poi);
}
