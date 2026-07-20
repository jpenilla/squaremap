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
import { getSettingsBridge } from "../bridge/settingsBridge.js";
import { getMapBridge } from "../bridge/mapBridge.js";
import { MapToolButton } from "./MapToolButton.jsx";

export function MapFloatingControls() {
    const mapBridge = getMapBridge();
    const layerBridge = getLayerBridge();
    const settingsBridge = getSettingsBridge();
    const [visible, setVisible] = useState(layerBridge.areControlsVisible());
    const [layersOpen, setLayersOpen] = useState(layerBridge.isLayersPanelExpanded());
    const [settingsOpen, setSettingsOpen] = useState(settingsBridge.isSettingsPanelOpen());

    useEffect(() => {
        const unsubscribeLayer = layerBridge.subscribe(() => {
            setVisible(layerBridge.areControlsVisible());
            setLayersOpen(layerBridge.isLayersPanelExpanded());
        });
        const unsubscribeSettings = settingsBridge.subscribe(() => {
            setSettingsOpen(settingsBridge.isSettingsPanelOpen());
        });
        return () => {
            unsubscribeLayer();
            unsubscribeSettings();
        };
    }, [layerBridge, settingsBridge]);

    if (!visible) {
        return null;
    }

    return (
        <div
            className="map-floating-controls"
            style={{ gap: "var(--weiran-ui-gap-control)" }}
            onClick={(event) => event.stopPropagation()}
        >
            <div className="map-zoom-control">
                <Tooltip title="放大" placement="right" mouseEnterDelay={0.4}>
                    <Button
                        className="map-zoom-control-btn map-zoom-control-btn-in"
                        type="text"
                        icon={<PlusOutlined />}
                        aria-label="放大"
                        onClick={() => mapBridge.zoomIn()}
                    />
                </Tooltip>
                <Tooltip title="缩小" placement="right" mouseEnterDelay={0.4}>
                    <Button
                        className="map-zoom-control-btn map-zoom-control-btn-out"
                        type="text"
                        icon={<MinusOutlined />}
                        aria-label="缩小"
                        onClick={() => mapBridge.zoomOut()}
                    />
                </Tooltip>
            </div>

            <MapToolButton
                title="图层"
                icon={<AppstoreOutlined />}
                active={layersOpen}
                ariaExpanded={layersOpen}
                onClick={() => layerBridge.toggleLayersPanel()}
            />

            <MapToolButton title="搜索" icon={<SearchOutlined />} />

            <MapToolButton title="组织树" icon={<ApartmentOutlined />} />

            <MapToolButton
                title="设置"
                icon={<SettingOutlined />}
                active={settingsOpen}
                ariaExpanded={settingsOpen}
                onClick={() => settingsBridge.toggleSettingsPanel()}
            />
        </div>
    );
}
