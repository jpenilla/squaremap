import { SKIN_PREVIEW_TOKENS } from "../../theme/skinPreviews.js";

/**
 * @param {object} props
 * @param {{ id: string, label: string }} props.skin
 * @param {boolean} props.selected
 * @param {() => void} props.onSelect
 */
export function SkinPreviewCard({ skin, selected, onSelect }) {
    const tokens = SKIN_PREVIEW_TOKENS[skin.id];
    if (tokens == null) {
        return null;
    }

    return (
        <button
            type="button"
            className={`skin-preview-card${selected ? " is-selected" : ""}`}
            data-preview={skin.id}
            aria-pressed={selected}
            aria-label={`主题：${skin.label}`}
            onClick={onSelect}
            style={{
                "--skin-preview-bg": tokens.bg,
                "--skin-preview-border": tokens.border,
                "--skin-preview-text": tokens.text,
                "--skin-preview-text-muted": tokens.textMuted,
                "--skin-preview-primary": tokens.primary,
                "--skin-preview-shadow": tokens.shadow,
            }}
        >
            <span className="skin-preview-mock" aria-hidden="true">
                <span className="skin-preview-mock-toolbar">
                    <span className="skin-preview-mock-btn" />
                    <span className="skin-preview-mock-btn skin-preview-mock-btn-primary" />
                </span>
                <span className="skin-preview-mock-panel">
                    <span className="skin-preview-mock-line skin-preview-mock-line-short" />
                    <span className="skin-preview-mock-line" />
                </span>
            </span>
            <span className="skin-preview-label">{skin.label}</span>
        </button>
    );
}
