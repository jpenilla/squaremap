import { P } from '../Pl3xMap.js';

class Fieldset {
    constructor(id, title) {
        this.element = P.createElement("fieldset", id);
        this.legend = P.createTextElement("legend", title);
        this.element.appendChild(this.legend);
    }
}

export { Fieldset };
