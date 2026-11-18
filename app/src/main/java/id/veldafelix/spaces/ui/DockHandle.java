package id.veldafelix.spaces.ui;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import id.veldafelix.spaces.core.EdgeDock;

/** The visible tab is thinner than its touch target and flush with the physical edge. */
public final class DockHandle extends View {
    private final Paint p=new Paint(3);
    private String edge=EdgeDock.RIGHT;
    private boolean collapse=false;
    public DockHandle(Context context) { super(context);setHapticFeedbackEnabled(false);setClickable(true);setFocusable(true); }
    public void direction(String side,boolean collapsing) { edge=EdgeDock.edge(side);collapse=collapsing;invalidate(); }
    @Override protected void onDraw(Canvas c) {
        float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();
        float visible=Math.min(12*d,EdgeDock.TOP.equals(edge)?h:w);
        RectF r=EdgeDock.TOP.equals(edge)?(collapse?new RectF(0,h-visible,w,h):new RectF(0,0,w,visible)):EdgeDock.LEFT.equals(edge)?new RectF(0,0,visible,h):new RectF(w-visible,0,w,h);
        Path shape=new Path(); float corner=6*d;
        float[] corners=EdgeDock.TOP.equals(edge)?new float[]{0,0,0,0,corner,corner,corner,corner}:EdgeDock.LEFT.equals(edge)?new float[]{0,0,corner,corner,corner,corner,0,0}:new float[]{corner,corner,0,0,0,0,corner,corner};
        shape.addRoundRect(r,corners,Path.Direction.CW);
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(r.left,r.top,r.right+1,r.bottom+1,isPressed()?0xEEFFFFFF:0xB8FFFFFF,0x889DAFBD,Shader.TileMode.CLAMP));c.drawPath(shape,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*.7f);p.setColor(0xCCFFFFFF);c.drawPath(shape,p);
        float x=r.centerX(),y=r.centerY(),s=2.4f*d;
        Path arrow=new Path();
        if(EdgeDock.TOP.equals(edge)) {
            float sign=collapse?-1:1;arrow.moveTo(x-s,y-sign*s*.6f);arrow.lineTo(x,y+sign*s*.6f);arrow.lineTo(x+s,y-sign*s*.6f);
        } else {
            float sign=(EdgeDock.LEFT.equals(edge)?1:-1)*(collapse?-1:1);
            arrow.moveTo(x-sign*s*.6f,y-s);arrow.lineTo(x+sign*s*.6f,y);arrow.lineTo(x-sign*s*.6f,y+s);
        }
        p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(1.3f*d);p.setColor(0xFF486175);c.drawPath(arrow,p);p.setStyle(Paint.Style.FILL);
    }
    @Override public void setPressed(boolean pressed) { super.setPressed(pressed);invalidate(); }
}
