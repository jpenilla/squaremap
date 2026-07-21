import { useEffect, useRef, useState } from "react";
import { getCoordinatesBridge } from "../bridge/coordinatesBridge.js";
import { useCompactMapLayout } from "../hooks/useCompactMapLayout.js";
import { readSkinPxVar } from "../theme/skinTokens.js";
import { MapScaleBar } from "./MapScaleBar.jsx";

export function MapCoordinates() {
    const coordinatesBridge = getCoordinatesBridge();
    const compactLayout = useCompactMapLayout();
    const rootRef = useRef(/** @type {HTMLDivElement | null} */ (null));
    const [visible, setVisible] = useState(coordinatesBridge.isVisible());
    const [html, setHtml] = useState(coordinatesBridge.getFormattedHtml());

    useEffect(() => {
        return coordinatesBridge.subscribe(() => {
            setVisible(coordinatesBridge.isVisible());
            setHtml(coordinatesBridge.getFormattedHtml());
        });
    }, [coordinatesBridge]);

    useEffect(() => {
        if (compactLayout) {
            const scaleBar = document.querySelector(".leaflet-bottom.leaflet-left .scale-bar");
            if (scaleBar instanceof HTMLElement) {
                scaleBar.style.marginLeft = "";
            }
            return undefined;
        }

        const updateScaleBarOffset = () => {
            const scaleBar = document.querySelector(".leaflet-bottom.leaflet-left .scale-bar");
            if (scaleBar == null) {
                return;
            }
            if (!visible) {
                scaleBar.style.marginLeft = "";
                return;
            }
            const width = rootRef.current?.offsetWidth ?? 0;
            const gap = readSkinPxVar("--weiran-ui-gap-control", 8);
            scaleBar.style.marginLeft = width > 0 ? `${width + gap}px` : "";
        };

        updateScaleBarOffset();

        const root = rootRef.current;
        const resizeObserver = root != null ? new ResizeObserver(updateScaleBarOffset) : null;
        if (root != null) {
            resizeObserver?.observe(root);
        }

        const retryId = window.setInterval(() => {
            updateScaleBarOffset();
            if (document.querySelector(".leaflet-bottom.leaflet-left .scale-bar") != null) {
                window.clearInterval(retryId);
            }
        }, 200);

        window.addEventListener("resize", updateScaleBarOffset);

        return () => {
            window.clearInterval(retryId);
            resizeObserver?.disconnect();
            window.removeEventListener("resize", updateScaleBarOffset);
            const scaleBar = document.querySelector(".leaflet-bottom.leaflet-left .scale-bar");
            if (scaleBar instanceof HTMLElement) {
                scaleBar.style.marginLeft = "";
            }
        };
    }, [visible, html, compactLayout]);

    if (compactLayout) {
        if (!visible) {
            return (
                <div className="map-bottom-bar map-bottom-bar--compact">
                    <MapScaleBar />
                </div>
            );
        }

        return (
            <div className="map-bottom-bar map-bottom-bar--compact">
                <div className="map-coordinates">
                    <div className="map-coordinates-content" dangerouslySetInnerHTML={{ __html: html }} />
                </div>
                <MapScaleBar />
            </div>
        );
    }

    if (!visible) {
        return null;
    }

    return (
        <div ref={rootRef} className="map-coordinates">
            <div className="map-coordinates-content" dangerouslySetInnerHTML={{ __html: html }} />
        </div>
    );
}
