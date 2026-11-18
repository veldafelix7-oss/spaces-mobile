package id.veldafelix.spaces.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import android.graphics.Typeface;

public final class Glass {
    public static final int INK=Color.rgb(35,51,65), MUTED=Color.rgb(111,128,142), ACCENT=Color.rgb(80,111,140);
    private Glass() {}
    public static int dp(Context c,float n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    public static android.graphics.drawable.Drawable surface(int fill,int radius) {
        return new GlassSurface(fill,radius,Math.max(1,Math.min(2.5f,radius/23f)));
    }
    public static android.graphics.drawable.Drawable ripple(android.graphics.drawable.Drawable background) {
        return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x3057748D),background,null);
    }
    public static TextView text(Context c,String s,int sp,int color,boolean bold) {
        TextView t=new TextView(c); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        t.setFontFeatureSettings("tnum"); t.setFontFeatureSettings("kern");
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",bold?Typeface.NORMAL:Typeface.NORMAL));
        t.setIncludeFontPadding(false); return t;
    }
    public static TextView button(Context c,String title,boolean primary,Runnable action) {
        TextView t=text(c,title,13,primary?Color.WHITE:INK,true);
        int p=dp(c,13); t.setPadding(p,dp(c,11),p,dp(c,11)); t.setGravity(android.view.Gravity.CENTER);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,primary?new int[]{0xFF68859E,0xFF415C74}:new int[]{0xEEFFFFFF,0xB8FFFFFF});
        bg.setCornerRadius(dp(c,15)); bg.setStroke(1,0xCCFFFFFF);
        t.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x2257748D),bg,null));
        t.setMinHeight(dp(c,44)); t.setClickable(true); t.setFocusable(true); t.setHapticFeedbackEnabled(false);
        t.setOnClickListener(v->action.run()); return t;
    }
    public static LinearLayout column(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    public static LinearLayout row(Context c) { LinearLayout l=new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(android.view.Gravity.CENTER_VERTICAL); return l; }
    public static LinearLayout card(Context c) {
        LinearLayout l=column(c); int p=dp(c,18); l.setPadding(p,p,p,p); l.setBackground(surface(0xBFFFFFFF,dp(c,23))); return l;
    }
    public static void gap(LinearLayout parent,int dp) {
        View v=new View(parent.getContext());
        if(parent.getOrientation()==LinearLayout.VERTICAL) parent.addView(v,new LinearLayout.LayoutParams(1,dp(parent.getContext(),dp)));
        else parent.addView(v,new LinearLayout.LayoutParams(dp(parent.getContext(),dp),1));
    }
    public static final class Background extends View {
        private final Paint p=new Paint(3);
        public Background(Context c) { super(c); setLayerType(View.LAYER_TYPE_NONE,null); }
        @Override protected void onDraw(Canvas c) {
            float w=getWidth(),h=getHeight();
            p.setShader(new LinearGradient(0,0,w,h,new int[]{0xFFEDF0F3,0xFFDDE5EB,0xFFEFF2F5},null,Shader.TileMode.CLAMP)); c.drawRect(0,0,w,h,p);
            p.setShader(new RadialGradient(w*.74f,h*.18f,Math.max(1,w*.4f),new int[]{0xAAFFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP)); c.drawRect(0,0,w,h,p);
            p.setShader(new RadialGradient(w*.3f,h*.85f,Math.max(1,w*.36f),new int[]{0x447B95A8,0x007B95A8},null,Shader.TileMode.CLAMP)); c.drawRect(0,0,w,h,p);
            p.setShader(null); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(0x77FFFFFF);
            c.drawOval(w*.43f,-h*.8f,w*1.2f,h*.8f,p); c.drawOval(w*.6f,-h*.65f,w*1.35f,h*.95f,p);
            p.setStyle(Paint.Style.FILL);
        }
    }
}
