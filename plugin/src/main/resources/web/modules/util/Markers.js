import { P } from '../../map.js';

class Options {
    constructor(json) {
        for (const prop in json) {
            this[prop] = json[prop];
        }
    }
    pop(key) {
        const val = this[key];
        delete this[key];
        return val;
    }
}

class Rectangle {
    constructor(opts) {
        const points = opts.pop("points");
        this.rectangle = L.rectangle([P.unproject(points[0].x, points[0].z), P.unproject(points[1].x, points[1].z)]);
        for (const key in opts) {
            this.rectangle.options[key] = opts[key];
        }
    }
    draw(layer) {
        this.rectangle.remove();
        this.rectangle.addTo(layer);
    }
}

class PolyLine {
    constructor(opts) {
        const points = opts.pop("points");
        const latlng = [];
        for (let i = 0; i < points.length; i++) {
            latlng.push(P.unproject(points[i].x, points[i].z));
        }
        this.polyline = L.polyline(latlng);
        for (const key in opts) {
            this.polyline.options[key] = opts[key];
        }
    }
    draw(layer) {
        this.polyline.remove();
        this.polyline.addTo(layer);
    }
}

class Polygon {
    constructor(opts) {
        const points = opts.pop("points");
        const latlng = [];
        for (let i = 0; i < points.length; i++) {
            latlng.push(P.unproject(points[i].x, points[i].z));
        }
        this.polygon = L.polygon(latlng);
        for (const key in opts) {
            this.polygon.options[key] = opts[key];
        }
    }
    draw(layer) {
        this.polygon.remove();
        this.polygon.addTo(layer);
    }
}

class Circle {
    constructor(opts) {
        const center = opts.pop("center");
        this.circle = L.circle(P.unproject(center.x, center.z), opts.pop("radius"));
        for (const key in opts) {
            this.circle.options[key] = opts[key];
        }
    }
    draw(layer) {
        this.circle.remove();
        this.circle.addTo(layer);
    }
}

export { Options, Rectangle, PolyLine, Polygon, Circle };
