import { Sidebar } from "./modules/Sidebar.js";
import { PlayerList } from "./modules/PlayerList.js";
import { WorldList } from "./modules/WorldList.js";
import { UICoordinates } from "./modules/UICoordinates.js";
import { UILink } from "./modules/UILink.js";

class Pl3xMap {
    constructor() {
        this.map = L.map("map", {
            crs: L.CRS.Simple,
            center: [0, 0],
            attributionControl: false,
            noWrap: true
        });

        this.currentLayer = 1;
        this.updateInterval = 60;

        this.playersLayer = new L.LayerGroup()
            .setZIndex(1000)
            .addTo(this.map);
        this.playersLayer.order = 1;

        this.controls = L.control.layers({}, {}, {
            position: 'topleft',
            sortLayers: true,
            sortFunction: function (a, b) {
                return a.order - b.order;
            }
        })
            .addTo(this.map)
            .addOverlay(this.playersLayer, "Players");

        this.init();
    }
    tick(count) {
        if (count === undefined) {
            count = 0;
        }
        // tick players every 1 second
        this.playerList.tick();
        // tick world every 10 seconds
        if (count % 5 == 0) {
            this.worldList.curWorld.tick();
        }
        // refresh map tile layer
        if (count % this.updateInterval == 0) {
            this.updateTileLayer();
        }
        setTimeout(() => P.tick(++count), 1000);
    }
    init() {
        this.getJSON("tiles/settings.json", (json) => {
            this.title = json.ui.title;
            this.sidebar = new Sidebar(json.ui.sidebar);
            this.playerList = new PlayerList();
            this.worldList = new WorldList(json.worlds);
            if (json.ui.coordinates) {
                this.coordinates = new UICoordinates();
            }
            if (json.ui.link) {
                this.uiLink = new UILink();
            }
            this.worldList.loadWorld(this.getUrlParam("world", "world"), (world) => {
                P.tick();
            });
        });
    }
    updateTileLayer() {
        // redraw background tile layer
        if (this.currentLayer == 1) {
            P.tileLayer2.redraw();
        } else {
            P.tileLayer1.redraw();
        }
    }
    switchTileLayer() {
        // swap current tile layer
        if (this.currentLayer == 1) {
            P.tileLayer1.setZIndex(0);
            P.tileLayer2.setZIndex(1);
            this.currentLayer = 2;
        } else {
            P.tileLayer1.setZIndex(1);
            P.tileLayer2.setZIndex(0);
            this.currentLayer = 1;
        }
    }
    centerOn(x, z, zoom) {
        this.map.setView(this.unproject(x, z), zoom);
        P.uiLink.update();
        return this.map;
    }
    unproject(x, z) {
        return this.map.unproject([x, z], this.worldList.curWorld.zoom.max);
    }
    project(latlng) {
        return this.map.project(latlng, this.worldList.curWorld.zoom.max);
    }
    pixelsToMeters(num) {
        return this.unproject(num, 0).lng - this.unproject(0, 0).lng;
    }
    createElement(tag, id, parent) {
        const element = document.createElement(tag);
        element.id = id;
        element.parent = parent;
        return element;
    }
    createTextElement(tag, text) {
        const element = document.createElement(tag);
        element.appendChild(document.createTextNode(text));
        return element;
    }
    getHeadUrl(player) {
        return this.worldList.curWorld.player_tracker.nameplates.heads_url
            .replaceAll("{uuid}", player.uuid)
            .replaceAll("{name}", player.name);
    }
    getJSON(url, fn) {
        fetch(url, {cache: "no-store"})
            .then(async res => {
                if (res.ok) {
                    fn(await res.json());
                }
            });
    }
    getUrlParam(query, def) {
        const url = window.location.search.substring(1);
        const vars = url.split('&');
        for (let i = 0; i < vars.length; i++) {
            const param = vars[i].split('=');
            if (param[0] === query) {
                const value = param[1] === undefined ? '' : decodeURIComponent(param[1]);
                return value === '' ? def : value;
            }
        }
        return def;
    }
    getUrlFromView() {
        const center = this.project(this.map.getCenter());
        const zoom = this.map.getZoom();
        const x = Math.floor(center.x);
        const z = Math.floor(center.y);
        return `?world=${this.worldList.curWorld.name}&zoom=${zoom}&x=${x}&z=${z}`;
    }
    updateBrowserUrl(url) {
        window.history.replaceState(this.stateObj, "", url);
    }
}


export const P = new Pl3xMap();


// https://stackoverflow.com/a/3955096
Array.prototype.remove = function() {
    var what, a = arguments, L = a.length, ax;
    while (L && this.length) {
        what = a[--L];
        while ((ax = this.indexOf(what)) !== -1) {
            this.splice(ax, 1);
        }
    }
    return this;
};
