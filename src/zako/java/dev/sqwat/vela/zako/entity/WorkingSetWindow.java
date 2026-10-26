package dev.sqwat.vela.zako.entity;

/** Peak pages over a short window, so large scenes do not permanently pin native memory. */
public final class WorkingSetWindow {
    private long since=Long.MIN_VALUE;
    private int peak;
    public void used(int pages){peak=Math.max(peak,pages);}
    public int retainedPages(long now){
        if(since==Long.MIN_VALUE){since=now;return -1;}
        if(now-since<5_000_000_000L)return -1;
        int keep=peak;peak=0;since=now;return keep;
    }
    public void clear(){since=Long.MIN_VALUE;peak=0;}
    public static int capacityAfterWindow(int capacity,int used){
        long target=Math.max(1L<<20,((long)used+(1L<<20)-1)/(1L<<20)*(1L<<20));
        return target<=capacity/2L?(int)target:capacity;
    }
}
