package dev.sqwat.vela.render.entity;

/**
 * Voxel line-of-sight between the camera and an entity box. Pure logic: no world, entity or GPU state.
 * Only full opaque cubes occlude, and every uncertain case (step limit, empty box) counts as visible.
 */
public final class OcclusionRay {
    @FunctionalInterface public interface Blocks {boolean opaque(int x,int y,int z);}
    private OcclusionRay(){}

    /** True when the centre or any corner of the (slightly shrunk) box is reachable from the eye. */
    public static boolean visible(Blocks blocks,double ex,double ey,double ez,
                                  double minX,double minY,double minZ,double maxX,double maxY,double maxZ){
        // Shrink so points on a face that touches a wall (paintings, frames) lie inside the entity's own voxel.
        double sx=Math.min(.05,(maxX-minX)/4),sy=Math.min(.05,(maxY-minY)/4),sz=Math.min(.05,(maxZ-minZ)/4);
        minX+=sx;maxX-=sx;minY+=sy;maxY-=sy;minZ+=sz;maxZ-=sz;
        if(clear(blocks,ex,ey,ez,(minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2))return true;
        for(int corner=0;corner<8;corner++){
            if(clear(blocks,ex,ey,ez,(corner&1)==0?minX:maxX,(corner&2)==0?minY:maxY,(corner&4)==0?minZ:maxZ))return true;
        }
        return false;
    }

    /** Amanatides–Woo traversal. The start and end voxels never occlude. */
    static boolean clear(Blocks blocks,double ax,double ay,double az,double bx,double by,double bz){
        int x=floor(ax),y=floor(ay),z=floor(az);
        final int tx=floor(bx),ty=floor(by),tz=floor(bz);
        double dx=bx-ax,dy=by-ay,dz=bz-az;
        int stepX=dx>0?1:dx<0?-1:0,stepY=dy>0?1:dy<0?-1:0,stepZ=dz>0?1:dz<0?-1:0;
        double deltaX=stepX==0?Double.POSITIVE_INFINITY:Math.abs(1/dx);
        double deltaY=stepY==0?Double.POSITIVE_INFINITY:Math.abs(1/dy);
        double deltaZ=stepZ==0?Double.POSITIVE_INFINITY:Math.abs(1/dz);
        double maxX=stepX>0?(x+1-ax)*deltaX:stepX<0?(ax-x)*deltaX:Double.POSITIVE_INFINITY;
        double maxY=stepY>0?(y+1-ay)*deltaY:stepY<0?(ay-y)*deltaY:Double.POSITIVE_INFINITY;
        double maxZ=stepZ>0?(z+1-az)*deltaZ:stepZ<0?(az-z)*deltaZ:Double.POSITIVE_INFINITY;
        int limit=Math.abs(tx-x)+Math.abs(ty-y)+Math.abs(tz-z);
        for(int i=0;i<limit;i++){
            if(maxX<maxY&&maxX<maxZ){x+=stepX;maxX+=deltaX;}
            else if(maxY<maxZ){y+=stepY;maxY+=deltaY;}
            else{z+=stepZ;maxZ+=deltaZ;}
            if(x==tx&&y==ty&&z==tz)return true;
            if(blocks.opaque(x,y,z))return false;
        }
        return true;
    }
    private static int floor(double value){int i=(int)value;return value<i?i-1:i;}
}
