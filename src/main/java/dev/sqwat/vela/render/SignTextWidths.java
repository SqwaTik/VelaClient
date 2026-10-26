package dev.sqwat.vela.render;

import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;
import net.minecraft.util.FormattedCharSequence;

/** Only for immutable cached render messages supplied by SignText, not arbitrary chat sequences. */
public final class SignTextWidths {
    private static final int LIMIT=2048;
    private static final Reference2IntLinkedOpenHashMap<FormattedCharSequence> WIDTHS=new Reference2IntLinkedOpenHashMap<>();
    private static Object font;
    static {WIDTHS.defaultReturnValue(-1);}
    private SignTextWidths(){}
    public static int get(Object owner,FormattedCharSequence line){
        if(font!=owner){clear();font=owner;}
        return WIDTHS.getAndMoveToLast(line);
    }
    public static void put(FormattedCharSequence line,int width){
        WIDTHS.putAndMoveToLast(line,width);
        if(WIDTHS.size()>LIMIT)WIDTHS.removeFirstInt();
    }
    public static void clear(){WIDTHS.clear();font=null;}
}
