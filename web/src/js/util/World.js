import { Options, Rectangle, PolyLine, Polygon, Circle, Ellipse, Icon } from "./Markers.js";
import { S } from "../Squaremap.js";
import L from "leaflet";

class World {
    /** @type {string} */
    name;
    /** @type {string} */
    display_name;
    /** @type {Map<string, L.LayerGroup>} */
    markerLayers;
    /** @type {WorldSettings_PlayerTracker} */
    player_tracker;
    /** @type {number} */
    marker_update_interval;
    /** @type {number} */
    tiles_update_interval;
    /** @type {boolean} */
    staticNeedsMarkerTick;
    /** @type {WorldSettings_Zoom} */
    zoom;
    /** @type {WorldSettings_Spawn} */
    spawn;

    /**
     * @param json {Settings_World}
     */
    constructor(json) {
        this.name = json.name;
        this.order = json.order;
        this.icon = json.icon;
        this.type = json.type;
        this.display_name = json.display_name;
        this.markerLayers = new Map();
        this.player_tracker = {};
        this.marker_update_interval = 5;
        this.tiles_update_interval = 15;
        this.staticNeedsMarkerTick = false;
    }
    tick() {
        // refresh map tile layer
        if (!S.staticMode && S.tick_count % this.tiles_update_interval === 0) {
            S.layerControl.updateTileLayer();
        }
        // load and draw markers
        if (!S.staticMode && S.tick_count % this.marker_update_interval === 0) {
            this.tickMarkers();
        }
        if (S.staticMode && this.staticNeedsMarkerTick) {
            this.tickMarkers();
            this.staticNeedsMarkerTick = false;
        }
    }
    tickMarkers() {
        S.getJSON(
            `tiles/${this.name}/markers.json`,
            (json) => {
                if (this === S.worldList.curWorld) {
                    this.markers(json);
                }
            },
            true,
        );
    }
    unload() {
        S.playerList.clearPlayerMarkers();
        const keys = Array.from(this.markerLayers.keys());
        for (let i = 0; i < keys.length; i++) {
            const layer = this.markerLayers.get(keys[i]);
            S.layerControl.controls.removeLayer(layer);
            layer.remove();
            this.markerLayers.delete(keys[i]);
        }
    }
    /**
     * @param callback {(world: World) => void}
     */
    load(callback) {
        S.getJSON(
            `tiles/${this.name}/settings.json`,
            /** @param {WorldSettings} json */
            (json) => {
                this.player_tracker = json.player_tracker;
                this.zoom = json.zoom;
                this.spawn = json.spawn;
                this.marker_update_interval = json.marker_update_interval;
                this.tiles_update_interval = json.tiles_update_interval;
                this.staticNeedsMarkerTick = true;

                // set the scale for our projection calculations
                S.setScale(this.zoom.max);

                // set center and zoom
                S.centerOn(this.spawn.x, this.spawn.z, this.zoom.def)
                    .setMinZoom(0) // extra zoom out doesn't work :(
                    .setMaxZoom(this.zoom.max + this.zoom.extra);

                // update page title
                document.title = S.title.replace(/{world}/g, this.display_name);

                // setup background
                document.getElementById("map").style.background = this.getBackground();

                // setup tile layers
                S.layerControl.setupTileLayers(this);

                // force clear player markers
                S.playerList.clearPlayerMarkers();

                // tick now, reset counter
                S.tick_count = 0;
                S.tick();

                // force clear player markers
                S.playerList.clearPlayerMarkers();

                if (callback != null) {
                    callback(this);
                }
            },
        );
    }
    getBackground() {
        switch (this.type) {
            case "nether":
                return "url('images/nether_sky.png')";
            case "the_end":
                return "url('images/end_sky.png')";
            case "normal":
            default:
                return "url('images/overworld_sky.png')";
        }
    }
    markers(json) {
        // check if json is iterable
        if (json == null || !(Symbol.iterator in Object(json))) {
            return;
        }
        // iterate layers
        for (const entry of json) {
            // check if layer exists and needs updating
            let layer = this.markerLayers.get(entry.id);
            if (layer != null) {
                if (layer.timestamp === entry.timestamp) {
                    continue; // skip
                }
                // clear existing layer to rebuild
                S.layerControl.removeOverlay(layer);
                // TODO
                // implement marker tracker instead of clearing
                // to reduce possible client side lag
            }

            // setup the layer
            layer = new L.LayerGroup();
            layer.order = entry.order;
            layer.id = entry.id;
            layer.timestamp = entry.timestamp;
            layer.setZIndex(entry.z_index);
            this.markerLayers.set(layer.id, layer);

            // setup the layer control
            if (entry.control === true) {
                S.layerControl.addOverlay(entry.name, layer, entry.hide);
            }

            // setup the markers
            for (const shape in entry.markers) {
                let marker;
                const opts = new Options(entry.markers[shape]);
                switch (opts.pop("type")) {
                    case "rectangle":
                        marker = new Rectangle(opts);
                        break;
                    case "polyline":
                        marker = new PolyLine(opts);
                        break;
                    case "polygon":
                        marker = new Polygon(opts);
                        break;
                    case "circle":
                        marker = new Circle(opts);
                        break;
                    case "ellipse":
                        marker = new Ellipse(opts);
                        break;
                    case "icon":
                        marker = new Icon(opts);
                        break;
                }
                if (marker != null) {
                    marker.addTo(layer);
                }
            }
        }
    }
}

export { World };
