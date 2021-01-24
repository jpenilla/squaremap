import { P } from './Pl3xMap.js';

class UICoordinates {
    constructor() {
        const Coords = L.Control.extend({
            _container: null,
            options: {
                position: 'bottomleft'
            },
            onAdd: function () {
                const coords = L.DomUtil.create('div', 'leaflet-control-layers coordinates');
                this._coords = coords;
                this.update();
                return coords;
            },
            update: function (point) {
                this.x = point == null ? "---" : Math.round(point.x);
                this.z = point == null ? "---" : Math.round(point.y);
                this._coords.innerHTML = `Coordinates<br />${this.x}, ${this.z}`;
            }
        });
        this.coords = new Coords();
        P.map.addControl(this.coords)
            .addEventListener('mousemove', (event) => {
                if (P.worldList.curWorld != null) {
                    this.coords.update(P.toPoint(event.latlng));
                }
            });
    }
}

export { UICoordinates };
