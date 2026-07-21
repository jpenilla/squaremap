import { useMemo, useState } from "react";
import { getSearchablePois, searchPois } from "../search/poiCatalog.js";

/**
 * @param {string} query
 */
export function usePoiSearch(query) {
    const [catalog] = useState(() => getSearchablePois());

    return useMemo(() => searchPois(query, catalog), [query, catalog]);
}
