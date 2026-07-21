import { Card, Typography } from "antd";
import { SIDE_PANEL } from "../../bridge/panelBridge.js";
import { useSidePanelOpen } from "../../hooks/useSidePanelOpen.js";

export function OrgPanel() {
    const open = useSidePanelOpen(SIDE_PANEL.ORG);

    if (!open) {
        return null;
    }

    return (
        <Card className="org-panel map-side-panel" size="small">
            <Typography.Text className="map-side-panel-section-title">组织树</Typography.Text>
            <Typography.Text type="secondary">即将推出</Typography.Text>
        </Card>
    );
}
