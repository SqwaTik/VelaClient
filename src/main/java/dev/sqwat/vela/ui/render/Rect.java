package dev.sqwat.vela.ui.render;

public record Rect(int x, int y, int width, int height) {
    public boolean contains(double px, double py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
}
