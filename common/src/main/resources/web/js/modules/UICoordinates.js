import { P } from './Squaremap.js';

class UICoordinates {
    constructor(json, show) {
        const Coords = L.Control.extend({
            _container: null,
            options: {
                position: 'bottomleft'
            },
            onAdd: function () {
                const coords = L.DomUtil.create('div', 'leaflet-control-layers coordinates');
                this._coords = coords;
                if (!show) {
                    this._coords.style.display = "none";
                }
                return coords;
            },
            update: function (html, point) {
                this.x = point == null ? "---" : Math.floor(point.x);
                this.z = point == null ? "---" : Math.floor(point.y);
                if (html != null) {
                    this._coords.innerHTML = html
                        .replace(/{x}/g, this.x)
                        .replace(/{z}/g, this.z);
                }
            }
        });
        this.showCoordinates = show;
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
