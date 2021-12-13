import { P } from './Squaremap.js';
import { SquaremapTileLayer } from './SquaremapTileLayer.js';

class LayerControl {
    constructor() {
        this.layers = new Map();
    }
    init() {
        this.currentLayer = 0;
        this.updateInterval = 60;

        this.playersLayer = new L.LayerGroup();
        this.playersLayer.id = "players_layer";

        this.controls = L.control.layers({}, {}, {
            position: 'topleft',
            sortLayers: true,
            sortFunction: (a, b) => {
                return a.order - b.order;
            }
        })
        .addTo(P.map);
    }
    addOverlay(name, layer, hide) {
        this.controls.addOverlay(layer, name);
        if (this.shouldHide(layer, hide) !== true) {
            layer.addTo(P.map);
        }
    }
    removeOverlay(layer) {
        this.ignoreLayer = layer;
        this.controls.removeLayer(layer);
        layer.remove();
        this.ignoreLayer = null;
    }
    shouldHide(layer, def) {
        const value = window.localStorage.getItem(`hide_${layer.id}`);
        return value == null ? def : value === 'true';
    }
    hideLayer(layer) {
        if (layer != this.ignoreLayer) {
            window.localStorage.setItem(`hide_${layer.id}`, 'true');
        }
    }
    showLayer(layer) {
        if (layer != this.ignoreLayer) {
            window.localStorage.setItem(`hide_${layer.id}`, 'false');
        }
    }
    setupTileLayers(world) {
        // setup the map tile layers
        // we need 2 layers to swap between for seamless refreshing
        if (this.tileLayer1 != null) {
            P.map.removeLayer(this.tileLayer1);
        }
        if (this.tileLayer2 != null) {
            P.map.removeLayer(this.tileLayer2);
        }
        this.tileLayer1 = this.createTileLayer(world);
        this.tileLayer2 = this.createTileLayer(world);

        // refresh player's control
        this.removeOverlay(this.playersLayer);
        if (world.player_tracker.show_controls) {
            this.addOverlay(world.player_tracker.label,
                this.playersLayer,
                world.player_tracker.default_hidden);
        }
        this.playersLayer.order = world.player_tracker.priority;
        this.playersLayer.setZIndex(world.player_tracker.z_index);
    }
    createTileLayer(world) {
        return new SquaremapTileLayer(`tiles/${world.name}/{z}/{x}_{y}.png`, {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: world.zoom.max
        }).addTo(P.map)
        .addEventListener("load", () => {
            // when all tiles are loaded, switch to this layer
            this.switchTileLayer();
        });
    }
    updateTileLayer() {
        // redraw background tile layer
        if (this.currentLayer == 1) {
            this.tileLayer2.redraw();
        } else {
            this.tileLayer1.redraw();
        }
    }
    switchTileLayer() {
        // swap current tile layer
        if (this.currentLayer == 1) {
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
