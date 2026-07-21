import {
    ApartmentOutlined,
    AppstoreOutlined,
    MinusOutlined,
    PlusOutlined,
    SearchOutlined,
    SettingOutlined,
} from "@ant-design/icons";
import { Button, Tooltip } from "antd";
import { useEffect, useState } from "react";
import { getLayerBridge } from "../bridge/layerBridge.js";
import { getPanelBridge, SIDE_PANEL } from "../bridge/panelBridge.js";
import { getMapBridge } from "../bridge/mapBridge.js";
import { MapToolButton } from "./MapToolButton.jsx";
import { useCompactMapLayout } from "../hooks/useCompactMapLayout.js";

export function MapFloatingControls() {
    const mapBridge = getMapBridge();
    const layerBridge = getLayerBridge();
    const panelBridge = getPanelBridge();
    const compactLayout = useCompactMapLayout();
    const tooltipPlacement = compactLayout ? "left" : "right";
    const [visible, setVisible] = useState(layerBridge.areControlsVisible());
    const [activePanel, setActivePanel] = useState(panelBridge.getActivePanel());

    useEffect(() => {
        const unsubscribeLayer = layerBridge.subscribe(() => {
            setVisible(layerBridge.areControlsVisible());
        });
        const unsubscribePanel = panelBridge.subscribe(() => {
            setActivePanel(panelBridge.getActivePanel());
        });
        return () => {
            unsubscribeLayer();
            unsubscribePanel();
        };
    }, [layerBridge, panelBridge]);

    if (!visible) {
        return null;
    }

    return (
        <div className="map-floating-controls" onClick={(event) => event.stopPropagation()}>
            <div className="map-zoom-control">
                <Tooltip title="放大" placement={tooltipPlacement} mouseEnterDelay={0.4}>
                    <Button
                        className="map-zoom-control-btn map-zoom-control-btn-in"
                        type="text"
                        icon={<PlusOutlined />}
                        aria-label="放大"
                        onClick={() => mapBridge.zoomIn()}
                    />
                </Tooltip>
                <Tooltip title="缩小" placement={tooltipPlacement} mouseEnterDelay={0.4}>
                    <Button
                        className="map-zoom-control-btn map-zoom-control-btn-out"
                        type="text"
                        icon={<MinusOutlined />}
                        aria-label="缩小"
                        onClick={() => mapBridge.zoomOut()}
                    />
                </Tooltip>
            </div>

            <div className="map-toolbar-controls">
                <MapToolButton
                    title="搜索"
                    icon={<SearchOutlined />}
                    className="map-tool-btn--search"
                    active={activePanel === SIDE_PANEL.SEARCH}
                    ariaExpanded={activePanel === SIDE_PANEL.SEARCH}
                    tooltipPlacement={tooltipPlacement}
                    onClick={() => panelBridge.toggle(SIDE_PANEL.SEARCH)}
                />

                <MapToolButton
                    title="图层"
                    icon={<AppstoreOutlined />}
                    className="map-tool-btn--layers"
                    active={activePanel === SIDE_PANEL.LAYERS}
                    ariaExpanded={activePanel === SIDE_PANEL.LAYERS}
                    tooltipPlacement={tooltipPlacement}
                    onClick={() => panelBridge.toggle(SIDE_PANEL.LAYERS)}
                />

                <MapToolButton
                    title="组织树"
                    icon={<ApartmentOutlined />}
                    className="map-tool-btn--org"
                    active={activePanel === SIDE_PANEL.ORG}
                    ariaExpanded={activePanel === SIDE_PANEL.ORG}
                    tooltipPlacement={tooltipPlacement}
                    onClick={() => panelBridge.toggle(SIDE_PANEL.ORG)}
                />

                <MapToolButton
                    title="设置"
                    icon={<SettingOutlined />}
                    className="map-tool-btn--settings"
                    active={activePanel === SIDE_PANEL.SETTINGS}
                    ariaExpanded={activePanel === SIDE_PANEL.SETTINGS}
                    tooltipPlacement={tooltipPlacement}
                    onClick={() => panelBridge.toggle(SIDE_PANEL.SETTINGS)}
                />
            </div>
        </div>
    );
}
