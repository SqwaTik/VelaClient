// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
package dev.sqwat.vela.zako.particle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

/** Frame barriers join completed tasks instead of consuming CPU in spin loops. */
public final class Workers {
    private static final int COUNT=Math.max(1,Math.min(8,Runtime.getRuntime().availableProcessors()-2));
    private static ExecutorService pool;
    private static final AtomicInteger IDS=new AtomicInteger();
    private Workers(){}
    private static synchronized ExecutorService pool(){
        if(pool==null)pool=Executors.newFixedThreadPool(COUNT,r->{var t=new Thread(r,"vela-zako-worker-"+IDS.incrementAndGet());t.setDaemon(true);return t;});
        return pool;
    }
    public static synchronized void shutdown(){if(pool!=null){pool.shutdown();pool=null;}}
    public static Throwable run(int blocks,IntConsumer task){
        if(blocks<=0)return null;
        var error=new AtomicReference<Throwable>();var next=new AtomicInteger();
        Runnable worker=()->{for(int i;(i=next.getAndIncrement())<blocks;)try{if(error.get()==null)task.accept(i);}catch(Throwable t){error.compareAndSet(null,t);}};
        var futures=new ArrayList<Future<?>>();
        try{for(int n=0;n<Math.min(COUNT,blocks-1);n++)futures.add(pool().submit(worker));}
        catch(RuntimeException failure){error.compareAndSet(null,failure);}
        worker.run();join(futures,error);return error.get();
    }
    public static Stream stream(IntConsumer task){return new Stream(task);}
    private static void join(List<Future<?>> futures,AtomicReference<Throwable> error){
        boolean interrupted=false;
        for(var f:futures){boolean done=false;while(!done)try{f.get();done=true;}catch(InterruptedException e){interrupted=true;error.compareAndSet(null,e);}catch(ExecutionException e){error.compareAndSet(null,e.getCause());done=true;}}
        if(interrupted)Thread.currentThread().interrupt();
    }
    public static final class Stream {
        private static final int BATCH=8;
        private final IntConsumer task;
        private final List<Future<?>> futures=new ArrayList<>();
        private final AtomicReference<Throwable> error=new AtomicReference<>();
        private int published;
        private int submitted;
        private boolean finished;
        private Stream(IntConsumer task){this.task=task;}
        public void publish(int count){
            if(finished||count<published)throw new IllegalStateException("Invalid model batch publication");
            published=count;
            while(published-submitted>=BATCH)submit(submitted+BATCH);
        }
        private void submit(int end){
            int start=submitted;submitted=end;
            try{futures.add(pool().submit(()->{for(int i=start;i<end&&error.get()==null;i++)try{task.accept(i);}catch(Throwable t){error.compareAndSet(null,t);}}));}
            catch(RuntimeException failure){error.compareAndSet(null,failure);}
        }
        public Throwable finish(int count){if(count!=published)publish(count);if(submitted<published)submit(published);finished=true;join(futures,error);return error.get();}
        public Throwable await(int count){publish(Math.max(count,published));if(submitted<published)submit(published);join(futures,error);return error.get();}
        public int submittedTasks(){return futures.size();}
    }
}
