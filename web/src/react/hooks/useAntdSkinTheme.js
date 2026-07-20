import { useEffect, useState } from "react";
import { buildAntdSkinTheme } from "../theme/skinTokens.js";

/**
 * 从 #react-root 上的 CSS 皮肤变量同步 Ant Design ConfigProvider theme。
 */
export function useAntdSkinTheme() {
    const [antdTheme, setAntdTheme] = useState(() => buildAntdSkinTheme());

    useEffect(() => {
        const root = document.getElementById("react-root");
        if (root == null) {
            return undefined;
        }

        const sync = () => setAntdTheme(buildAntdSkinTheme(root));

        sync();

        const observer = new MutationObserver((mutations) => {
            for (const mutation of mutations) {
                if (mutation.type === "attributes" && mutation.attributeName === "data-ui-skin") {
                    sync();
                    return;
                }
            }
        });
        observer.observe(root, { attributes: true, attributeFilter: ["data-ui-skin"] });

        return () => observer.disconnect();
    }, []);

    return antdTheme;
}
