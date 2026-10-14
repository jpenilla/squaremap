/** @type {Record<string, string>} */
const MC_COLORS = {
    '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
    '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
    '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
    'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF',
    'g': '#DDD605', 'h': '#E3D4D1', 'i': '#CECACA', 'j': '#443A3B',
    'p': '#DEB12D', 'q': '#47A036', 's': '#2CBAA8', 't': '#21497B',
    'u': '#9A5CC6', 'v': '#EB7114', 'w': '#8BB3FF'
};

// Matches any Minecraft &X color or format code (case-insensitive).
// Colors: 0-9, a-j, p-w (excluding k, l, m, n, o, r which are format codes).
// Formats: k (obfuscated), l (bold), m (strikethrough), n (underline), o (italic), r (reset).
const MC_CODE_PATTERN = /(&[0-9a-w])/gi;

/**
 * Converts a plain-text segment containing &X codes into an HTML string with inline styles.
 * The input is raw (unescaped) text content from a DOM text node.
 * @param {string} text
 * @returns {string}
 */
function processMinecraftText(text) {
    const tokens = text.split(MC_CODE_PATTERN);
    let result = '';
    let color = null, bold = false, italic = false, underline = false, strikethrough = false;

    for (const token of tokens) {
        const codeMatch = token.match(/^&([0-9a-w])$/i);
        if (codeMatch) {
            const code = codeMatch[1].toLowerCase();
            if (code in MC_COLORS) {
                color = MC_COLORS[code];
            } else if (code === 'l') {
                bold = true;
            } else if (code === 'o') {
                italic = true;
            } else if (code === 'm') {
                strikethrough = true;
            } else if (code === 'n') {
                underline = true;
            } else if (code === 'r') {
                color = null;
                bold = false;
                italic = false;
                underline = false;
                strikethrough = false;
            }
            // &k (obfuscated) — not rendered
        } else if (token.length > 0) {
            const styleParts = [];
            // White is invisible on the popup's white background — render as black instead.
            if (color) styleParts.push(`color:${color === '#FFFFFF' ? '#000000' : color}`);
            if (bold) styleParts.push('font-weight:bold');
            if (italic) styleParts.push('font-style:italic');
            const textDecorations = [];
            if (underline) textDecorations.push('underline');
            if (strikethrough) textDecorations.push('line-through');
            if (textDecorations.length > 0) styleParts.push(`text-decoration:${textDecorations.join(' ')}`);

            const escaped = token.replace(/_/g, ' ').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

            // Keep surrounding parentheses in the default (unstyled) colour.
            const prefixMatch = styleParts.length > 0 ? escaped.match(/^(\(+)/) : null;
            const suffixMatch = styleParts.length > 0 ? escaped.match(/(\)+)$/) : null;
            const prefix = prefixMatch ? prefixMatch[1] : '';
            const suffix = suffixMatch ? suffixMatch[1] : '';
            const inner = escaped.slice(prefix.length, escaped.length - suffix.length);

            result += prefix;
            if (inner.length > 0) {
                result += styleParts.length > 0
                    ? `<span style="${styleParts.join(';')}">${inner}</span>`
                    : inner;
            }
            result += suffix;
        }
    }

    return result;
}

/**
 * Recursively walks DOM nodes, converting Minecraft &X codes in text nodes to styled spans.
 * @param {Node} node
 */
function applyToNode(node) {
    for (const child of Array.from(node.childNodes)) {
        if (child.nodeType === Node.TEXT_NODE) {
            if (MC_CODE_PATTERN.test(child.textContent)) {
                MC_CODE_PATTERN.lastIndex = 0; // reset stateful regex after .test()
                const span = document.createElement('span');
                span.innerHTML = processMinecraftText(child.textContent);
                node.replaceChild(span, child);
            }
        } else if (child.nodeType === Node.ELEMENT_NODE) {
            applyToNode(child);
        }
    }
}

/**
 * Converts Minecraft color/format codes (&X) embedded in an HTML string to inline-styled spans.
 * Only text nodes are affected; HTML tags and attributes are left untouched.
 * @param {string | null | undefined} html
 * @returns {string | null | undefined}
 */
function applyMinecraftColors(html) {
    if (!html || typeof html !== 'string') return html;
    if (!MC_CODE_PATTERN.test(html)) {
        MC_CODE_PATTERN.lastIndex = 0;
        return html; // fast path: no codes present
    }
    MC_CODE_PATTERN.lastIndex = 0;

    const container = document.createElement('div');
    container.innerHTML = html;
    applyToNode(container);
    return container.innerHTML;
}

export { applyMinecraftColors };
