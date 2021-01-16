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

class Box {
    constructor(opts) {
        const points = opts.pop("points");
        this.rect = L.rectangle([P.unproject(points[0].x, points[0].z), P.unproject(points[1].x, points[1].z)]);
        for (const key in opts) {
            this.rect.options[key] = opts[key];
        }
    }
    draw(layer) {
        this.rect.remove();
        this.rect.addTo(layer);
    }
}

class Line {
    constructor(opts) {
        const points = opts.pop("points");
        const latlng = [];
        for (let i = 0; i < points.length; i++) {
            latlng.push(P.unproject(points[i].x, points[i].z));
        }
        this.line = L.polyline(latlng);
        for (const key in opts) {
            this.line.options[key] = opts[key];
        }
    }
    draw(layer) {
        this.line.remove();
        this.line.addTo(layer);
    }
}

export { Options, Box, Line };
