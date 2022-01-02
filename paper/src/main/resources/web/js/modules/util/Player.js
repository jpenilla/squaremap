import { P } from '../Squaremap.js';

class Player {
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
            pane: "nameplate"
        });
        this.marker = L.marker(P.toLatLng(json.x, json.z), {
            icon: L.icon({
                iconUrl: 'images/icon/player.png',
                iconSize: [17, 16],
                iconAnchor: [8, 9],
                tooltipAnchor: [0, 0]
            }),
            rotationAngle: (180 + json.yaw)
        });
        if (P.worldList.curWorld.player_tracker.nameplates.enabled) {
            this.updateNameplate(json);
            this.marker.bindTooltip(this.tooltip);
        }
    }
    getHeadUrl() {
        return P.worldList.curWorld.player_tracker.nameplates.heads_url
            .replace(/{uuid}/g, this.uuid)
            .replace(/{name}/g, this.name);
    }
    updateNameplate(player) {
        let headImg = "";
        let armorImg = "";
        let healthImg = "";
        if (P.worldList.curWorld.player_tracker.nameplates.show_heads) {
            headImg = `<img src='${this.getHeadUrl()}' class="head" />`;
        }
        if (P.worldList.curWorld.player_tracker.nameplates.show_armor && player.armor != null) {
            armorImg = `<img src="images/armor/${Math.min(Math.max(player.armor, 0), 20)}.png" class="armor" />`;
        }
        if (P.worldList.curWorld.player_tracker.nameplates.show_health && player.health != null) {
            healthImg = `<img src="images/health/${Math.min(Math.max(player.health, 0), 20)}.png" class="health" />`;
        }
        this.tooltip.setContent(`<ul><li>${headImg}</li><li>${this.displayName}${healthImg}${armorImg}</li>`);
    }
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
        if (P.worldList.curWorld.name == player.world) {
            if (P.worldList.curWorld.player_tracker.enabled) {
                this.addMarker();
            }
            const latlng = P.toLatLng(player.x, player.z);
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
        P.playerList.markers.delete(this.uuid);
        P.map.removeLayer(this.marker);
        P.layerControl.playersLayer.removeLayer(this.marker);
    }
    addMarker() {
        if (!P.playerList.markers.has(this.uuid)) {
            this.marker.addTo(P.layerControl.playersLayer);
            P.playerList.markers.set(this.uuid, this.marker);
        }
    }
}

export { Player };
