import jdk.jfr.consumer.*;
import java.nio.file.Path;
import java.util.*;

/** Offline allocation attribution; prints no account or event payload data. */
class AllocationSites {
    public static void main(String[] args) throws Exception {
        Map<String,Long> totals=new HashMap<>();
        try(var recording=new RecordingFile(Path.of(args[0]))){
            while(recording.hasMoreEvents()){
                var event=recording.readEvent();
                if(!event.getEventType().getName().equals("jdk.ObjectAllocationSample")||event.getStackTrace()==null)continue;
                StringBuilder key=new StringBuilder(event.getThread().getJavaName()).append('\n');
                for(var frame:event.getStackTrace().getFrames()){
                    var method=frame.getMethod();
                    key.append(method.getType().getName()).append('.').append(method.getName()).append('\n');
                }
                totals.merge(key.toString(),event.getLong("weight"),Long::sum);
            }
        }
        totals.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(8)
            .forEach(e->System.out.println("Estimated bytes: "+e.getValue()+"\n"+e.getKey()));
    }
}
