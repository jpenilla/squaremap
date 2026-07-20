import L from "leaflet";
import { buildColoredMakiIconSvg } from "./util/makiIcons.js";

/** @typedef {'squaremap' | 'geo-region' | 'unit-point'} LayerSource */

/** @typedef {{ icon: string, iconColor: string, iconBackgroundColor: string, iconBackgroundOpacity: number }} LayerLegend */

/**
 * @typedef {object} LayerEntry
 * @property {L.Layer} layer
 * @property {number} order
 * @property {string} id
 * @property {string} name
 * @property {LayerSource} source
 * @property {LayerLegend | null} legend
 * @property {boolean} checked
 */

/** @type {Record<LayerSource, string>} */
export const SECTION_TITLES = {
    squaremap: "SQUARE MAP",
    "geo-region": "GEO REGION",
    "unit-point": "UNIT POINT",
};

/** @type {LayerSource[]} */
export const SOURCE_ORDER = ["squaremap", "geo-region", "unit-point"];

class GroupedLayerControl extends L.Control {
    /** @type {L.Map | null} */
    _map = null;

    /** @type {Record<LayerSource, { entries: LayerEntry[] }>} */
    _sections = {
        squaremap: { entries: [] },
        "geo-region": { entries: [] },
        "unit-point": { entries: [] },
    };

    /** @type {(layer: L.Layer, def: boolean) => boolean} */
    _shouldHide = () => false;

    /** @type {(layer: L.Layer) => void} */
    _onShow = () => {};

    /** @type {(layer: L.Layer) => void} */
    _onHide = () => {};

    /** @type {(() => void) | null} */
    _layersChangeListener = null;

    constructor() {
        super({ position: "topleft" });
    }

    /**
     * @param {() => void} listener
     */
    onLayersChange(listener) {
        this._layersChangeListener = listener;
    }

    /**
     * @param {(layer: L.Layer, def: boolean) => boolean} shouldHide
     * @param {(layer: L.Layer) => void} onShow
     * @param {(layer: L.Layer) => void} onHide
     */
    setHandlers(shouldHide, onShow, onHide) {
        this._shouldHide = shouldHide;
        this._onShow = onShow;
        this._onHide = onHide;
    }

    onAdd(map) {
        this._map = map;
        const container = L.DomUtil.create("div", "weiran-layer-control-host");
        container.style.display = "none";
        return container;
    }

    /**
     * @returns {Array<{ source: LayerSource, title: string, layers: Array<{ id: string, name: string, checked: boolean, legendHtml: string | null }> }>}
     */
    getGroupedLayers() {
        /** @type {Array<{ source: LayerSource, title: string, layers: Array<{ id: string, name: string, checked: boolean, legendHtml: string | null }> }>} */
        const groups = [];

        for (const source of SOURCE_ORDER) {
            const section = this._sections[source];
            if (section.entries.length === 0) {
                continue;
            }

            groups.push({
                source,
                title: SECTION_TITLES[source],
                layers: section.entries.map((entry) => ({
                    id: entry.id,
                    name: entry.name,
                    checked: entry.checked,
                    legendHtml:
                        entry.legend != null && source === "unit-point"
                            ? buildColoredMakiIconSvg(entry.legend.icon, entry.legend.iconColor, 12)
                            : null,
                })),
            });
        }

        return groups;
    }

    /**
     * @param {string} id
     * @param {boolean} checked
     */
    setLayerVisible(id, checked) {
        const entry = this._findEntry(id);
        if (entry == null || entry.checked === checked) {
            return;
        }

        entry.checked = checked;
        if (checked) {
            entry.layer.addTo(/** @type {L.Map} */ (this._map));
            this._onShow(entry.layer);
        } else {
            entry.layer.remove();
            this._onHide(entry.layer);
        }

        this._notifyLayersChange();
    }

    /**
     * @param {string} name
     * @param {L.Layer} layer
     * @param {boolean} hide
     * @param {LayerSource} source
     * @param {LayerLegend | null} [legend]
     */
    addOverlay(name, layer, hide, source = "squaremap", legend = null) {
        const section = this._sections[source] ?? this._sections.squaremap;
        const checked = this._shouldHide(layer, hide) !== true;
        const id = layer.id != null ? String(layer.id) : `${source}-${name}-${section.entries.length}`;

        section.entries.push({
            layer,
            order: layer.order ?? 0,
            id,
            name,
            source,
            legend,
            checked,
        });
        this._sortSection(section);

        if (checked && this._map != null) {
            layer.addTo(this._map);
        }

        this._notifyLayersChange();
    }

    /**
     * @param {L.Layer} layer
     */
    removeOverlay(layer) {
        for (const source of SOURCE_ORDER) {
            const section = this._sections[source];
            const index = section.entries.findIndex((entry) => entry.layer === layer);
            if (index === -1) {
                continue;
            }
            section.entries.splice(index, 1);
            layer.remove();
            this._notifyLayersChange();
            return;
        }
    }

    /**
     * @param {L.Layer} layer
     */
    removeLayer(layer) {
        this.removeOverlay(layer);
    }

    /**
     * @param {string} id
     * @returns {LayerEntry | null}
     */
    _findEntry(id) {
        for (const source of SOURCE_ORDER) {
            const entry = this._sections[source].entries.find((item) => item.id === id);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    /**
     * @param {{ entries: LayerEntry[] }} section
     */
    _sortSection(section) {
        section.entries.sort((a, b) => a.order - b.order);
    }

    _notifyLayersChange() {
        this._layersChangeListener?.();
    }
}

export { GroupedLayerControl };
