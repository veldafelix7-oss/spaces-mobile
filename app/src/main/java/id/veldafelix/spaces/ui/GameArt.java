package id.veldafelix.spaces.ui;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original, lightweight vector scenery, not downloaded game artwork. */
public final class GameArt extends View {
    private final boolean flash;
    private final Paint p=new Paint(3);
    public GameArt(Context c,boolean flash) { super(c); this.flash=flash; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) {
        float w=getWidth(),h=getHeight();
        Path clip=new Path(); clip.addRoundRect(0,0,w,h,Glass.dp(getContext(),22),Glass.dp(getContext(),22),Path.Direction.CW); canvas.save(); canvas.clipPath(clip);
        p.setShader(new LinearGradient(0,0,w,h,flash?new int[]{0xFFCBD7D9,0xFF8EA4AA,0xFF536C79}:new int[]{0xFFD5DCE4,0xFFA5B5C9,0xFF586F91},null,Shader.TileMode.CLAMP)); canvas.drawRect(0,0,w,h,p); p.setShader(null);
        p.setColor(0x55FFFFFF); canvas.drawCircle(w*.78f,h*.24f,h*.22f,p);
        for(int layer=0;layer<3;layer++) {
            Path ridge=new Path(); ridge.moveTo(0,h*(.48f+layer*.13f));
            for(int i=0;i<=8;i++) ridge.lineTo(w*i/8f,h*(.4f+layer*.14f)+(float)Math.sin(i*1.9+layer)*h*.1f);
            ridge.lineTo(w,h); ridge.lineTo(0,h); ridge.close(); p.setColor(new int[]{0x22718C95,0x33516C7D,0x66455F71}[layer]); canvas.drawPath(ridge,p);
        }
        p.setColor(0xFF405766);
        if(flash) {
            float x=w*.72f,y=h*.48f,s=h*.4f;
            Path body=new Path(); body.moveTo(x-s*.4f,h); body.lineTo(x-s*.3f,y+s*.3f); body.lineTo(x-s*.15f,y+s*.2f); body.lineTo(x+s*.17f,y+s*.2f); body.lineTo(x+s*.38f,h); body.close(); canvas.drawPath(body,p);
            canvas.drawRoundRect(x-s*.2f,y-s*.2f,x+s*.22f,y+s*.22f,s*.1f,s*.1f,p);
            p.setColor(0xFFB5C4C9); canvas.drawRoundRect(x-s*.24f,y-s*.24f,x+s*.25f,y+s*.04f,s*.15f,s*.15f,p);
            p.setColor(0xFF344A5A); canvas.drawRect(x-s*.25f,y-s*.01f,x+s*.27f,y+s*.06f,p);
        } else {
            Path slash=new Path(); slash.moveTo(w*.7f,h*.12f); slash.lineTo(w*.53f,h*.51f); slash.lineTo(w*.75f,h*.47f); slash.lineTo(w*.59f,h*.88f); slash.lineTo(w*.94f,h*.36f); slash.lineTo(w*.72f,h*.38f); slash.close();
            p.setShader(new LinearGradient(0,0,w,h,0xCCF1F8FF,0x557C9CB6,Shader.TileMode.CLAMP)); canvas.drawPath(slash,p); p.setShader(null);
        }
        p.setShader(new LinearGradient(0,h*.3f,0,h,0x00405565,0xD9233646,Shader.TileMode.CLAMP)); canvas.drawRect(0,0,w,h,p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0xAAFFFFFF); canvas.drawRoundRect(1,1,w-1,h-1,Glass.dp(getContext(),22),Glass.dp(getContext(),22),p); p.setStyle(Paint.Style.FILL);
        canvas.restore();
    }
}
