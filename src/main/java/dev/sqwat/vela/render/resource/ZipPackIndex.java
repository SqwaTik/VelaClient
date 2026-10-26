package dev.sqwat.vela.render.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Sorted entry index of a zipped resource pack. Vanilla walks every ZIP entry for each listResources and
 * getNamespaces call, which for a large server pack means thousands of full scans per reload; with the index each
 * lookup is a binary search over the prefix range. Held weakly per open ZipFile, so a closed pack frees it.
 */
public final class ZipPackIndex {
    private static final Map<ZipFile,ZipPackIndex> INDEXES=Collections.synchronizedMap(new WeakHashMap<>());
    private final String[] names;
    private final ZipEntry[] entries;
    private final Map<String,Set<String>> namespaces=new ConcurrentHashMap<>();

    ZipPackIndex(String[] sortedNames,ZipEntry[] entries){this.names=sortedNames;this.entries=entries;}

    public static ZipPackIndex of(ZipFile zip){
        synchronized(INDEXES){
            var index=INDEXES.get(zip);
            if(index==null){index=build(zip);INDEXES.put(zip,index);}
            return index;
        }
    }
    private static ZipPackIndex build(ZipFile zip){
        var list=new ArrayList<ZipEntry>(zip.size());
        for(var e=zip.entries();e.hasMoreElements();){var entry=e.nextElement();if(!entry.isDirectory())list.add(entry);}
        list.sort((a,b)->a.getName().compareTo(b.getName()));
        String[] names=new String[list.size()];
        for(int i=0;i<names.length;i++)names[i]=list.get(i).getName();
        return new ZipPackIndex(names,list.toArray(ZipEntry[]::new));
    }

    /** Every file whose name starts with {@code prefix}, in name order. */
    public void forEachWithPrefix(String prefix,BiConsumer<String,ZipEntry> action){
        for(int i=firstAtOrAfter(prefix);i<names.length&&names[i].startsWith(prefix);i++)action.accept(names[i],entries[i]);
    }
    int firstAtOrAfter(String prefix){int i=Arrays.binarySearch(names,prefix);return i<0?-i-1:i;}

    /**
     * Namespaces directly under {@code root} ("assets/"): the path segment after it, as vanilla's extractNamespace
     * reads it. Validity filtering stays with the caller.
     */
    public Set<String> namespaces(String root){
        return namespaces.computeIfAbsent(root,key->{
            var result=new HashSet<String>();
            for(int i=firstAtOrAfter(key);i<names.length&&names[i].startsWith(key);i++){
                String name=names[i];int slash=name.indexOf('/',key.length());
                String namespace=slash<0?name.substring(key.length()):name.substring(key.length(),slash);
                if(!namespace.isEmpty())result.add(namespace);
                // Skip the rest of this namespace in one search.
                if(slash>=0){int next=firstAtOrAfter(name.substring(0,slash)+(char)('/'+1));i=next-1;}
            }
            return Set.copyOf(result);
        });
    }
}
