import { P } from './Squaremap.js';

class UILink {
    constructor(json, show) {
        const Link = L.Control.extend({
            _container: null,
            options: {
                position: 'bottomleft'
            },
            onAdd: function () {
                const link = L.DomUtil.create('div', 'leaflet-control-layers link');
                this._link = link;
                this._link.innerHTML = `<img src='images/clear.png'/>`;
                this._link.onclick = async () => {
                    const url = P.worldList.curWorld == null ? "" : P.getUrlFromView();
                    window.history.pushState(null, "", url);
                    await navigator.clipboard.writeText(window.location.href);
                };
                if (!show) {
                    this._link.style.display = "none";
                }
                return link;
            },
        });
        this.showLinkButton = show;
        this.link = new Link();
        if (!json.enabled) {
            this.link._link.style.display = "none";
        }
        P.map.addControl(this.link);
    }
}

export { UILink };
