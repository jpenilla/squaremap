import { Card, Checkbox, Divider, Typography } from "antd";
import { useEffect, useState } from "react";
import { getLayerBridge } from "../bridge/layerBridge.js";

/**
 * @param {{ html: string | null }} props
 */
function LayerLegendIcon({ html }) {
    if (html == null) {
        return null;
    }
    return <span className="layer-panel-legend" dangerouslySetInnerHTML={{ __html: html }} />;
}

export function LayerPanel() {
    const layerBridge = getLayerBridge();
    const [open, setOpen] = useState(layerBridge.isLayersPanelExpanded());
    const [groups, setGroups] = useState(layerBridge.getGroupedLayers());

    useEffect(() => {
        return layerBridge.subscribe(() => {
            setOpen(layerBridge.isLayersPanelExpanded());
            setGroups(layerBridge.getGroupedLayers());
        });
    }, [layerBridge]);

    useEffect(() => {
        if (!open) {
            return undefined;
        }

        const handlePointerDown = (event) => {
            const target = /** @type {HTMLElement} */ (event.target);
            if (target.closest(".map-floating-controls") || target.closest(".layer-panel") || target.closest(".settings-panel")) {
                return;
            }
            layerBridge.collapseLayersPanel();
        };

        document.addEventListener("pointerdown", handlePointerDown);
        return () => document.removeEventListener("pointerdown", handlePointerDown);
    }, [open, layerBridge]);

    if (!open || groups.length === 0) {
        return null;
    }

    return (
        <Card
            className="layer-panel map-side-panel"
            size="small"
            style={{
                left: "calc(var(--weiran-ui-inset-edge) + var(--weiran-ui-control-width) + var(--weiran-ui-gap-control))",
            }}
        >
            {groups.map((group, groupIndex) => (
                <div key={group.source} className="layer-panel-section">
                    {groupIndex > 0 ? <Divider className="layer-panel-divider" /> : null}
                    <Typography.Text className="map-side-panel-section-title">{group.title}</Typography.Text>
                    <div className="layer-panel-items">
                        {group.layers.map((layer) => (
                            <Checkbox
                                key={layer.id}
                                className="layer-panel-item"
                                checked={layer.checked}
                                onChange={(event) =>
                                    layerBridge.setLayerVisible(layer.id, event.target.checked)
                                }
                            >
                                <span className="layer-panel-label">
                                    <LayerLegendIcon html={layer.legendHtml} />
                                    <span className="layer-panel-label-text">{layer.name}</span>
                                </span>
                            </Checkbox>
                        ))}
                    </div>
                </div>
            ))}
        </Card>
    );
}
