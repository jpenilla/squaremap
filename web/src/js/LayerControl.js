import { S } from "./Squaremap.js";
import L from "leaflet";
import { SquaremapTileLayer } from "./SquaremapTileLayer.js";

class LayerControl {
    /** @type {L.LayerGroup} */
    playersLayer;
    /** @type {L.Control.Layers} */
    controls;
    /** @type {SquaremapTileLayer} */
    tileLayer;
    /** @type {number | null} */
    lastTileUpdate;
    /** @type {L.Layer} */
    ignoreLayer;

    init() {
        this.lastTileUpdate = null;

        this.playersLayer = new L.LayerGroup();
        this.playersLayer.id = "players_layer";

        this.controls = L.control
            .layers(
                {},
                {},
                {
                    position: "topleft",
                    sortLayers: true,
                    sortFunction: (a, b) => {
                        return a.order - b.order;
                    },
                },
            )
            .addTo(S.map);
    }
    /**
     * @param name {string}
     * @param layer {L.Layer}
     * @param hide {boolean}
     */
    addOverlay(name, layer, hide) {
        this.controls.addOverlay(layer, name);
        if (this.shouldHide(layer, hide) !== true) {
            layer.addTo(S.map);
        }
    }
    /**
     * @param layer {L.Layer}
     */
    removeOverlay(layer) {
        this.ignoreLayer = layer;
        this.controls.removeLayer(layer);
        layer.remove();
        this.ignoreLayer = null;
    }
    /**
     * @param layer {L.Layer}
     * @param def {boolean}
     * @returns {boolean}
     */
    shouldHide(layer, def) {
        const value = window.localStorage.getItem(`hide_${layer.id}`);
        return value == null ? def : value === "true";
    }
    /**
     * @param layer {L.Layer}
     */
    hideLayer(layer) {
        if (layer !== this.ignoreLayer) {
            window.localStorage.setItem(`hide_${layer.id}`, "true");
        }
    }
    /**
     * @param layer {L.Layer}
     */
    showLayer(layer) {
        if (layer !== this.ignoreLayer) {
            window.localStorage.setItem(`hide_${layer.id}`, "false");
        }
    }
    /**
     * @param world {World}
     */
    setupTileLayer(world) {
        // setup the map tile layer
        if (this.tileLayer != null) {
            S.map.removeLayer(this.tileLayer);
        }
        this.tileLayer = this.createTileLayer(world);
        this.lastTileUpdate = null;

        // refresh player's control
        this.removeOverlay(this.playersLayer);
        if (world.player_tracker.show_controls) {
            this.addOverlay(world.player_tracker.label, this.playersLayer, world.player_tracker.default_hidden);
        }
        this.playersLayer.order = world.player_tracker.priority;
        this.playersLayer.setZIndex(world.player_tracker.z_index);
    }
    /**
     * @param world {World}
     * @returns {SquaremapTileLayer}
     */
    createTileLayer(world) {
        return new SquaremapTileLayer(`tiles/${world.name}/{z}/{x}_{y}.png`, {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: world.zoom.max,
            errorTileUrl: "images/clear.png",
        }).addTo(S.map);
    }
    /**
     * Reload the tiles the server has rewritten since the last manifest we read.
     *
     * @param json {TileUpdates}
     */
    updateTileLayer(json) {
        if (this.tileLayer == null || json == null) {
            return;
        }

        const previous = this.lastTileUpdate;
        this.lastTileUpdate = json.timestamp;

        if (previous == null) {
            // first manifest of this session, the tiles we already loaded are current
            return;
        }

        if (json.dropped > previous) {
            // the server discarded updates before we read them, so we can't tell what changed
            this.tileLayer.reloadDisplayedTiles(json.timestamp);
            return;
        }

        const changed = new Map();
        for (const key in json.tiles) {
            if (json.tiles[key] > previous) {
                changed.set(key, json.tiles[key]);
            }
        }
        if (changed.size > 0) {
            this.tileLayer.updateTiles(changed);
        }
    }
}

export { LayerControl };
