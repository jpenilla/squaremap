import { Spawn } from "./Spawn.js";

class World {
    constructor(world, P) {
        this.name = world.name;
        this.type = world.type;
        this.display_name = world.display_name;
        this.P = P;
        this.spawn = new Spawn(this);
    }
    load(callback) {
        const P = this.P;
        P.getJSON(`tiles/${this.name}/settings.json`, (json) => P.worldList.curWorld.__load(json, callback));
    }
    unload() {
        this.spawn.hide();
    }
    // "internal" function so we get a proper scope for "this"
    __load(json, callback) {
        this.player_tracker = json.settings.player_tracker;
        this.zoom = json.settings.zoom;

        // setup page title
        document.title = this.P.settings.ui.title
            .replaceAll("{world}", json.display_name);

        // setup background
        const mapDom = document.getElementById("map");
        switch (json.type) {
            case "nether":
                mapDom.style.background = "url('images/nether_sky.png')";
                break;
            case "the_end":
                mapDom.style.background = "url('images/end_sky.png')";
                break;
            case "normal":
            default:
                mapDom.style.background = "url('images/overworld_sky.png')";
                break;
        }

        // setup the map tiles layer
        if (this.P.tileLayer != null) {
            this.P.map.removeLayer(this.P.tileLayer);
        }
        this.P.tileLayer = L.tileLayer(`tiles/${this.name}/{z}/{x}_{y}.png`, {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: this.zoom.max
        }).addTo(this.P.map);

        // update and show spawn point
        this.spawn.x = json.settings.spawn.x;
        this.spawn.z = json.settings.spawn.z;
        this.spawn.show();

        // center on spawn
        this.P.centerOn(this.spawn.x, this.spawn.z, this.zoom.def);

        // force self update with existing player list
        this.P.playerList.update(Array.from(this.P.playerList.players.values()));

        if (callback != null) {
            callback(this);
        }
    }
}

export { World };
