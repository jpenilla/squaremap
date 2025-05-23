import { World } from "./util/World.js";
import { S } from "./Squaremap.js";

class WorldList {
    /** @type {Map<string, World>} */
    worlds;
    /** @type {World} */
    curWorld;

    /**
     * @param json {Settings_World[]}
     */
    constructor(json) {
        // get worlds from json
        /** @type {Map<string, World>} */
        const unorderedMap = new Map();
        for (let i = 0; i < json.length; i++) {
            const world = new World(json[i]);
            unorderedMap.set(world.name, world);
        }

        // sort worlds by order
        this.worlds = new Map([...unorderedMap].sort((a, b) => a[1].order - b[1].order));

        // set up world list link elements
        for (const [name, world] of this.worlds) {
            const link = S.createElement("a", name, this);
            link.onclick = function () {
                const curWorld = this.parent.curWorld;
                if (curWorld.name === name) {
                    S.centerOn(world.spawn.x, world.spawn.z, world.zoom.def);
                    return;
                }
                S.playerList.clearPlayerMarkers();
                this.parent.loadWorld(name, (world) => {
                    S.centerOn(world.spawn.x, world.spawn.z, world.zoom.def);
                });
            };

            const img = document.createElement("img");
            img.src = this.getIcon(world);

            link.appendChild(img);
            link.appendChild(S.createTextElement("span", world.display_name));

            S.sidebar.worlds.element.appendChild(link);
        }
    }
    /**
     * @param {World} world
     * @returns {string}
     */
    getIcon(world) {
        if (world.icon != null && world.icon !== "") {
            return `images/icon/${world.icon}.png`;
        }
        switch (world.type) {
            case "nether":
                return "images/icon/red-cube-smol.png";
            case "the_end":
                return "images/icon/purple-cube-smol.png";
            case "normal":
            default:
                return "images/icon/green-cube-smol.png";
        }
    }
    /**
     * @param json {Settings}
     * @param callback {(world: World) => void}
     */
    loadInitialWorld(json, callback) {
        let updateUrl = false;
        let name = S.getUrlParam("world", null);
        if (name != null) {
            const world = this.worlds.get(name);
            if (world == null) {
                updateUrl = true;
                name = null;
            }
        }
        if (name == null) {
            name = json.worlds.sort((a, b) => a.order - b.order)[0].name;
        }
        this.loadWorld(name, (a) => {
            callback(a);
            if (updateUrl) {
                S.updateBrowserUrl(`?world=${this.curWorld.name}`);
            }
        });
    }
    /**
     * @param {string} name
     * @param {(world: World) => void} callback
     */
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
    /**
     * @param {string} name
     * @param {(world: World) => void} callback
     */
    showWorld(name, callback) {
        if (this.curWorld.name === name) {
            if (callback != null) {
                callback();
            }
            return;
        }
        this.loadWorld(name, () => {
            callback();
            S.updateBrowserUrl(`?world=${this.curWorld.name}`);
        });
    }
}

export { WorldList };
