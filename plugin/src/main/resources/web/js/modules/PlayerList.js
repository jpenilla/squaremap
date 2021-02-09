import { Player } from "./util/Player.js";
import { P } from './Pl3xMap.js';

class PlayerList {
    constructor(json) {
        this.players = new Map();
        this.following = null;
        this.label = json.player_list_label;
        P.map.createPane("nameplate").style.zIndex = 1000;
    }
    tick() {
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
    showPlayer(link) {
        const uuid = link.id;
        const keys = Array.from(P.playerList.players.keys());
        for (let i = 0; i < keys.length; i++) {
            const player = P.playerList.players.get(keys[i]);
            if (uuid == player.uuid && player.world != world) {
                if (P.worldList.worlds.has(player.world)) {
                    P.worldList.showWorld(player.world, () => {
                        P.map.panTo(P.toLatLng(player.x, player.z));
                    });
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
    addToList(player) {
        const head = document.createElement("img");
        head.src = player.getHeadUrl();
        const span = P.createTextElement("span", player.name);
        const link = P.createElement("a", player.uuid, this);
        link.onclick = function (e) {
            if (this.parent.showPlayer(this)) {
                this.parent.follow(this.id);
                e.stopPropagation();
            }
        };
        link.appendChild(head);
        link.appendChild(span);
        const fieldset = P.sidebar.players.element;
        fieldset.appendChild(link);
    }
    removeFromList(uuid) {
        const player = document.getElementById(uuid);
        if (player != null) {
            player.remove();
        }
    }
    updatePlayerList(players) {
        const playersToRemove = Array.from(this.players.keys());

        // update players from json
        for (let i = 0; i < players.length; i++) {
            let player = this.players.get(players[i].uuid);
            if (player == null) {
                // new player
                player = new Player(players[i]);
                this.players.set(player.uuid, player);
                this.addToList(player);
            }
            player.update(players[i]);
            playersToRemove.remove(players[i].uuid);
        }

        // remove players not in json
        for (let i = 0; i < playersToRemove.length; i++) {
            const player = this.players.get(playersToRemove[i]);
            player.marker.remove();
            this.players.delete(player.uuid);
            this.removeFromList(player.uuid);
        }

        // follow highlighted player
        if (this.following != null) {
            const player = this.players.get(this.following);
            if (player == null) {
                this.followPlayerMarker(null);
            } else {
                P.map.panTo(P.toLatLng(player.x, player.z));
            }
        }
    }
    clearPlayerMarkers() {
        const keys = Array.from(this.players.keys());
        for (let i = 0; i < keys.length; i++) {
            const player = this.players.get(keys[i]);
            player.marker.remove();
        }
        P.layerControl.playersLayer.clearLayers();
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

export { PlayerList };
