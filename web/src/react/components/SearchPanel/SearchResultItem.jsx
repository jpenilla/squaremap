/**
 * @param {object} props
 * @param {import("../../search/poiCatalog.js").SearchPoi} props.poi
 * @param {() => void} props.onSelect
 */
export function SearchResultItem({ poi, onSelect }) {
    return (
        <button type="button" className="search-result-item" onClick={onSelect}>
            <span className="search-result-item-name">{poi.name}</span>
            <span className="search-result-item-meta">{poi.layerName}</span>
        </button>
    );
}
