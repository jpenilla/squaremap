import { useEffect, useState } from "react";

/** 手机端紧凑布局（Pad 及以上保持桌面布局） */
const COMPACT_LAYOUT_QUERY = "(max-width: 639px)";

/**
 * @returns {boolean}
 */
export function useCompactMapLayout() {
    const [compact, setCompact] = useState(() =>
        typeof window !== "undefined" ? window.matchMedia(COMPACT_LAYOUT_QUERY).matches : false,
    );

    useEffect(() => {
        const media = window.matchMedia(COMPACT_LAYOUT_QUERY);
        const sync = () => setCompact(media.matches);
        sync();
        media.addEventListener("change", sync);
        return () => media.removeEventListener("change", sync);
    }, []);

    return compact;
}
