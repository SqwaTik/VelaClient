package dev.sqwat.vela.module.setting;

import java.util.List;

public class ChoiceSetting extends Setting<String> {
    private final List<String> options;

    public ChoiceSetting(String id, String name, String description, String value, List<String> options) {
        super(id, name, description, value);
        this.options = List.copyOf(options);
        if (this.options.isEmpty() || !this.options.contains(value)) throw new IllegalArgumentException("Invalid choice");
    }

    @Override protected String validate(String next) {
        if (!options.contains(next)) throw new IllegalArgumentException("Unknown choice: " + next);
        return next;
    }

    public List<String> options() { return options; }
}
