package dev.sqwat.vela.module.setting;

import java.util.List;
import java.util.function.Supplier;

/**
 * A choice whose options come from a supplier (for example files in a folder). The list is never empty: when the
 * supplier has nothing, {@code placeholder} is the only option. A saved value missing from the current list is kept,
 * so a temporarily absent file does not reset the user's selection.
 */
public final class DynamicChoiceSetting extends ChoiceSetting {
    private final Supplier<List<String>> source;
    private final String placeholder;
    public DynamicChoiceSetting(String id,String name,String description,String placeholder,Supplier<List<String>> source){
        super(id,name,description,placeholder,List.of(placeholder));
        this.source=source;this.placeholder=placeholder;
    }
    @Override public List<String> options(){
        var current=source.get();
        return current.isEmpty()?List.of(placeholder):current;
    }
    @Override protected String validate(String next){
        if(next==null||next.isBlank())throw new IllegalArgumentException("Empty choice");
        return next;
    }
    /** The selected value if it is an existing option other than the placeholder, otherwise the first real option or null. */
    public String selected(){
        var current=source.get();
        if(current.isEmpty())return null;
        return current.contains(value())?value():current.get(0);
    }
}
