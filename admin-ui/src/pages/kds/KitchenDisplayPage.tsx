import { useEffect, useMemo, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    IconButton,
    ListItemIcon,
    Menu,
    MenuItem,
    Paper,
    Snackbar,
    Stack,
    Tooltip,
    Typography,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import UndoIcon from "@mui/icons-material/Undo";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import CancelOutlinedIcon from "@mui/icons-material/CancelOutlined";
import BlockIcon from "@mui/icons-material/Block";
import NotificationsActiveOutlinedIcon from "@mui/icons-material/NotificationsActiveOutlined";
import NotificationsOffOutlinedIcon from "@mui/icons-material/NotificationsOffOutlined";
import PlaylistRemoveIcon from "@mui/icons-material/PlaylistRemove";

import { changeLineStatus, type KitchenLineResponseDto, type LineStatus, type Station } from "../../api/kitchen.api";
import { setItemStopped } from "../../api/items.api";
import { getApiErrorMessage } from "../../api/error";
import { useAuth } from "../../auth/AuthContext";
import { useKitchenFeed } from "../../hooks/useKitchenFeed";
import CancelLineDialog from "../../components/CancelLineDialog";
import StopListDrawer from "../../components/StopListDrawer";

const STATION: Record<Station, { title: string; other: Station; otherTitle: string }> = {
    MEAL: { title: "Kitchen", other: "DRINK", otherTitle: "Bar" },
    DRINK: { title: "Bar", other: "MEAL", otherTitle: "Kitchen" },
};
const PATH: Record<Station, string> = { MEAL: "/kds/kitchen", DRINK: "/kds/bar" };

// the main button moves a line one step forward; undo moves it one step back
const NEXT: Partial<Record<LineStatus, { status: LineStatus; label: string }>> = {
    NEW: { status: "PREPARING", label: "Start" },
    PREPARING: { status: "READY", label: "Ready" },
    READY: { status: "SERVED", label: "Served" },
};
const PREVIOUS: Partial<Record<LineStatus, LineStatus>> = { PREPARING: "NEW", READY: "PREPARING" };

const STATUS_CHIP: Record<string, { label: string; color: "default" | "warning" | "success" }> = {
    NEW: { label: "new", color: "default" },
    PREPARING: { label: "preparing", color: "warning" },
    READY: { label: "ready", color: "success" },
};

// minutes since the order was sent; tickets turn orange after 10 and red after 20
const LATE_MIN = 10;
const VERY_LATE_MIN = 20;

type Ticket = { orderId: string; tableLabel: string; guestName?: string | null; placedAt: string; lines: KitchenLineResponseDto[] };

function toTickets(lines: KitchenLineResponseDto[]): Ticket[] {
    const byOrder = new Map<string, Ticket>();
    for (const line of lines) {
        const ticket = byOrder.get(line.orderId) ?? {
            orderId: line.orderId,
            tableLabel: line.tableLabel,
            guestName: line.guestName,
            placedAt: line.placedAt,
            lines: [],
        };
        ticket.lines.push(line);
        byOrder.set(line.orderId, ticket);
    }
    return [...byOrder.values()];
}

// a short beep for a new order; browsers allow sound only after the first click on the page
function beep() {
    try {
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.frequency.value = 880;
        gain.gain.setValueAtTime(0.15, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
        osc.connect(gain).connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.4);
        osc.onended = () => void ctx.close();
    } catch {
        // no audio available
    }
}

/**
 * Full-screen kitchen (MEAL) or bar (DRINK) display: one ticket per order in the order they were sent.
 * Lines leave the screen when served or cancelled.
 */
export default function KitchenDisplayPage({ station }: { station: Station }) {
    const config = STATION[station];
    const [sound, setSound] = useState(true);
    const [now, setNow] = useState(() => Date.now());
    const [menu, setMenu] = useState<{ anchor: HTMLElement; line: KitchenLineResponseDto } | null>(null);
    const [cancelling, setCancelling] = useState<KitchenLineResponseDto | null>(null);
    const [stopListOpen, setStopListOpen] = useState(false);
    const [notice, setNotice] = useState<{ text: string; error: boolean } | null>(null);

    const feed = useKitchenFeed(station, sound ? beep : undefined);
    const tickets = useMemo(() => toTickets(feed.lines), [feed.lines]);

    const { venue, can } = useAuth();

    useEffect(() => {
        const timer = window.setInterval(() => setNow(Date.now()), 15_000);
        return () => window.clearInterval(timer);
    }, []);

    const statusMutation = useMutation({
        mutationFn: ({ line, status, reason }: { line: KitchenLineResponseDto; status: LineStatus; reason?: string }) =>
            changeLineStatus(line.lineId, status, reason),
        onSuccess: (line) => {
            feed.applyLine(line);
            setCancelling(null);
        },
        onError: (e) => setNotice({ text: getApiErrorMessage(e, "Failed to change the status"), error: true }),
    });

    const stopMutation = useMutation({
        mutationFn: (line: KitchenLineResponseDto) => setItemStopped(line.itemId, true),
        onSuccess: (item) => setNotice({ text: `${item.name} is on the stop-list`, error: false }),
        onError: (e) => setNotice({ text: getApiErrorMessage(e, "Failed to update the stop-list"), error: true }),
    });

    const busy = statusMutation.isPending;

    return (
        <Box sx={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
            <Stack
                direction="row"
                alignItems="center"
                spacing={1.5}
                sx={{ px: 2, py: 1.2, borderBottom: "1px solid", borderColor: "divider", bgcolor: "rgba(34,40,49,0.7)" }}
            >
                <Tooltip title="Back to admin">
                    <IconButton component={RouterLink} to={can("visits") ? "/visits" : "/"}>
                        <ArrowBackIcon />
                    </IconButton>
                </Tooltip>
                <Typography variant="h5">{config.title}</Typography>
                {venue ? <Typography color="text.secondary">{venue.name}</Typography> : null}
                <Chip
                    size="small"
                    color={feed.online ? "success" : "warning"}
                    variant="outlined"
                    label={feed.online ? "live" : "no connection"}
                />
                <Typography color="text.secondary" sx={{ display: { xs: "none", sm: "block" } }}>
                    {feed.lines.length} in work
                </Typography>

                <Box sx={{ flexGrow: 1 }} />

                <Tooltip title={sound ? "Sound on new orders: on" : "Sound on new orders: off"}>
                    <IconButton onClick={() => setSound((s) => !s)}>
                        {sound ? <NotificationsActiveOutlinedIcon /> : <NotificationsOffOutlinedIcon />}
                    </IconButton>
                </Tooltip>
                <Button variant="outlined" startIcon={<PlaylistRemoveIcon />} onClick={() => setStopListOpen(true)}>
                    Stop-list
                </Button>
                <Button component={RouterLink} to={PATH[config.other]}>
                    {config.otherTitle}
                </Button>
            </Stack>

            <Box sx={{ p: 2, flexGrow: 1 }}>
                {feed.error ? <Alert severity="error" sx={{ mb: 2 }}>{feed.error}</Alert> : null}
                {feed.loading ? <CircularProgress /> : null}
                {!feed.loading && tickets.length === 0 && !feed.error ? (
                    <Typography color="text.secondary" sx={{ mt: 6, textAlign: "center" }} variant="h6">
                        No orders in the queue
                    </Typography>
                ) : null}

                <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 300px), 1fr))" }}>
                    {tickets.map((ticket) => {
                        const minutes = Math.max(0, Math.floor((now - new Date(ticket.placedAt).getTime()) / 60_000));
                        const ageColor = minutes >= VERY_LATE_MIN ? "error.main" : minutes >= LATE_MIN ? "warning.main" : "divider";
                        return (
                            <Paper key={ticket.orderId} variant="outlined" sx={{ borderColor: ageColor, borderWidth: 2, overflow: "hidden" }}>
                                <Stack direction="row" alignItems="baseline" spacing={1} sx={{ px: 2, py: 1.2, bgcolor: "rgba(255,255,255,0.04)" }}>
                                    <Typography variant="h6">Table {ticket.tableLabel}</Typography>
                                    <Typography color="text.secondary" noWrap sx={{ flexGrow: 1 }}>
                                        {ticket.guestName ?? ""}
                                    </Typography>
                                    <Typography fontWeight={800} sx={{ color: minutes >= LATE_MIN ? ageColor : "text.secondary" }}>
                                        {minutes} min
                                    </Typography>
                                </Stack>

                                <Stack sx={{ px: 1, py: 1 }} spacing={0.5}>
                                    {ticket.lines.map((line) => {
                                        const next = NEXT[line.status];
                                        const previous = PREVIOUS[line.status];
                                        const chip = STATUS_CHIP[line.status];
                                        return (
                                            <Stack key={line.lineId} direction="row" alignItems="center" spacing={1} sx={{ px: 1, py: 0.5 }}>
                                                <Typography fontWeight={850} sx={{ minWidth: 28 }}>
                                                    {line.quantity}×
                                                </Typography>
                                                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                                    <Typography fontWeight={700} sx={{ overflowWrap: "anywhere" }}>
                                                        {line.itemName}
                                                    </Typography>
                                                    {chip ? <Chip size="small" color={chip.color} label={chip.label} /> : null}
                                                </Box>
                                                {previous ? (
                                                    <Tooltip title="Undo">
                                                        <span>
                                                            <IconButton
                                                                size="small"
                                                                disabled={busy}
                                                                onClick={() => statusMutation.mutate({ line, status: previous })}
                                                            >
                                                                <UndoIcon fontSize="small" />
                                                            </IconButton>
                                                        </span>
                                                    </Tooltip>
                                                ) : null}
                                                {next ? (
                                                    <Button
                                                        variant={line.status === "READY" ? "contained" : "outlined"}
                                                        color={line.status === "PREPARING" ? "success" : "primary"}
                                                        disabled={busy}
                                                        onClick={() => statusMutation.mutate({ line, status: next.status })}
                                                        sx={{ minWidth: 84 }}
                                                    >
                                                        {next.label}
                                                    </Button>
                                                ) : null}
                                                <IconButton size="small" onClick={(e) => setMenu({ anchor: e.currentTarget, line })}>
                                                    <MoreVertIcon fontSize="small" />
                                                </IconButton>
                                            </Stack>
                                        );
                                    })}
                                </Stack>
                            </Paper>
                        );
                    })}
                </Box>
            </Box>

            <Menu anchorEl={menu?.anchor} open={!!menu} onClose={() => setMenu(null)}>
                <MenuItem
                    onClick={() => {
                        if (menu) setCancelling(menu.line);
                        setMenu(null);
                    }}
                >
                    <ListItemIcon>
                        <CancelOutlinedIcon fontSize="small" />
                    </ListItemIcon>
                    Cancel this line…
                </MenuItem>
                <MenuItem
                    disabled={stopMutation.isPending}
                    onClick={() => {
                        if (menu) stopMutation.mutate(menu.line);
                        setMenu(null);
                    }}
                >
                    <ListItemIcon>
                        <BlockIcon fontSize="small" />
                    </ListItemIcon>
                    Stop selling {menu?.line.itemName}
                </MenuItem>
            </Menu>

            {cancelling ? (
                <CancelLineDialog
                    open
                    line={cancelling}
                    loading={busy}
                    onClose={() => setCancelling(null)}
                    onConfirm={(reason) => statusMutation.mutate({ line: cancelling, status: "CANCELLED", reason })}
                />
            ) : null}

            {stopListOpen ? <StopListDrawer open station={station} onClose={() => setStopListOpen(false)} /> : null}

            <Snackbar open={!!notice} autoHideDuration={4000} onClose={() => setNotice(null)}>
                {notice ? (
                    <Alert severity={notice.error ? "error" : "success"} variant="filled" onClose={() => setNotice(null)}>
                        {notice.text}
                    </Alert>
                ) : undefined}
            </Snackbar>
        </Box>
    );
}
