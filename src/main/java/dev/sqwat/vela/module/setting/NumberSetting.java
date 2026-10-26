package dev.sqwat.vela.module.setting;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String id, String name, String description, double value, double min, double max, double step) {
        super(id, name, description, value);
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(step) || min > max || step <= 0)
            throw new IllegalArgumentException("Invalid number range");
        this.min = min;
        this.max = max;
        this.step = step;
        set(value);
    }

    @Override protected Double validate(Double next) {
        if (!Double.isFinite(next)) throw new IllegalArgumentException("Value must be finite");
        double clamped = Math.max(min, Math.min(max, next));
        double snapped = min + Math.round((clamped - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }

    public double fraction() { return max == min ? 0 : (value() - min) / (max - min); }
    public void setFraction(double fraction) { set(min + Math.max(0, Math.min(1, fraction)) * (max - min)); }
}
