import { useDeferredValue, useEffect, useMemo, useState } from "react";

type Options<T> = {
    initialSearch?: string;
    initialRowsPerPage?: number;
    rowsPerPageOptions?: number[];
    filterFn: (row: T, query: string) => boolean;
};

export function useSearchPagination<T>(rows: T[], options: Options<T>) {
    const {
        initialSearch = "",
        initialRowsPerPage = 10,
        rowsPerPageOptions = [5, 10, 25, 50],
        filterFn,
    } = options;

    const [search, setSearch] = useState(initialSearch);
    const deferredSearch = useDeferredValue(search);

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(initialRowsPerPage);

    const filtered = useMemo(() => {
        const q = deferredSearch.trim().toLowerCase();
        if (!q) return rows;
        return rows.filter((r) => filterFn(r, q));
    }, [rows, deferredSearch, filterFn]);

    // reset page on new search
    useEffect(() => {
        setPage(0);
    }, [deferredSearch]);

    // keep page in range if rows count changes
    useEffect(() => {
        const maxPage = Math.max(0, Math.ceil(filtered.length / rowsPerPage) - 1);
        setPage((p) => Math.min(p, maxPage));
    }, [filtered.length, rowsPerPage]);

    const paged = useMemo(() => {
        const start = page * rowsPerPage;
        return filtered.slice(start, start + rowsPerPage);
    }, [filtered, page, rowsPerPage]);

    return {
        search,
        setSearch,
        deferredSearch,

        filtered,
        paged,

        page,
        setPage,
        rowsPerPage,
        setRowsPerPage,
        rowsPerPageOptions,

        total: rows.length,
        totalFiltered: filtered.length,
    };
}