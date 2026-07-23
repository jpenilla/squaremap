/** squaremap 世界 type 与 GIS 实例 dimension 共用同一套取值 */

export const GIS_DIMENSION = {
    NORMAL: "normal",
    NETHER: "nether",
    THE_END: "the_end",
};

/** @typedef {typeof GIS_DIMENSION[keyof typeof GIS_DIMENSION]} GisDimension */

export const GIS_DIMENSION_DEFAULT = GIS_DIMENSION.NORMAL;

/** @type {GisDimension[]} */
export const GIS_DIMENSION_VALUES = [
    GIS_DIMENSION.NORMAL,
    GIS_DIMENSION.NETHER,
    GIS_DIMENSION.THE_END,
];

/**
 * @param {unknown} value
 * @returns {GisDimension}
 */
export function normalizeGisDimension(value) {
    const raw = String(value ?? "")
        .trim()
        .toLowerCase();
    if (raw === GIS_DIMENSION.NETHER || raw === "下界") {
        return GIS_DIMENSION.NETHER;
    }
    if (raw === GIS_DIMENSION.THE_END || raw === "end" || raw === "末地") {
        return GIS_DIMENSION.THE_END;
    }
    if (raw === GIS_DIMENSION.NORMAL || raw === "overworld" || raw === "主世界") {
        return GIS_DIMENSION.NORMAL;
    }
    return GIS_DIMENSION_DEFAULT;
}

/**
 * @param {unknown} worldType squaremap World.type
 * @returns {GisDimension}
 */
export function worldTypeToGisDimension(worldType) {
    const raw = String(worldType ?? "").toLowerCase();
    if (raw === GIS_DIMENSION.NETHER) {
        return GIS_DIMENSION.NETHER;
    }
    if (raw === GIS_DIMENSION.THE_END) {
        return GIS_DIMENSION.THE_END;
    }
    return GIS_DIMENSION.NORMAL;
}

/**
 * @param {unknown} instanceDimension
 * @param {unknown} worldType
 */
export function gisDimensionMatchesWorld(instanceDimension, worldType) {
    return (
        normalizeGisDimension(instanceDimension) === worldTypeToGisDimension(worldType)
    );
}

/**
 * @param {{ dimension?: unknown }} instance
 * @returns {GisDimension}
 */
export function getInstanceDimension(instance) {
    return normalizeGisDimension(instance?.dimension);
}
