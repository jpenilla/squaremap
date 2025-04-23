import { Sidebar } from "./Sidebar.js";
import { PlayerList } from "./PlayerList.js";
import { WorldList } from "./WorldList.js";
import { UICoordinates } from "./UICoordinates.js";
import { UILink } from "./UILink.js";
import { LayerControl } from "./LayerControl.js";
import L from "leaflet";
import "./addons/Ellipse.js";
import "./addons/RotateMarker.js";
import "leaflet/dist/leaflet.css";
import "../css/styles.css";

class SquaremapMap {
    /** @type {L.Map} */
    map;
    /** @type {LayerControl} */
    layerControl;
    /** @type {boolean} */
    staticMode;
    /** @type {string} */
    title;
    /** @type {Sidebar} */
    sidebar;
    /** @type {PlayerList} */
    playerList;
    /** @type {WorldList} */
    worldList;
    /** @type {UICoordinates} */
    coordinates;
    /** @type {UILink} */
    uiLink;
    /** @type {number} */
    tick_count;
    /** @type {boolean} */
    showControls;

    constructor() {
        this.map = L.map("map", {
            crs: L.CRS.Simple,
            center: [0, 0],
            attributionControl: false,
            preferCanvas: true,
            noWrap: true,
        })
            .on("overlayadd", (e) => {
                this.layerControl.showLayer(e.layer);
            })
            .on("overlayremove", (e) => {
                this.layerControl.hideLayer(e.layer);
            })
            .on("click", () => {
                this.playerList.followPlayerMarker(null);
            })
            .on("dblclick", () => {
                this.playerList.followPlayerMarker(null);
            });

        this.tick_count = 1;

        this.layerControl = new LayerControl();

        this.init();
    }
    loop() {
        if (document.visibilityState === "visible") {
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
        this.getJSON(
            "tiles/settings.json",
            /** @param {Settings} json */
            (json) => {
                this.layerControl.init();

                this.staticMode = json.static || false;
                this.title = json.ui.title;
                this.sidebar = new Sidebar(json.ui.sidebar, this.getUrlParam("show_sidebar", "true") === "true");
                this.playerList = new PlayerList(json.ui.sidebar);
                this.worldList = new WorldList(json.worlds);
                this.coordinates = new UICoordinates(
                    json.ui.coordinates,
                    this.getUrlParam("show_coordinates", "true") === "true",
                );
                this.uiLink = new UILink(json.ui.link, this.getUrlParam("show_link_button", "true") === "true");

                this.showControls = this.getUrlParam("show_controls", "true") === "true";
                if (!this.showControls) {
                    let controlLayers = document.getElementsByClassName("leaflet-top leaflet-left");
                    controlLayers[0].style.display = "none";
                }

                this.worldList.loadInitialWorld(json, (world) => {
                    this.loop();
                    this.centerOn(
                        this.getUrlParam("x", world.spawn.x),
                        this.getUrlParam("z", world.spawn.z),
                        this.getUrlParam("zoom", world.zoom.def),
                    );
                });
            },
        );
    }
    centerOn(x, z, zoom) {
        this.map.setView(this.toLatLng(x, z), zoom);
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
        this.scale = 1 / Math.pow(2, zoom);
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
    /**
     * @param {string} url
     * @param {(json: any) => void} fn
     */
    getJSON(url, fn) {
        fetch(url, { cache: "no-store" }).then(async (res) => {
            if (res.ok) {
                fn(await res.json());
            }
        });
    }
    getUrlParam(query, def) {
        const url = window.location.search.substring(1);
        const vars = url.split("&");
        for (let i = 0; i < vars.length; i++) {
            const param = vars[i].split("=");
            if (param[0] === query) {
                const value = param[1] === undefined ? "" : decodeURIComponent(param[1]);
                return value === "" ? def : value;
            }
        }
        return def;
    }
    getUrlFromView() {
        const center = this.toPoint(this.map.getCenter());
        const zoom = this.map.getZoom();
        const x = Math.floor(center.x);
        const z = Math.floor(center.y);
        let link;
        if (this.playerList.following) {
            link = `?uuid=${this.playerList.following}&zoom=${zoom}`;
        } else {
            link = `?world=${this.worldList.curWorld.name}&zoom=${zoom}&x=${x}&z=${z}`;
        }
        if (!this.showControls) {
            link += "&show_controls=false";
        }
        if (!this.uiLink.showLinkButton) {
            link += "&show_link_button=false";
        }
        if (!this.coordinates.showCoordinates) {
            link += "&show_coordinates=false";
        }
        if (!this.sidebar.showSidebar) {
            link += "&show_sidebar=false";
        }
        return link;
    }
    updateBrowserUrl(url) {
        window.history.replaceState(null, "", url);
    }
}

export const S = new SquaremapMap();

// https://stackoverflow.com/a/3955096
Array.prototype.remove = function () {
    var what,
        a = arguments,
        L = a.length,
        ax;
    while (L && this.length) {
        what = a[--L];
        while ((ax = this.indexOf(what)) !== -1) {
            this.splice(ax, 1);
        }
    }
    return this;
};

if (import.meta.hot) {
    import.meta.hot.accept(() => {
        // Just reload the page on JS changes for now
        window.location.reload();
    });
}
