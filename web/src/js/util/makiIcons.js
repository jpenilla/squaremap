/** @type {Record<string, string>} */
const makiIconByName = {};

for (const [path, url] of Object.entries(
    import.meta.glob("../../../node_modules/@mapbox/maki/icons/*.svg", {
        eager: true,
        query: "?url",
        import: "default",
    }),
)) {
    const name = path.replace(/^.*\/([^/]+)\.svg$/, "$1");
    makiIconByName[name] = /** @type {string} */ (url);
}

/**
 * @param {unknown} icon
 * @returns {string | null | undefined}
 */
export function resolveMakiIcon(icon) {
    if (icon == null || icon === "") {
        return undefined;
    }
    const value = String(icon);
    const name = value.startsWith("maki:") ? value.slice(5) : value;
    if (name.includes("/") || name.startsWith("data:")) {
        return value;
    }
    return makiIconByName[name] ?? value;
}
