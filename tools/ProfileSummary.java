import jdk.jfr.consumer.*;
import java.nio.file.*;
import java.util.*;
public class ProfileSummary {
 public static void main(String[] args)throws Exception {
  Map<String,Long> allocation=new HashMap<>();
  List<RecordedEvent> pauses=new ArrayList<>(),slow=new ArrayList<>();
  try(var file=new RecordingFile(Path.of(args[0]))){while(file.hasMoreEvents()){
   var e=file.readEvent();String type=e.getEventType().getName();
   if(type.equals("jdk.GCPhasePause"))pauses.add(e);
   if(type.equals("vela.SlowFrame")&&e.getDouble("milliseconds")>16.67)slow.add(e);
   if(type.equals("jdk.ObjectAllocationSample")&&e.getStackTrace()!=null){
    StringBuilder key=new StringBuilder();int n=0;
    for(var f:e.getStackTrace().getFrames()){if(n++==8)break;key.append(f.getMethod().getType().getName()).append('.').append(f.getMethod().getName()).append(" <- ");}
    allocation.merge(key.toString(),e.getLong("weight"),Long::sum);
   }
  }}
  allocation.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(8).forEach(System.out::println);
  for(var s:slow){double ms=s.getDouble("milliseconds");var start=s.getStartTime().minusNanos((long)(ms*1e6));long overlap=pauses.stream().filter(g->g.getStartTime().isBefore(s.getStartTime())&&g.getEndTime().isAfter(start)).count();System.out.println("SLOW "+s.getStartTime()+" ms="+ms+" GC="+overlap);}
 }
}
