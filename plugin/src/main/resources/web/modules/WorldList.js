import { World } from "./util/World.js";

class WorldList {
    constructor(json, P) {
        this.worlds = new Map();
        this.P = P;

        for (let i = 0; i < json.length; i++) {
            const world = new World(json[i], this.P);
            this.worlds.set(world.name, world);

            const link = this.P.createElement("a", world.name, this);
            link.onclick = function () {
                const curWorld = this.parent.curWorld;
                const name = this.id;
                if (curWorld.name == name) {
                    this.parent.P.centerOn(curWorld.spawn.x, curWorld.spawn.z, curWorld.zoom.def);
                    return;
                }
                this.parent.P.playerList.clearMarkers();
                this.parent.loadWorld(name);
            };

            const img = document.createElement("img");
            switch (world.type) {
                case "nether":
                    img.src = "images/red-cube-smol.png";
                    break;
                case "the_end":
                    img.src = "images/purple-cube-smol.png";
                    break;
                case "normal":
                default:
                    img.src = "images/green-cube-smol.png";
                    break;
            }

            link.appendChild(img);
            link.appendChild(this.P.createTextElement("span", world.display_name));

            this.P.sidebar.worldList.element.appendChild(link);
        }
    }
    loadWorld(name, callback) {
        // unload current world
        if (this.curWorld != null) {
            this.curWorld.unload();
        }

        // load new world
        const world = this.worlds.get(name);
        this.curWorld = world;
        world.load(callback);
    }
    showWorld(world, callback) {
        if (this.curWorld.name == world) {
            this.P.centerOn(this.curWorld.spawn.x, this.curWorld.spawn.z, this.curWorld.zoom.def);
            if (callback != null) {
                callback();
            }
            return;
        }
        this.P.playerList.clearMarkers();
        this.loadWorld(world, callback);
        this.P.updateBrowserUrl(this.P.getUrlFromView());
    }
}

export { WorldList };
