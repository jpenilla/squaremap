import { Options, Rectangle, PolyLine, Polygon, Circle, Ellipse, Icon } from "./Markers.js";
import { P } from '../Pl3xMap.js';

class World {
    constructor(world) {
        this.name = world.name;
        this.type = world.type;
        this.display_name = world.display_name;
        this.markerLayers = new Map();
    }
    tick() {
        // load and draw markers
        P.getJSON(`tiles/${this.name}/markers.json`, (json) => {
            this.markers(json);
        });
    }
    unload() {
        const keys = Array.from(this.markerLayers.keys());
        for (let i = 0; i < keys.length; i++) {
            const layer = this.markerLayers.get(keys[i]);
            P.controls.removeLayer(layer);
            layer.remove();
            this.markerLayers.delete(keys[i]);
        }
    }
    load(callback) {
        P.getJSON(`tiles/${this.name}/settings.json`, (json) => {
            this.player_tracker = json.settings.player_tracker;
            this.zoom = json.settings.zoom;
            this.spawn = json.settings.spawn;
            P.updateInterval = json.settings.update_interval;

            // set the scale for our projection calculations
            P.setScale(this.zoom.max);

            // set center and zoom
            P.centerOn(this.spawn.x, this.spawn.z, this.zoom.def)
                .setMinZoom(0) // extra zoom out doesn't work :(
                .setMaxZoom(this.zoom.max + this.zoom.extra);

            // update page title
            document.title = P.title
                .replaceAll("{world}", this.display_name);

            // setup background
            document.getElementById("map").style.background = this.getBackground();

            // setup the map tile layers
            // we need 2 layers to swap between for seamless refreshing
            if (P.tileLayer1 != null) {
                P.map.removeLayer(P.tileLayer1);
            }
            if (P.tileLayer2 != null) {
                P.map.removeLayer(P.tileLayer2);
            }
            P.tileLayer1 = this.createTileLayer();
            P.tileLayer2 = this.createTileLayer();

            // force self update with existing player list
            P.playerList.update(Array.from(P.playerList.players.values()));

            // tick now, dont wait for counter
            this.tick();

            if (callback != null) {
                callback(this);
            }
        });
    }
    createTileLayer() {
        return L.tileLayer(`tiles/${this.name}/{z}/{x}_{y}.png?{rand}`, {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: this.zoom.max,
            rand: function() { 
                return Math.random(); 
            }
        }).addTo(P.map)
        .addEventListener("load", function() {
            // when all tiles are loaded, switch to this layer
            P.switchTileLayer();
        });
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
                layer.remove();
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
                P.controls.addOverlay(layer, entry.name);
            }
            if (entry.hide !== true) {
                layer.addTo(P.map);
            }

            // setup the markers
            for (const shape in entry.markers) {
                let marker;
                const opts = new Options(entry.markers[shape]);
                switch(opts.pop("type")) {
                    case "rectangle": marker = new Rectangle(opts); break;
                    case "polyline": marker = new PolyLine(opts); break;
                    case "polygon": marker = new Polygon(opts); break;
                    case "circle": marker = new Circle(opts); break;
                    case "ellipse": marker = new Ellipse(opts); break;
                    case "icon": marker = new Icon(opts); break;
                }
                if (marker != null) {
                    marker.addTo(layer);
                }
            }
        }
    }
}

export { World };
