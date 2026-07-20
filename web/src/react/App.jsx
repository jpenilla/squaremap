import { ConfigProvider } from "antd";
import { LayerPanel } from "./components/LayerPanel.jsx";
import { MapCoordinates } from "./components/MapCoordinates.jsx";
import { MapFloatingControls } from "./components/MapFloatingControls.jsx";
import { SettingPanel } from "./components/SettingPanel/SettingPanel.jsx";
import { useAntdSkinTheme } from "./hooks/useAntdSkinTheme.js";

export function App() {
    const antdTheme = useAntdSkinTheme();

    return (
        <ConfigProvider theme={antdTheme}>
            <MapFloatingControls />
            <LayerPanel />
            <SettingPanel />
            <MapCoordinates />
        </ConfigProvider>
    );
}
