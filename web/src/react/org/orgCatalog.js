import instanceCatalog from "../../../data/weiran-gis/instances.json";
import layerCatalog from "../../../data/weiran-gis/layers.json";
import { getInstanceDimension, gisDimensionMatchesWorld } from "../../js/util/gisDimension.js";
import { WEIRAN_GIS_ID_PREFIX } from "../../js/util/weiranGis.js";
import { listToTree } from "../utils/listToTree.js";

/** @typedef {'category' | 'alpha'} OrgSortMode */

/** @typedef {import('../../js/util/gisDimension.js').GisDimension} GisDimension */

/** @typedef {{ id: string, parentId: string | null, title: string, layerKey: string, layerName: string, layerId: string, dimension: GisDimension, x: number, z: number, minZoom: number | null }} OrgListNode */

/** @typedef {OrgListNode & { children?: OrgTreeBranch[] }} OrgTreeBranch */

/**
 * @typedef {import('antd').TreeDataNode & {
 *   layerKey?: string,
 *   layerName?: string,
 *   org?: { x: number, z: number, layerId: string, minZoom: number | null },
 * }} OrgTreeDataNode
 */

export const ORG_SORT = {
    /** @type {OrgSortMode} */
    CATEGORY: "category",
    /** @type {OrgSortMode} */
    ALPHA: "alpha",
};

const NATION_ORDER = ["nation-weiran", "nation-ocieania", "nation-plato", "nation-doublezero"];

/** @type {Map<string, number>} */
const layerOrder = new Map(
    Object.entries(layerCatalog.layers ?? {}).map(([key, layer]) => [key, Number(layer.order ?? 0)]),
);

/** @type {Map<string, string>} */
const layerNames = new Map(
    Object.entries(layerCatalog.layers ?? {}).map(([key, layer]) => [key, String(layer.name ?? key)]),
);

/** @type {Map<string, number | null>} */
const layerMinZoom = new Map(
    Object.entries(layerCatalog.layers ?? {}).map(([key, layer]) => [
        key,
        layer.minZoom == null ? null : Number(layer.minZoom),
    ]),
);

/** @type {OrgListNode[] | null} */
let cachedOrgList = null;

/**
 * @returns {OrgListNode[]}
 */
export function getOrgListNodes() {
    if (cachedOrgList != null) {
        return cachedOrgList;
    }

    /** @type {Array<{ layer: string, id: string, parentNode: string | null, point?: { x?: number, z?: number }, text?: string }>} */
    const instances = instanceCatalog.instances ?? [];

    cachedOrgList = instances.map((instance) => {
        const layerKey = String(instance.layer);
        return {
            id: String(instance.id),
            parentId: instance.parentNode == null ? null : String(instance.parentNode),
            title: String(instance.text ?? instance.id),
            layerKey,
            layerName: layerNames.get(layerKey) ?? layerKey,
            layerId: `${WEIRAN_GIS_ID_PREFIX}-${layerKey}`,
            dimension: getInstanceDimension(instance),
            x: Number(instance.point?.x),
            z: Number(instance.point?.z),
            minZoom: layerMinZoom.get(layerKey) ?? null,
        };
    });

    return cachedOrgList;
}

/**
 * 保留当前维度节点及其祖先，以维持组织树结构。
 *
 * @param {OrgListNode[]} nodes
 * @param {string | null | undefined} worldType
 */
function filterOrgListByWorld(nodes, worldType) {
    if (worldType == null) {
        return nodes;
    }

    /** @type {Map<string, OrgListNode>} */
    const byId = new Map(nodes.map((node) => [node.id, node]));
    /** @type {Set<string>} */
    const keepIds = new Set();

    for (const node of nodes) {
        if (!gisDimensionMatchesWorld(node.dimension, worldType)) {
            continue;
        }
        keepIds.add(node.id);
        let parentId = node.parentId;
        while (parentId != null) {
            keepIds.add(parentId);
            const parent = byId.get(parentId);
            parentId = parent?.parentId ?? null;
        }
    }

    return nodes.filter((node) => keepIds.has(node.id));
}

/**
 * @param {OrgTreeBranch} a
 * @param {OrgTreeBranch} b
 */
function compareOrgNodesByCategory(a, b) {
    if (a.layerKey === "nation" && b.layerKey === "nation") {
        return NATION_ORDER.indexOf(a.id) - NATION_ORDER.indexOf(b.id);
    }

    const rankA = a.layerKey === "central-department" ? -1 : (layerOrder.get(a.layerKey) ?? 999);
    const rankB = b.layerKey === "central-department" ? -1 : (layerOrder.get(b.layerKey) ?? 999);
    if (rankA !== rankB) {
        return rankA - rankB;
    }

    return a.title.localeCompare(b.title, "zh-Hans");
}

/**
 * @param {OrgTreeBranch} a
 * @param {OrgTreeBranch} b
 */
function compareOrgNodesByAlpha(a, b) {
    return a.title.localeCompare(b.title, "zh-Hans");
}

/**
 * @param {OrgSortMode} sortMode
 */
function getOrgCompareFn(sortMode) {
    return sortMode === ORG_SORT.ALPHA ? compareOrgNodesByAlpha : compareOrgNodesByCategory;
}

/**
 * @param {OrgTreeBranch} node
 * @returns {OrgTreeDataNode}
 */
function toTreeDataNode(node) {
    const hasChildren = (node.children?.length ?? 0) > 0;

    return {
        key: node.id,
        title: node.title,
        layerKey: node.layerKey,
        layerName: node.layerName,
        isLeaf: !hasChildren,
        children: hasChildren ? node.children?.map(toTreeDataNode) : undefined,
        org: {
            x: node.x,
            z: node.z,
            layerId: node.layerId,
            minZoom: node.minZoom,
        },
    };
}

/**
 * @param {OrgSortMode} [sortMode]
 * @param {string | null | undefined} [worldType]
 * @returns {OrgTreeDataNode[]}
 */
export function getOrgTreeData(sortMode = ORG_SORT.CATEGORY, worldType) {
    const nodes = filterOrgListByWorld(getOrgListNodes(), worldType);
    const tree = listToTree(nodes, { sort: getOrgCompareFn(sortMode) });
    return tree.map(toTreeDataNode);
}

/**
 * @param {import("antd").TreeDataNode[]} nodes
 * @returns {string[]}
 */
export function collectExpandableTreeKeys(nodes) {
    /** @type {string[]} */
    const keys = [];

    for (const node of nodes) {
        if (node.children != null && node.children.length > 0) {
            keys.push(String(node.key));
            keys.push(...collectExpandableTreeKeys(node.children));
        }
    }

    return keys;
}
