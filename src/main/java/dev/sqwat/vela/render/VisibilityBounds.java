package dev.sqwat.vela.render;

/** Conservative bounds include both chest halves, raised lids and shulker extensions. */
public record VisibilityBounds(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
    public static VisibilityBounds storage(int x,int y,int z){return new VisibilityBounds(x-1.25,y-.25,z-1.25,x+2.25,y+2.25,z+2.25);}
    public boolean contains(double x,double y,double z){return x>=minX&&x<=maxX&&y>=minY&&y<=maxY&&z>=minZ&&z<=maxZ;}
}
