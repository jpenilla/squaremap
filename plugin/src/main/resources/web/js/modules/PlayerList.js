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
            this.update(json.players);
            const title = `${this.label}`
                .replace(/{cur}/g, json.players.length)
                .replace(/{max}/g, json.max == null ? "???" : json.max)
            if (P.sidebar.playerList.legend.innerHTML !== title) {
                P.sidebar.playerList.legend.innerHTML = title;
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
    add(player) {
        const head = document.createElement("img");
        head.src = P.getHeadUrl(player);
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
        const fieldset = P.sidebar.playerList.element;
        fieldset.appendChild(link);
    }
    remove(uuid) {
        const player = document.getElementById(uuid);
        if (player != null) {
            player.remove();
        }
    }
    update(players) {
        const playersToRemove = Array.from(this.players.keys());
        for (let i = 0; i < players.length; i++) {
            let player = this.players.get(players[i].uuid);
            if (player == null) {
                player = new Player(players[i]);
                this.players.set(player.uuid, player);
                this.add(player);
            }
            player.update(players[i]);
            playersToRemove.remove(players[i].uuid);
        }
        for (let i = 0; i < playersToRemove.length; i++) {
            const player = this.players.get(playersToRemove[i]);
            player.marker.remove();
            this.players.delete(player.uuid);
            this.remove(player.uuid);
        }
        if (this.following != null) {
            const player = this.players.get(this.following);
            if (player == null) {
                this.follow(null);
            } else {
                P.map.panTo(P.toLatLng(player.x, player.z));
            }
        }
    }
    clearMarkers() {
        const keys = Array.from(this.players.keys());
        for (let i = 0; i < keys.length; i++) {
            const player = this.players.get(keys[i]);
            player.marker.remove();
        }
    }
    follow(uuid) {
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
