import { useEffect, useState } from "react";
import { getPanelBridge, shouldDismissSidePanels } from "../bridge/panelBridge.js";

/**
 * @param {import("../bridge/panelBridge.js").SidePanelId} panelId
 */
export function useSidePanelOpen(panelId) {
    const panelBridge = getPanelBridge();
    const [open, setOpen] = useState(() => panelBridge.isOpen(panelId));

    useEffect(() => {
        return panelBridge.subscribe(() => {
            setOpen(panelBridge.isOpen(panelId));
        });
    }, [panelBridge, panelId]);

    useEffect(() => {
        if (!open) {
            return undefined;
        }

        const handlePointerDown = (event) => {
            if (!shouldDismissSidePanels(event.target)) {
                return;
            }
            panelBridge.collapseAll();
        };

        document.addEventListener("pointerdown", handlePointerDown);
        return () => document.removeEventListener("pointerdown", handlePointerDown);
    }, [open, panelBridge, panelId]);

    return open;
}
