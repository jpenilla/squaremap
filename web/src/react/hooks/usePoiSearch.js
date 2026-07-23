import { useEffect, useMemo, useState } from "react";
import { getMapBridge } from "../bridge/mapBridge.js";
import { filterPoisByWorld, getSearchablePois, searchPois } from "../search/poiCatalog.js";

/**
 * @param {string} query
 */
export function usePoiSearch(query) {
    const mapBridge = getMapBridge();
    const [catalog] = useState(() => getSearchablePois());
    const [worldType, setWorldType] = useState(() => mapBridge.getCurrentWorldType());

    useEffect(() => {
        return mapBridge.subscribeWorldChange(() => {
            setWorldType(mapBridge.getCurrentWorldType());
        });
    }, [mapBridge]);

    const scopedCatalog = useMemo(
        () => filterPoisByWorld(catalog, worldType),
        [catalog, worldType],
    );

    return useMemo(() => searchPois(query, scopedCatalog), [query, scopedCatalog]);
}
