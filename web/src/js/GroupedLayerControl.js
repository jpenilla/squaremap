import L from "leaflet";

/** @typedef {'squaremap' | 'weiran-gis'} LayerSource */

/** @type {Record<LayerSource, string>} */
const SECTION_TITLES = {
    squaremap: "Square Map",
    "weiran-gis": "Weiran GIS",
};

class GroupedLayerControl extends L.Control {
    /** @type {L.Map | null} */
    _map = null;

    /** @type {Record<LayerSource, { list: HTMLElement | null, section: HTMLElement | null, entries: Array<{ layer: L.Layer, order: number, label: HTMLElement }> }>} */
    _sections = {
        squaremap: { list: null, section: null, entries: [] },
        "weiran-gis": { list: null, section: null, entries: [] },
    };

    /** @type {(layer: L.Layer, def: boolean) => boolean} */
    _shouldHide = () => false;

    /** @type {(layer: L.Layer) => void} */
    _onShow = () => {};

    /** @type {(layer: L.Layer) => void} */
    _onHide = () => {};

    constructor() {
        super({ position: "topleft" });
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

        const container = L.DomUtil.create("div", "leaflet-control-layers leaflet-control weiran-layer-control");
        const toggle = L.DomUtil.create("a", "leaflet-control-layers-toggle", container);
        toggle.href = "#";
        toggle.title = "Layers";

        const form = L.DomUtil.create("form", "leaflet-control-layers-list", container);

        /** @type {LayerSource[]} */
        const sourceOrder = ["squaremap", "weiran-gis"];
        for (let i = 0; i < sourceOrder.length; i++) {
            const source = sourceOrder[i];
            if (i > 0) {
                L.DomUtil.create("div", "weiran-layer-section-divider", form);
            }

            const section = L.DomUtil.create("div", "weiran-layer-section", form);
            section.dataset.source = source;
            const title = L.DomUtil.create("div", "weiran-layer-section-title", section);
            title.textContent = SECTION_TITLES[source];
            const list = L.DomUtil.create("div", "weiran-layer-section-list", section);
            this._sections[source].list = list;
            this._sections[source].section = section;
        }

        L.DomEvent.disableClickPropagation(container);
        L.DomEvent.on(toggle, "click", L.DomEvent.stop);
        L.DomEvent.on(toggle, "click", this._toggleExpand, this);

        return container;
    }

    _toggleExpand() {
        const container = this.getContainer();
        if (container == null) {
            return;
        }
        L.DomUtil.addClass(container, "leaflet-control-layers-expanded");
        L.DomEvent.on(document, "click", this._collapseIfOutside, this);
    }

    _collapseIfOutside(e) {
        const container = this.getContainer();
        if (container == null || container.contains(/** @type {Node} */ (e.target))) {
            return;
        }
        L.DomUtil.removeClass(container, "leaflet-control-layers-expanded");
        L.DomEvent.off(document, "click", this._collapseIfOutside, this);
    }

    /**
     * @param {string} name
     * @param {L.Layer} layer
     * @param {boolean} hide
     * @param {LayerSource} source
     */
    addOverlay(name, layer, hide, source = "squaremap") {
        const section = this._sections[source] ?? this._sections.squaremap;
        if (section.list == null) {
            return;
        }
        const label = L.DomUtil.create("label", "", section.list);
        const input = L.DomUtil.create("input", "leaflet-control-layers-selector", label);
        input.type = "checkbox";
        input.checked = this._shouldHide(layer, hide) !== true;

        L.DomUtil.create("span", "", label).textContent = ` ${name}`;

        L.DomEvent.on(input, "click", (e) => {
            L.DomEvent.stopPropagation(e);
            if (input.checked) {
                layer.addTo(/** @type {L.Map} */ (this._map));
                this._onShow(layer);
            } else {
                layer.remove();
                this._onHide(layer);
            }
        });

        const order = layer.order ?? 0;
        section.entries.push({ layer, order, label });
        this._sortSection(section);
        this._updateSectionVisibility(source);

        if (input.checked && this._map != null) {
            layer.addTo(this._map);
        }
    }

    /**
     * @param {LayerSource} source
     */
    _updateSectionVisibility(source) {
        const section = this._sections[source];
        if (section.section == null) {
            return;
        }
        section.section.style.display = section.entries.length === 0 ? "none" : "";
    }

    /**
     * @param {{ list: HTMLElement, entries: Array<{ layer: L.Layer, order: number, label: HTMLElement }> }} section
     */
    _sortSection(section) {
        section.entries.sort((a, b) => a.order - b.order);
        for (const entry of section.entries) {
            section.list.appendChild(entry.label);
        }
    }

    /**
     * @param {L.Layer} layer
     */
    removeOverlay(layer) {
        for (const source of /** @type {LayerSource[]} */ (["squaremap", "weiran-gis"])) {
            const section = this._sections[source];
            const index = section.entries.findIndex((entry) => entry.layer === layer);
            if (index === -1) {
                continue;
            }
            section.entries[index].label.remove();
            section.entries.splice(index, 1);
            layer.remove();
            this._updateSectionVisibility(source);
            return;
        }
    }
}

export { GroupedLayerControl };
