import { Sidebar } from "./Sidebar.js";
import { PlayerList } from "./PlayerList.js";
import { WorldList } from "./WorldList.js";
import { UICoordinates } from "./UICoordinates.js";
import { UILink } from "./UILink.js";
import { LayerControl } from "./LayerControl.js";

class SquaremapMap {
    constructor() {
        this.map = L.map("map", {
            crs: L.CRS.Simple,
            center: [0, 0],
            attributionControl: false,
            preferCanvas: true,
            noWrap: true
        })
        .on('overlayadd', (e) => {
            this.layerControl.showLayer(e.layer);
        })
        .on('overlayremove', (e) => {
            this.layerControl.hideLayer(e.layer);
        })
        .on('click', (e) => {
            this.playerList.followPlayerMarker(null);
        })
        .on('dblclick', (e) => {
            this.playerList.followPlayerMarker(null);
        });

        this.tick_count = 1;

        this.layerControl = new LayerControl();

        this.init();
    }
    loop() {
        if (document.visibilityState === 'visible') {
            this.tick();
            this.tick_count++;
        }
        setTimeout(() => this.loop(), 1000);
    }
    tick() {
        // tick player tracker
        this.playerList.tick();
        // tick world
        this.worldList.curWorld.tick();
    }
    init() {
        this.getJSON("tiles/settings.json", (json) => {
            this.layerControl.init();

            this.title = json.ui.title;
            this.sidebar = new Sidebar(json.ui.sidebar);
            this.playerList = new PlayerList(json.ui.sidebar);
            this.worldList = new WorldList(json.worlds);
            this.coordinates = new UICoordinates(json.ui.coordinates);
            this.uiLink = new UILink(json.ui.link);

            this.worldList.loadWorld(this.getUrlParam("world", json.worlds[0].name), (world) => {
                this.loop();
                this.centerOn(
                    this.getUrlParam("x", world.spawn.x),
                    this.getUrlParam("z", world.spawn.z),
                    this.getUrlParam("zoom", world.zoom.def));
            });
        });
    }
    centerOn(x, z, zoom) {
        this.map.setView(this.toLatLng(x, z), zoom);
        this.uiLink.update();
        return this.map;
    }
    toLatLng(x, z) {
        return L.latLng(this.pixelsToMeters(-z), this.pixelsToMeters(x));
        //return this.map.unproject([x, z], this.worldList.curWorld.zoom.max);
    }
    toPoint(latlng) {
        return L.point(this.metersToPixels(latlng.lng), this.metersToPixels(-latlng.lat));
        //return this.map.project(latlng, this.worldList.curWorld.zoom.max);
    }
    pixelsToMeters(num) {
        return num * this.scale;
    }
    metersToPixels(num) {
        return num / this.scale;
    }
    setScale(zoom) {
        this.scale = (1 / Math.pow(2, zoom));
        // store this on map for ellipse
        this.map.options.scale = this.scale;
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
        const center = this.toPoint(this.map.getCenter());
        const zoom = this.map.getZoom();
        const x = Math.floor(center.x);
        const z = Math.floor(center.y);
        const following = this.playerList.following ? `&uuid=${this.playerList.following}` : '';
        return `?world=${this.worldList.curWorld.name}&zoom=${zoom}&x=${x}&z=${z}${following}`;
    }
    updateBrowserUrl(url) {
        window.history.replaceState(null, "", url);
    }
}


export const P = new SquaremapMap();


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
