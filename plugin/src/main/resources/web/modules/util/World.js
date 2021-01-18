import { Options, Rectangle, PolyLine, Polygon, Circle, Icon } from "./Markers.js";
import { P } from '../../map.js';

class World {
    constructor(world) {
        this.name = world.name;
        this.type = world.type;
        this.display_name = world.display_name;
    }
    unload() {
        for (let i = 0; i < P.markerLayers.length; i++) {
            const layer = P.markerLayers[i];
            P.controls.removeLayer(layer);
            layer.remove();
        }
    }
    load(callback) {
        P.getJSON(`tiles/${this.name}/settings.json`, (json) => {
        this.player_tracker = json.settings.player_tracker;
        this.zoom = json.settings.zoom;
        this.spawn = json.settings.spawn;

        // set center and zoom
        P.centerOn(this.spawn.x, this.spawn.z, this.zoom.def)
                    .setMinZoom(0) // extra zoom out doesn't work :(
                    .setMaxZoom(this.zoom.max + this.zoom.extra);

        // update page title
        document.title = P.title
            .replaceAll("{world}", this.display_name);

        // setup background
        document.getElementById("map").style.background = this.getBackground();

        // setup the map tiles layer
        if (P.tileLayer != null) {
            P.map.removeLayer(P.tileLayer);
        }
        P.tileLayer = L.tileLayer(`tiles/${this.name}/{z}/{x}_{y}.png`, {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: this.zoom.max
        }).addTo(P.map);

        // force self update with existing player list
        P.playerList.update(Array.from(P.playerList.players.values()));

        // load and draw markers
        this.markers();

        if (callback != null) {
            callback(this);
        }
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
    markers() {
        // json will be in /web/tiles/world/markers.json files
        const json = [
            {
                "name":"Spawn",
                "control":true,
                "hide":false,
                "order":0,
                "markers":[
                    {"type":"icon","point":{"x":0,"z":0},"icon":"spawn.png","size":{"x":16,"z":16},"anchor":{"x":8,"z":8},"tooltip":"World Spawn","tooltip_anchor":{"x":0,"z":-10}},
                ]
            },
            {
                "name":"Regions",
                "control":true,
                "hide":false,
                "order":2,
                "markers":[
                    {"type":"rectangle","points":[{"x":25,"z":25},{"x":150,"z":250}],"color":"#ff0000","fillColor":"#0000ff","tooltip":"Some region owned<br />by BillyGalbreath"},
                    {"type":"polyline","points":[{"x":25,"z":25},{"x":150,"z":250},{"x":125,"z":125}],"color":"#ffff00"},
                    {"type":"polygon","points":[{"x":0,"z":-15},{"x":-30,"z":80},{"x":45,"z":20},{"x":-45,"z":20},{"x":30,"z":80},{"x":0,"z":-15}],"color":"#00ffff","fillColor":"#550000","fillOpacity":0.5},
                    {"type":"polygon","points":[{"x":-25,"z":85},{"x":-36,"z":120},{"x":-70,"z":120},{"x":-43,"z":142},{"x":-55,"z":180},{"x":-25,"z":157},{"x":5,"z":180},{"x":-7,"z":142},{"x":20,"z":120},{"x":-14,"z":120},{"x":-25,"z":85}],"color":"#ff00ff","fillColor":"#00ff00","fillOpacity":0.5},
                    {"type":"circle","center":{"x":50,"z":-100},"radius":50,"color":"#00ffff","fillColor":"#ff00ff"},
                ]
            }
        ];

        for (const entry of json) {
            // setup the layer
            const layer = new L.LayerGroup();
            layer.order = entry.order;
            P.markerLayers.push(layer);

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
