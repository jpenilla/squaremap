import { P } from './Squaremap.js';

class UICoordinates {
    constructor(json) {
        const Coords = L.Control.extend({
            _container: null,
            options: {
                position: 'bottomleft'
            },
            onAdd: function () {
                const coords = L.DomUtil.create('div', 'leaflet-control-layers coordinates');
                this._coords = coords;
                return coords;
            },
            update: function (html, point) {
                this.x = point == null ? "---" : Math.round(point.x);
                this.z = point == null ? "---" : Math.round(point.y);
                if (html != null) {
                    this._coords.innerHTML = html
                        .replace(/{x}/g, this.x)
                        .replace(/{z}/g, this.z);
                }
            }
        });
        this.html = json.html == null ? "undefined" : json.html;
        this.coords = new Coords();
        P.map.addControl(this.coords)
            .addEventListener('mousemove', (event) => {
                if (P.worldList.curWorld != null) {
                    this.coords.update(this.html, P.toPoint(event.latlng));
                }
            });
        if (!json.enabled) {
            this.coords._coords.style.display = "none";
        }
        this.coords.update(this.html);
    }
}

export { UICoordinates };
