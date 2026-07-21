import { ConfigProvider } from "antd";
import { LayerPanel } from "./components/LayerPanel/LayerPanel.jsx";
import { MapCoordinates } from "./components/MapCoordinates.jsx";
import { MapFloatingControls } from "./components/MapFloatingControls.jsx";
import { OrgPanel } from "./components/OrgPanel/OrgPanel.jsx";
import { SearchPanel } from "./components/SearchPanel/SearchPanel.jsx";
import { SettingPanel } from "./components/SettingPanel/SettingPanel.jsx";
import { useAntdSkinTheme } from "./hooks/useAntdSkinTheme.js";

export function App() {
    const antdTheme = useAntdSkinTheme();

    return (
        <ConfigProvider theme={antdTheme}>
            <MapFloatingControls />
            <SearchPanel />
            <LayerPanel />
            <OrgPanel />
            <SettingPanel />
            <MapCoordinates />
        </ConfigProvider>
    );
}
