package dev.sqwat.vela.render;
import java.util.IdentityHashMap;
public final class FriendNameTags {
    private static final IdentityHashMap<Object,Boolean> FRIENDS=new IdentityHashMap<>();
    private static final java.util.ArrayDeque<Object> ORDER=new java.util.ArrayDeque<>();
    private FriendNameTags(){}
    public static synchronized void mark(Object text,boolean friend){if(text==null)return;if(!friend){if(FRIENDS.remove(text)!=null)ORDER.removeIf(old->old==text);return;}if(FRIENDS.containsKey(text))return;while(ORDER.size()>=512)FRIENDS.remove(ORDER.removeFirst());ORDER.addLast(text);FRIENDS.put(text,true);}
    public static synchronized int background(Object text,int original){return original!=0&&FRIENDS.containsKey(text)?0x506FD92B:original;}
}
