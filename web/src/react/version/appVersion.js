import instanceCatalog from "../../../data/weiran-gis/instances.json";
import releaseCatalog from "../../../data/weiran-gis/release.json";

/**
 * @typedef {{ major: number, minor: number, edit: number, label: string }} AppVersion
 */

/**
 * 蔚然地图前端版本：大版本.小版本.编辑版本
 * @returns {AppVersion}
 */
export function getAppVersion() {
    const major = Number(releaseCatalog.major);
    const minor = Number(releaseCatalog.minor);
    const edit = Number(instanceCatalog.version);

    return {
        major,
        minor,
        edit,
        label: `${major}.${minor}.${edit}`,
    };
}
