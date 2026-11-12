import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    Alert,
    Box,
    CircularProgress,
    Drawer,
    List,
    ListItem,
    ListItemText,
    Switch,
    Typography,
} from "@mui/material";

import { getAllItems, setItemStopped } from "../api/items.api";
import type { Station } from "../api/kitchen.api";
import { getApiErrorMessage } from "../api/error";

type Props = {
    open: boolean;
    station: Station;
    onClose: () => void;
};

// the station's items with an "on sale" switch: the kitchen/bar stops what ran out without leaving the screen
export default function StopListDrawer({ open, station, onClose }: Props) {
    const qc = useQueryClient();
    const itemsQuery = useQuery({ queryKey: ["items"], queryFn: getAllItems });

    const toggle = useMutation({
        mutationFn: ({ id, stopped }: { id: number; stopped: boolean }) => setItemStopped(id, stopped),
        onSuccess: async () => {
            await qc.invalidateQueries({ queryKey: ["items"] });
        },
    });

    const items = (itemsQuery.data ?? [])
        .filter((item) => item.type === station)
        // stopped first, so what is off sale is seen at once
        .sort((a, b) => Number(b.stopped) - Number(a.stopped) || a.name.localeCompare(b.name));

    return (
        <Drawer anchor="right" open={open} onClose={onClose}>
            <Box sx={{ width: { xs: "100vw", sm: 360 }, p: 2 }}>
                <Typography variant="h6">Stop-list</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Turn off what ran out; guests cannot order it and the TV shows SOLD OUT.
                </Typography>
                {itemsQuery.isLoading ? <CircularProgress /> : null}
                {itemsQuery.isError ? <Alert severity="error">Error loading items</Alert> : null}
                {toggle.isError ? (
                    <Alert severity="error">{getApiErrorMessage(toggle.error, "Failed to update the stop-list")}</Alert>
                ) : null}
                <List dense>
                    {items.map((item) => (
                        <ListItem
                            key={item.itemId}
                            secondaryAction={
                                <Switch
                                    edge="end"
                                    checked={!item.stopped}
                                    disabled={toggle.isPending}
                                    onChange={(e) => toggle.mutate({ id: item.itemId, stopped: !e.target.checked })}
                                />
                            }
                        >
                            <ListItemText
                                primary={item.name}
                                secondary={item.stopped ? "stopped" : "on sale"}
                                slotProps={{ secondary: { color: item.stopped ? "error" : "text.secondary" } }}
                            />
                        </ListItem>
                    ))}
                </List>
            </Box>
        </Drawer>
    );
}
