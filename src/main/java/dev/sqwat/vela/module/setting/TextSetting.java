package dev.sqwat.vela.module.setting;

/** Single-line, bounded display text. Never interpreted as formatting or commands. */
public final class TextSetting extends Setting<String> {
    public TextSetting(String id,String name,String description,String initial){super(id,name,description,initial);}
    @Override protected String validate(String value){
        StringBuilder out=new StringBuilder();
        value.strip().codePoints().filter(c->!Character.isISOControl(c)&&c!=0xA7&&Character.getType(c)!=Character.FORMAT)
            .limit(24).forEach(out::appendCodePoint);
        return out.isEmpty()?defaultValue():out.toString();
    }
}
