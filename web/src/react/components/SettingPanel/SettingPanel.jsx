import { Card, Typography } from "antd";
import { useEffect, useState } from "react";
import { getSettingsBridge } from "../../bridge/settingsBridge.js";
import { SIDE_PANEL } from "../../bridge/panelBridge.js";
import { useSidePanelOpen } from "../../hooks/useSidePanelOpen.js";
import { SkinPreviewCard } from "./SkinPreviewCard.jsx";
import "../../styles/skin-previews.css";

export function SettingPanel() {
    const settingsBridge = getSettingsBridge();
    const open = useSidePanelOpen(SIDE_PANEL.SETTINGS);
    const [currentSkin, setCurrentSkin] = useState(settingsBridge.getCurrentSkin());
    const skins = settingsBridge.getAvailableSkins();

    useEffect(() => {
        return settingsBridge.subscribe(() => {
            setCurrentSkin(settingsBridge.getCurrentSkin());
        });
    }, [settingsBridge]);

    if (!open) {
        return null;
    }

    return (
        <Card className="settings-panel map-side-panel" size="small">
            <Typography.Text className="map-side-panel-section-title">主题</Typography.Text>
            <div className="settings-panel-skin-list" role="radiogroup" aria-label="主题">
                {skins.map((skin) => (
                    <SkinPreviewCard
                        key={skin.id}
                        skin={skin}
                        selected={currentSkin === skin.id}
                        onSelect={() => settingsBridge.setSkin(skin.id)}
                    />
                ))}
            </div>
        </Card>
    );
}
