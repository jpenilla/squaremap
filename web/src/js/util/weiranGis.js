import layerCatalog from "../../../data/weiran-gis/layers.json";
import instanceCatalog from "../../../data/weiran-gis/instances.json";
import { resolveMakiIcon } from "./makiIcons.js";

/** @typedef {import('../../../data/weiran-gis/layers.json')} LayerCatalog */
/** @typedef {import('../../../data/weiran-gis/instances.json')} InstanceCatalog */

export const WEIRAN_GIS_ID_PREFIX = "weiran-gis";

/**
 * @param {string} value
 * @returns {number}
 */
function hashString(value) {
    let hash = 0;
    for (let i = 0; i < value.length; i++) {
        hash = (hash * 31 + value.charCodeAt(i)) | 0;
    }
    return hash;
}

/**
 * @returns {Set<string>}
 */
export function getWeiranGisLayerNames() {
    /** @type {Record<string, { name: string }>} */
    const layers = layerCatalog.layers ?? {};
    return new Set(Object.values(layers).map((layer) => layer.name));
}

/**
 * @param {string} layerKey
 * @param {{ version?: number }} layerDef
 * @param {unknown[]} layerInstances
 */
function computeLayerTimestamp(layerKey, layerDef, layerInstances) {
    return hashString(
        `${layerCatalog.version}:${instanceCatalog.version}:${layerKey}:${layerDef.version ?? 1}:${JSON.stringify(layerInstances)}`,
    );
}

/**
 * @param {Record<string, unknown>} layerDef
 * @returns {'geo-region' | 'unit-point'}
 */
function getControlSource(layerDef) {
    return layerDef.markerType === "iconWithText" ? "unit-point" : "geo-region";
}

/**
 * @param {Record<string, unknown>} layerDef
 * @returns {{ icon: string, iconColor: string, iconBackgroundColor: string, iconBackgroundOpacity: number } | null}
 */
export function buildLayerLegend(layerDef) {
    if (layerDef.markerType !== "iconWithText") {
        return null;
    }
    /** @type {Record<string, unknown>} */
    const style = /** @type {Record<string, unknown>} */ (layerDef.style) ?? {};
    if (style.icon == null || style.iconColor == null) {
        return null;
    }
    return {
        icon: String(style.icon),
        iconColor: String(style.iconColor),
        iconBackgroundColor: String(style.iconBackgroundColor ?? "#ffffff"),
        iconBackgroundOpacity: Number(style.iconBackgroundOpacity ?? 0.72),
    };
}

/**
 * @param {Record<string, unknown>} layerDef
 * @param {Record<string, unknown>} instance
 */
function buildMarkerPayload(layerDef, instance) {
    /** @type {Record<string, unknown>} */
    const style = { ...(/** @type {Record<string, unknown>} */ (layerDef.style) ?? {}) };
    if (instance.icon != null) {
        style.icon = instance.icon;
    }
    if (style.icon != null) {
        style.icon = resolveMakiIcon(style.icon);
    }

    return {
        type: layerDef.markerType ?? "label",
        point: instance.point,
        text: instance.text,
        style,
    };
}

/**
 * 将图层定义 + 实例合并为 World.applyMarkerEntry 可用的图层列表
 * @returns {any[]}
 */
export function buildWeiranGisLayers() {
    /** @type {Record<string, Record<string, unknown>>} */
    const layerDefs = layerCatalog.layers ?? {};
    /** @type {Array<Record<string, unknown>>} */
    const instances = instanceCatalog.instances ?? [];

    /** @type {Map<string, Array<Record<string, unknown>>>} */
    const instancesByLayer = new Map();
    for (const instance of instances) {
        if (instance == null || instance.layer == null || instance.id == null) {
            continue;
        }
        const layerKey = String(instance.layer);
        if (layerDefs[layerKey] == null) {
            continue;
        }
        if (!instancesByLayer.has(layerKey)) {
            instancesByLayer.set(layerKey, []);
        }
        instancesByLayer.get(layerKey).push(instance);
    }

    /** @type {any[]} */
    const layers = [];
    for (const [layerKey, layerDef] of Object.entries(layerDefs)) {
        const layerInstances = instancesByLayer.get(layerKey) ?? [];
        /** @type {Record<string, unknown>} */
        const markers = {};
        for (const instance of layerInstances) {
            markers[String(instance.id)] = buildMarkerPayload(layerDef, instance);
        }

        layers.push({
            id: `${WEIRAN_GIS_ID_PREFIX}-${layerKey}`,
            name: layerDef.name,
            control: layerDef.control !== false,
            hide: layerDef.hide === true,
            order: layerDef.order ?? 0,
            z_index: layerDef.z_index ?? 0,
            minZoom: layerDef.minZoom ?? null,
            maxZoom: layerDef.maxZoom ?? null,
            controlSource: getControlSource(layerDef),
            legend: buildLayerLegend(layerDef),
            timestamp: computeLayerTimestamp(layerKey, layerDef, layerInstances),
            markers,
        });
    }

    return layers;
}
