import { useLayoutEffect, useRef } from 'react';

export const TOP_GAP = 24;
export const BOTTOM_GAP = 20;

const MIN_HEIGHT = 240;
const SMOOTH_TIME_MS = 110;
const SETTLE_DISTANCE_PX = 0.5;
const MAX_FRAME_MS = 50;
const NOMINAL_FRAME_MS = 16;

export function useFillToBottom(enabled = true) {
    const slotRef = useRef(null);
    const paneRef = useRef(null);

    useLayoutEffect(() => {
        const slot = slotRef.current;
        const pane = paneRef.current;
        if (!enabled || !slot || !pane) {
            if (pane) pane.style.height = '';
            return undefined;
        }

        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

        const measureTarget = () => {
            const top = Math.max(slot.getBoundingClientRect().top, TOP_GAP);
            return Math.max(window.innerHeight - top - BOTTOM_GAP, MIN_HEIGHT);
        };

        let height = measureTarget();
        let frameId = 0;
        let lastFrameTime = 0;

        const paint = () => {
            pane.style.height = `${height}px`;
        };

        const tick = (now) => {
            const target = measureTarget();
            const elapsed = lastFrameTime ? Math.min(now - lastFrameTime, MAX_FRAME_MS) : NOMINAL_FRAME_MS;
            lastFrameTime = now;

            const settled = reducedMotion.matches || Math.abs(target - height) < SETTLE_DISTANCE_PX;
            height = settled
                ? target
                : height + (target - height) * (1 - Math.exp(-elapsed / SMOOTH_TIME_MS));

            paint();
            frameId = settled ? 0 : requestAnimationFrame(tick);
        };

        const follow = () => {
            if (frameId) return;
            lastFrameTime = 0;
            frameId = requestAnimationFrame(tick);
        };

        const jump = () => {
            cancelAnimationFrame(frameId);
            frameId = 0;
            height = measureTarget();
            paint();
        };

        paint();

        window.addEventListener('scroll', follow, { passive: true });
        window.addEventListener('resize', jump, { passive: true });

        const observer = new ResizeObserver(follow);
        observer.observe(slot);
        if (slot.parentElement) observer.observe(slot.parentElement);

        return () => {
            cancelAnimationFrame(frameId);
            window.removeEventListener('scroll', follow);
            window.removeEventListener('resize', jump);
            observer.disconnect();
        };
    }, [enabled]);

    return { slotRef, paneRef };
}
