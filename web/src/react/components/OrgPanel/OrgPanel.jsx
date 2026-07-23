import { DownOutlined, UpOutlined } from "@ant-design/icons";
import { Button, Card, Segmented, Tooltip, Tree, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { getLayerBridge } from "../../bridge/layerBridge.js";
import { getMapBridge } from "../../bridge/mapBridge.js";
import { SIDE_PANEL } from "../../bridge/panelBridge.js";
import { useSidePanelOpen } from "../../hooks/useSidePanelOpen.js";
import { collectExpandableTreeKeys, getOrgTreeData, ORG_SORT } from "../../org/orgCatalog.js";
import { OrgTreeNodeTitle } from "./OrgTreeNodeTitle.jsx";
import "../../styles/org-panel.css";

export function OrgPanel() {
    const open = useSidePanelOpen(SIDE_PANEL.ORG);
    const mapBridge = getMapBridge();
    const layerBridge = getLayerBridge();
    const [sortMode, setSortMode] = useState(/** @type {import('../../org/orgCatalog.js').OrgSortMode} */ (ORG_SORT.CATEGORY));
    const [worldType, setWorldType] = useState(() => mapBridge.getCurrentWorldType());
    const treeData = useMemo(() => getOrgTreeData(sortMode, worldType), [sortMode, worldType]);
    const expandableKeys = useMemo(() => collectExpandableTreeKeys(treeData), [treeData]);
    const [expandedKeys, setExpandedKeys] = useState(() => treeData.map((node) => String(node.key)));

    useEffect(() => {
        return mapBridge.subscribeWorldChange(() => {
            setWorldType(mapBridge.getCurrentWorldType());
        });
    }, [mapBridge]);

    useEffect(() => {
        setExpandedKeys(treeData.map((node) => String(node.key)));
    }, [treeData]);

    if (!open || treeData.length === 0) {
        return null;
    }

    /**
     * @param {import('antd').TreeDataNode[]} _keys
     * @param {{ node: import('../../org/orgCatalog.js').OrgTreeDataNode }} info
     */
    const handleSelect = (_keys, { node }) => {
        const org = node.org;
        if (org == null || !Number.isFinite(org.x) || !Number.isFinite(org.z)) {
            return;
        }

        layerBridge.setLayerVisible(org.layerId, true);
        const zoom =
            org.minZoom != null && Number.isFinite(org.minZoom)
                ? org.minZoom
                : mapBridge.getMap()?.getZoom();
        mapBridge.flyTo(org.x, org.z, zoom);
    };

    return (
        <Card className="org-panel map-side-panel" size="small">
            <div className="org-panel-header">
                <Typography.Text className="map-side-panel-section-title">组织树</Typography.Text>
                <div className="org-panel-tree-actions">
                    <Tooltip title="展开全部" mouseEnterDelay={0.4}>
                        <Button
                            type="text"
                            size="small"
                            className="org-panel-tree-action-btn"
                            icon={<DownOutlined aria-hidden="true" />}
                            aria-label="展开全部"
                            onClick={() => setExpandedKeys(expandableKeys)}
                        />
                    </Tooltip>
                    <Tooltip title="折叠全部" mouseEnterDelay={0.4}>
                        <Button
                            type="text"
                            size="small"
                            className="org-panel-tree-action-btn"
                            icon={<UpOutlined aria-hidden="true" />}
                            aria-label="折叠全部"
                            onClick={() => setExpandedKeys([])}
                        />
                    </Tooltip>
                </div>
            </div>
            <Segmented
                className="org-panel-sort"
                size="small"
                value={sortMode}
                options={[
                    { label: "按类别", value: ORG_SORT.CATEGORY },
                    { label: "按字母", value: ORG_SORT.ALPHA },
                ]}
                onChange={(value) => setSortMode(/** @type {import('../../org/orgCatalog.js').OrgSortMode} */ (value))}
            />
            <Tree
                className="org-panel-tree"
                treeData={treeData}
                expandedKeys={expandedKeys}
                titleRender={(node) => (
                    <OrgTreeNodeTitle node={/** @type {import('../../org/orgCatalog.js').OrgTreeDataNode} */ (node)} />
                )}
                onExpand={(keys) => setExpandedKeys(keys.map(String))}
                onSelect={handleSelect}
                blockNode
            />
        </Card>
    );
}
