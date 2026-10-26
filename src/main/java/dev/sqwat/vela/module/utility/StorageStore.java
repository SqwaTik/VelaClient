package dev.sqwat.vela.module.utility;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Remembered container contents for one server. Per container only item totals are kept (no slot layout), and the
 * file is a gzip'd binary with a string table, so even thousands of chests stay in the low hundreds of kilobytes.
 * Not thread-safe; the owning module copies before saving off-thread.
 */
public final class StorageStore {
    public static final int MAX_CONTAINERS=2000;
    private static final int MAGIC=0x56534931; // "VSI1"
    public enum Kind{CHEST,BARREL,SHULKER,ENDER,OTHER}
    public record Key(String dimension,int x,int y,int z){}
    /** {@code nested}: inside a shulker box stored in this container. {@code name}: custom name or empty. */
    public record Stack(String item,String name,int count,boolean nested){}
    public record Container(Key key,Kind kind,long seenAt,List<Stack> items){
        public int total(String item){int n=0;for(var s:items)if(s.item.equals(item))n+=s.count;return n;}
    }
    /** One searched item across containers, most first. */
    public record Hit(String item,String name,int total,List<Container> containers){}

    private final Map<Key,Container> containers=new LinkedHashMap<>();
    private boolean dirty;

    public void put(Container container){
        containers.remove(container.key);
        containers.put(container.key,container);
        while(containers.size()>MAX_CONTAINERS)containers.remove(oldest());
        dirty=true;
    }
    private Key oldest(){Key best=null;long at=Long.MAX_VALUE;for(var c:containers.values())if(c.seenAt<at){at=c.seenAt;best=c.key;}return best;}
    public boolean remove(Key key){boolean removed=containers.remove(key)!=null;dirty|=removed;return removed;}
    public Container get(Key key){return containers.get(key);}
    public Collection<Container> all(){return Collections.unmodifiableCollection(containers.values());}
    public int size(){return containers.size();}
    public boolean dirty(){return dirty;}
    public void clean(){dirty=false;}
    public StorageStore copy(){var c=new StorageStore();c.containers.putAll(containers);return c;}

    /**
     * Items whose id or name contains {@code query} (empty: everything), merged across containers. {@code nameOf}
     * resolves an item id to the client's display name so searching works in the game's language.
     */
    public List<Hit> search(String query,java.util.function.Function<String,String> nameOf){
        String needle=query==null?"":query.strip().toLowerCase(Locale.ROOT);
        Map<String,int[]> totals=new HashMap<>();Map<String,Set<Container>> where=new HashMap<>();Map<String,String> names=new HashMap<>();
        for(var c:containers.values())for(var s:c.items){
            String key=s.item+"\u0000"+s.name;
            String shown=s.name.isEmpty()?nameOf.apply(s.item):s.name;
            if(!needle.isEmpty()&&!shown.toLowerCase(Locale.ROOT).contains(needle)&&!s.item.toLowerCase(Locale.ROOT).replace('_',' ').contains(needle.replace('_',' ')))continue;
            totals.computeIfAbsent(key,k->new int[1])[0]+=s.count;
            where.computeIfAbsent(key,k->new LinkedHashSet<>()).add(c);names.put(key,shown);
        }
        var hits=new ArrayList<Hit>();
        for(var e:totals.entrySet()){
            String item=e.getKey().substring(0,e.getKey().indexOf('\u0000'));
            var list=new ArrayList<>(where.get(e.getKey()));list.sort(Comparator.comparingLong((Container c)->-c.seenAt));
            hits.add(new Hit(item,names.get(e.getKey()),e.getValue()[0],List.copyOf(list)));
        }
        hits.sort(Comparator.comparingInt((Hit h)->-h.total).thenComparing(Hit::name));
        return hits;
    }

    public void save(Path file)throws IOException{
        Files.createDirectories(file.getParent());
        Path temp=file.resolveSibling(file.getFileName()+".tmp");
        var strings=new LinkedHashMap<String,Integer>();
        for(var c:containers.values()){strings.putIfAbsent(c.key.dimension,strings.size());for(var s:c.items){strings.putIfAbsent(s.item,strings.size());strings.putIfAbsent(s.name,strings.size());}}
        try(var out=new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(temp))))){
            out.writeInt(MAGIC);
            out.writeInt(strings.size());for(String s:strings.keySet())out.writeUTF(s);
            out.writeInt(containers.size());
            for(var c:containers.values()){
                varint(out,strings.get(c.key.dimension));out.writeInt(c.key.x);out.writeShort(c.key.y);out.writeInt(c.key.z);
                out.writeByte(c.kind.ordinal());out.writeLong(c.seenAt);
                varint(out,c.items.size());
                for(var s:c.items){varint(out,strings.get(s.item));varint(out,strings.get(s.name));varint(out,s.count);out.writeBoolean(s.nested);}
            }
        }
        Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    public static StorageStore load(Path file)throws IOException{
        var store=new StorageStore();
        if(!Files.isRegularFile(file))return store;
        try(var in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file))))){
            if(in.readInt()!=MAGIC)return store;
            String[] strings=new String[in.readInt()];
            for(int i=0;i<strings.length;i++)strings[i]=in.readUTF();
            int count=in.readInt();var kinds=Kind.values();
            for(int i=0;i<count;i++){
                var key=new Key(strings[varint(in)],in.readInt(),in.readShort(),in.readInt());
                var kind=kinds[Math.min(kinds.length-1,in.readByte()&0xFF)];long seen=in.readLong();
                int n=varint(in);var items=new ArrayList<Stack>(n);
                for(int j=0;j<n;j++)items.add(new Stack(strings[varint(in)],strings[varint(in)],varint(in),in.readBoolean()));
                store.containers.put(key,new Container(key,kind,seen,List.copyOf(items)));
            }
        }catch(EOFException|IndexOutOfBoundsException corrupt){return new StorageStore();}
        return store;
    }
    private static void varint(DataOutputStream out,int v)throws IOException{while((v&~0x7F)!=0){out.writeByte((v&0x7F)|0x80);v>>>=7;}out.writeByte(v);}
    private static int varint(DataInputStream in)throws IOException{int v=0,shift=0,b;do{b=in.readByte();v|=(b&0x7F)<<shift;shift+=7;}while((b&0x80)!=0&&shift<35);return v;}
}
