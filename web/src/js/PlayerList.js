import { Player } from "./util/Player.js";
import { S } from "./Squaremap.js";

class PlayerList {
    /** @type {Map<string, Player>} */
    players;
    /** @type {Map} */
    markers;
    /** @type {string | null} */
    following;
    /** @type {boolean} */
    firstTick;
    /** @type {string} */
    label;
    /** @type {PlayersData | null} */
    jsonCache;

    /**
     * @param {Settings_UI_Sidebar} json
     */
    constructor(json) {
        this.players = new Map();
        this.markers = new Map();
        this.jsonCache = null;
        this.following = null;
        this.firstTick = true;
        this.label = json.player_list_label;
        S.map.createPane("nameplate").style.zIndex = 1000;
    }
    tick() {
        const update = () => {
            this.updatePlayerList(this.jsonCache.players);
            const title = `${this.label}`
                .replace(/{cur}/g, this.jsonCache.players.length)
                .replace(/{max}/g, this.jsonCache.max == null ? "???" : this.jsonCache.max);
            if (S.sidebar.players.legend.innerHTML !== title) {
                S.sidebar.players.legend.innerHTML = title;
            }
        };
        const fetchPlayers = (callback) => {
            S.getJSON(
                "tiles/players.json",
                /** @param {PlayersData} json */
                (json) => {
                    this.jsonCache = json;
                    callback();
                },
            );
        };

        if (S.tick_count % S.worldList.curWorld.player_tracker.update_interval === 0) {
            if (S.staticMode) {
                if (this.jsonCache === null) {
                    fetchPlayers(() => update());
                } else {
                    update();
                }
            } else {
                fetchPlayers(() => update());
            }
        }
    }
    /**
     * @param {string} uuid
     */
    showPlayer(uuid) {
        const player = this.players.get(uuid);
        if (!S.worldList.worlds.has(player.world)) {
            return false;
        }
        S.worldList.showWorld(player.world, () => {
            S.map.panTo(S.toLatLng(player.x, player.z));
        });
        return true;
    }
    /**
     * @param {Player} player
     */
    addToList(player) {
        const head = document.createElement("img");
        head.classList.add("head");
        head.src = player.getHeadUrl();

        const span = document.createElement("span");
        span.innerHTML = player.displayName;

        const link = S.createElement("a", player.uuid, this);
        link.onclick = function (e) {
            if (this.parent.showPlayer(this.id)) {
                this.parent.followPlayerMarker(this.id);
                e.stopPropagation();
            }
        };
        link.appendChild(head);
        link.appendChild(span);
        const fieldset = S.sidebar.players.element;
        fieldset.appendChild(link);
        Array.from(fieldset.getElementsByTagName("a"))
            .sort((a, b) => {
                return plain(a.getElementsByTagName("span")[0]).localeCompare(plain(b.getElementsByTagName("span")[0]));
            })
            .forEach((link) => fieldset.appendChild(link));
    }
    /**
     * @param {Player} player
     */
    removeFromList(player) {
        const link = document.getElementById(player.uuid);
        if (link != null) {
            link.remove();
        }
        this.players.delete(player.uuid);
        player.removeMarker();
    }
    /**
     * @param {PlayerData[]} players
     */
    updatePlayerList(players) {
        const playersToRemove = Array.from(this.players.keys());

        let needsSort = false;

        // update players from json
        for (let i = 0; i < players.length; i++) {
            let player = this.players.get(players[i].uuid);
            if (player == null) {
                // new player
                player = new Player(players[i]);
                this.players.set(player.uuid, player);
                this.addToList(player);
            } else {
                const oldDisplayName = player.displayName;
                player.update(players[i]);
                if (oldDisplayName !== player.displayName) {
                    needsSort = true;
                    document.getElementById(player.uuid).getElementsByTagName("span")[0].innerHTML = player.displayName;
                }
            }
            playersToRemove.remove(players[i].uuid);
        }

        // remove players not in json
        for (let i = 0; i < playersToRemove.length; i++) {
            const player = this.players.get(playersToRemove[i]);
            this.removeFromList(player);
        }

        if (needsSort) {
            const fieldset = S.sidebar.players.element;
            Array.from(fieldset.getElementsByTagName("a"))
                .sort((a, b) => {
                    return plain(a.getElementsByTagName("span")[0]).localeCompare(
                        plain(b.getElementsByTagName("span")[0]),
                    );
                })
                .forEach((link) => fieldset.appendChild(link));
        }

        // first tick only
        if (this.firstTick) {
            this.firstTick = false;

            // follow uuid from url
            const follow = S.getUrlParam("uuid", null);
            if (follow != null && this.players.get(follow) != null) {
                this.followPlayerMarker(follow);
            }
        }

        // follow highlighted player
        if (this.following != null) {
            const player = this.players.get(this.following);
            if (player != null && S.worldList.curWorld != null) {
                if (player.world !== S.worldList.curWorld.name) {
                    S.worldList.showWorld(player.world, () => {
                        S.map.panTo(S.toLatLng(player.x, player.z));
                    });
                } else {
                    S.map.panTo(S.toLatLng(player.x, player.z));
                }
            }
        }
    }
    clearPlayerMarkers() {
        const playersToRemove = Array.from(this.players.keys());
        for (let i = 0; i < playersToRemove.length; i++) {
            const player = this.players.get(playersToRemove[i]);
            player.removeMarker();
        }
        this.markers.clear();
        // S.layerControl.playersLayer.clearLayers();
    }
    /**
     * @param {string} uuid
     */
    followPlayerMarker(uuid) {
        if (this.following !== null && this.following !== uuid) {
            document.getElementById(this.following).classList.remove("following");
        }
        this.following = uuid;
        if (this.following != null) {
            document.getElementById(this.following).classList.add("following");
        }
    }
}

/**
 * @param {HTMLElement} element
 */
function plain(element) {
    return element.textContent || element.innerText || "";
}

export { PlayerList };
