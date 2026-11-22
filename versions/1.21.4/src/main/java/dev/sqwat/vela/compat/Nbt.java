package dev.sqwat.vela.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 1.21.5's NBT getters on 1.21.4, where a missing or mistyped value reads as zero or empty: {@code tag.getIntOr(key, d)}
 * becomes {@code Nbt.getIntOr(tag, key, d)}, and the getters that return an {@link Optional} keep doing so here.
 */
public final class Nbt {
    private Nbt(){}

    private static boolean number(CompoundTag t,String k){return t.get(k) instanceof NumericTag;}
    public static int getIntOr(CompoundTag t,String k,int d){return number(t,k)?t.getInt(k):d;}
    public static long getLongOr(CompoundTag t,String k,long d){return number(t,k)?t.getLong(k):d;}
    public static float getFloatOr(CompoundTag t,String k,float d){return number(t,k)?t.getFloat(k):d;}
    public static double getDoubleOr(CompoundTag t,String k,double d){return number(t,k)?t.getDouble(k):d;}
    public static byte getByteOr(CompoundTag t,String k,byte d){return number(t,k)?t.getByte(k):d;}
    public static short getShortOr(CompoundTag t,String k,short d){return number(t,k)?t.getShort(k):d;}
    public static boolean getBooleanOr(CompoundTag t,String k,boolean d){return number(t,k)?t.getBoolean(k):d;}
    public static String getStringOr(CompoundTag t,String k,String d){return t.contains(k,Tag.TAG_STRING)?t.getString(k):d;}
    public static CompoundTag getCompoundOrEmpty(CompoundTag t,String k){return t.getCompound(k);}
    public static ListTag getListOrEmpty(CompoundTag t,String k){return t.get(k) instanceof ListTag l?l:new ListTag();}

    public static float getFloatOr(ListTag l,int i,float d){return l.get(i) instanceof NumericTag n?n.getAsFloat():d;}
    public static double getDoubleOr(ListTag l,int i,double d){return l.get(i) instanceof NumericTag n?n.getAsDouble():d;}
    public static int getIntOr(ListTag l,int i,int d){return l.get(i) instanceof NumericTag n?n.getAsInt():d;}
    public static Optional<CompoundTag> getCompound(ListTag l,int i){return l.get(i) instanceof CompoundTag c?Optional.of(c):Optional.empty();}
    public static Stream<CompoundTag> compoundStream(ListTag l){return l.stream().filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast);}

    public static Optional<Integer> getInt(CompoundTag t,String k){return number(t,k)?Optional.of(t.getInt(k)):Optional.empty();}
    public static Optional<Long> getLong(CompoundTag t,String k){return number(t,k)?Optional.of(t.getLong(k)):Optional.empty();}
    public static Optional<String> getString(CompoundTag t,String k){return t.contains(k,Tag.TAG_STRING)?Optional.of(t.getString(k)):Optional.empty();}
    public static Optional<CompoundTag> getCompound(CompoundTag t,String k){return t.get(k) instanceof CompoundTag c?Optional.of(c):Optional.empty();}
    public static Optional<ListTag> getList(CompoundTag t,String k){return t.get(k) instanceof ListTag l?Optional.of(l):Optional.empty();}
    public static Optional<int[]> getIntArray(CompoundTag t,String k){return t.contains(k,Tag.TAG_INT_ARRAY)?Optional.of(t.getIntArray(k)):Optional.empty();}
    public static Optional<long[]> getLongArray(CompoundTag t,String k){return t.contains(k,Tag.TAG_LONG_ARRAY)?Optional.of(t.getLongArray(k)):Optional.empty();}
    public static Optional<byte[]> getByteArray(CompoundTag t,String k){return t.contains(k,Tag.TAG_BYTE_ARRAY)?Optional.of(t.getByteArray(k)):Optional.empty();}
}
