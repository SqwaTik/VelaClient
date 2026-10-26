package dev.sqwat.vela.ui.render;

public final class VelaTheme {
    private VelaTheme() {}

    public static final int OVERLAY = 0xB507090D;
    public static final int WINDOW = 0xFF10141C;
    public static final int SIDEBAR = 0xFF0B0E14;
    public static final int SURFACE = 0xFF141923;
    public static final int SURFACE_HOVER = 0xFF232B3D;
    public static final int SURFACE_DEEP = 0xFF0C1017;
    public static final int BORDER = 0xFF252D3D;
    public static final int BORDER_HOVER = 0xFF39445B;
    public static volatile int PRIMARY = 0xFF756CFF;
    public static volatile int PRIMARY_SOFT = 0x1F756CFF;
    public static final int CYAN = 0xFF55D5FF;
    public static final int POSITIVE = 0xFF4DE3AE;
    public static final int DANGER = 0xFFFF6178;
    public static final int TEXT = 0xFFF3F5FA;
    public static final int TEXT_SECONDARY = 0xFFADB8CF;
    public static final int TEXT_MUTED = 0xFF8996B0;

    public static int mix(int from, int to, float amount) {
        float t = Math.max(0, Math.min(1, amount));
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = Math.round(((from >>> 16) & 255) + (((to >>> 16) & 255) - ((from >>> 16) & 255)) * t);
        int g = Math.round(((from >>> 8) & 255) + (((to >>> 8) & 255) - ((from >>> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void setPrimary(int rgb) {
        PRIMARY = 0xFF000000 | (rgb & 0x00FFFFFF);
        PRIMARY_SOFT = 0x1F000000 | (rgb & 0x00FFFFFF);
    }

    /** Readable foreground for user-selected accents, including white and yellow. */
    public static int onPrimary() { return contrastText(PRIMARY); }
    public static int readableAccentFill(){
        int color=PRIMARY;
        for(int i=0;i<16&&luminance(color)>.18;i++)color=mix(color,0xFF10141C,.08f);
        return color;
    }
    private static double luminance(int color){return .2126*linear((color>>16)&255)+.7152*linear((color>>8)&255)+.0722*linear(color&255);}
    public static int contrastText(int color) {
        double l = .2126 * linear((color >> 16) & 255) + .7152 * linear((color >> 8) & 255) + .0722 * linear(color & 255);
        return (l + .05) / .05 >= 1.05 / (l + .05) ? 0xFF10141C : 0xFFF3F5FA;
    }
    private static double linear(int channel) { double v=channel/255.0; return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4); }

    public static int parseHex(String hex) {
        if (hex == null) return 0x7D72FF;
        String value = hex.strip().replace("#", "");
        if (value.length() != 6) return 0x7D72FF;
        try { return Integer.parseInt(value, 16); } catch (NumberFormatException ignored) { return 0x7D72FF; }
    }
}
