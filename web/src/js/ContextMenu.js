import { S } from "./Squaremap.js";

class ContextMenu {
    /** @type {HTMLDivElement} */
    menu;
    /** @type {L.LatLng | null} */
    latlng;
    /** @type {L.Point | null} */
    point;

    constructor() {
        this.menu = document.createElement("div");
        this.menu.id = "context-menu";
        S.map.getContainer().appendChild(this.menu);

        this.addItem("Zoom in", () => this.zoom(1));
        this.addItem("Zoom out", () => this.zoom(-1));
        this.addSeparator();
        this.addItem("Copy coordinates", () => this.copyCoordinates());
        this.addItem("Copy coordinates (Y: 150)", () => this.copyCoordinates(150));

        S.map.on("contextmenu", (e) => {
            this.latlng = e.latlng;
            this.point = S.toPoint(e.latlng);
            this.open(e.containerPoint);
        });
        S.map.on("movestart zoomstart click", () => this.close());
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                this.close();
            }
        });
        document.addEventListener("click", (e) => {
            if (!this.menu.contains(e.target)) {
                this.close();
            }
        });
    }

    /**
     * @param {string} label
     * @param {() => void} onClick
     */
    addItem(label, onClick) {
        const item = document.createElement("div");
        item.className = "context-menu-item";
        item.textContent = label;
        item.addEventListener("click", () => {
            onClick();
            this.close();
        });
        this.menu.appendChild(item);
    }

    addSeparator() {
        const separator = document.createElement("div");
        separator.className = "context-menu-separator";
        this.menu.appendChild(separator);
    }

    /**
     * @param {number} delta
     */
    zoom(delta) {
        if (this.latlng == null) {
            return;
        }
        S.map.setZoomAround(this.latlng, S.map.getZoom() + delta);
    }

    /**
     * @param {number} [y]
     */
    async copyCoordinates(y) {
        if (this.point == null) {
            return;
        }
        const x = Math.floor(this.point.x);
        const z = Math.floor(this.point.y);
        const text = y == null ? `${x}, ${z}` : `${x}, ${y}, ${z}`;
        await navigator.clipboard.writeText(text);
    }

    /**
     * @param {L.Point} containerPoint
     */
    open(containerPoint) {
        this.menu.style.display = "block";
        const mapSize = S.map.getSize();
        let x = containerPoint.x;
        let y = containerPoint.y;
        if (x + this.menu.offsetWidth > mapSize.x) {
            x -= this.menu.offsetWidth;
        }
        if (y + this.menu.offsetHeight > mapSize.y) {
            y -= this.menu.offsetHeight;
        }
        this.menu.style.left = `${x}px`;
        this.menu.style.top = `${y}px`;
    }

    close() {
        this.menu.style.display = "none";
    }
}

export { ContextMenu };
