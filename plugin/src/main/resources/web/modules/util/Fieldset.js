class Fieldset {
    constructor(id, title, P) {
        this.element = P.createElement("fieldset", id);
        const legend = P.createTextElement("legend", title);
        this.element.appendChild(legend);
    }
}

export { Fieldset };
