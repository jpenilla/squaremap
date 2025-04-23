import { S } from "../Squaremap.js";
import L from "leaflet";

class Player {
    /** @type {string} */
    name;
    /** @type {string} */
    uuid;
    /** @type {string} */
    world;
    /** @type {string} */
    displayName;
    /** @type {number} */
    x;
    /** @type {number} */
    z;
    /** @type {number} */
    armor;
    /** @type {number} */
    health;
    /** @type {L.Tooltip} */
    tooltip;
    /** @type {L.Marker} */
    marker;

    /**
     * @param {PlayerData} json
     */
    constructor(json) {
        this.name = json.name;
        this.uuid = json.uuid;
        this.world = json.world;
        this.displayName = json.display_name !== undefined ? json.display_name : json.name;
        this.x = 0;
        this.z = 0;
        this.armor = 0;
        this.health = 20;
        this.tooltip = L.tooltip({
            permanent: true,
            direction: "right",
            offset: [10, 0],
            pane: "nameplate",
            content: this.makeNameplateContent(json),
        });
        this.marker = L.marker(S.toLatLng(json.x, json.z), {
            icon: L.icon({
                iconUrl: "images/icon/player.png",
                iconSize: [17, 16],
                iconAnchor: [8, 9],
                tooltipAnchor: [0, 0],
            }),
            rotationAngle: 180 + json.yaw,
        });
        if (S.worldList.curWorld.player_tracker.nameplates.enabled) {
            this.updateNameplate(json);
            this.marker.bindTooltip(this.tooltip);
        }
    }
    getHeadUrl() {
        return S.worldList.curWorld.player_tracker.nameplates.heads_url
            .replace(/{uuid}/g, this.uuid)
            .replace(/{name}/g, this.name);
    }
    makeNameplateContent(player) {
        const ul = document.createElement("ul");
        let headImg = "";
        let armorImg = "";
        let healthImg = "";
        if (S.worldList.curWorld.player_tracker.nameplates.show_heads) {
            headImg = `<img src='${this.getHeadUrl()}' class="head" />`;
        }
        if (S.worldList.curWorld.player_tracker.nameplates.show_armor && player.armor != null) {
            armorImg = `<img src="images/armor/${Math.min(Math.max(player.armor, 0), 20)}.png" class="armor" />`;
        }
        if (S.worldList.curWorld.player_tracker.nameplates.show_health && player.health != null) {
            healthImg = `<img src="images/health/${Math.min(Math.max(player.health, 0), 20)}.png" class="health" />`;
        }
        ul.innerHTML = `<li>${headImg}</li><li><span class="display-name">${this.displayName}</span>${healthImg}${armorImg}</li>`;
        return ul;
    }
    setNameplateContent(player) {
        this.tooltip.setContent(this.makeNameplateContent(player));
    }
    updateNameplate(player) {
        const head = this.tooltip.getContent().querySelectorAll(".head")[0];
        if (!head && S.worldList.curWorld.player_tracker.nameplates.show_heads) {
            this.setNameplateContent(player);
            return;
        }
        const armor = this.tooltip.getContent().querySelectorAll(".armor")[0];
        if (!armor && S.worldList.curWorld.player_tracker.nameplates.show_armor && player.armor != null) {
            this.setNameplateContent(player);
            return;
        }
        const health = this.tooltip.getContent().querySelectorAll(".health")[0];
        if (!health && S.worldList.curWorld.player_tracker.nameplates.show_health && player.health != null) {
            this.setNameplateContent(player);
            return;
        }
        const displayName = this.tooltip.getContent().querySelectorAll(".display-name")[0];

        if (displayName.innerText !== this.displayName) {
            displayName.innerText = this.displayName;
        }

        // Only update the src if it's different
        const headSrc = this.getHeadUrl();
        const currentHeadSrc = head.src.split("/").pop();
        if (currentHeadSrc !== headSrc.split("/").pop()) {
            head.src = headSrc;
        }
        const healthSrc = `images/health/${Math.min(Math.max(player.health, 0), 20)}.png`;
        const currentHealthSrc = health.src.split("/").pop();
        if (currentHealthSrc !== healthSrc.split("/").pop()) {
            health.src = healthSrc;
        }
        const armorSrc = `images/armor/${Math.min(Math.max(player.armor, 0), 20)}.png`;
        const currentArmorSrc = armor.src.split("/").pop();
        if (currentArmorSrc !== armorSrc.split("/").pop()) {
            armor.src = armorSrc;
        }
    }
    /**
     * @param {PlayerData} player
     */
    update(player) {
        this.x = player.x;
        this.z = player.z;
        this.world = player.world;
        this.armor = player.armor;
        this.health = player.health;
        this.displayName = player.display_name !== undefined ? player.display_name : player.name;
        const link = document.getElementById(player.uuid);
        const img = link.getElementsByTagName("img")[0];
        const span = link.getElementsByTagName("span")[0];
        if (S.worldList.curWorld.name == player.world) {
            if (S.worldList.curWorld.player_tracker.enabled) {
                this.addMarker();
            }
            const latlng = S.toLatLng(player.x, player.z);
            if (!this.marker.getLatLng().equals(latlng)) {
                this.marker.setLatLng(latlng);
            }
            const angle = 180 + player.yaw;
            if (this.marker.options.rotationAngle != angle) {
                this.marker.setRotationAngle(angle);
            }
            img.classList.remove("other-world");
            span.classList.remove("other-world");
        } else {
            this.removeMarker();
            img.classList.add("other-world");
            span.classList.add("other-world");
        }
        this.updateNameplate(player);
    }
    removeMarker() {
        this.marker.remove();
        S.playerList.markers.delete(this.uuid);
        S.map.removeLayer(this.marker);
        S.layerControl.playersLayer.removeLayer(this.marker);
    }
    addMarker() {
        if (!S.playerList.markers.has(this.uuid)) {
            this.marker.addTo(S.layerControl.playersLayer);
            S.playerList.markers.set(this.uuid, this.marker);
        }
    }
}

export { Player };
