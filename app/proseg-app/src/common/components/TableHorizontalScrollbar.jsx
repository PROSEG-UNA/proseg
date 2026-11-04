import { useCallback, useEffect, useRef, useState } from 'react';
import { Box } from '@mui/material';

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

const EDGE_SIZE = 48;
const MAX_SPEED = 24;
const CLAMP_TOLERANCE = 96;
const WHEEL_LINE_HEIGHT = 16;

export default function TableHorizontalScrollbar({ containerRef, tableElRef }) {
    const trackRef = useRef(null);
    const dragRef = useRef(null);
    const autoScrollRef = useRef({ selecting: false, pointerX: 0, rafId: 0, active: false });
    const scrollXRef = useRef(0);
    const [scrollX, setScrollX] = useState(0);
    const [scrollWidth, setScrollWidth] = useState(0);
    const [clientWidth, setClientWidth] = useState(0);
    const [trackWidth, setTrackWidth] = useState(0);

    const applyScrollX = useCallback((next) => {
        scrollXRef.current = next;
        setScrollX(next);
        const tableEl = tableElRef?.current;
        if (tableEl) tableEl.style.transform = next ? `translateX(${-next}px)` : '';
    }, [tableElRef]);

    const measureRafIdRef = useRef(0);

    const measure = useCallback(() => {
        const container = containerRef?.current;
        const track = trackRef.current;
        if (!container) return;
        setScrollWidth(container.scrollWidth);
        setClientWidth(container.clientWidth);
        if (track) setTrackWidth(track.clientWidth);
    }, [containerRef]);

    const scheduleMeasure = useCallback(() => {
        if (measureRafIdRef.current) return;
        measureRafIdRef.current = requestAnimationFrame(() => {
            measureRafIdRef.current = 0;
            measure();
        });
    }, [measure]);

    useEffect(() => {
        const container = containerRef?.current;
        if (!container) return undefined;
        measure();
        const resizeObserver = new ResizeObserver(scheduleMeasure);
        resizeObserver.observe(container);
        if (trackRef.current) resizeObserver.observe(trackRef.current);
        const mutationObserver = new MutationObserver(scheduleMeasure);
        mutationObserver.observe(container, { childList: true, subtree: true });
        return () => {
            if (measureRafIdRef.current) cancelAnimationFrame(measureRafIdRef.current);
            resizeObserver.disconnect();
            mutationObserver.disconnect();
        };
    }, [containerRef, measure, scheduleMeasure]);

    const maxScroll = scrollWidth - clientWidth;

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            if (maxScroll >= 0 && scrollXRef.current > maxScroll + CLAMP_TOLERANCE) {
                applyScrollX(maxScroll);
            }
        }, 250);
        return () => clearTimeout(timeoutId);
    }, [maxScroll, applyScrollX]);

    const setClampedScrollX = useCallback((next) => {
        applyScrollX(clamp(next, 0, Math.max(0, maxScroll)));
    }, [maxScroll, applyScrollX]);

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
        setClampedScrollX(scrollXRef.current + velocity * MAX_SPEED);
        state.rafId = requestAnimationFrame(step);
    }, [containerRef, setClampedScrollX, stopAutoScroll]);

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

    const handleContainerWheel = useCallback((event) => {
        if (maxScroll <= 0) return;
        const horizontalIntent = event.shiftKey || Math.abs(event.deltaX) > Math.abs(event.deltaY);
        if (!horizontalIntent) return;
        const rawDelta = event.deltaX !== 0 ? event.deltaX : event.deltaY;
        const delta = event.deltaMode === 1 ? rawDelta * WHEEL_LINE_HEIGHT : rawDelta;
        event.preventDefault();
        setClampedScrollX(scrollXRef.current + delta);
    }, [maxScroll, setClampedScrollX]);

    useEffect(() => {
        const container = containerRef?.current;
        if (!container) return undefined;
        container.addEventListener('mousedown', handleContainerMouseDown);
        container.addEventListener('wheel', handleContainerWheel, { passive: false });
        document.addEventListener('mousemove', handleDocumentMouseMove);
        document.addEventListener('mouseup', handleDocumentMouseUp);
        return () => {
            container.removeEventListener('mousedown', handleContainerMouseDown);
            container.removeEventListener('wheel', handleContainerWheel);
            document.removeEventListener('mousemove', handleDocumentMouseMove);
            document.removeEventListener('mouseup', handleDocumentMouseUp);
            stopAutoScroll();
        };
    }, [containerRef, handleContainerMouseDown, handleContainerWheel, handleDocumentMouseMove, handleDocumentMouseUp, stopAutoScroll]);

    const thumbWidth = trackWidth > 0 ? Math.max(40, (clientWidth / scrollWidth) * trackWidth) : 0;
    const thumbTravel = trackWidth - thumbWidth;
    const thumbLeft = maxScroll > 0 && thumbTravel > 0 ? (scrollX / maxScroll) * thumbTravel : 0;

    const scrollToClientX = useCallback((clientX) => {
        const track = trackRef.current;
        if (!track || thumbTravel <= 0) return;
        const trackLeft = track.getBoundingClientRect().left;
        const target = ((clientX - trackLeft - thumbWidth / 2) / thumbTravel) * maxScroll;
        setClampedScrollX(target);
    }, [thumbTravel, thumbWidth, maxScroll, setClampedScrollX]);

    const handleThumbPointerDown = useCallback((event) => {
        event.preventDefault();
        event.stopPropagation();
        event.currentTarget.setPointerCapture(event.pointerId);
        dragRef.current = { startX: event.clientX, startScrollX: scrollXRef.current };
    }, []);

    const handleThumbPointerMove = useCallback((event) => {
        const drag = dragRef.current;
        if (!drag || thumbTravel <= 0) return;
        const delta = event.clientX - drag.startX;
        const target = drag.startScrollX + (delta / thumbTravel) * maxScroll;
        setClampedScrollX(target);
    }, [thumbTravel, maxScroll, setClampedScrollX]);

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
