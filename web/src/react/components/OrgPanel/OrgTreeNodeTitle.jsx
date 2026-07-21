import { Tag } from "antd";
import { getOrgLayerLegendHtml } from "../../org/orgLayerLegend.js";

/**
 * @param {{ node: import("../../org/orgCatalog.js").OrgTreeDataNode }} props
 */
export function OrgTreeNodeTitle({ node }) {
    const layerKey = String(node.layerKey ?? "");
    const legendHtml = getOrgLayerLegendHtml(layerKey);
    const showTag = layerKey !== "nation" && node.layerName != null;

    return (
        <span className="org-tree-node-title">
            {legendHtml != null ? (
                <span
                    className="org-tree-node-icon"
                    aria-hidden="true"
                    dangerouslySetInnerHTML={{ __html: legendHtml }}
                />
            ) : null}
            <span className="org-tree-node-name">{node.title}</span>
            {showTag ? (
                <Tag bordered={false} className="org-tree-node-tag">
                    {node.layerName}
                </Tag>
            ) : null}
        </span>
    );
}
