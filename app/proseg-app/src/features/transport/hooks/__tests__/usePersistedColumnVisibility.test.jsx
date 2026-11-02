import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { usePersistedColumnVisibility } from '../usePersistedColumnVisibility.js';

const STORAGE_KEY = 'test-table-column-visibility';

beforeEach(() => {
    localStorage.clear();
});

describe('usePersistedColumnVisibility', () => {
    it('starts empty when nothing was stored', () => {
        const { result } = renderHook(() => usePersistedColumnVisibility(STORAGE_KEY));

        expect(result.current[0]).toEqual({});
    });

    it('stores the hidden columns and restores them on the next mount', () => {
        const first = renderHook(() => usePersistedColumnVisibility(STORAGE_KEY));
        act(() => first.result.current[1]({ status: false }));
        first.unmount();

        const second = renderHook(() => usePersistedColumnVisibility(STORAGE_KEY));

        expect(second.result.current[0]).toEqual({ status: false });
    });

    it('ignores stored values that are not a visibility object', () => {
        localStorage.setItem(STORAGE_KEY, '[1,2]');

        const { result } = renderHook(() => usePersistedColumnVisibility(STORAGE_KEY));

        expect(result.current[0]).toEqual({});
    });

    it('ignores corrupted stored values', () => {
        localStorage.setItem(STORAGE_KEY, '{not-json');

        const { result } = renderHook(() => usePersistedColumnVisibility(STORAGE_KEY));

        expect(result.current[0]).toEqual({});
    });
});
