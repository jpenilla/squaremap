import { S } from "./Squaremap.js";
import L from "leaflet";
import { SquaremapTileLayer } from "./SquaremapTileLayer.js";
/** @import { TileChanges } from "./types.ts" */

class LayerControl {
    /** @type {L.LayerGroup} */
    playersLayer;
    /** @type {L.Control.Layers} */
    controls;
    /** @type {L.TileLayer} */
    tileLayer;
    /** @type {L.Layer} */
    ignoreLayer;

    init() {
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
     * @param changes {TileChanges | null}
     * @returns {L.TileLayer}
     */
    setupTileLayer(world, changes) {
        // setup the map tile layer
        if (this.tileLayer != null) {
            S.map.removeLayer(this.tileLayer);
        }
        this.tileLayer = this.createTileLayer(world, changes);

        // refresh player's control
        this.removeOverlay(this.playersLayer);
        if (world.player_tracker.show_controls) {
            this.addOverlay(world.player_tracker.label, this.playersLayer, world.player_tracker.default_hidden);
        }
        this.playersLayer.order = world.player_tracker.priority;
        this.playersLayer.setZIndex(world.player_tracker.z_index);
        return this.tileLayer;
    }
    /**
     * @param world {World}
     * @param changes {TileChanges | null}
     * @returns {L.TileLayer}
     */
    createTileLayer(world, changes) {
        return new SquaremapTileLayer({
            url: `tiles/${world.name}/{z}/{x}_{y}.png`,
            changes,
            revalidate: !S.staticMode,
            leafletOptions: {
                tileSize: 512,
                minNativeZoom: 0,
                maxNativeZoom: world.zoom.max,
                errorTileUrl: "images/clear.png",
            },
        }).addTo(S.map);
    }
}

export { LayerControl };
