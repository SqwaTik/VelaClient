package dev.sqwat.vela.module.setting;

import dev.sqwat.vela.ui.Lang;
import java.util.Locale;

/**
 * Value labels with units, read from what the setting already says about itself ("…, мс", "в секундах", "в блоках",
 * "%"…), so "60" becomes "1 мин", "250" becomes "250 мс". Durations of a minute or more are shown in minutes.
 * A setting can name its unit explicitly with {@link NumberSetting#unit(Unit)}.
 */
public final class NumberUnits {
    public enum Unit{NONE,MILLIS,SECONDS,PERCENT,BLOCKS,CHUNKS,DEGREES,DEGREES_PER_SECOND,HEALTH,MESSAGES,HOURS}
    private NumberUnits(){}

    public static String format(NumberSetting setting){return format(setting.value(),setting.unit());}
    public static String format(double value,Unit unit){
        String n=number(value);
        return switch(unit){
            case NONE->n;
            case MILLIS->n+Lang.pick(" ms"," мс");
            case SECONDS->seconds(value);
            case PERCENT->n+"%";
            case BLOCKS->n+Lang.pick(" bl."," бл.");
            case CHUNKS->n+Lang.pick(" ch."," чанк.");
            case DEGREES->n+"°";
            case DEGREES_PER_SECOND->n+Lang.pick("°/s","°/с");
            case HEALTH->n+" HP";
            case MESSAGES->n+Lang.pick(" msgs"," сообщ.");
            case HOURS->String.format(Locale.ROOT,"%d:%02d",(int)Math.floor(value)%24,Math.round((value-Math.floor(value))*60)%60);
        };
    }
    /** "45 с", "1 мин", "1 мин 30 с", "10 мин"; fractions below a minute keep one decimal ("0.8 с"). */
    static String seconds(double value){
        String s=Lang.pick(" s"," с"),min=Lang.pick(" min"," мин");
        if(value<60)return number(value)+s;
        long total=Math.round(value),minutes=total/60,rest=total%60;
        return minutes+min+(rest==0?"":" "+rest+s);
    }
    static String number(double value){
        return value==Math.rint(value)?Long.toString(Math.round(value)):String.format(Locale.ROOT,Math.abs(value*10-Math.rint(value*10))<1e-6?"%.1f":"%.2f",value);
    }

    /** Unit inferred from the setting's name and description. */
    static Unit infer(String name,String description){
        String text=(name+" "+description).toLowerCase(Locale.ROOT);
        if(text.contains("мс")||text.contains("миллисекунд"))return Unit.MILLIS; // not UI
        if(text.contains("градусов в секунду"))return Unit.DEGREES_PER_SECOND; // not UI
        if(text.contains("секунд")||name.endsWith(", с"))return Unit.SECONDS; // not UI
        if(text.contains("%")||text.contains("процент"))return Unit.PERCENT; // not UI
        if(text.contains("чанк"))return Unit.CHUNKS; // not UI
        if(text.contains("блок"))return Unit.BLOCKS; // not UI
        if(text.contains(" hp"))return Unit.HEALTH; // not UI
        if(text.contains("сообщени"))return Unit.MESSAGES; // not UI
        if(text.contains("24-часов"))return Unit.HOURS; // not UI
        if(text.contains("поворот")||text.contains("вращение"))return Unit.DEGREES; // not UI
        return Unit.NONE;
    }
}
