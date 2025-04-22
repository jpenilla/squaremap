import { S } from "../Squaremap.js";

class Fieldset {
    /** @type {HTMLFieldSetElement} */
    element;
    /** @type {HTMLLegendElement} */
    legend;

    constructor(id, title) {
        this.element = S.createElement("fieldset", id);
        this.legend = S.createTextElement("legend", title);
        this.element.appendChild(this.legend);
    }
}

export { Fieldset };
