package dev.sqwat.vela.render.perf;

import jdk.jfr.consumer.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Plain-text summary written next to each saved profile, so a stutter can be read without opening JMC. */
public final class ProfileReport {
    private static final String RENDER_THREAD="Render thread";
    private ProfileReport(){}

    public static Path write(Path jfr)throws IOException{
        Path output=jfr.resolveSibling(jfr.getFileName().toString().replace(".jfr",".txt"));
        Files.writeString(output,summarize(jfr));
        return output;
    }

    static String summarize(Path jfr)throws IOException{
        Map<String,Integer> hot=new HashMap<>(),vela=new HashMap<>();Map<String,Long> allocation=new HashMap<>();Map<String,Long> locks=new HashMap<>();
        List<long[]> pauses=new ArrayList<>(),slow=new ArrayList<>();
        int renderSamples=0,frames16=0,frames33=0,frames50=0,frames100=0;double worst=0;
        java.time.Instant first=null,last=null;
        try(var file=new RecordingFile(jfr)){
            while(file.hasMoreEvents()){
                var e=file.readEvent();String type=e.getEventType().getName();
                if(first==null||e.getStartTime().isBefore(first))first=e.getStartTime();
                if(last==null||e.getEndTime().isAfter(last))last=e.getEndTime();
                switch(type){
                    case "vela.SlowFrame"->{
                        double ms=e.getDouble("milliseconds");worst=Math.max(worst,ms);
                        if(ms>16.67)frames16++;if(ms>33.4)frames33++;if(ms>50)frames50++;if(ms>100)frames100++;
                        if(ms>33.4){long end=nanos(e.getStartTime());slow.add(new long[]{end-(long)(ms*1e6),end});}
                    }
                    case "jdk.GCPhasePause"->pauses.add(new long[]{nanos(e.getStartTime()),nanos(e.getEndTime())});
                    case "jdk.ExecutionSample"->{
                        var thread=e.getThread("sampledThread");var stack=e.getStackTrace();
                        if(thread==null||stack==null||stack.getFrames().isEmpty()||!RENDER_THREAD.equals(thread.getJavaName()))continue;
                        renderSamples++;
                        hot.merge(frame(stack.getFrames().getFirst()),1,Integer::sum);
                        for(var f:stack.getFrames()){String name=f.getMethod().getType().getName();if(name.startsWith("dev.sqwat.")){vela.merge(frame(f),1,Integer::sum);break;}}
                    }
                    case "jdk.ObjectAllocationSample"->{
                        var stack=e.getStackTrace();if(stack==null)continue;
                        StringBuilder key=new StringBuilder();int n=0;
                        for(var f:stack.getFrames()){if(n++==4)break;if(n>1)key.append(" <- ");key.append(frame(f));}
                        allocation.merge(key.toString(),e.getLong("weight"),Long::sum);
                    }
                    case "jdk.JavaMonitorEnter"->{
                        var stack=e.getStackTrace();if(stack==null||stack.getFrames().isEmpty())continue;
                        locks.merge(frame(stack.getFrames().getFirst()),e.getDuration().toNanos(),Long::sum);
                    }
                    default->{}
                }
            }
        }
        long gcTotal=0,gcMax=0;for(var p:pauses){long d=p[1]-p[0];gcTotal+=d;gcMax=Math.max(gcMax,d);}
        int slowWithGc=0;
        for(var s:slow)for(var p:pauses)if(p[0]<s[1]&&p[1]>s[0]){slowWithGc++;break;}
        var out=new StringBuilder();
        out.append("Vela profile ").append(jfr.getFileName()).append('\n');
        if(first!=null)out.append(String.format(Locale.ROOT,"Span: %.1f s%n",(nanos(last)-nanos(first))/1e9));
        out.append(String.format(Locale.ROOT,"Frames over 16.7 ms: %d, over 33 ms: %d, over 50 ms: %d, over 100 ms: %d, worst %.1f ms%n",frames16,frames33,frames50,frames100,worst));
        out.append(String.format(Locale.ROOT,"GC pauses: %d, total %.1f ms, longest %.1f ms; frames over 33 ms during a GC pause: %d of %d%n",pauses.size(),gcTotal/1e6,gcMax/1e6,slowWithGc,slow.size()));
        out.append("\nRender thread, hottest methods (").append(renderSamples).append(" samples)\n");
        top(out,hot,15,renderSamples);
        out.append("\nRender thread time inside Vela code (innermost Vela method)\n");
        top(out,vela,12,renderSamples);
        out.append("\nAllocation, MiB by call site\n");
        allocation.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(10)
            .forEach(e->out.append(String.format(Locale.ROOT,"  %8.1f  %s%n",e.getValue()/1048576.0,e.getKey())));
        if(!locks.isEmpty()){
            out.append("\nLock waits over 2 ms, total ms\n");
            locks.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(6)
                .forEach(e->out.append(String.format(Locale.ROOT,"  %8.1f  %s%n",e.getValue()/1e6,e.getKey())));
        }
        return out.toString();
    }
    private static void top(StringBuilder out,Map<String,Integer> counts,int limit,int total){
        counts.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed()).limit(limit)
            .forEach(e->out.append(String.format(Locale.ROOT,"  %5.1f%%  %s%n",total==0?0:100.0*e.getValue()/total,e.getKey())));
    }
    private static String frame(RecordedFrame f){var m=f.getMethod();String type=m.getType().getName();return type.substring(type.lastIndexOf('.')+1)+"."+m.getName();}
    private static long nanos(java.time.Instant t){return t.getEpochSecond()*1_000_000_000L+t.getNano();}
}
