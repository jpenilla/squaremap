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
    /** @type {{x: number, z: number, yaw: number} | null} */
    position;
    /** @type {number | undefined} */
    armor;
    /** @type {number | undefined} */
    health;
    /** @type {L.Tooltip} */
    tooltip;
    /** @type {L.Marker | null} */
    marker;

    /**
     * @param {PlayerData} json
     */
    constructor(json) {
        this.name = json.name;
        this.uuid = json.uuid;
        this.world = json.world;
        this.displayName = json.display_name !== undefined ? json.display_name : json.name;
        this.updatePosition(json);
        this.armor = json.armor;
        this.health = json.health;
        this.tooltip = L.tooltip({
            permanent: true,
            direction: "right",
            offset: [10, 0],
            pane: "nameplate",
            content: this.makeNameplateContent(json),
        });
        this.marker = null;
        this.syncMarker(json);
    }
    /**
     * @param {PlayerData} player
     */
    syncMarker(player) {
        const world = S.worldList.curWorld;
        if (world == null || world.name !== this.world || !world.player_tracker.enabled || this.position == null) {
            this.removeMarker();
            return;
        }

        const latlng = S.toLatLng(this.position.x, this.position.z);
        if (this.marker == null) {
            this.marker = L.marker(latlng, {
                icon: L.icon({
                    iconUrl: "images/icon/player.png",
                    iconSize: [17, 16],
                    iconAnchor: [8, 9],
                    tooltipAnchor: [0, 0],
                }),
                rotationAngle: 180 + this.position.yaw,
            });
            if (world.player_tracker.nameplates.enabled) {
                this.updateNameplate(player);
                this.marker.bindTooltip(this.tooltip);
            }
        } else {
            if (!this.marker.getLatLng().equals(latlng)) {
                this.marker.setLatLng(latlng);
            }
            const angle = 180 + this.position.yaw;
            if (this.marker.options.rotationAngle != angle) {
                this.marker.setRotationAngle(angle);
            }
        }
        if (!S.playerList.markers.has(this.uuid)) {
            this.marker.addTo(S.layerControl.playersLayer);
            S.playerList.markers.set(this.uuid, this.marker);
        }
    }
    getHeadUrl() {
        return S.worldList.curWorld.player_tracker.nameplates.heads_url
            .replace(/{uuid}/g, this.uuid)
            .replace(/{name}/g, this.name);
    }
    makeNameplateContent(player) {
        const container = document.createElement("div");
        container.classList.add("nameplate-container");

        if (S.worldList.curWorld.player_tracker.nameplates.show_heads) {
            const head = document.createElement("img");
            head.src = this.getHeadUrl();
            head.classList.add("head");
            container.appendChild(head);
        }

        const col2 = document.createElement("div");
        col2.classList.add("nameplate-col2");

        const displayName = document.createElement("span");
        displayName.classList.add("display-name");
        displayName.innerHTML = this.displayName;
        col2.appendChild(displayName);

        if (S.worldList.curWorld.player_tracker.nameplates.show_health && player.health != null) {
            const health = document.createElement("img");
            health.src = `images/health/${Math.min(Math.max(player.health, 0), 20)}.png`;
            health.classList.add("health");
            col2.appendChild(health);
        }

        if (S.worldList.curWorld.player_tracker.nameplates.show_armor && player.armor != null) {
            const armor = document.createElement("img");
            armor.src = `images/armor/${Math.min(Math.max(player.armor, 0), 20)}.png`;
            armor.classList.add("armor");
            col2.appendChild(armor);
        }

        container.appendChild(col2);
        return container;
    }
    setNameplateContent(player) {
        this.tooltip.setContent(this.makeNameplateContent(player));
    }
    /**
     * @param {PlayerData} player
     */
    updateNameplate(player) {
        /**
         * @param {PlayerData} player
         * @param {boolean} a
         * @param {boolean} b
         */
        const resetOnMismatch = (player, a, b) => {
            if (a !== b) {
                this.setNameplateContent(player);
                return true;
            }
            return false;
        };

        const head = this.tooltip.getContent().querySelectorAll(".head")[0];
        if (resetOnMismatch(player, head !== undefined, S.worldList.curWorld.player_tracker.nameplates.show_heads)) {
            return;
        }
        const armor = this.tooltip.getContent().querySelectorAll(".armor")[0];
        if (
            resetOnMismatch(
                player,
                armor !== undefined,
                S.worldList.curWorld.player_tracker.nameplates.show_armor && player.armor != null,
            )
        ) {
            return;
        }
        const health = this.tooltip.getContent().querySelectorAll(".health")[0];
        if (
            resetOnMismatch(
                player,
                health !== undefined,
                S.worldList.curWorld.player_tracker.nameplates.show_health && player.health != null,
            )
        ) {
            return;
        }
        /** @type {HTMLSpanElement} */
        const displayName = this.tooltip.getContent().querySelectorAll(".display-name")[0];

        // Only update if it's different
        if (displayName.innerHTML !== this.displayName) {
            displayName.innerHTML = this.displayName;
        }
        if (head) {
            const headSrc = this.getHeadUrl();
            const currentHeadSrc = head.src.split("/").pop();
            if (currentHeadSrc !== headSrc.split("/").pop()) {
                head.src = headSrc;
            }
        }
        if (health && player.health != null) {
            const healthSrc = `images/health/${Math.min(Math.max(player.health, 0), 20)}.png`;
            const currentHealthSrc = health.src.split("/").pop();
            if (currentHealthSrc !== healthSrc.split("/").pop()) {
                health.src = healthSrc;
            }
        }
        if (armor && player.armor != null) {
            const armorSrc = `images/armor/${Math.min(Math.max(player.armor, 0), 20)}.png`;
            const currentArmorSrc = armor.src.split("/").pop();
            if (currentArmorSrc !== armorSrc.split("/").pop()) {
                armor.src = armorSrc;
            }
        }
    }
    /**
     * @param {PlayerData} player
     */
    update(player) {
        this.updatePosition(player);
        this.world = player.world;
        this.armor = player.armor;
        this.health = player.health;
        this.displayName = player.display_name !== undefined ? player.display_name : player.name;
        const link = document.getElementById(player.uuid);
        if (link != null) {
            const img = link.getElementsByTagName("img")[0];
            const span = link.getElementsByTagName("span")[0];
            if (S.worldList.curWorld.name == player.world) {
                img.classList.remove("other-world");
                span.classList.remove("other-world");
            } else {
                img.classList.add("other-world");
                span.classList.add("other-world");
            }
        }
        this.syncMarker(player);
        this.updateNameplate(player);
    }
    removeMarker() {
        const marker = this.marker;
        if (marker == null) {
            S.playerList.markers.delete(this.uuid);
            return;
        }
        marker.remove();
        S.playerList.markers.delete(this.uuid);
        S.map.removeLayer(marker);
        if (S.layerControl.playersLayer != null) {
            S.layerControl.playersLayer.removeLayer(marker);
        }
        this.marker = null;
    }
    /**
     * @param {PlayerData} player
     */
    updatePosition(player) {
        this.position =
            player.x === undefined
                ? null
                : /** @type {{x: number, z: number, yaw: number}} */ ({
                      x: player.x,
                      z: player.z,
                      yaw: player.yaw,
                  });
    }
}

export { Player };
