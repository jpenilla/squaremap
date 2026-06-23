import { S } from "./Squaremap.js";
import L from "leaflet";
import { SquaremapTileLayer } from "./SquaremapTileLayer.js";
import { GroupedLayerControl } from "./GroupedLayerControl.js";

class LayerControl {
    /** @type {number} */
    currentLayer;
    /** @type {number} */
    updateInterval;
    /** @type {L.LayerGroup} */
    playersLayer;
    /** @type {GroupedLayerControl} */
    controls;
    /** @type {L.TileLayer} */
    tileLayer1;
    /** @type {L.TileLayer} */
    tileLayer2;
    /** @type {L.Layer} */
    ignoreLayer;

    init() {
        this.currentLayer = 0;
        this.updateInterval = 60;

        this.playersLayer = new L.LayerGroup();
        this.playersLayer.id = "players_layer";

        this.controls = new GroupedLayerControl();
        this.controls.setHandlers(
            (layer, def) => this.shouldHide(layer, def),
            (layer) => this.showLayer(layer),
            (layer) => this.hideLayer(layer),
        );
        this.controls.addTo(S.map);
    }
    /**
     * @param {string} name
     * @param {L.Layer} layer
     * @param {boolean} hide
     * @param {'squaremap' | 'geo-region' | 'unit-point'} [source]
     * @param {{ icon: string, iconColor: string, iconBackgroundColor: string, iconBackgroundOpacity: number } | null} [legend]
     */
    addOverlay(name, layer, hide, source = "squaremap", legend = null) {
        this.controls.addOverlay(name, layer, hide, source, legend);
    }
    /**
     * @param {L.Layer} layer
     */
    removeOverlay(layer) {
        this.ignoreLayer = layer;
        this.controls.removeOverlay(layer);
        this.ignoreLayer = null;
    }
    /**
     * @param {L.Layer} layer
     * @param {boolean} def
     * @returns {boolean}
     */
    shouldHide(layer, def) {
        const value = window.localStorage.getItem(`hide_${layer.id}`);
        return value == null ? def : value === "true";
    }
    /**
     * @param {L.Layer} layer
     */
    hideLayer(layer) {
        if (layer !== this.ignoreLayer) {
            window.localStorage.setItem(`hide_${layer.id}`, "true");
        }
    }
    /**
     * @param {L.Layer} layer
     */
    showLayer(layer) {
        if (layer !== this.ignoreLayer) {
            window.localStorage.setItem(`hide_${layer.id}`, "false");
        }
    }
    /**
     * @param world {World}
     */
    setupTileLayers(world) {
        // setup the map tile layers
        // we need 2 layers to swap between for seamless refreshing
        if (this.tileLayer1 != null) {
            S.map.removeLayer(this.tileLayer1);
        }
        if (this.tileLayer2 != null) {
            S.map.removeLayer(this.tileLayer2);
        }
        this.tileLayer1 = this.createTileLayer(world);
        this.tileLayer2 = this.createTileLayer(world);

        // refresh player's control
        this.removeOverlay(this.playersLayer);
        if (world.player_tracker.show_controls) {
            this.addOverlay(
                world.player_tracker.label,
                this.playersLayer,
                world.player_tracker.default_hidden,
                "squaremap",
            );
        }
        this.playersLayer.order = world.player_tracker.priority;
        this.playersLayer.setZIndex(world.player_tracker.z_index);
    }
    /**
     * @param world {World}
     * @returns {L.TileLayer}
     */
    createTileLayer(world) {
        return new SquaremapTileLayer(`tiles/${world.name}/{z}/{x}_{y}.png`, {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: world.zoom.max,
            errorTileUrl: "images/clear.png",
        })
            .addTo(S.map)
            .addEventListener("load", () => {
                // when all tiles are loaded, switch to this layer
                this.switchTileLayer();
            });
    }
    updateTileLayer() {
        // redraw background tile layer
        if (this.currentLayer === 1) {
            this.tileLayer2.redraw();
        } else {
            this.tileLayer1.redraw();
        }
    }
    switchTileLayer() {
        // swap current tile layer
        if (this.currentLayer === 1) {
            this.tileLayer1.setZIndex(0);
            this.tileLayer2.setZIndex(1);
            this.currentLayer = 2;
        } else {
            this.tileLayer1.setZIndex(1);
            this.tileLayer2.setZIndex(0);
            this.currentLayer = 1;
        }
    }
}

export { LayerControl };
