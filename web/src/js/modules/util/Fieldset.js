import { P } from '../Squaremap.js';

class Fieldset {
    /** @type {HTMLFieldSetElement} */
    element;
    /** @type {HTMLLegendElement} */
    legend;

    constructor(id, title) {
        this.element = P.createElement("fieldset", id);
        this.legend = P.createTextElement("legend", title);
        this.element.appendChild(this.legend);
    }
}

export { Fieldset };
