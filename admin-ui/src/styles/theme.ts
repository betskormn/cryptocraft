// src/styles/theme.ts
import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
    palette: {
        mode: "dark",
        background: {
            default: "#1B1F24", // світліший графіт
            paper: "#222831",   // поверхні (Paper, Dialog)
        },
        primary: {
            main: "#F2A23A",      // м’який orange
            light: "#FFD08A",
            dark: "#C87B1E",
            contrastText: "#14181D",
        },
        text: {
            primary: "#E7ECF2",
            secondary: "#AAB4C2",
        },
        divider: "rgba(255,255,255,0.10)",
        error: { main: "#EF4444" },
        warning: { main: "#F59E0B" },
        info: { main: "#60A5FA" },
        success: { main: "#22C55E" },
    },

    shape: { borderRadius: 14 },

    typography: {
        fontFamily: [
            "Inter",
            "system-ui",
            "-apple-system",
            "Segoe UI",
            "Roboto",
            "Arial",
            "sans-serif",
        ].join(","),
        h5: { fontWeight: 850, letterSpacing: 0.2 },
        h6: { fontWeight: 800, letterSpacing: 0.2 },
        button: { textTransform: "none", fontWeight: 750 },
    },

    components: {
        MuiCssBaseline: {
            styleOverrides: {
                body: {
                    backgroundImage:
                        "radial-gradient(1100px 520px at 20% -10%, rgba(242,162,58,0.16), transparent 55%)," +
                        "radial-gradient(900px 520px at 80% 0%, rgba(110,231,183,0.08), transparent 55%)",
                    backgroundAttachment: "fixed",
                },
                "*::-webkit-scrollbar": { width: 10, height: 10 },
                "*::-webkit-scrollbar-thumb": {
                    backgroundColor: "rgba(255,255,255,0.12)",
                    borderRadius: 10,
                    border: "2px solid rgba(0,0,0,0)",
                    backgroundClip: "padding-box",
                },
                "*::-webkit-scrollbar-thumb:hover": {
                    backgroundColor: "rgba(255,255,255,0.18)",
                },
            },
        },

        MuiAppBar: {
            styleOverrides: {
                root: {
                    background: "rgba(34,40,49,0.75)",
                    backdropFilter: "blur(10px)",
                    borderBottom: "1px solid rgba(255,255,255,0.10)",
                },
            },
        },

        MuiPaper: {
            styleOverrides: {
                root: {
                    backgroundImage: "none",
                    border: "1px solid rgba(255,255,255,0.10)",
                },
            },
        },

        MuiButton: {
            styleOverrides: {
                root: {
                    borderRadius: 12,
                    paddingInline: 14,
                },
                contained: {
                    boxShadow: "0 12px 28px rgba(0,0,0,0.35)",
                },
                outlined: {
                    borderColor: "rgba(255,255,255,0.18)",
                },
            },
        },

        MuiOutlinedInput: {
            styleOverrides: {
                root: {
                    borderRadius: 12,
                    backgroundColor: "rgba(255,255,255,0.04)",
                    "&:hover .MuiOutlinedInput-notchedOutline": {
                        borderColor: "rgba(255,255,255,0.24)",
                    },
                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                        borderColor: "rgba(242,162,58,0.85)",
                        borderWidth: 1,
                    },
                },
                notchedOutline: {
                    borderColor: "rgba(255,255,255,0.16)",
                },
            },
        },

        MuiTableHead: {
            styleOverrides: {
                root: { backgroundColor: "rgba(255,255,255,0.03)" },
            },
        },
        MuiTableCell: {
            styleOverrides: {
                head: {
                    color: "#C8D0DC",
                    fontWeight: 800,
                    borderBottom: "1px solid rgba(255,255,255,0.12)",
                },
                body: {
                    borderBottom: "1px solid rgba(255,255,255,0.10)",
                },
            },
        },
        MuiTableRow: {
            styleOverrides: {
                root: {
                    transition: "background-color 140ms ease",
                    "&:hover": { backgroundColor: "rgba(242,162,58,0.06)" },
                },
            },
        },

        MuiDialog: {
            styleOverrides: {
                paper: {
                    borderRadius: 18,
                    border: "1px solid rgba(255,255,255,0.12)",
                    backgroundImage:
                        "radial-gradient(900px 220px at 10% 0%, rgba(242,162,58,0.16), transparent 55%)",
                },
            },
        },
    },
});