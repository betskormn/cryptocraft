import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useParams } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    IconButton,
    Paper,
    Snackbar,
    Stack,
    Tab,
    Tabs,
    TextField,
    Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import RemoveIcon from "@mui/icons-material/Remove";
import HourglassTopIcon from "@mui/icons-material/HourglassTop";

import {
    getMenu,
    getMyBill,
    joinTable,
    priceSocketUrl,
    sendOrder,
    type JoinTableResponseDto,
    type PriceMessage,
    type SellableItem,
} from "../../api/guest.api";
import type { GuestStatus, OrderResponseDto } from "../../api/sessions.api";
import type { LineStatus } from "../../api/kitchen.api";
import { getApiErrorMessage } from "../../api/error";

// A minimal guest page for the QR code of a table (/g/{venue id}/{table code}): live menu, cart, sending to the
// kitchen/bar and the guest's own bill. The real guest app comes in stage 6.

const tokenKey = (tenantId: string, code: string) => `guest.${tenantId}.${code}`;

function readToken(key: string): string | null {
    try {
        return localStorage.getItem(key);
    } catch {
        return null;
    }
}

function writeToken(key: string, token: string | null) {
    try {
        if (token) localStorage.setItem(key, token);
        else localStorage.removeItem(key);
    } catch {
        // storage blocked: the guest joins again after a reload
    }
}

const LINE_LABEL: Record<LineStatus, string> = {
    NEW: "sent",
    PREPARING: "preparing",
    READY: "ready",
    SERVED: "served",
    CANCELLED: "cancelled",
};

// the venue's live prices: every item's current price on connect, then each change
function useLivePrices(slug: string | null | undefined): Record<number, PriceMessage> {
    const [prices, setPrices] = useState<Record<number, PriceMessage>>({});
    useEffect(() => {
        if (!slug) return;
        let socket: WebSocket | null = null;
        let retry: number | undefined;
        let stopped = false;
        const connect = () => {
            socket = new WebSocket(priceSocketUrl(slug));
            socket.onmessage = (message) => {
                try {
                    const event = JSON.parse(message.data as string) as PriceMessage;
                    setPrices((current) => ({ ...current, [event.itemId]: event }));
                } catch {
                    // not a price
                }
            };
            socket.onclose = () => {
                if (!stopped) retry = window.setTimeout(connect, 3_000);
            };
        };
        connect();
        return () => {
            stopped = true;
            window.clearTimeout(retry);
            socket?.close();
        };
    }, [slug]);
    return prices;
}

export default function GuestPage() {
    const { tenantId = "", code = "" } = useParams();
    const storageKey = tokenKey(tenantId, code);
    const [storedToken] = useState(() => readToken(storageKey));
    const [name, setName] = useState("");
    const [tab, setTab] = useState(0);
    const [cart, setCart] = useState<Record<number, number>>({});
    const [notice, setNotice] = useState<{ text: string; error: boolean } | null>(null);

    const joinMutation = useMutation({
        mutationFn: ({ token, displayName }: { token: string | null; displayName?: string }) =>
            joinTable(tenantId, token, code, displayName),
        onSuccess: (joined) => writeToken(storageKey, joined.guestToken),
        // an expired token: forget it, the guest joins again
        onError: (e) => {
            if (axios.isAxiosError(e) && e.response?.status === 401) writeToken(storageKey, null);
        },
    });

    // back on the page (reload, tab reopened): the same guest, the same bill
    useEffect(() => {
        if (storedToken) joinMutation.mutate({ token: storedToken });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [storedToken]);

    const joined: JoinTableResponseDto | undefined = joinMutation.data;
    const token = joined?.guestToken ?? null;

    const menuQuery = useQuery({ queryKey: ["guest-menu", tenantId], queryFn: () => getMenu(tenantId), enabled: !!joined });
    const billQuery = useQuery({
        queryKey: ["guest-bill", tenantId, joined?.participantId],
        queryFn: () => getMyBill(tenantId, token!),
        enabled: !!token,
        refetchInterval: 10_000,
    });
    const prices = useLivePrices(joined?.venueSlug);

    const orderMutation = useMutation({
        mutationFn: () =>
            sendOrder(
                tenantId,
                token!,
                crypto.randomUUID(),
                Object.entries(cart).map(([itemId, quantity]) => ({ itemId: Number(itemId), quantity }))
            ),
        onSuccess: async (order) => {
            setCart({});
            setTab(1);
            setNotice({
                text: order.status === "AWAITING_APPROVAL" ? "Sent. It goes to the kitchen once the waiter confirms you." : "Sent to the kitchen and bar",
                error: false,
            });
            await billQuery.refetch();
        },
        onError: (e) => setNotice({ text: getApiErrorMessage(e, "Could not send the order"), error: true }),
    });

    const status: GuestStatus | undefined = billQuery.data?.status ?? joined?.status;
    const currency = joined?.currency ?? "";
    const money = (value: number) => `${value.toFixed(2)} ${currency}`.trim();

    const menu = useMemo(() => {
        const items = (menuQuery.data ?? []).filter((i) => i.available && !prices[i.itemId]?.stopped);
        return [
            { title: "Drinks", items: items.filter((i) => i.type === "DRINK") },
            { title: "Food", items: items.filter((i) => i.type === "MEAL") },
        ].filter((group) => group.items.length > 0);
    }, [menuQuery.data, prices]);

    const cartCount = Object.values(cart).reduce((a, b) => a + b, 0);
    const cartEstimate = Object.entries(cart).reduce((sum, [id, qty]) => sum + (prices[Number(id)]?.price ?? 0) * qty, 0);
    const change = (itemId: number, delta: number) =>
        setCart((current) => {
            const quantity = Math.max(0, Math.min(10, (current[itemId] ?? 0) + delta));
            const next = { ...current };
            if (quantity === 0) delete next[itemId];
            else next[itemId] = quantity;
            return next;
        });

    // --- not joined yet: name, then join ---
    if (!joined) {
        const waitingForStored = storedToken && joinMutation.isPending && !joinMutation.isError;
        return (
            <Shell>
                {waitingForStored ? (
                    <CircularProgress />
                ) : (
                    <Paper variant="outlined" sx={{ p: 3 }}>
                        <Typography variant="h5" gutterBottom>
                            Welcome!
                        </Typography>
                        <Typography color="text.secondary" sx={{ mb: 2 }}>
                            Order from your table. Prices move like on an exchange; the price is fixed the moment you send.
                        </Typography>
                        {joinMutation.isError && !(axios.isAxiosError(joinMutation.error) && joinMutation.error.response?.status === 401) ? (
                            <Alert severity="error" sx={{ mb: 2 }}>
                                {getApiErrorMessage(joinMutation.error, "Could not join the table")}
                            </Alert>
                        ) : null}
                        <TextField
                            fullWidth
                            label="Your name"
                            helperText="So the waiter knows who you are"
                            value={name}
                            inputProps={{ maxLength: 50 }}
                            onChange={(e) => setName(e.target.value)}
                        />
                        <Button
                            fullWidth
                            size="large"
                            variant="contained"
                            sx={{ mt: 2 }}
                            disabled={joinMutation.isPending}
                            onClick={() => joinMutation.mutate({ token: null, displayName: name.trim() })}
                        >
                            Join the table
                        </Button>
                    </Paper>
                )}
            </Shell>
        );
    }

    return (
        <Shell>
            <Stack direction="row" justifyContent="space-between" alignItems="baseline" sx={{ mb: 1 }}>
                <Typography variant="h5">{joined.venueName ?? "CryptoCraft"}</Typography>
                <Typography color="text.secondary">Table {joined.tableLabel}</Typography>
            </Stack>

            {status === "PENDING" ? (
                <Alert severity="info" icon={<HourglassTopIcon />} sx={{ mb: 1.5 }}>
                    Your first order goes to the kitchen once the waiter confirms you at the table.
                </Alert>
            ) : null}
            {status === "REJECTED" ? (
                <Alert severity="warning" sx={{ mb: 1.5 }}>
                    The waiter could not confirm you at this table. Please ask the staff.
                </Alert>
            ) : null}

            <Tabs value={tab} onChange={(_, value: number) => setTab(value)} sx={{ mb: 2 }}>
                <Tab label="Menu" />
                <Tab label={billQuery.data ? `My bill · ${money(billQuery.data.total)}` : "My bill"} />
            </Tabs>

            {tab === 0 ? (
                <Box sx={{ pb: cartCount > 0 ? 12 : 2 }}>
                    {menuQuery.isLoading ? <CircularProgress /> : null}
                    {menu.map((group) => (
                        <Box key={group.title} sx={{ mb: 2 }}>
                            <Typography variant="overline" color="text.secondary">
                                {group.title}
                            </Typography>
                            <Paper variant="outlined">
                                {group.items.map((item, idx) => (
                                    <MenuRow
                                        key={item.itemId}
                                        item={item}
                                        price={prices[item.itemId]?.price}
                                        money={money}
                                        quantity={cart[item.itemId] ?? 0}
                                        divider={idx > 0}
                                        onChange={(delta) => change(item.itemId, delta)}
                                    />
                                ))}
                            </Paper>
                        </Box>
                    ))}
                </Box>
            ) : (
                <BillView bill={billQuery.data} money={money} />
            )}

            {cartCount > 0 && tab === 0 ? (
                <Paper
                    elevation={8}
                    sx={{ position: "fixed", left: 0, right: 0, bottom: 0, p: 2, borderRadius: 0, borderTop: "1px solid", borderColor: "divider" }}
                >
                    <Box sx={{ maxWidth: 560, mx: "auto" }}>
                        <Button
                            fullWidth
                            size="large"
                            variant="contained"
                            disabled={orderMutation.isPending || status === "REJECTED"}
                            onClick={() => orderMutation.mutate()}
                        >
                            Send {cartCount} item{cartCount > 1 ? "s" : ""} · ≈ {money(cartEstimate)}
                        </Button>
                        <Typography variant="caption" color="text.secondary" sx={{ display: "block", textAlign: "center", mt: 0.5 }}>
                            The price is fixed at the moment you send
                        </Typography>
                    </Box>
                </Paper>
            ) : null}

            <Snackbar open={!!notice} autoHideDuration={5000} onClose={() => setNotice(null)} anchorOrigin={{ vertical: "top", horizontal: "center" }}>
                {notice ? (
                    <Alert severity={notice.error ? "error" : "success"} variant="filled" onClose={() => setNotice(null)}>
                        {notice.text}
                    </Alert>
                ) : undefined}
            </Snackbar>
        </Shell>
    );
}

function Shell({ children }: { children: ReactNode }) {
    return (
        <Box sx={{ minHeight: "100vh", px: 2, py: 3 }}>
            <Box sx={{ maxWidth: 560, mx: "auto" }}>{children}</Box>
        </Box>
    );
}

function MenuRow({
    item,
    price,
    money,
    quantity,
    divider,
    onChange,
}: {
    item: SellableItem;
    price?: number;
    money: (value: number) => string;
    quantity: number;
    divider: boolean;
    onChange: (delta: number) => void;
}) {
    return (
        <>
            {divider ? <Divider /> : null}
            <Stack direction="row" alignItems="center" spacing={1} sx={{ px: 2, py: 1.2 }}>
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                    <Typography fontWeight={700} sx={{ overflowWrap: "anywhere" }}>
                        {item.name}
                    </Typography>
                    <Typography color="primary.main" fontWeight={800}>
                        {price !== undefined ? money(price) : "…"}
                    </Typography>
                </Box>
                {quantity > 0 ? (
                    <>
                        <IconButton size="small" onClick={() => onChange(-1)} aria-label={`One ${item.name} less`}>
                            <RemoveIcon fontSize="small" />
                        </IconButton>
                        <Typography fontWeight={800} sx={{ minWidth: 20, textAlign: "center" }}>
                            {quantity}
                        </Typography>
                    </>
                ) : null}
                <IconButton
                    size="small"
                    disabled={price === undefined || quantity >= 10}
                    onClick={() => onChange(1)}
                    aria-label={`Add ${item.name}`}
                    sx={{ border: "1px solid", borderColor: "divider" }}
                >
                    <AddIcon fontSize="small" />
                </IconButton>
            </Stack>
        </>
    );
}

function BillView({ bill, money }: { bill?: { total: number; orders: OrderResponseDto[] }; money: (value: number) => string }) {
    if (!bill) return <CircularProgress />;
    if (bill.orders.length === 0) {
        return <Typography color="text.secondary">Nothing ordered yet</Typography>;
    }
    return (
        <Stack spacing={1.5}>
            {bill.orders
                .slice()
                .reverse()
                .map((order) => (
                    <Paper key={order.orderId} variant="outlined" sx={{ p: 1.5 }}>
                        <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.5 }}>
                            <Typography variant="body2" color="text.secondary">
                                {new Date(order.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                            </Typography>
                            {order.status === "AWAITING_APPROVAL" ? <Chip size="small" color="warning" label="waits for the waiter" /> : null}
                        </Stack>
                        {order.lines.map((line) => {
                            const cancelled = line.status === "CANCELLED";
                            return (
                                <Stack key={line.lineId} direction="row" spacing={1} alignItems="baseline" sx={{ py: 0.3 }}>
                                    <Typography sx={{ flexGrow: 1, textDecoration: cancelled ? "line-through" : undefined, color: cancelled ? "text.disabled" : undefined }}>
                                        {line.quantity}× {line.itemName}
                                        {order.status !== "AWAITING_APPROVAL" ? (
                                            <Typography component="span" variant="caption" color="text.secondary">
                                                {" "}
                                                · {LINE_LABEL[line.status]}
                                            </Typography>
                                        ) : null}
                                    </Typography>
                                    <Typography sx={{ color: cancelled ? "text.disabled" : undefined }}>{money(line.lineTotal)}</Typography>
                                </Stack>
                            );
                        })}
                    </Paper>
                ))}
            <Stack direction="row" justifyContent="space-between" sx={{ pt: 1 }}>
                <Typography variant="h6">Total</Typography>
                <Typography variant="h6" color="primary.main">
                    {money(bill.total)}
                </Typography>
            </Stack>
            <Typography variant="caption" color="text.secondary">
                You pay at the end of the visit.
            </Typography>
        </Stack>
    );
}
