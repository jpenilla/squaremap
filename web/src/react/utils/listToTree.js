/**
 * @template {Record<string, unknown>} T
 * @typedef {T & { children: TreeNode<T>[] }} TreeNode
 */

/**
 * 将扁平 List（id + parentId）转为嵌套 Tree。
 *
 * @template {Record<string, unknown>} T
 * @param {T[]} list
 * @param {{
 *   idKey?: keyof T & string,
 *   parentKey?: keyof T & string,
 *   rootParentValue?: unknown,
 *   sort?: (a: TreeNode<T>, b: TreeNode<T>) => number,
 * }} [options]
 * @returns {TreeNode<T>[]}
 */
export function listToTree(list, options = {}) {
    const { idKey = "id", parentKey = "parentId", rootParentValue = null, sort } = options;

    /** @type {Map<string, TreeNode<T>>} */
    const nodes = new Map();

    for (const item of list) {
        const id = String(item[idKey]);
        nodes.set(id, { ...item, children: [] });
    }

    /** @type {TreeNode<T>[]} */
    const roots = [];

    for (const item of list) {
        const id = String(item[idKey]);
        const node = nodes.get(id);
        if (node == null) {
            continue;
        }

        const parentId = item[parentKey];
        if (parentId == null || parentId === rootParentValue) {
            roots.push(node);
            continue;
        }

        const parent = nodes.get(String(parentId));
        if (parent != null) {
            parent.children.push(node);
        } else {
            roots.push(node);
        }
    }

    /**
     * @param {TreeNode<T>[]} branch
     */
    const sortBranch = (branch) => {
        if (sort != null) {
            branch.sort(sort);
        }
        for (const node of branch) {
            if (node.children.length > 0) {
                sortBranch(node.children);
            }
        }
    };

    sortBranch(roots);
    return roots;
}
