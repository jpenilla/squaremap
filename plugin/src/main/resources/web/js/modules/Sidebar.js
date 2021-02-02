import { Pin } from "./util/Pin.js";
import { Fieldset } from "./util/Fieldset.js";
import { P } from './Pl3xMap.js';

class Sidebar {
    constructor(json) {
        this.sidebar = P.createElement("div", "sidebar", this);
        document.body.appendChild(this.sidebar);

        if (json.pinned != "hide") {
            this.pin = new Pin(json.pinned == "pinned");
            this.sidebar.appendChild(this.pin.element);
            this.show(this.pin.pinned);
        }

        this.worldList = new Fieldset("worlds", json.world_list_label);
        this.sidebar.appendChild(this.worldList.element);

        this.playerList = new Fieldset("players", json.player_list_label
            .replace(/{cur}/g, 0)
            .replace(/{max}/g, 0));
        this.sidebar.appendChild(this.playerList.element);

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
