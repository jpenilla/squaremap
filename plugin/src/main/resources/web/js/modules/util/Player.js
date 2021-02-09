import { P } from '../Pl3xMap.js';

class Player {
    constructor(player) {
        this.name = player.name;
        this.uuid = player.uuid;
        this.world = player.world;
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
        this.marker = L.marker(P.toLatLng(player.x, player.z), {
            icon: L.icon({
                iconUrl: 'images/icon/player.png',
                iconSize: [17, 16],
                iconAnchor: [8, 9],
                tooltipAnchor: [0, 0]
            }),
            rotationAngle: (180 + player.yaw)
        });
        if (P.worldList.curWorld.player_tracker.nameplates.enabled) {
            this.updateNameplate(player);
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
        if (P.worldList.curWorld.player_tracker.nameplates.show_armor) {
            armorImg = `<img src="images/armor/${player.armor}.png" class="armor" />`;
        }
        if (P.worldList.curWorld.player_tracker.nameplates.show_health) {
            healthImg = `<img src="images/health/${player.health}.png" class="health" />`;
        }
        this.tooltip.setContent(`<ul><li>${headImg}</li><li>${player.name}${healthImg}${armorImg}</li>`);
    }
    update(player) {
        this.x = player.x;
        this.z = player.z;
        this.world = player.world;
        this.armor = player.armor;
        this.health = player.health;
        const link = document.getElementById(player.uuid);
        const img = link.getElementsByTagName("img")[0];
        const span = link.getElementsByTagName("span")[0];
        if (P.worldList.curWorld.name == player.world) {
            if (P.worldList.curWorld.player_tracker.enabled) {
                this.marker.addTo(P.layerControl.playersLayer);
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
            this.marker.remove();
            img.classList.add("other-world");
            span.classList.add("other-world");
        }
        this.updateNameplate(player);
    }
}

export { Player };
