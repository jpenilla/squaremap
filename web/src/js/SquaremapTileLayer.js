import L from "leaflet";

//Cap on how many tile versions are remembered for tiles that aren't currently displayed
const MAX_TRACKED_VERSIONS = 4096;

/**
 * @param coords {L.Coords}
 * @returns {string}
 */
function tileKey(coords) {
    return `${coords.z}/${coords.x}_${coords.y}`;
}

export const SquaremapTileLayer = L.TileLayer.extend({
    initialize: function (url, options) {
        L.TileLayer.prototype.initialize.call(this, url, options);
        //Version published by the server for each tile, keyed as it appears in the update manifest
        this._tileVersions = new Map();
    },

    // @method getTileUrl(coords: Object): String
    // Overrides TileLayer's [`getTileUrl()`](#tilelayer-gettileurl) to append the version the server
    // last published for this tile. Tiles carrying a version are served with a long cache lifetime,
    // so the changing URL is what makes the browser fetch a tile that has been rewritten.
    getTileUrl: function (coords) {
        const url = L.TileLayer.prototype.getTileUrl.call(this, coords);
        const version = this._tileVersions.get(tileKey(coords));
        return version === undefined ? url : `${url}?v=${version}`;
    },

    // @method updateTiles(versions: Map): void
    // Records the given tile versions and reloads any of those tiles that are currently displayed.
    // Tiles that aren't displayed are picked up by `getTileUrl` whenever they are next created.
    updateTiles: function (versions) {
        for (const [key, version] of versions) {
            //Delete first so that the map stays ordered from least to most recently updated
            this._tileVersions.delete(key);
            this._tileVersions.set(key, version);
        }
        this._pruneTileVersions();
        for (const key in this._tiles) {
            const tile = this._tiles[key];
            if (versions.has(tileKey(tile.coords))) {
                this._reloadTile(tile);
            }
        }
    },

    // @method reloadDisplayedTiles(version: Number): void
    // Reloads every displayed tile at the given version. Used when the server dropped updates before
    // this client read them, leaving no way to tell which tiles changed.
    reloadDisplayedTiles: function (version) {
        const versions = new Map();
        for (const key in this._tiles) {
            versions.set(tileKey(this._tiles[key].coords), version);
        }
        this.updateTiles(versions);
    },

    /**
     * @param tile {{el: HTMLImageElement, coords: L.Coords, loaded?: number}}
     */
    _reloadTile: function (tile) {
        const url = this.getTileUrl(tile.coords);
        const key = this._tileCoordsToKey(tile.coords);

        const swap = () => {
            //The tile may have been pruned or replaced while the new image was loading
            if (this._tiles[key] !== tile) {
                return;
            }
            if (tile.loaded) {
                //GridLayer fades a tile in from fully transparent every time its load event fires, so
                //drop its listener before swapping. A tile that has loaded no longer needs to report in.
                L.DomEvent.off(tile.el, "load");
            }
            tile.el.src = url;
        };

        //Fully decode the replacement before swapping it in, otherwise the tile blanks out while it loads
        const next = document.createElement("img");
        if (this.options.crossOrigin || this.options.crossOrigin === "") {
            next.crossOrigin = this.options.crossOrigin === true ? "" : this.options.crossOrigin;
        }
        next.src = url;
        if (typeof next.decode === "function") {
            //Leave the existing image in place if the replacement fails to load
            next.decode().then(swap, () => {});
        } else {
            L.DomEvent.on(next, "load", swap);
        }
    },

    _pruneTileVersions: function () {
        if (this._tileVersions.size <= MAX_TRACKED_VERSIONS) {
            return;
        }
        const displayed = new Set();
        for (const key in this._tiles) {
            displayed.add(tileKey(this._tiles[key].coords));
        }
        for (const key of this._tileVersions.keys()) {
            if (this._tileVersions.size <= MAX_TRACKED_VERSIONS) {
                break;
            }
            if (!displayed.has(key)) {
                this._tileVersions.delete(key);
            }
        }
    },
});
