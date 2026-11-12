import { useCallback, useEffect, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";

import { getQueue, IN_WORK, type KitchenLineResponseDto, type Station } from "../api/kitchen.api";
import { getApiErrorMessage } from "../api/error";
import { getCurrentTenantId } from "../tenant/currentTenant";

// a kitchen does not need the order in the same second: a few seconds of delay keeps the screen a plain HTTP client
const POLL_EVERY_MS = 5_000;

export type KitchenFeed = {
    lines: KitchenLineResponseDto[];
    loading: boolean;
    // false while the last reload failed; the screen keeps showing the last queue it got
    online: boolean;
    error: string | null;
    // the answer of our own status change, shown before the next reload
    applyLine: (line: KitchenLineResponseDto) => void;
};

/**
 * The queue of one station, reloaded from Order Service every few seconds.
 * {@code onNewOrder} is called when a reload brings an order the screen has not shown yet.
 */
export function useKitchenFeed(station: Station, onNewOrder?: () => void): KitchenFeed {
    const queryClient = useQueryClient();
    const tenantId = getCurrentTenantId();
    const queryKey = ["kitchen-queue", tenantId, station];

    const query = useQuery({
        queryKey,
        queryFn: () => getQueue(station),
        enabled: !!tenantId,
        refetchInterval: POLL_EVERY_MS,
        refetchIntervalInBackground: true,
    });

    const onNewOrderRef = useRef(onNewOrder);
    useEffect(() => {
        onNewOrderRef.current = onNewOrder;
    }, [onNewOrder]);

    // the first load of a station only remembers its orders; later loads ring for orders not seen before
    const seenRef = useRef<Set<string> | null>(null);
    useEffect(() => {
        seenRef.current = null;
    }, [station, tenantId]);
    useEffect(() => {
        if (!query.data) return;
        const orderIds = new Set(query.data.map((l) => l.orderId));
        const seen = seenRef.current;
        if (seen && [...orderIds].some((id) => !seen.has(id))) {
            onNewOrderRef.current?.();
        }
        seenRef.current = orderIds;
    }, [query.data]);

    const applyLine = useCallback(
        (line: KitchenLineResponseDto) => {
            queryClient.setQueryData<KitchenLineResponseDto[]>(["kitchen-queue", tenantId, station], (current) => {
                const others = (current ?? []).filter((l) => l.lineId !== line.lineId);
                return IN_WORK.includes(line.status) ? [...others, line].sort(byPlacedAt) : others;
            });
        },
        [queryClient, tenantId, station]
    );

    let error: string | null = null;
    if (!tenantId) {
        error = "Select a venue in the admin first";
    } else if (query.isError && !query.data) {
        error = getApiErrorMessage(query.error, "Cannot load the queue");
    }

    return {
        lines: query.data ?? [],
        loading: query.isPending && !!tenantId,
        online: !query.isError,
        error,
        applyLine,
    };
}

function byPlacedAt(a: KitchenLineResponseDto, b: KitchenLineResponseDto): number {
    return a.placedAt.localeCompare(b.placedAt) || a.lineId - b.lineId;
}
