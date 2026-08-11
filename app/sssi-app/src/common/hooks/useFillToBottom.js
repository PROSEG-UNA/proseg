import { useEffect, useRef } from 'react';

export const TOP_GAP = 24;
export const BOTTOM_GAP = 20;

const MIN_HEIGHT = 240;

export function useFillToBottom(enabled = true) {
    const slotRef = useRef(null);
    const paneRef = useRef(null);

    useEffect(() => {
        const slot = slotRef.current;
        const pane = paneRef.current;
        if (!enabled || !slot || !pane) {
            if (pane) pane.style.height = '';
            return undefined;
        }

        const update = () => {
            const top = Math.max(slot.getBoundingClientRect().top, TOP_GAP);
            const height = Math.max(window.innerHeight - top - BOTTOM_GAP, MIN_HEIGHT);
            pane.style.height = `${height}px`;
        };

        update();

        window.addEventListener('scroll', update, { passive: true });
        window.addEventListener('resize', update, { passive: true });

        const observer = new ResizeObserver(update);
        observer.observe(slot);
        if (slot.parentElement) observer.observe(slot.parentElement);

        return () => {
            window.removeEventListener('scroll', update);
            window.removeEventListener('resize', update);
            observer.disconnect();
        };
    }, [enabled]);

    return { slotRef, paneRef };
}
