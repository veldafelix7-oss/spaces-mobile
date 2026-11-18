package id.veldafelix.spaces.ui;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public final class SessionChart extends View {
    private final JSONArray points;
    private final Paint p=new Paint(3);
    public SessionChart(Context c,JSONArray points) { super(c); this.points=points==null?new JSONArray():points; setContentDescription("Battery temperature over session; degrees Celsius"); }
    @Override protected void onDraw(Canvas c) {
        float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();
        float left=35*d,top=15*d,bottom=h-25*d,right=w-12*d;
        float min=Float.MAX_VALUE,max=-Float.MAX_VALUE; int count=0;
        long first=Long.MAX_VALUE,last=0;
        for(int i=0;i<points.length();i++) { JSONObject o=points.optJSONObject(i); if(o!=null && o.has("temp")) {
            float t=(float)o.optDouble("temp"); min=Math.min(min,t); max=Math.max(max,t); count++;
            first=Math.min(first,o.optLong("at")); last=Math.max(last,o.optLong("at"));
        }}
        if(count<2) { p.setColor(Glass.MUTED); p.setTextSize(12*d); c.drawText("—",left,h/2,p); return; }
        min=(float)Math.floor(min-1); max=(float)Math.ceil(max+1);
        p.setTextSize(10*d);
        for(int i=0;i<3;i++) {
            float y=top+(bottom-top)*i/2f; p.setColor(0x22536D85); p.setStrokeWidth(d); c.drawLine(left,y,right,y,p);
            p.setColor(Glass.MUTED); c.drawText(String.format(Locale.US,"%.0f°",max-(max-min)*i/2f),0,y+4*d,p);
        }
        Path path=new Path(); boolean started=false;
        for(int i=0;i<points.length();i++) { JSONObject o=points.optJSONObject(i); if(o==null||!o.has("temp")) { started=false; continue; }
            float x=left+(right-left)*(o.optLong("at")-first)/(float)Math.max(1,last-first);
            float y=bottom-(bottom-top)*((float)o.optDouble("temp")-min)/(max-min);
            if(!started) { path.moveTo(x,y); started=true; } else path.lineTo(x,y);
        }
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2*d); p.setColor(Glass.ACCENT); c.drawPath(path,p); p.setStyle(Paint.Style.FILL);
        p.setColor(Glass.MUTED); c.drawText("0 min",left,h-5*d,p); c.drawText((last-first)/60000+" min",right-40*d,h-5*d,p);
    }
}
