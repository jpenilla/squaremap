class Spawn {
    constructor(world) {
        this.x = 0;
        this.z = 0;
        this.P = world.P
        this.spawn = L.marker([this.x, this.z], {
            icon: L.icon({
                iconUrl: 'images/spawn.png',
                iconSize: [16, 16],
                iconAnchor: [8, 8],
                popupAnchor: [0, -10]
            })
        }).bindPopup(`${world.display_name} Spawn`);
    }
    show() {
        this.spawn.addTo(this.P.map);
        this.spawn.setLatLng(this.P.unproject(this.x, this.z));
    }
    hide() {
        this.P.map.removeLayer(this.spawn);
    }
}

export { Spawn };
