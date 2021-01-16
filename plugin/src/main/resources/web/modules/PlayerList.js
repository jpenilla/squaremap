import { Player } from "./util/Player.js";

class PlayerList {
    constructor(P) {
        this.players = new Map();
        this.P = P;
        this.P.map.createPane("nameplate").style.zIndex = 1000;
    }
    showPlayer(link) {
        const uuid = link.id;
        const keys = Array.from(this.P.playerList.players.keys());
        for (let i = 0; i < keys.length; i++) {
            const player = this.P.playerList.players.get(keys[i]);
            if (uuid == player.uuid && player.world != world) {
                const P = this.P;
                P.worldList.showWorld(player.world, function () {
                    P.map.panTo(P.unproject(player.x, player.z));
                });
            }
        }
    }
    add(player) {
        const head = document.createElement("img");
        head.src = this.P.getHeadUrl(player);
        const span = this.P.createTextElement("span", player.name);
        const link = this.P.createElement("a", player.uuid, this);
        link.onclick = function () {
            this.parent.showPlayer(this);
        };
        link.appendChild(head);
        link.appendChild(span);
        const fieldset = this.P.sidebar.playerList.element;
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
                player = new Player(players[i], this.P);
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
    }
    clearMarkers() {
        const keys = Array.from(this.players.keys());
        for (let i = 0; i < keys.length; i++) {
            const player = this.players.get(keys[i]);
            player.marker.remove();
        }
    }
}

export { PlayerList };
