import { buildWeiranGisLayers, WEIRAN_GIS_ID_PREFIX } from "../../js/util/weiranGis.js";

/** @typedef {{ id: string, name: string, layerKey: string, layerName: string, layerId: string, x: number, z: number, minZoom: number | null, maxZoom: number | null }} SearchPoi */

/** @type {SearchPoi[] | null} */
let cachedPois = null;

/**
 * @returns {SearchPoi[]}
 */
export function getSearchablePois() {
    if (cachedPois != null) {
        return cachedPois;
    }

    cachedPois = buildWeiranGisLayers().flatMap((layer) =>
        Object.entries(layer.markers ?? {}).map(([id, marker]) => ({
            id,
            name: String(marker.text ?? ""),
            layerKey: String(layer.id).replace(`${WEIRAN_GIS_ID_PREFIX}-`, ""),
            layerName: String(layer.name ?? ""),
            layerId: String(layer.id),
            x: Number(marker.point?.x),
            z: Number(marker.point?.z),
            minZoom: layer.minZoom ?? null,
            maxZoom: layer.maxZoom ?? null,
        })),
    );

    return cachedPois;
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
