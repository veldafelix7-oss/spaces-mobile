package id.veldafelix.spaces.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** A selectable, timestamp-based battery-temperature chart; it never synthesizes samples. */
public final class SessionChart extends View {
    private final JSONArray points;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tooltip=new RectF();
    private float d,left,right,top,bottom,min,max;
    private long first,last;
    private int nearest=-1;

    public SessionChart(Context context,JSONArray points) {
        super(context);
        this.points=points==null?new JSONArray():points;
        d=getResources().getDisplayMetrics().density;
        setFocusable(true);
        setContentDescription("Battery temperature chart; time is local; touch a point for its recorded time and value");
    }
    private String time(long millis) {
        boolean spansDate=millis>0 && first>0 && last>0 &&
            new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date(first)).compareTo(new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date(last)))!=0;
        return new SimpleDateFormat(spansDate?"dd MMM HH:mm":"HH:mm",Locale.getDefault()).format(new Date(millis));
    }
    private int validCount() {
        int count=0;
        for(int i=0;i<points.length();i++) { JSONObject point=points.optJSONObject(i);if(point!=null && point.has("at") && point.has("temp")) count++; }
        return count;
    }
    private boolean prepare(float width,float height) {
        left=49*d;right=width-12*d;top=20*d;bottom=height-38*d;
        first=Long.MAX_VALUE;last=Long.MIN_VALUE;min=Float.MAX_VALUE;max=-Float.MAX_VALUE;
        for(int i=0;i<points.length();i++) {
            JSONObject point=points.optJSONObject(i);if(point==null || !point.has("at") || !point.has("temp")) continue;
            long at=point.optLong("at",-1);double raw=point.optDouble("temp",Double.NaN);
            if(at<0 || !Double.isFinite(raw)) continue;
            first=Math.min(first,at);last=Math.max(last,at);min=Math.min(min,(float)raw);max=Math.max(max,(float)raw);
        }
        if(first==Long.MAX_VALUE || last<=first || !Float.isFinite(min) || !Float.isFinite(max)) return false;
        float pad=Math.max(.5f,(max-min)*.12f);min-=pad;max+=pad;
        return right>left && bottom>top;
    }
    private float xFor(long at) { return left+(right-left)*(at-first)/(float)Math.max(1,last-first); }
    private float yFor(double value) { return bottom-(bottom-top)*((float)value-min)/(max-min); }
    private int nearestAt(float x) {
        int index=-1;float distance=Float.MAX_VALUE;
        for(int i=0;i<points.length();i++) {
            JSONObject point=points.optJSONObject(i);if(point==null || !point.has("at") || !point.has("temp")) continue;
            long at=point.optLong("at",-1);if(at<first || at>last) continue;
            float candidate=Math.abs(xFor(at)-x);if(candidate<distance) { distance=candidate;index=i; }
        }
        return distance<=40*d?index:-1;
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth(),height=getHeight();
        p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1*d);p.setTextSize(10*d);p.setColor(Glass.MUTED);p.setTextAlign(Paint.Align.LEFT);
        if(validCount()<2 || !prepare(width,height)) {
            canvas.drawText("—",left,height/2,p);
            setContentDescription("Battery temperature chart; insufficient recorded samples");
            return;
        }
        String firstLabel=time(first),middleLabel=time(first+(last-first)/2),lastLabel=time(last);
        for(int i=0;i<3;i++) {
            float fraction=i/2f,y=top+(bottom-top)*fraction;
            p.setColor(0x22536D85);p.setStrokeWidth(1*d);canvas.drawLine(left,y,right,y,p);
            p.setColor(Glass.MUTED);canvas.drawText(String.format(Locale.getDefault(),"%.1f°",max-(max-min)*fraction),0,y+3*d,p);
        }
        p.setColor(Glass.MUTED);p.setTextSize(10*d);p.setTextAlign(Paint.Align.LEFT);canvas.drawText(firstLabel,left,bottom+17*d,p);
        p.setTextAlign(Paint.Align.CENTER);canvas.drawText(middleLabel,(left+right)/2,bottom+17*d,p);
        p.setTextAlign(Paint.Align.RIGHT);canvas.drawText(lastLabel,right,bottom+17*d,p);
        p.setColor(Glass.ACCENT);p.setTextSize(9*d);p.setTextAlign(Paint.Align.LEFT);canvas.drawText("°C",left,11*d,p);

        Path line=new Path();boolean started=false;long previous=Long.MIN_VALUE;float breakGap=30000; // samples are nominally 10 seconds apart; preserve visible gaps after missed samples.
        for(int i=0;i<points.length();i++) {
            JSONObject point=points.optJSONObject(i);if(point==null || !point.has("at") || !point.has("temp")) { started=false;previous=Long.MIN_VALUE;continue; }
            long at=point.optLong("at",-1);double value=point.optDouble("temp",Double.NaN);
            if(at<first || at>last || !Double.isFinite(value)) { started=false;previous=Long.MIN_VALUE;continue; }
            float x=xFor(at),y=yFor(value);
            if(previous!=Long.MIN_VALUE && at-previous>breakGap) started=false; // show missing intervals as gaps.
            if(!started) { line.moveTo(x,y);started=true; } else line.lineTo(x,y);
            previous=at;
        }
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*d);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(Glass.ACCENT);canvas.drawPath(line,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<points.length();i++) {
            JSONObject point=points.optJSONObject(i);if(point==null || !point.has("at") || !point.has("temp")) continue;
            long at=point.optLong("at",-1);double value=point.optDouble("temp",Double.NaN);
            if(at<first || at>last || !Double.isFinite(value)) continue;
            float x=xFor(at),y=yFor(value);
            p.setColor(Glass.ACCENT);canvas.drawCircle(x,y,2.3f*d,p);
            if(i==nearest) drawSelection(canvas,point,x,y);
        }
        setContentDescription("Battery temperature in degrees Celsius, local time from "+firstLabel+" to "+lastLabel+
            (nearest>=0?selectedDescription(nearest):". Touch a sample for its exact time and value."));
    }
    private String selectedDescription(int index) {
        JSONObject point=points.optJSONObject(index);if(point==null) return "";
        return ". Selected sample "+time(point.optLong("at"))+", "+String.format(Locale.getDefault(),"%.1f °C",point.optDouble("temp"));
    }
    private void drawSelection(Canvas canvas,JSONObject point,float x,float y) {
        p.setColor(Glass.INK);canvas.drawCircle(x,y,4.3f*d,p);
        p.setColor(Glass.ACCENT);canvas.drawCircle(x,y,2*d,p);
        p.setColor(0xFF223244);canvas.drawLine(x,top,x,bottom,p);
        String label=time(point.optLong("at"))+" · "+String.format(Locale.getDefault(),"%.1f °C",point.optDouble("temp"));
        p.setTextSize(10*d);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        float textWidth=p.measureText(label),boxWidth=textWidth+16*d,boxHeight=24*d;
        float boxLeft=Math.max(left,Math.min(right-boxWidth,x-boxWidth/2));
        float boxTop=y-boxHeight-7*d;if(boxTop<0) boxTop=Math.min(heightSafe()-boxHeight,y+8*d);
        tooltip.set(boxLeft,boxTop,boxLeft+boxWidth,boxTop+boxHeight);
        p.setColor(0xEEFFFFFF);canvas.drawRoundRect(tooltip,7*d,7*d,p);
        p.setColor(Glass.INK);p.setTypeface(android.graphics.Typeface.DEFAULT);p.setTextAlign(Paint.Align.LEFT);canvas.drawText(label,boxLeft+8*d,boxTop+16*d,p);
    }
    private float heightSafe() { return getHeight(); }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN || event.getAction()==MotionEvent.ACTION_MOVE) {
            int selected=nearestAt(event.getX());
            if(selected!=nearest) { nearest=selected;invalidate(); }
            if(getParent()!=null) getParent().requestDisallowInterceptTouchEvent(true);return true;
        }
        if(event.getAction()==MotionEvent.ACTION_UP) { if(getParent()!=null) getParent().requestDisallowInterceptTouchEvent(false);performClick();return true; }
        if(event.getAction()==MotionEvent.ACTION_CANCEL) { if(getParent()!=null) getParent().requestDisallowInterceptTouchEvent(false);return true; }
        return true;
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
