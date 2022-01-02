import { Pin } from "./util/Pin.js";
import { Fieldset } from "./util/Fieldset.js";
import { P } from './Squaremap.js';

class Sidebar {
    constructor(json) {
        this.sidebar = P.createElement("div", "sidebar", this);
        this.sidebar.addEventListener("click", (e) => {
            P.playerList.followPlayerMarker(null);
        });
        document.body.appendChild(this.sidebar);

        this.pin = new Pin(json.pinned == "pinned");
        this.show(this.pin.pinned);
        if (json.pinned != "hide") {
            this.sidebar.appendChild(this.pin.element);
        }

        this.worlds = new Fieldset("worlds", json.world_list_label);
        this.sidebar.appendChild(this.worlds.element);

        this.players = new Fieldset("players", json.player_list_label
            .replace(/{cur}/g, 0)
            .replace(/{max}/g, 0));
        this.sidebar.appendChild(this.players.element);

        this.sidebar.onmouseleave = () => {
            if (!this.pin.pinned) {
                this.show(false);
            }
        };
        this.sidebar.onmouseenter = () => {
            if (!this.pin.pinned) {
                this.show(true);
            }
        };
    }
    show(show) {
        this.sidebar.className = show ? "show" : "";
    }
}

export { Sidebar };
