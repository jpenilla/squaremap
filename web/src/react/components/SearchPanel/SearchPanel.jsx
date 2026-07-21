import { SearchOutlined } from "@ant-design/icons";
import { Input } from "antd";
import { useEffect, useRef, useState } from "react";
import { getLayerBridge } from "../../bridge/layerBridge.js";
import { getMapBridge } from "../../bridge/mapBridge.js";
import { SIDE_PANEL } from "../../bridge/panelBridge.js";
import { usePoiSearch } from "../../hooks/usePoiSearch.js";
import { useSidePanelOpen } from "../../hooks/useSidePanelOpen.js";
import { SearchResultItem } from "./SearchResultItem.jsx";
import "../../styles/search-panel.css";

export function SearchPanel() {
    const open = useSidePanelOpen(SIDE_PANEL.SEARCH);
    const mapBridge = getMapBridge();
    const layerBridge = getLayerBridge();
    const inputRef = useRef(/** @type {import("antd/es/input").InputRef | null} */ (null));
    const [query, setQuery] = useState("");
    const results = usePoiSearch(query);
    const hasQuery = query.trim() !== "";

    useEffect(() => {
        if (!open) {
            setQuery("");
            return;
        }
        const frameId = window.requestAnimationFrame(() => {
            inputRef.current?.focus();
        });
        return () => window.cancelAnimationFrame(frameId);
    }, [open]);

    if (!open) {
        return null;
    }

    /**
     * @param {import("../../search/poiCatalog.js").SearchPoi} poi
     */
    const handleSelect = (poi) => {
        layerBridge.setLayerVisible(poi.layerId, true);
        const zoom =
            poi.minZoom != null && Number.isFinite(poi.minZoom)
                ? poi.minZoom
                : mapBridge.getMap()?.getZoom();
        mapBridge.flyTo(poi.x, poi.z, zoom);
    };

    return (
        <div className="search-panel" role="dialog" aria-label="搜索">
            <div className={`search-panel-shell${hasQuery ? " has-results" : ""}`}>
                <div className="search-panel-input-wrap">
                    <Input
                        ref={inputRef}
                        className="search-panel-input"
                        variant="borderless"
                        size="large"
                        value={query}
                        placeholder="搜索地点、单位…"
                        prefix={<SearchOutlined aria-hidden="true" />}
                        allowClear
                        aria-label="搜索关键词"
                        onChange={(event) => setQuery(event.target.value)}
                    />
                </div>

                {hasQuery ? (
                    <div className="search-panel-results" role="listbox" aria-label="搜索结果">
                        {results.length === 0 ? (
                            <p className="search-panel-empty">未找到匹配结果</p>
                        ) : (
                            results.map((poi) => (
                                <SearchResultItem
                                    key={poi.id}
                                    poi={poi}
                                    onSelect={() => handleSelect(poi)}
                                />
                            ))
                        )}
                    </div>
                ) : null}
            </div>
        </div>
    );
}
