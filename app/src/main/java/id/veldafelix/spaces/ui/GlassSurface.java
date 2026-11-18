package id.veldafelix.spaces.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Static translucent material with a polished rim. No live blur, bitmap or render loop. */
public final class GlassSurface extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius,density;
    private final int fill;
    private int opacity=255;
    public GlassSurface(int fill,float radius,float density) { this.fill=fill;this.radius=radius;this.density=density; }
    @Override public void draw(Canvas c) {
        Rect b=getBounds(); RectF r=new RectF(b); r.inset(density*.7f,density*.7f);
        if(r.width()<=0 || r.height()<=0) return;
        int a=Math.round(Color.alpha(fill)*opacity/255f);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,
            new int[]{Color.argb(a,255,255,255),Color.argb(Math.max(0,a-20),237,242,247),Color.argb(a,248,250,252)},new float[]{0,.65f,1},Shader.TileMode.CLAMP));
        c.drawRoundRect(r,radius,radius,p); p.setShader(null);
        Path clip=new Path();clip.addRoundRect(r,radius,radius,Path.Direction.CW);c.save();c.clipPath(clip);
        p.setShader(new LinearGradient(0,r.top,0,r.top+r.height()*.55f,Color.argb(Math.round(opacity*.4f),255,255,255),0x00FFFFFF,Shader.TileMode.CLAMP));
        Path sheen=new Path(); sheen.moveTo(r.left,r.top);sheen.lineTo(r.right,r.top);sheen.lineTo(r.right,r.top+r.height()*.1f);
        sheen.cubicTo(r.centerX(),r.top+r.height()*.45f,r.left,r.top+r.height()*.1f,r.left,r.top+r.height()*.7f);sheen.close();c.drawPath(sheen,p);
        p.setShader(null);c.restore();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density);p.setColor(Color.argb(Math.round(opacity*.19f),74,97,117));c.drawRoundRect(r,radius,radius,p);
        r.inset(density,density);p.setColor(Color.argb(Math.round(opacity*.88f),255,255,255));p.setStrokeWidth(density*.9f);c.drawRoundRect(r,Math.max(0,radius-density),Math.max(0,radius-density),p);
        p.setStyle(Paint.Style.FILL);
    }
    @Override public void getOutline(Outline outline) { outline.setRoundRect(getBounds(),radius); }
    @Override public void setAlpha(int alpha) { if(opacity!=alpha) { opacity=alpha;invalidateSelf(); } }
    @Override public void setColorFilter(ColorFilter filter) { if(p.getColorFilter()!=filter) { p.setColorFilter(filter);invalidateSelf(); } }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
