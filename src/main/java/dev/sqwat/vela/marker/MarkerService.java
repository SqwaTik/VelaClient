package dev.sqwat.vela.marker;

import dev.sqwat.vela.config.JsonFileStore;
import java.nio.file.Path;
import java.util.*;

public final class MarkerService {
    public record Point(String id,String name,String world,String dimension,double x,double y,double z,int color,String icon,boolean death) {
        public Point {x=Math.floor(x);y=Math.floor(y);z=Math.floor(z);}
    }
    public static final class State {
        public boolean death,latest,limited,quick;
        public int limit=50,key=-1;
        public int nextNumber=1;
        public boolean enabled=true;
        public List<Point> points=new ArrayList<>();
    }
    private final JsonFileStore<State> store;
    private final State state;
    private java.util.function.Consumer<Point> created=p->{};
    public void onCreated(java.util.function.Consumer<Point> listener){created=Objects.requireNonNull(listener);}
    public MarkerService(Path root){store=new JsonFileStore<>(root.resolve("markers.json"),State.class,State::new);state=store.load();if(state.points==null)state.points=new ArrayList<>();state.limit=Math.clamp(Math.round(state.limit/10f)*10,10,250);}
    public State state(){return state;}
    public String nextName(){return nextName("","");}
    public String nextName(String world,String dimension){
        Set<String> names=new HashSet<>();for(var point:state.points)if(point.world().equals(world)&&point.dimension().equals(dimension))names.add(point.name());
        int number=1;while(names.contains("Метка "+number))number++;return "Метка "+number;
    }
    public void save(){try{store.save(state);}catch(java.io.IOException e){throw new IllegalStateException("Не удалось сохранить метки",e);}}
    public void remove(String id){state.points.removeIf(p->p.id().equals(id));save();}
    public void update(String id,String name,double x,double y,double z,int color,String icon){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Проверьте координаты");
        for(int i=0;i<state.points.size();i++){Point p=state.points.get(i);if(p.id().equals(id)){state.points.set(i,new Point(id,name.isBlank()?"Метка":name.strip(),p.world(),p.dimension(),x,y,z,color,icon,p.death()));save();return;}}
    }
    public void add(String name,String world,String dimension,double x,double y,double z,int color,String icon,boolean death){
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Проверьте координаты");
        if(death&&!state.death)return;
        if(death&&state.latest)state.points.removeIf(p->p.death()&&p.world().equals(world)&&p.dimension().equals(dimension));
        if(state.limited&&state.points.size()>=state.limit)throw new IllegalArgumentException("Достигнут лимит меток");
        Point point=new Point(UUID.randomUUID().toString(),name.isBlank()?"Метка":name.strip(),world,dimension,x,y,z,color,icon,death);
        state.points.add(point);save();created.accept(point);
    }
}
