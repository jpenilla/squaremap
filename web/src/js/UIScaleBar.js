import { S } from "./Squaremap.js";
import L from "leaflet";

/** @type {number[]} */
const NICE_STEPS = [1, 2, 5, 10, 20, 25, 50, 100, 200, 400, 500, 1000, 2000, 5000, 10000];

const MAX_RULER_WIDTH_PX = 120;

/**
 * @param {L.Map} map
 * @returns {number | null}
 */
function getPixelsPerBlock(map) {
    if (S.worldList.curWorld == null) {
        return null;
    }

    const origin = map.latLngToContainerPoint(S.toLatLng(0, 0));
    const offset = map.latLngToContainerPoint(S.toLatLng(100, 0));
    const pixelsPerBlock = Math.abs(offset.x - origin.x) / 100;
    if (!Number.isFinite(pixelsPerBlock) || pixelsPerBlock <= 0) {
        return null;
    }
    return pixelsPerBlock;
}

/**
 * Ticks at 0, step, 2×step, 4×step with segment lengths step, step, 2×step.
 * @param {number} pixelsPerBlock
 * @returns {{ step: number, ticks: number[], segmentWidthsPx: number[], totalWidthPx: number }}
 */
function pickScaleTicks(pixelsPerBlock) {
    for (let i = NICE_STEPS.length - 1; i >= 0; i--) {
        const step = NICE_STEPS[i];
        const totalBlocks = step * 4;
        const totalWidthPx = totalBlocks * pixelsPerBlock;
        if (totalWidthPx <= MAX_RULER_WIDTH_PX) {
            const segmentPx = step * pixelsPerBlock;
            return {
                step,
                ticks: [0, step, step * 2, step * 4],
                segmentWidthsPx: [
                    Math.round(segmentPx),
                    Math.round(segmentPx),
                    Math.round(segmentPx * 2),
                ],
                totalWidthPx: Math.round(totalWidthPx),
            };
        }
    }

    const step = NICE_STEPS[0];
    const segmentPx = step * pixelsPerBlock;
    return {
        step,
        ticks: [0, step, step * 2, step * 4],
        segmentWidthsPx: [
            Math.max(1, Math.round(segmentPx)),
            Math.max(1, Math.round(segmentPx)),
            Math.max(1, Math.round(segmentPx * 2)),
        ],
        totalWidthPx: Math.max(1, Math.round(step * 4 * pixelsPerBlock)),
    };
}

/** Tick positions along a 0 → 4×step ruler (fractions 0, ¼, ½, 1). */
const TICK_POSITIONS = [0, 0.25, 0.5, 1];

class UIScaleBar {
    /** @type {L.Control} */
    control;

    constructor() {
        const ScaleBarControl = L.Control.extend({
            /** @type {HTMLElement | null} */
            _ruler: null,
            /** @type {HTMLElement | null} */
            _labels: null,
            options: {
                position: "bottomleft",
            },
            onAdd: function () {
                const container = L.DomUtil.create("div", "leaflet-control scale-bar");
                this._ruler = L.DomUtil.create("div", "scale-bar-ruler", container);
                this._labels = L.DomUtil.create("div", "scale-bar-labels", container);
                return container;
            },
            /**
             * @param {{ ticks: number[], segmentWidthsPx: number[], totalWidthPx: number }} scale
             */
            update: function (scale) {
                if (this._ruler == null || this._labels == null) {
                    return;
                }

                this._ruler.style.width = `${scale.totalWidthPx}px`;
                this._labels.style.width = `${scale.totalWidthPx}px`;
                this._ruler.replaceChildren();
                this._labels.replaceChildren();

                for (const widthPx of scale.segmentWidthsPx) {
                    const segment = L.DomUtil.create("div", "scale-bar-segment", this._ruler);
                    segment.style.width = `${widthPx}px`;
                }

                for (let i = 0; i < scale.ticks.length; i++) {
                    const label = L.DomUtil.create("span", "scale-bar-tick-label", this._labels);
                    label.textContent = String(scale.ticks[i]);
                    label.style.left = `${TICK_POSITIONS[i] * 100}%`;
                }
            },
        });

        this.control = new ScaleBarControl();
        S.map.addControl(this.control);

        S.map.on("zoomend", () => this.update());
        S.map.on("moveend", () => this.update());
        this.update();
    }

    update() {
        const pixelsPerBlock = getPixelsPerBlock(S.map);
        if (pixelsPerBlock == null) {
            return;
        }

        this.control.update(pickScaleTicks(pixelsPerBlock));
    }
}

export { UIScaleBar, getPixelsPerBlock, pickScaleTicks };
