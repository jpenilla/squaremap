import { Pin } from "./util/Pin.js";
import { Fieldset } from "./util/Fieldset.js";
import { S } from "./Squaremap.js";

class Sidebar {
    /** @type {HTMLDivElement} */
    sidebar;
    /** @type {boolean} */
    showSidebar;
    /** @type {Pin} */
    pin;
    /** @type {Fieldset} */
    worlds;
    /** @type {Fieldset} */
    players;

    /**
     * @param {Settings_UI_Sidebar} json
     * @param {boolean} show
     */
    constructor(json, show) {
        this.sidebar = S.createElement("div", "sidebar", this);
        this.showSidebar = show;
        if (!show) {
            this.sidebar.style.display = "none";
        }
        this.sidebar.addEventListener("click", (e) => {
            S.playerList.followPlayerMarker(null);
            e.stopPropagation();
        });
        document.body.appendChild(this.sidebar);

        this.pin = new Pin(json.pinned === "pinned");
        this.show(this.pin.pinned);
        if (json.pinned !== "hide") {
            this.sidebar.appendChild(this.pin.element);
        }

        this.worlds = new Fieldset("worlds", json.world_list_label);
        this.sidebar.appendChild(this.worlds.element);

        this.players = new Fieldset("players", json.player_list_label.replace(/{cur}/g, 0).replace(/{max}/g, 0));
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

        document.addEventListener("click", (e) => {
            if (!this.sidebar.contains(e.target) && !this.pin.pinned && this.sidebar.className === "show") {
                this.show(false);
            }
        });
    }
    show(show) {
        this.sidebar.className = show ? "show" : "";
    }
}

export { Sidebar };
