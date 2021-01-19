import { P } from '../../map.js';

class Marker {
    constructor(opts) {
        this.opts = opts;
        this.id = this.opts.pop("id");
        this.popup = this.opts.pop("popup");
        this.tooltip = this.opts.pop("tooltip");
    }
    init() {
        if (this.popup != null) {
            this.marker.bindPopup(() => this.popup);
        }
        if (this.tooltip != null) {
            this.marker.bindTooltip(() => this.tooltip);
        }
        for (const key in this.opts) {
            this.marker.options[key] = this.opts[key];
        }
    }
    addTo(layer) {
        this.marker.remove();
        this.marker.addTo(layer);
    }
}

class Options {
    constructor(json) {
        for (const prop in json) {
            this[prop] = json[prop];
        }
    }
    pop(key, def) {
        const val = this[key];
        delete this[key];
        return val == null ? def : val;
    }
}

class Rectangle extends Marker {
    constructor(opts) {
        super(opts);
        const points = this.opts.pop("points");
        this.marker = L.rectangle([P.unproject(points[0].x, points[0].z), P.unproject(points[1].x, points[1].z)]);
        super.init();
    }
}

class PolyLine extends Marker {
    constructor(opts) {
        super(opts);
        const points = this.opts.pop("points");
        const outer = [];
        for (let i = 0; i < points.length; i++) {
            if (Symbol.iterator in Object(points[i])) {
                const inner = [];
                for (let j = 0; j < points[i].length; j++) {
                    inner.push(P.unproject(points[i][j].x, points[i][j].z));
                }
                outer.push(inner);
            } else {
                outer.push(P.unproject(points[i].x, points[i].z));
            }
        }
        this.marker = L.polyline(outer);
        super.init();
    }
}

class Polygon extends Marker {
    constructor(opts) {
        super(opts);
        const points = this.opts.pop("points");
        const outer = [];
        for (let i = 0; i < points.length; i++) {
            if (Symbol.iterator in Object(points[i])) {
                const inner = [];
                for (let j = 0; j < points[i].length; j++) {
                    if (Symbol.iterator in Object(points[i][j])) {
                        const inner2 = [];
                        for (let k = 0; k < points[i][j].length; k++) {
                            inner2.push(P.unproject(points[i][j][k].x, points[i][j][k].z));
                        }
                        inner.push(inner2);
                    } else {
                        inner.push(P.unproject(points[i][j].x, points[i][j].z));
                    }
                }
                outer.push(inner);
            } else {
                outer.push(P.unproject(points[i].x, points[i].z));
            }
        }
        this.marker = L.polygon(outer);
        super.init();
    }
}

class Circle extends Marker {
    constructor(opts) {
        super(opts);
        const center = this.opts.pop("center");
        const radius = this.opts.pop("radius");
        this.marker = L.circle(P.unproject(center.x, center.z), {
            radius: P.pixelsToMeters(radius)
        });
        super.init();
    }
}

class Icon extends Marker {
    constructor(opts) {
        super(opts);
        const point = this.opts.pop("point");
        const size = this.opts.pop("size");
        const anchor = this.opts.pop("anchor");
        const popupAnchor = this.opts.pop("popup_anchor", L.point(0, 0));
        const tooltipAnchor = this.opts.pop("tooltip_anchor", L.point(0, 0));
        this.marker = L.marker(P.unproject(point.x, point.z), {
            icon: L.icon({
                iconUrl: `images/icon/${opts.pop("icon")}`,
                iconSize: [size.x, size.z],
                iconAnchor: [anchor.x, anchor.z],
                popupAnchor: [popupAnchor.x, popupAnchor.z],
                tooltipAnchor: [tooltipAnchor.x, tooltipAnchor.z]
            })
        });
        super.init();
    }
}

export { Marker, Options, Rectangle, PolyLine, Polygon, Circle, Icon };
