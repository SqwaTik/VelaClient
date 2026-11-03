package dev.sqwat.vela.render.perf;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import java.util.ArrayList;

/**
 * A GUI layer's element list that also answers "does anything here overlap this rectangle" without scanning every
 * element. Vanilla asks that for each new element against every layer, which is quadratic: a full tab list or a busy
 * HUD spent most of its GUI time there. Answers are exactly those of {@link ScreenRectangle#intersects}, zero-size
 * rectangles included.
 */
public final class IndexedAreaList<E extends ScreenArea> extends ArrayList<E> {
    static final int LINEAR=32,CELL_SHIFT=5,MAX_CELLS=64;
    private final IntArrayList rects=new IntArrayList();
    private Long2ObjectOpenHashMap<IntArrayList> cells;
    private IntArrayList large;

    @Override public boolean add(E element){
        super.add(element);
        ScreenRectangle bounds=element.bounds();
        if(bounds!=null)index(bounds.left(),bounds.top(),bounds.right(),bounds.bottom());
        return true;
    }

    void index(int left,int top,int right,int bottom){
        int id=rects.size()/4;
        rects.add(left);rects.add(top);rects.add(right);rects.add(bottom);
        if(cells!=null)place(id);
        else if(id+1>=LINEAR){cells=new Long2ObjectOpenHashMap<>();large=new IntArrayList();for(int i=0;i<=id;i++)place(i);}
    }

    private void place(int id){
        int left=rects.getInt(id*4),top=rects.getInt(id*4+1),right=rects.getInt(id*4+2),bottom=rects.getInt(id*4+3);
        // Vanilla's test lets a zero-size rectangle overlap its neighbours; those few are always checked directly.
        if(right<=left||bottom<=top){large.add(id);return;}
        int l=left>>CELL_SHIFT,t=top>>CELL_SHIFT,r=(right-1)>>CELL_SHIFT,b=(bottom-1)>>CELL_SHIFT;
        if((long)(r-l+1)*(b-t+1)>MAX_CELLS){large.add(id);return;}
        for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)cells.computeIfAbsent(key(x,y),k->new IntArrayList(4)).add(id);
    }

    public boolean intersects(ScreenRectangle bounds){return intersects(bounds.left(),bounds.top(),bounds.right(),bounds.bottom());}

    boolean intersects(int left,int top,int right,int bottom){
        if(cells==null||right<=left||bottom<=top)return linear(left,top,right,bottom);
        int l=left>>CELL_SHIFT,t=top>>CELL_SHIFT,r=(right-1)>>CELL_SHIFT,b=(bottom-1)>>CELL_SHIFT;
        if((long)(r-l+1)*(b-t+1)>MAX_CELLS)return linear(left,top,right,bottom);
        for(int i=0,n=large.size();i<n;i++)if(hits(large.getInt(i),left,top,right,bottom))return true;
        for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){
            IntArrayList ids=cells.get(key(x,y));
            if(ids!=null)for(int i=0,n=ids.size();i<n;i++)if(hits(ids.getInt(i),left,top,right,bottom))return true;
        }
        return false;
    }

    private boolean linear(int left,int top,int right,int bottom){
        for(int i=0,n=rects.size()/4;i<n;i++)if(hits(i,left,top,right,bottom))return true;
        return false;
    }
    private boolean hits(int id,int left,int top,int right,int bottom){
        int o=id*4;
        return rects.getInt(o)<right&&rects.getInt(o+2)>left&&rects.getInt(o+1)<bottom&&rects.getInt(o+3)>top;
    }
    private static long key(int x,int y){return (long)x<<32|(y&0xffffffffL);}
}
