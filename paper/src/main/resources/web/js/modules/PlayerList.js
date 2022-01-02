import { Player } from "./util/Player.js";
import { P } from './Squaremap.js';

class PlayerList {
    constructor(json) {
        this.players = new Map();
        this.markers = new Map();
        this.following = null;
        this.firstTick = true;
        this.label = json.player_list_label;
        P.map.createPane("nameplate").style.zIndex = 1000;
    }
    tick() {
        if (P.tick_count % P.worldList.curWorld.player_tracker.update_interval == 0) {
            P.getJSON("tiles/players.json", (json) => {
                this.updatePlayerList(json.players);
                const title = `${this.label}`
                    .replace(/{cur}/g, json.players.length)
                    .replace(/{max}/g, json.max == null ? "???" : json.max)
                if (P.sidebar.players.legend.innerHTML !== title) {
                    P.sidebar.players.legend.innerHTML = title;
                }
            });
        }
    }
    showPlayer(uuid) {
        const player = this.players.get(uuid);
        if (!P.worldList.worlds.has(player.world)) {
            return false;
        }
        P.worldList.showWorld(player.world, () => {
            P.map.panTo(P.toLatLng(player.x, player.z));
        });
        return true;
    }
    addToList(player) {
        const head = document.createElement("img");
        head.src = player.getHeadUrl();

        const span = document.createElement("span");
        span.innerHTML = player.displayName

        const link = P.createElement("a", player.uuid, this);
        link.onclick = function (e) {
            if (this.parent.showPlayer(this.id)) {
                this.parent.followPlayerMarker(this.id);
                e.stopPropagation();
            }
        };
        link.appendChild(head);
        link.appendChild(span);
        const fieldset = P.sidebar.players.element;
        fieldset.appendChild(link);
        Array.from(fieldset.getElementsByTagName("a"))
            .sort((a, b) => {
                return plain(a.getElementsByTagName("span")[0])
                    .localeCompare(plain(b.getElementsByTagName("span")[0]));
            })
            .forEach(link => fieldset.appendChild(link));
    }
    removeFromList(player) {
        const link = document.getElementById(player.uuid);
        if (link != null) {
            link.remove();
        }
        this.players.delete(player.uuid);
        player.removeMarker();
    }
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
                    document.getElementById(player.uuid)
                        .getElementsByTagName("span")[0]
                        .innerHTML = player.displayName;
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
            const fieldset = P.sidebar.players.element;
            Array.from(fieldset.getElementsByTagName("a"))
                .sort((a, b) => {
                    return plain(a.getElementsByTagName("span")[0])
                        .localeCompare(plain(b.getElementsByTagName("span")[0]));
                })
                .forEach(link => fieldset.appendChild(link));
        }

        // first tick only
        if (this.firstTick) {
            this.firstTick = false;

            // follow uuid from url
            const follow = P.getUrlParam("uuid", null);
            if (follow != null && this.players.get(follow) != null) {
                this.followPlayerMarker(follow);
            }
        }

        // follow highlighted player
        if (this.following != null) {
            const player = this.players.get(this.following);
            if (player == null || P.worldList.curWorld == null || player.world !== P.worldList.curWorld.name) {
                this.followPlayerMarker(null);
            } else {
                P.map.panTo(P.toLatLng(player.x, player.z));
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
        //P.layerControl.playersLayer.clearLayers();
    }
    followPlayerMarker(uuid) {
        if (this.following != null) {
            document.getElementById(this.following).classList.remove("following");
            this.following = null;
        }
        if (uuid != null) {
            this.following = uuid;
            document.getElementById(this.following).classList.add("following");
        }
    }
}

function plain(element) {
    return element.textContent || element.innerText || "";
}

export { PlayerList };
