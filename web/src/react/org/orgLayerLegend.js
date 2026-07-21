import layerCatalog from "../../../data/weiran-gis/layers.json";
import { buildColoredMakiIconSvg } from "../../js/util/makiIcons.js";
import { buildLayerLegend } from "../../js/util/weiranGis.js";

/** @type {Map<string, string | null>} */
const legendHtmlCache = new Map();

/**
 * 组织树节点图标 — 与图层图例一致，仅「单位点」图层显示 icon。
 *
 * @param {string} layerKey
 * @param {number} [size]
 * @returns {string | null}
 */
export function getOrgLayerLegendHtml(layerKey, size = 14) {
    const cacheKey = `${layerKey}:${size}`;
    if (legendHtmlCache.has(cacheKey)) {
        return legendHtmlCache.get(cacheKey) ?? null;
    }

    /** @type {Record<string, Record<string, unknown>>} */
    const layers = layerCatalog.layers ?? {};
    const layerDef = layers[layerKey];
    if (layerDef == null || layerDef.markerType !== "iconWithText") {
        legendHtmlCache.set(cacheKey, null);
        return null;
    }

    const legend = buildLayerLegend(layerDef);
    if (legend == null) {
        legendHtmlCache.set(cacheKey, null);
        return null;
    }

    const html = buildColoredMakiIconSvg(legend.icon, legend.iconColor, size);
    legendHtmlCache.set(cacheKey, html);
    return html;
}
