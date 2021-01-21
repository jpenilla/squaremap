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
                const x = point == null ? "---" : Math.round(point.x);
                const z = point == null ? "---" : Math.round(point.y);
                this._coords.innerHTML = `Coordinates<br />${x}, ${z}`;
            }
        });
        const coords = new Coords();
        P.map.addControl(coords)
            .addEventListener('mousemove', (event) => {
                if (P.worldList.curWorld != null) {
                    coords.update(P.toPoint(event.latlng));
                }
            });
    }
}

export { UICoordinates };
