import { useEffect, useState } from "react";
import { getScaleBarBridge } from "../bridge/scaleBarBridge.js";

/** @type {readonly number[]} */
const TICK_POSITIONS = [0, 0.25, 0.5, 1];

export function MapScaleBar() {
    const scaleBarBridge = getScaleBarBridge();
    const [scale, setScale] = useState(scaleBarBridge.getScale());

    useEffect(() => {
        return scaleBarBridge.subscribe(() => {
            setScale(scaleBarBridge.getScale());
        });
    }, [scaleBarBridge]);

    if (scale == null) {
        return null;
    }

    return (
        <div className="map-scale-bar scale-bar" aria-hidden="true">
            <div className="scale-bar-ruler" style={{ width: `${scale.totalWidthPx}px` }}>
                {scale.segmentWidthsPx.map((widthPx, index) => (
                    <div
                        key={`segment-${index}`}
                        className="scale-bar-segment"
                        style={{ width: `${widthPx}px` }}
                    />
                ))}
            </div>
            <div className="scale-bar-labels" style={{ width: `${scale.totalWidthPx}px` }}>
                {scale.ticks.map((tick, index) => (
                    <span
                        key={`tick-${index}`}
                        className="scale-bar-tick-label"
                        style={{ left: `${TICK_POSITIONS[index] * 100}%` }}
                    >
                        {tick}
                    </span>
                ))}
            </div>
        </div>
    );
}
