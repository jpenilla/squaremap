import { S } from "../Squaremap.js";
import L from "leaflet";
import { buildColoredMakiIconSvg, resolveMakiIcon } from "./makiIcons.js";

class Marker {
    /** @type {L.Marker} */
    marker;
    opts;
    id;
    popup;
    /** @type {boolean} */
    popup_sticky;
    tooltip;
    /** @type {boolean} */
    tooltip_sticky;

    constructor(opts) {
        this.opts = opts;
        this.id = this.opts.pop("id");
        this.popup = this.opts.pop("popup");
        this.popup_sticky = true;
        this.tooltip = this.opts.pop("tooltip");
        this.tooltip_sticky = true;
    }
    init() {
        if (this.popup != null) {
            if (this.popup_sticky) {
                this.marker.on("click", () => {
                    L.popup({
                        direction: this.opts.pop("tooltip_direction", "top"),
                    })
                        .setLatLng(S.toLatLng(S.coordinates.coords.x, S.coordinates.coords.z))
                        .setContent(this.popup)
                        .openOn(S.map);
                });
            } else {
                this.marker.bindPopup(() => this.popup, {
                    direction: this.opts.pop("tooltip_direction", "top"),
                });
            }
        }
        if (this.tooltip != null) {
            this.marker.bindTooltip(() => this.tooltip, {
                direction: this.opts.pop("tooltip_direction", "top"),
                sticky: this.tooltip_sticky,
            });
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
        this.marker = L.rectangle([S.toLatLng(points[0].x, points[0].z), S.toLatLng(points[1].x, points[1].z)]);
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
                    inner.push(S.toLatLng(points[i][j].x, points[i][j].z));
                }
                outer.push(inner);
            } else {
                outer.push(S.toLatLng(points[i].x, points[i].z));
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
                            inner2.push(S.toLatLng(points[i][j][k].x, points[i][j][k].z));
                        }
                        inner.push(inner2);
                    } else {
                        inner.push(S.toLatLng(points[i][j].x, points[i][j].z));
                    }
                }
                outer.push(inner);
            } else {
                outer.push(S.toLatLng(points[i].x, points[i].z));
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
        this.marker = L.circle(S.toLatLng(center.x, center.z), {
            radius: S.pixelsToMeters(radius),
        });
        super.init();
    }
}

class Ellipse extends Marker {
    constructor(opts) {
        super(opts);
        const center = this.opts.pop("center");
        const radiusX = this.opts.pop("radiusX");
        const radiusZ = this.opts.pop("radiusZ");
        const tilt = 0;
        this.marker = L.ellipse(S.toLatLng(center.x, center.z), [radiusX, radiusZ], tilt);
        super.init();
    }
}

class Icon extends Marker {
    constructor(opts) {
        super(opts);
        const point = this.opts.pop("point");
        const size = this.opts.pop("size");
        const anchor = this.opts.pop("anchor");
        const tooltipAnchor = this.opts.pop("tooltip_anchor", L.point(0, -size.z / 2));
        this.marker = L.marker(S.toLatLng(point.x, point.z), {
            icon: L.icon({
                iconUrl: `images/icon/registered/${opts.pop("icon")}.png`,
                iconSize: [size.x, size.z],
                iconAnchor: [anchor.x, anchor.z],
                popupAnchor: [tooltipAnchor.x, tooltipAnchor.z],
                tooltipAnchor: [tooltipAnchor.x, tooltipAnchor.z],
            }),
        });
        this.popup_sticky = false;
        this.tooltip_sticky = false;
        super.init();
    }
}

class IconWithText {
    /** @type {L.Marker} */
    marker;

    /**
     * @param {Options} opts
     */
    constructor(opts) {
        const point = opts.pop("point");
        const text = opts.pop("text");
        const style = opts.pop("style", {});

        const iconBackgroundSize = Number(style.iconBackgroundSize ?? 28);
        const html = buildIconWithTextHtml(text, style);

        this.marker = L.marker(S.toLatLng(point.x, point.z), {
            icon: L.divIcon({
                className: "weiran-icon-with-text-icon",
                html,
                iconAnchor: [iconBackgroundSize / 2, iconBackgroundSize / 2],
            }),
            interactive: false,
        });
    }
    addTo(layer) {
        this.marker.remove();
        this.marker.addTo(layer);
    }
}

class RegionLabel {
    /** @type {L.Marker} */
    marker;

    /**
     * @param {Options} opts
     */
    constructor(opts) {
        const point = opts.pop("point");
        const text = opts.pop("text");
        const style = opts.pop("style", {});

        const html = buildRegionLabelHtml(text, style);
        this.marker = L.marker(S.toLatLng(point.x, point.z), {
            icon: L.divIcon({
                className: "weiran-region-label-icon",
                html,
            }),
            interactive: false,
        });
    }
    addTo(layer) {
        this.marker.remove();
        this.marker.addTo(layer);
    }
}

/**
 * @param {string} text
 * @param {Record<string, unknown>} style
 */
function buildIconWithTextHtml(text, style) {
    const icon = style.icon;
    const iconSize = Number(style.iconSize ?? 12);
    const iconBackgroundSize = Number(style.iconBackgroundSize ?? 28);
    const gap = Number(style.gap ?? 6);
    const badgeStyle = buildIconWithTextBadgeStyle(style, iconBackgroundSize);
    const labelStyle = buildIconWithTextLabelStyle(style);
    const iconHtml =
        icon != null && icon !== ""
            ? buildColoredMakiIconHtml(icon, style, iconSize, 0, "weiran-icon-with-text-img")
            : "";
    return `<div class="weiran-icon-with-text"><div class="weiran-icon-with-text-badge" style="${badgeStyle}">${iconHtml}</div><span class="weiran-icon-with-text-label" style="${labelStyle};margin-left:${gap}px">${escapeHtml(String(text))}</span></div>`;
}

/**
 * @param {Record<string, unknown>} style
 * @param {number} iconBackgroundSize
 */
function buildIconWithTextBadgeStyle(style, iconBackgroundSize) {
    /** @type {string[]} */
    const parts = [
        `width:${iconBackgroundSize}px`,
        `height:${iconBackgroundSize}px`,
        "border-radius:50%",
        "display:flex",
        "align-items:center",
        "justify-content:center",
        "flex-shrink:0",
        "box-shadow:0 1px 4px rgba(0,0,0,0.35)",
    ];
    const bg = style.iconBackgroundColor ?? "#ffffff";
    parts.push(`background:${colorWithOpacity(String(bg), Number(style.iconBackgroundOpacity ?? 1))}`);
    return parts.join(";");
}

/**
 * @param {Record<string, unknown>} style
 */
function buildIconWithTextLabelStyle(style) {
    /** @type {string[]} */
    const parts = ["white-space:nowrap", "line-height:1.25"];
    if (style.fontSize != null) {
        parts.push(`font-size:${Number(style.fontSize)}px`);
    }
    if (style.fontWeight != null) {
        parts.push(`font-weight:${style.fontWeight}`);
    }
    if (style.color != null) {
        parts.push(`color:${colorWithOpacity(String(style.color), Number(style.colorOpacity ?? 1))}`);
    }
    const strokeColor = style.textStrokeColor ?? "#ffffff";
    const strokeWidth = Number(style.textStrokeWidth ?? 2);
    parts.push(`-webkit-text-stroke:${strokeWidth}px ${strokeColor}`);
    parts.push("paint-order:stroke fill");
    return parts.join(";");
}

/**
 * @param {string} text
 * @param {Record<string, unknown>} style
 */
function buildRegionLabelHtml(text, style) {
    const inlineStyle = buildRegionLabelStyle(style);
    const icon = style.icon;
    const iconSize = Number(style.iconSize ?? 16);
    const gap = Number(style.gap ?? 6);
    const iconHtml =
        icon != null && icon !== ""
            ? buildColoredMakiIconHtml(icon, style, iconSize, gap, "weiran-region-label-img")
            : "";
    return `<div class="weiran-region-label" style="${inlineStyle}">${iconHtml}<span>${escapeHtml(String(text))}</span></div>`;
}

/**
 * @param {Record<string, unknown>} style
 */
function buildRegionLabelStyle(style) {
    /** @type {string[]} */
    const parts = [
        "display:inline-flex",
        "align-items:center",
        "transform:translate(-50%,-50%)",
        "white-space:nowrap",
        "pointer-events:none",
        "line-height:1.25",
    ];

    if (style.fontSize != null) {
        parts.push(`font-size:${Number(style.fontSize)}px`);
    }
    if (style.fontWeight != null) {
        parts.push(`font-weight:${style.fontWeight}`);
    }
    if (style.color != null) {
        parts.push(`color:${colorWithOpacity(String(style.color), Number(style.colorOpacity ?? 1))}`);
    }
    if (style.backgroundColor != null) {
        parts.push(
            `background:${colorWithOpacity(String(style.backgroundColor), Number(style.backgroundOpacity ?? 0.55))}`,
        );
    }
    if (style.borderRadius != null) {
        parts.push(`border-radius:${Number(style.borderRadius)}px`);
    }
    if (style.paddingX != null || style.paddingY != null) {
        parts.push(`padding:${Number(style.paddingY ?? 4)}px ${Number(style.paddingX ?? 10)}px`);
    }
    if (style.boxShadow != null && style.boxShadow !== "") {
        parts.push(`box-shadow:${style.boxShadow}`);
    }

    return parts.join(";");
}

/**
 * @param {string | unknown} icon
 * @param {Record<string, unknown>} style
 * @param {number} iconSize
 * @param {number} gap
 * @param {string} imgClass
 */
function buildColoredMakiIconHtml(icon, style, iconSize, gap, imgClass) {
    const iconUrl = escapeAttr(String(resolveMakiIcon(icon) ?? icon));
    const sizeStyle = `width:${iconSize}px;height:${iconSize}px`;
    const gapStyle = gap > 0 ? `margin-right:${gap}px` : "";

    if (style.iconColor != null) {
        const color = colorWithOpacity(String(style.iconColor), Number(style.iconColorOpacity ?? 1));
        const inlineSvg = buildColoredMakiIconSvg(icon, color, iconSize);
        if (inlineSvg != null) {
            return `<span class="weiran-maki-icon ${imgClass}" style="${sizeStyle};${gapStyle};display:block;line-height:0">${inlineSvg}</span>`;
        }
    }

    return `<img class="${imgClass}" src="${iconUrl}" alt="" style="${sizeStyle};${gapStyle}">`;
}

/**
 * @param {string} color
 * @param {number} opacity
 */
function colorWithOpacity(color, opacity) {
    const alpha = Math.min(1, Math.max(0, opacity));
    if (color.startsWith("rgba(") || color.startsWith("rgb(")) {
        return color;
    }
    const hex = color.replace("#", "");
    if (hex.length === 3) {
        const r = parseInt(hex[0] + hex[0], 16);
        const g = parseInt(hex[1] + hex[1], 16);
        const b = parseInt(hex[2] + hex[2], 16);
        return `rgba(${r},${g},${b},${alpha})`;
    }
    if (hex.length === 6) {
        const r = parseInt(hex.slice(0, 2), 16);
        const g = parseInt(hex.slice(2, 4), 16);
        const b = parseInt(hex.slice(4, 6), 16);
        return `rgba(${r},${g},${b},${alpha})`;
    }
    return color;
}

/**
 * @param {string} text
 */
function escapeAttr(text) {
    return escapeHtml(text).replace(/'/g, "&#39;");
}

/**
 * @param {string} text
 */
function escapeHtml(text) {
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

export { Marker, Options, Rectangle, PolyLine, Polygon, Circle, Ellipse, Icon, RegionLabel, IconWithText };
