import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { useServerTableState } from '../useServerTableState.js';

const STORAGE_KEY = 'test-server-table-state';
const FILTERABLE_COLUMNS = new Set(['name']);

function renderTableState(options = {}) {
    return renderHook(() => useServerTableState({
        initialSorting: [],
        filterableColumns: FILTERABLE_COLUMNS,
        storageKey: STORAGE_KEY,
        ...options,
    }));
}

beforeEach(() => {
    localStorage.clear();
});

describe('useServerTableState initial column visibility', () => {
    it('starts with every column visible when no initial visibility is given', () => {
        const { result } = renderTableState();

        expect(result.current.tableOptions.state.columnVisibility).toEqual({});
    });

    it('hides the initial hidden columns when nothing was stored', () => {
        const { result } = renderTableState({ initialColumnVisibility: { rowNumber: false } });

        expect(result.current.tableOptions.state.columnVisibility).toEqual({ rowNumber: false });
    });

    it('applies the initial hidden columns over a stored empty visibility', () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ columnVisibility: {} }));

        const { result } = renderTableState({ initialColumnVisibility: { rowNumber: false } });

        expect(result.current.tableOptions.state.columnVisibility).toEqual({ rowNumber: false });
    });

    it('keeps the stored choice when the user showed a column hidden by default', () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ columnVisibility: { rowNumber: true, driver: false } }));

        const { result } = renderTableState({ initialColumnVisibility: { rowNumber: false } });

        expect(result.current.tableOptions.state.columnVisibility).toEqual({ rowNumber: true, driver: false });
    });

    it('remembers a column the user shows after the default hid it', () => {
        const first = renderTableState({ initialColumnVisibility: { rowNumber: false } });
        act(() => first.result.current.tableOptions.onColumnVisibilityChange({ rowNumber: true }));
        first.unmount();

        const second = renderTableState({ initialColumnVisibility: { rowNumber: false } });

        expect(second.result.current.tableOptions.state.columnVisibility).toEqual({ rowNumber: true });
    });
});
