import { act, renderHook } from '@testing-library/react';
import dayjs from 'dayjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { usePersistedMonth } from '../usePersistedMonth.js';

const STORAGE_KEY = 'test-selected-month';

beforeEach(() => {
    localStorage.clear();
});

describe('usePersistedMonth', () => {
    it('starts without a month when nothing was stored', () => {
        const { result } = renderHook(() => usePersistedMonth(STORAGE_KEY));

        expect(result.current[0]).toBeNull();
    });

    it('stores the chosen month and restores it on the next mount', () => {
        const first = renderHook(() => usePersistedMonth(STORAGE_KEY));
        act(() => first.result.current[1](dayjs('2026-05-15')));
        first.unmount();

        const second = renderHook(() => usePersistedMonth(STORAGE_KEY));

        expect(localStorage.getItem(STORAGE_KEY)).toBe('2026-05');
        expect(second.result.current[0].year()).toBe(2026);
        expect(second.result.current[0].month()).toBe(4);
    });

    it('removes the stored month when it is cleared', () => {
        const { result } = renderHook(() => usePersistedMonth(STORAGE_KEY));
        act(() => result.current[1](dayjs('2026-05-15')));
        act(() => result.current[1](null));

        expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
        expect(result.current[0]).toBeNull();
    });

    it('ignores an invalid stored month', () => {
        localStorage.setItem(STORAGE_KEY, 'not-a-month');

        const { result } = renderHook(() => usePersistedMonth(STORAGE_KEY));

        expect(result.current[0]).toBeNull();
    });
});
