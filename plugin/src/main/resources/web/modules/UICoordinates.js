class UICoordinates {
    constructor(P) {
        const Coords = L.Control.extend({
            _container: null,
            options: {
                position: 'bottomleft'
            },
            onAdd: function (map) {
                const coords = L.DomUtil.create('div', 'leaflet-control-layers coordinates');
                this._coords = coords;
                this.updateHTML(null);
                return coords;
            },
            updateHTML: function (point) {
                const x = point == null ? "---" : Math.round(point.x);
                const z = point == null ? "---" : Math.round(point.y);
                this._coords.innerHTML = `Coordinates<br />${x}, ${z}`;
            }
        });
        const coords = new Coords();
        P.map.addControl(coords);
        P.map.addEventListener('mousemove', (event) => {
            if (P.worldList.curWorld != null) {
                coords.updateHTML(P.project(event.latlng));
            }
        });
    }
}

export { UICoordinates };
