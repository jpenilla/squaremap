import { S } from '../Squaremap.js';

class Pin {
    /** @type {boolean} */
    pinned;
    /** @type {HTMLImageElement} */
    element;

    /**
     * @param {boolean} def
     */
    constructor(def) {
        this.pinned = def;

        this.element = S.createElement("img", "pin", this);

        this.element.onclick = () => this.toggle();

        this.pin(this.pinned);
    }
    toggle() {
        this.pin(!this.pinned);
    }
    pin(pin) {
        this.pinned = pin;
        this.element.className = pin ? "pinned" : "unpinned";
        this.element.src = `images/${this.element.className}.png`;
    }
}

export { Pin };
