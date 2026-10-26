import jdk.jfr.consumer.*;
import java.nio.file.*;
import java.util.*;

/** Read-only correlation of sampled render stacks and blocking events with slow intervals. */
public class SlowFrameDetails {
 public static void main(String[] args) throws Exception {
  var frames=new ArrayList<RecordedEvent>();
  try(var file=new RecordingFile(Path.of(args[0]))) { while(file.hasMoreEvents()) {
   var e=file.readEvent();
   if(e.getEventType().getName().equals("vela.SlowFrame")&&e.getDouble("milliseconds")>16.67)frames.add(e);
  }}
  for(var frame:frames) {
   var end=frame.getStartTime(); var start=end.minusNanos((long)(frame.getDouble("milliseconds")*1e6));
   System.out.println("FRAME "+start+" .. "+end+" "+frame.getDouble("milliseconds")+"ms");
   try(var file=new RecordingFile(Path.of(args[0]))) { while(file.hasMoreEvents()) {
    var e=file.readEvent(); String name=e.getEventType().getName();
    if(!Set.of("jdk.ExecutionSample","jdk.NativeMethodSample","jdk.SafepointBegin","jdk.SafepointEnd","jdk.JavaMonitorEnter","jdk.ThreadPark","jdk.GCPhasePause").contains(name))continue;
    if(e.getEndTime().isBefore(start)||e.getStartTime().isAfter(end))continue;
    var thread=name.equals("jdk.ExecutionSample")||name.equals("jdk.NativeMethodSample")?e.getThread("sampledThread"):e.getThread();
    if(!name.startsWith("jdk.Safepoint")&&thread!=null&&!thread.getJavaName().equals("Render thread"))continue;
    System.out.println("  "+name+" "+e.getStartTime()+" duration="+e.getDuration());
    if(e.getStackTrace()!=null) e.getStackTrace().getFrames().stream().limit(12).forEach(f->System.out.println("    "+f.getMethod().getType().getName()+"."+f.getMethod().getName()));
   }}
  }
 }
}
