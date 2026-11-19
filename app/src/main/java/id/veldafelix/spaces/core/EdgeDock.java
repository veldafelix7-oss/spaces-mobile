package id.veldafelix.spaces.core;

/** Pure geometry shared by the overlay and JVM tests. Coordinates are screen-local pixels. */
public final class EdgeDock {
    public static final String LEFT="left", RIGHT="right", TOP="top";
    private EdgeDock() {}
    public static String edge(String value) { return LEFT.equals(value)||TOP.equals(value)?value:RIGHT; }
    public static float fraction(float value) {
        return Float.isNaN(value)||Float.isInfinite(value)?.2f:Math.max(0f,Math.min(1f,value));
    }
    public static final class Placement {
        public final String edge;
        public final float fraction;
        public final int x,y,width,height;
        Placement(String edge,float fraction,int x,int y,int width,int height) {
            this.edge=edge;this.fraction=fraction;this.x=x;this.y=y;this.width=width;this.height=height;
        }
    }
    public static Placement place(String requested,float offset,int screenWidth,int screenHeight,int thickness,int length) {
        return place(requested,offset,screenWidth,screenHeight,thickness,length,0);
    }
    /** Nearest top origin with a usable chevron center, without covering a visible bar/cutout. */
    public static int topOrigin(int visibleStatus,int cutout,int mandatory,int visibleThickness) {
        int glyphHalf=Math.max(1,visibleThickness)/2;
        return Math.max(Math.max(0,Math.max(visibleStatus,cutout)),Math.max(0,mandatory)>0?Math.max(0,mandatory)-glyphHalf+1:0);
    }
    /** topInset is the minimum safe visual origin; side tabs stay flush. */
    public static Placement place(String requested,float offset,int screenWidth,int screenHeight,int thickness,int length,int topInset) {
        String edge=edge(requested); float f=fraction(offset);
        int sw=Math.max(1,screenWidth),sh=Math.max(1,screenHeight);
        int w=Math.min(sw,TOP.equals(edge)?Math.max(1,length):Math.max(1,thickness));
        int h=Math.min(sh,TOP.equals(edge)?Math.max(1,thickness):Math.max(1,length));
        int x=TOP.equals(edge)?Math.round(f*(sw-w)):LEFT.equals(edge)?0:sw-w;
        int y=TOP.equals(edge)?Math.min(Math.max(0,topInset),sh-h):Math.round(f*(sh-h));
        return new Placement(edge,f,x,y,w,h);
    }
    public static Placement snap(float centerX,float centerY,int sw,int sh,int thickness,int length) {
        float left=Math.max(0,centerX),right=Math.max(0,sw-centerX),top=Math.max(0,centerY);
        String edge=top<left && top<right?TOP:left<right?LEFT:RIGHT;
        float f=TOP.equals(edge)?(centerX-length/2f)/Math.max(1,sw-length):(centerY-length/2f)/Math.max(1,sh-length);
        return place(edge,f,sw,sh,thickness,length);
    }
    public static float inward(String requested,float dx,float dy) {
        String edge=edge(requested); return TOP.equals(edge)?dy:LEFT.equals(edge)?dx:-dx;
    }
    public static boolean inwardSwipe(String edge,float dx,float dy,float threshold) {
        float along=TOP.equals(edge)?Math.abs(dx):Math.abs(dy);
        return inward(edge,dx,dy)>threshold && inward(edge,dx,dy)>along*1.25f;
    }
}
