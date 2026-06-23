/** @type {Record<string, string>} */
const makiIconByName = {};

/** @type {Record<string, string>} */
const makiNameByUrl = {};

/** @type {Record<string, string>} */
const makiSvgRawByName = {};

for (const [path, url] of Object.entries(
    import.meta.glob("../../../node_modules/@mapbox/maki/icons/*.svg", {
        eager: true,
        query: "?url",
        import: "default",
    }),
)) {
    const name = path.replace(/^.*\/([^/]+)\.svg$/, "$1");
    makiIconByName[name] = /** @type {string} */ (url);
    makiNameByUrl[/** @type {string} */ (url)] = name;
}

for (const [path, raw] of Object.entries(
    import.meta.glob("../../../node_modules/@mapbox/maki/icons/*.svg", {
        eager: true,
        query: "?raw",
        import: "default",
    }),
)) {
    const name = path.replace(/^.*\/([^/]+)\.svg$/, "$1");
    makiSvgRawByName[name] = /** @type {string} */ (raw);
}

/**
 * @param {unknown} icon
 * @returns {string | null}
 */
function resolveMakiIconName(icon) {
    if (icon == null || icon === "") {
        return null;
    }
    const value = String(icon);
    if (value.startsWith("maki:")) {
        return value.slice(5);
    }
    if (!value.includes("/") && !value.startsWith("data:")) {
        return value;
    }
    if (makiNameByUrl[value] != null) {
        return makiNameByUrl[value];
    }
    for (const [name, url] of Object.entries(makiIconByName)) {
        if (url === value || value.endsWith(`/${name}.svg`)) {
            return name;
        }
    }
    const matched = value.match(/\/([^/]+)\.svg(?:[?#]|$)/);
    return matched?.[1] ?? null;
}

/**
 * @param {unknown} icon
 * @returns {string | null | undefined}
 */
export function resolveMakiIcon(icon) {
    if (icon == null || icon === "") {
        return undefined;
    }
    const name = resolveMakiIconName(icon);
    if (name == null) {
        return String(icon);
    }
    return makiIconByName[name] ?? String(icon);
}

/**
 * @param {unknown} icon
 * @param {string} color
 * @param {number} size
 * @returns {string | null}
 */
export function buildColoredMakiIconSvg(icon, color, size) {
    const name = resolveMakiIconName(icon);
    if (name == null) {
        return null;
    }
    const raw = makiSvgRawByName[name];
    if (raw == null) {
        return null;
    }

    const safeColor = color.replace(/"/g, "");
    return raw
        .replace(/<\?xml[^?]*\?>\s*/i, "")
        .replace(/\swidth="[^"]*"/, ` width="${size}"`)
        .replace(/\sheight="[^"]*"/, ` height="${size}"`)
        .replace(/<path\b/, `<path fill="${safeColor}"`);
}
