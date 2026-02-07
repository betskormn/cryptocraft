import { useDeferredValue, useEffect, useMemo, useState } from "react";

export type SortDir = "asc" | "desc";

export type SortState<K extends string> = {
    key: K;
    dir: SortDir;
};

type Options<T, K extends string> = {
    initialSearch?: string;
    initialRowsPerPage?: number;
    rowsPerPageOptions?: number[];

    filterFn: (row: T, query: string) => boolean;

    sortKeys: Record<K, (row: T) => string | number | null | undefined>;
    initialSort: SortState<K>;
};

function compareValues(a: unknown, b: unknown) {
    // null/undefined -> last
    if (a == null && b == null) return 0;
    if (a == null) return 1;
    if (b == null) return -1;

    // number
    if (typeof a === "number" && typeof b === "number") return a - b;

    // fallback string compare
    const as = String(a).toLowerCase();
    const bs = String(b).toLowerCase();
    return as.localeCompare(bs);
}

function stableSort<T>(arr: T[], cmp: (a: T, b: T) => number) {
    return arr
        .map((v, i) => ({ v, i }))
        .sort((x, y) => {
            const r = cmp(x.v, y.v);
            return r !== 0 ? r : x.i - y.i;
        })
        .map((x) => x.v);
}

export function useTableQuery<T, K extends string>(rows: T[], opts: Options<T, K>) {
    const {
        initialSearch = "",
        initialRowsPerPage = 10,
        rowsPerPageOptions = [5, 10, 25, 50],
        filterFn,
        sortKeys,
        initialSort,
    } = opts;

    const [search, setSearch] = useState(initialSearch);
    const deferredSearch = useDeferredValue(search);

    const [sort, setSort] = useState<SortState<K>>(initialSort);

    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(initialRowsPerPage);

    const filtered = useMemo(() => {
        const q = deferredSearch.trim().toLowerCase();
        if (!q) return rows;
        return rows.filter((r) => filterFn(r, q));
    }, [rows, deferredSearch, filterFn]);

    const sorted = useMemo(() => {
        const getter = sortKeys[sort.key];
        const dirMul = sort.dir === "asc" ? 1 : -1;

        return stableSort(filtered, (a, b) => dirMul * compareValues(getter(a), getter(b)));
    }, [filtered, sort, sortKeys]);

    // reset page when search or sort changes
    useEffect(() => {
        setPage(0);
    }, [deferredSearch, sort.key, sort.dir]);

    // keep page in range if count changes
    useEffect(() => {
        const maxPage = Math.max(0, Math.ceil(sorted.length / rowsPerPage) - 1);
        setPage((p) => Math.min(p, maxPage));
    }, [sorted.length, rowsPerPage]);

    const paged = useMemo(() => {
        const start = page * rowsPerPage;
        return sorted.slice(start, start + rowsPerPage);
    }, [sorted, page, rowsPerPage]);

    function toggleSort(key: K) {
        setSort((prev) => {
            if (prev.key !== key) return { key, dir: "asc" };
            return { key, dir: prev.dir === "asc" ? "desc" : "asc" };
        });
    }

    return {
        search,
        setSearch,
        deferredSearch,

        sort,
        setSort,
        toggleSort,

        page,
        setPage,
        rowsPerPage,
        setRowsPerPage,
        rowsPerPageOptions,

        total: rows.length,
        totalFiltered: filtered.length,
        totalSorted: sorted.length,

        filtered,
        sorted,
        paged,
    };
}