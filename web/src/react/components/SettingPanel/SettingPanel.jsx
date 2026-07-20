import { Card, Typography } from "antd";
import { useEffect, useState } from "react";
import { getSettingsBridge } from "../../bridge/settingsBridge.js";
import { SkinPreviewCard } from "./SkinPreviewCard.jsx";
import "../../styles/skin-previews.css";

const PANEL_ANCHOR_LEFT =
    "calc(var(--weiran-ui-inset-edge) + var(--weiran-ui-control-width) + var(--weiran-ui-gap-control))";

export function SettingPanel() {
    const settingsBridge = getSettingsBridge();
    const [open, setOpen] = useState(settingsBridge.isSettingsPanelOpen());
    const [currentSkin, setCurrentSkin] = useState(settingsBridge.getCurrentSkin());
    const skins = settingsBridge.getAvailableSkins();

    useEffect(() => {
        return settingsBridge.subscribe(() => {
            setOpen(settingsBridge.isSettingsPanelOpen());
            setCurrentSkin(settingsBridge.getCurrentSkin());
        });
    }, [settingsBridge]);

    useEffect(() => {
        if (!open) {
            return undefined;
        }

        const handlePointerDown = (event) => {
            const target = /** @type {HTMLElement} */ (event.target);
            if (target.closest(".map-floating-controls") || target.closest(".settings-panel")) {
                return;
            }
            settingsBridge.collapseSettingsPanel();
        };

        document.addEventListener("pointerdown", handlePointerDown);
        return () => document.removeEventListener("pointerdown", handlePointerDown);
    }, [open, settingsBridge]);

    if (!open) {
        return null;
    }

    return (
        <Card
            className="settings-panel map-side-panel"
            size="small"
            style={{ left: PANEL_ANCHOR_LEFT }}
        >
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
