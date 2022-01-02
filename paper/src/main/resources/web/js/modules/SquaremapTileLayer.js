export var SquaremapTileLayer = L.TileLayer.extend({

    // @method createTile(coords: Object, done?: Function): HTMLElement
    // Called only internally, overrides GridLayer's [`createTile()`](#gridlayer-createtile)
    // to return an `<img>` HTML element with the appropriate image URL given `coords`. The `done`
    // callback is called when the tile has been loaded.
    createTile: function (coords, done) {
        var tile = document.createElement('img');

        L.DomEvent.on(tile, 'load', () => {
            //Once image has loaded revoke the object URL as we don't need it anymore
            URL.revokeObjectURL(tile.src);
            this._tileOnLoad(done, tile)
        });
        L.DomEvent.on(tile, 'error', L.Util.bind(this._tileOnError, this, done, tile));

        if (this.options.crossOrigin || this.options.crossOrigin === '') {
            tile.crossOrigin = this.options.crossOrigin === true ? '' : this.options.crossOrigin;
        }

        tile.alt = '';
        tile.setAttribute('role', 'presentation');

        //Retrieve image via a fetch instead of just setting the src
        //This works around the fact that browsers usually don't make a request for an image that was previously loaded,
        //without resorting to changing the URL (which would break caching).
        fetch(this.getTileUrl(coords))
            .then(res => {
                //Call leaflet's error handler if request fails for some reason
                if (!res.ok) {
                    this._tileOnError(this, done, tile);
                    return;
                }

                //Get image data and convert into object URL so it can be used as a src
                //Leaflet's onload listener will take it from here
                res.blob().then(blob => tile.src = URL.createObjectURL(blob));
            }).catch(() => this._tileOnError(this, done, tile));

        return tile;
    }
});
