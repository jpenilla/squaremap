import { Sidebar } from "./modules/Sidebar.js";
import { PlayerList } from "./modules/PlayerList.js";
import { WorldList } from "./modules/WorldList.js";
import { UICoordinates } from "./modules/UICoordinates.js";

class Pl3xMap {
    constructor() {
        this.settings = {};

        this.map = L.map("map", {
            crs: L.CRS.Simple,
            center: [0, 0],
            attributionControl: false,
            noWrap: true
        })
        .addEventListener('move', () => {
            this.updateBrowserUrl(this.getUrlFromView());
        })
        .addEventListener('zoom', () => {
            this.updateBrowserUrl(this.getUrlFromView());
        });

        this.playersLayer = new L.LayerGroup()
            .addTo(this.map);
        this.spawnLayer = new L.LayerGroup()
            .addTo(this.map);

        this.tileLayer;

        this.sidebar;
        this.worldList;
        this.playerList;

        this.init();
    }
    tick() {
        this.getJSON("tiles/players.json", (json) => P.playerList.update(json.players));
        setTimeout(() => P.tick(), 1000);
    }
    init() {
        this.getJSON("tiles/settings.json", (json) => P.__init(json));
    }
    __init(json) {
        this.settings.ui = json.ui;
        this.sidebar = new Sidebar(this);
        this.playerList = new PlayerList(this);
        this.worldList = new WorldList(json.worlds, this);
        if (P.settings.ui.coordinates) {
            new UICoordinates(this);
        }
        this.worldList.loadWorld(this.getUrlParam("world", "world"), (world) => {
            // setup view
            P.centerOn(P.getUrlParam("x", world.spawn.x),
                    P.getUrlParam("z", world.spawn.z),
                    P.getUrlParam("zoom", world.zoom.def))
                .setMinZoom(0) // extra zoom out doesn't work :(
                .setMaxZoom(world.zoom.max + world.zoom.extra);

            // add layer controls
            L.control.layers({}, {
                Players: P.playersLayer,
                Spawn: P.spawnLayer
            }, {
                position: 'topleft'
            }).addTo(P.map);

            // start the tick loop
            P.tick();
        });
    }
    centerOn(x, z, zoom) {
        return this.map.setView(this.unproject(x, z), zoom);
    }
    unproject(x, z) {
        return this.map.unproject([x, z], this.worldList.curWorld.zoom.max);
    }
    project(latlng) {
        return this.map.project(latlng, this.worldList.curWorld.zoom.max);
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
        window.history.replaceState(null, "", url);
    }
}


var P = new Pl3xMap();


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
