import { Player } from "./util/Player.js";
import { P } from './Pl3xMap.js';

class PlayerList {
    constructor() {
        this.players = new Map();
        this.following = null;
        P.map.createPane("nameplate").style.zIndex = 1000;
    }
    tick() {
        P.getJSON("tiles/players.json", (json) => {
            this.update(json.players);
        });
    }
    showPlayer(link) {
        const uuid = link.id;
        const keys = Array.from(P.playerList.players.keys());
        for (let i = 0; i < keys.length; i++) {
            const player = P.playerList.players.get(keys[i]);
            if (uuid == player.uuid && player.world != world) {
                P.worldList.showWorld(player.world, () => {
                    P.map.panTo(P.toLatLng(player.x, player.z));
                });
            }
        }
    }
    add(player) {
        const head = document.createElement("img");
        head.src = P.getHeadUrl(player);
        const span = P.createTextElement("span", player.name);
        const link = P.createElement("a", player.uuid, this);
        link.onclick = function (e) {
            this.parent.showPlayer(this);
            this.parent.follow(this.id);
            e.stopPropagation();
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
        if (uuid != null) {
            console.log("1");
            document.getElementById(uuid).classList.add("following");
            this.following = uuid;
        } else if (this.following != null) {
            console.log("2");
            document.getElementById(this.following).classList.remove("following");
            this.following = null;
        }
    }
}

export { PlayerList };
