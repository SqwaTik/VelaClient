package dev.sqwat.vela.compat.input;

/** 1.21.9's typed-character record; 1.21.5's charTyped takes (char, modifiers), turned into one of these (see manual.txt). */
public record CharacterEvent(int codepoint,int modifiers) {
    public CharacterEvent(char character,int modifiers){this((int)character,modifiers);}
    public String codepointAsString(){return Character.toString(codepoint);}
    public boolean isAllowedChatCharacter(){return net.minecraft.util.StringUtil.isAllowedChatCharacter((char)codepoint);}
}
