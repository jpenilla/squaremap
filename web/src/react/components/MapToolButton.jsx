import { Button, Tooltip } from "antd";

/**
 * 地图左上角方形工具按钮（统一尺寸与 Tooltip）。
 *
 * @param {object} props
 * @param {string} props.title Tooltip 与默认可访问名称
 * @param {import("react").ReactNode} props.icon
 * @param {string} [props.className]
 * @param {"default" | "primary" | "text"} [props.type]
 * @param {boolean} [props.active]
 * @param {() => void} [props.onClick]
 * @param {string} [props.ariaLabel]
 * @param {boolean} [props.ariaExpanded]
 */
export function MapToolButton({
    title,
    icon,
    className = "",
    type = "default",
    active = false,
    onClick,
    ariaLabel,
    ariaExpanded,
}) {
    const buttonType = active ? "primary" : type;

    return (
        <Tooltip title={title} placement="right" mouseEnterDelay={0.4}>
            <Button
                className={`map-tool-btn ${className}`.trim()}
                type={buttonType}
                icon={icon}
                aria-label={ariaLabel ?? title}
                aria-expanded={ariaExpanded}
                onClick={onClick}
            />
        </Tooltip>
    );
}
