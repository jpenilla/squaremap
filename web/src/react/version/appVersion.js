import buildCatalog from "../../../data/weiran-gis/build.json";
import instanceCatalog from "../../../data/weiran-gis/instances.json";
import releaseCatalog from "../../../data/weiran-gis/release.json";

/**
 * @typedef {{ major: number, minor: number, edit: number, label: string, builtAt: string | null }} AppVersion
 */

/**
 * 蔚然GIS 前端版本：大版本.小版本.编辑版本
 * @returns {AppVersion}
 */
export function getAppVersion() {
    const major = Number(releaseCatalog.major);
    const minor = Number(releaseCatalog.minor);
    const edit = Number(instanceCatalog.version);
    const builtAt = typeof buildCatalog.builtAt === "string" ? buildCatalog.builtAt : null;

    return {
        major,
        minor,
        edit,
        label: `${major}.${minor}.${edit}`,
        builtAt,
    };
}
