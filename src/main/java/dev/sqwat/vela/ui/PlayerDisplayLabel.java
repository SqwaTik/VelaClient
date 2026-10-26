package dev.sqwat.vela.ui;

/** Plain bitmap labels cannot interpret legacy formatting or server font icons. */
public final class PlayerDisplayLabel {
    private PlayerDisplayLabel(){}
    public static String clean(String value){
        if(value==null)return "";
        return value.replaceAll("(?i)§[0-9a-fk-orx]","")
            .replaceAll("[\\p{Co}●•○◉⬤]","").replaceAll("\\s+"," ").strip();
    }
}
