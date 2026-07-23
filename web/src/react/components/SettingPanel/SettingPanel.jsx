import { Card, Divider, Typography } from "antd";
import { useEffect, useState } from "react";
import { SIDE_PANEL } from "../../bridge/panelBridge.js";
import { getSettingsBridge } from "../../bridge/settingsBridge.js";
import { useSidePanelOpen } from "../../hooks/useSidePanelOpen.js";
import { getAppVersion } from "../../version/appVersion.js";
import { SkinPreviewCard } from "./SkinPreviewCard.jsx";
import "../../styles/skin-previews.css";
import "../../styles/settings-panel.css";

export function SettingPanel() {
    const settingsBridge = getSettingsBridge();
    const open = useSidePanelOpen(SIDE_PANEL.SETTINGS);
    const [currentSkin, setCurrentSkin] = useState(settingsBridge.getCurrentSkin());
    const skins = settingsBridge.getAvailableSkins();
    const appVersion = getAppVersion();

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

            <Divider className="settings-panel-divider" />

            <section className="settings-panel-about" aria-label="关于">
                <Typography.Text className="map-side-panel-section-title">关于</Typography.Text>
                <p className="settings-panel-about-version" aria-label={`版本 ${appVersion.label}`}>
                    {appVersion.label}
                </p>
                <Typography.Text type="secondary" className="settings-panel-about-caption">
                    蔚然 GIS 地图前端
                </Typography.Text>
            </section>
        </Card>
    );
}
