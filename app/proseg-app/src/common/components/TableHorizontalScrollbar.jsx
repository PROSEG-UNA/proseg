import { useCallback, useEffect, useRef, useState } from 'react';
import { Box } from '@mui/material';

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

const EDGE_SIZE = 48;
const MAX_SPEED = 24;

export default function TableHorizontalScrollbar({ containerRef }) {
    const trackRef = useRef(null);
    const dragRef = useRef(null);
    const autoScrollRef = useRef({ selecting: false, pointerX: 0, rafId: 0, active: false });
    const [scrollWidth, setScrollWidth] = useState(0);
    const [clientWidth, setClientWidth] = useState(0);
    const [scrollLeft, setScrollLeft] = useState(0);
    const [trackWidth, setTrackWidth] = useState(0);

    const measure = useCallback(() => {
        const container = containerRef?.current;
        const track = trackRef.current;
        if (!container) return;
        setScrollWidth(container.scrollWidth);
        setClientWidth(container.clientWidth);
        setScrollLeft(container.scrollLeft);
        if (track) setTrackWidth(track.clientWidth);
    }, [containerRef]);

    const syncFromContainer = useCallback(() => {
        const container = containerRef?.current;
        if (!container) return;
        setScrollLeft(container.scrollLeft);
    }, [containerRef]);

    useEffect(() => {
        const container = containerRef?.current;
        if (!container) return undefined;
        measure();
        container.addEventListener('scroll', syncFromContainer, { passive: true });
        const resizeObserver = new ResizeObserver(measure);
        resizeObserver.observe(container);
        if (trackRef.current) resizeObserver.observe(trackRef.current);
        const mutationObserver = new MutationObserver(measure);
        mutationObserver.observe(container, { childList: true, subtree: true });
        return () => {
            container.removeEventListener('scroll', syncFromContainer);
            resizeObserver.disconnect();
            mutationObserver.disconnect();
        };
    }, [containerRef, measure, syncFromContainer]);

    const stopAutoScroll = useCallback(() => {
        const state = autoScrollRef.current;
        if (state.rafId) cancelAnimationFrame(state.rafId);
        state.rafId = 0;
        state.active = false;
    }, []);

    const step = useCallback(() => {
        const container = containerRef?.current;
        const state = autoScrollRef.current;
        if (!container) {
            stopAutoScroll();
            return;
        }
        const rect = container.getBoundingClientRect();
        const x = state.pointerX;
        let velocity = 0;
        if (x > rect.right - EDGE_SIZE) {
            velocity = clamp((x - (rect.right - EDGE_SIZE)) / EDGE_SIZE, 0, 1);
        } else if (x < rect.left + EDGE_SIZE) {
            velocity = -clamp(((rect.left + EDGE_SIZE) - x) / EDGE_SIZE, 0, 1);
        }
        if (velocity === 0) {
            stopAutoScroll();
            return;
        }
        const max = container.scrollWidth - container.clientWidth;
        container.scrollLeft = clamp(container.scrollLeft + velocity * MAX_SPEED, 0, max);
        state.rafId = requestAnimationFrame(step);
    }, [containerRef, stopAutoScroll]);

    const maybeStartAutoScroll = useCallback(() => {
        const state = autoScrollRef.current;
        if (state.active) return;
        state.active = true;
        state.rafId = requestAnimationFrame(step);
    }, [step]);

    const handleContainerMouseDown = useCallback((event) => {
        const container = containerRef?.current;
        if (!container || event.button !== 0) return;
        if (container.scrollWidth <= container.clientWidth) return;
        autoScrollRef.current.selecting = true;
        autoScrollRef.current.pointerX = event.clientX;
    }, [containerRef]);

    const handleDocumentMouseMove = useCallback((event) => {
        const state = autoScrollRef.current;
        if (!state.selecting) return;
        if (event.buttons !== 1) {
            state.selecting = false;
            stopAutoScroll();
            return;
        }
        const container = containerRef?.current;
        if (!container) return;
        state.pointerX = event.clientX;
        const rect = container.getBoundingClientRect();
        if (event.clientX > rect.right - EDGE_SIZE || event.clientX < rect.left + EDGE_SIZE) {
            maybeStartAutoScroll();
        } else {
            stopAutoScroll();
        }
    }, [containerRef, maybeStartAutoScroll, stopAutoScroll]);

    const handleDocumentMouseUp = useCallback(() => {
        autoScrollRef.current.selecting = false;
        stopAutoScroll();
    }, [stopAutoScroll]);

    useEffect(() => {
        const container = containerRef?.current;
        if (!container) return undefined;
        container.addEventListener('mousedown', handleContainerMouseDown);
        document.addEventListener('mousemove', handleDocumentMouseMove);
        document.addEventListener('mouseup', handleDocumentMouseUp);
        return () => {
            container.removeEventListener('mousedown', handleContainerMouseDown);
            document.removeEventListener('mousemove', handleDocumentMouseMove);
            document.removeEventListener('mouseup', handleDocumentMouseUp);
            stopAutoScroll();
        };
    }, [containerRef, handleContainerMouseDown, handleDocumentMouseMove, handleDocumentMouseUp, stopAutoScroll]);

    const maxScroll = scrollWidth - clientWidth;
    const thumbWidth = trackWidth > 0 ? Math.max(40, (clientWidth / scrollWidth) * trackWidth) : 0;
    const thumbTravel = trackWidth - thumbWidth;
    const thumbLeft = maxScroll > 0 && thumbTravel > 0 ? (scrollLeft / maxScroll) * thumbTravel : 0;

    const scrollToClientX = useCallback((clientX) => {
        const container = containerRef?.current;
        const track = trackRef.current;
        if (!container || !track || thumbTravel <= 0) return;
        const trackLeft = track.getBoundingClientRect().left;
        const target = ((clientX - trackLeft - thumbWidth / 2) / thumbTravel) * maxScroll;
        container.scrollLeft = clamp(target, 0, maxScroll);
    }, [containerRef, thumbTravel, thumbWidth, maxScroll]);

    const handleThumbPointerDown = useCallback((event) => {
        const container = containerRef?.current;
        if (!container) return;
        event.preventDefault();
        event.stopPropagation();
        event.currentTarget.setPointerCapture(event.pointerId);
        dragRef.current = { startX: event.clientX, startScrollLeft: container.scrollLeft };
    }, [containerRef]);

    const handleThumbPointerMove = useCallback((event) => {
        const container = containerRef?.current;
        const drag = dragRef.current;
        if (!container || !drag || thumbTravel <= 0) return;
        const delta = event.clientX - drag.startX;
        const target = drag.startScrollLeft + (delta / thumbTravel) * maxScroll;
        container.scrollLeft = clamp(target, 0, maxScroll);
    }, [containerRef, thumbTravel, maxScroll]);

    const handleThumbPointerUp = useCallback((event) => {
        if (event.currentTarget.hasPointerCapture(event.pointerId)) {
            event.currentTarget.releasePointerCapture(event.pointerId);
        }
        dragRef.current = null;
    }, []);

    const handleTrackPointerDown = useCallback((event) => {
        scrollToClientX(event.clientX);
    }, [scrollToClientX]);

    if (scrollWidth <= clientWidth + 1) return null;

    return (
        <Box
            ref={trackRef}
            onPointerDown={handleTrackPointerDown}
            sx={(t) => ({
                position: 'relative',
                width: '100%',
                height: 14,
                cursor: 'default',
                touchAction: 'none',
                userSelect: 'none',
                backgroundColor: 'rgba(0, 0, 0, 0.06)',
                ...t.applyStyles('dark', {
                    backgroundColor: 'rgba(255, 255, 255, 0.10)',
                }),
            })}
        >
            <Box
                onPointerDown={handleThumbPointerDown}
                onPointerMove={handleThumbPointerMove}
                onPointerUp={handleThumbPointerUp}
                onLostPointerCapture={handleThumbPointerUp}
                sx={(t) => ({
                    position: 'absolute',
                    top: 3,
                    bottom: 3,
                    left: thumbLeft,
                    width: thumbWidth,
                    borderRadius: 4,
                    cursor: 'default',
                    touchAction: 'none',
                    backgroundColor: 'rgba(0, 0, 0, 0.40)',
                    transition: 'background-color 120ms ease',
                    '&:hover': {
                        backgroundColor: 'rgba(0, 0, 0, 0.60)',
                    },
                    ...t.applyStyles('dark', {
                        backgroundColor: 'rgba(255, 255, 255, 0.50)',
                        '&:hover': {
                            backgroundColor: 'rgba(255, 255, 255, 0.65)',
                        },
                    }),
                })}
            />
        </Box>
    );
}
