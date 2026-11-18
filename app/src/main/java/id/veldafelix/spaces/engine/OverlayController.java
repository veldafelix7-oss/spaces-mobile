package id.veldafelix.spaces.engine;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.ui.Glass;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

public final class OverlayController {
    private final Context c;
    private final Store store;
    private final WindowManager wm;
    private final Runnable end;
    private View handle,panel;
    private WindowManager.LayoutParams hp;
    private TextView metrics;
    private JSONObject sample=new JSONObject();
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean hidden=false;
    public OverlayController(Context c,Runnable end) { this.c=c;this.end=end;store=new Store(c);wm=c.getSystemService(WindowManager.class); }
    private int dp(float n) { return Glass.dp(c,n); }
    private WindowManager.LayoutParams params(int w,int h) {
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT; return p;
    }
    @SuppressWarnings("ClickableViewAccessibility")
    public void show() {
        if(hidden) return;
        TextView grip=Glass.text(c,"│",22,Glass.ACCENT,true); grip.setGravity(Gravity.CENTER);
        grip.setContentDescription(store.t("Tarik untuk buka panel; seret untuk pindah","Swipe to open; drag to reposition"));
        grip.setBackground(Glass.surface(0xCCFFFFFF,dp(12))); grip.setHapticFeedbackEnabled(false);
        handle=grip; hp=params(dp(24),dp(70));
        int width=c.getResources().getDisplayMetrics().widthPixels,height=c.getResources().getDisplayMetrics().heightPixels;
        hp.x=Math.max(0,Math.min(width-dp(24),store.p.getInt("handle_x",width-dp(26))));
        hp.y=Math.max(0,Math.min(height-dp(70),store.p.getInt("handle_y",dp(85))));
        grip.setOnClickListener(v->open());
        grip.setOnTouchListener(new View.OnTouchListener() {
            float sx,sy; int ox,oy; long down;
            @Override public boolean onTouch(View v,android.view.MotionEvent e) {
                if(e.getAction()==MotionEvent.ACTION_DOWN) { sx=e.getRawX(); sy=e.getRawY(); ox=hp.x; oy=hp.y; down=System.currentTimeMillis(); return true; }
                if(e.getAction()==MotionEvent.ACTION_MOVE) {
                    hp.x=Math.max(0,Math.min(width-dp(24),ox+(int)(e.getRawX()-sx)));
                    hp.y=Math.max(0,Math.min(height-dp(70),oy+(int)(e.getRawY()-sy)));
                    try { wm.updateViewLayout(handle,hp); } catch(Exception ignored) { }
                    return true;
                }
                if(e.getAction()==MotionEvent.ACTION_UP) {
                    float dx=e.getRawX()-sx,dy=e.getRawY()-sy;
                    // Quick inward horizontal swipe opens; long drag moves to any edge.
                    if(System.currentTimeMillis()-down<400 && Math.abs(dx)>dp(30) && Math.abs(dx)>Math.abs(dy)*1.5f) {
                        hp.x=ox; hp.y=oy; try { wm.updateViewLayout(handle,hp); } catch(Exception ignored) { } open();
                    } else if(Math.hypot(dx,dy)<dp(8)) v.performClick();
                    else store.p.edit().putInt("handle_x",hp.x).putInt("handle_y",hp.y).apply();
                    return true;
                }
                return true;
            }
        });
        try { wm.addView(handle,hp); } catch(Exception ignored) { handle=null; }
    }
    public void update(JSONObject point) { sample=point; renderMetrics(); }
    private void renderMetrics() {
        if(metrics==null) return;
        String temp=sample.has("temp")?String.format(Locale.getDefault(),"%.1f °C",sample.optDouble("temp")):"—";
        metrics.setText("FPS   — "+store.t("belum tersedia","unavailable")+"\n\n"+
            store.t("Suhu baterai   ","Battery temp   ")+temp+"\n\n"+
            store.t("RAM tersedia   ","Available RAM   ")+sample.optLong("ramMb")+" MB\n\n"+
            store.t("Baterai   ","Battery   ")+(sample.has("battery")?sample.optInt("battery")+"%":"—"));
    }
    private void open() {
        if(panel!=null || hidden) return;
        ScrollView scroll=new ScrollView(c); scroll.setFillViewport(true);
        LinearLayout content=Glass.card(c); scroll.addView(content);
        content.addView(Glass.text(c,"spaces : mobile",21,Glass.INK,true)); Glass.gap(content,4);
        content.addView(Glass.text(c,store.t("PANEL SESI","SESSION PANEL"),10,Glass.MUTED,true)); Glass.gap(content,17);
        metrics=Glass.text(c,"",14,Glass.INK,false); content.addView(metrics); renderMetrics(); Glass.gap(content,14);
        content.addView(Glass.text(c,store.t("Mode Fokus mengikuti profil sesi.","Focus follows the session profile."),11,Glass.MUTED,false)); Glass.gap(content,10);
        content.addView(Glass.button(c,store.t("Tes koneksi · bukan ping game","Test connection · not game ping"),false,()->testNetwork(content))); Glass.gap(content,10);
        content.addView(Glass.text(c,store.t("Volume & kecerahan sistem","System volume & brightness"),12,Glass.INK,true));
        android.media.AudioManager audio=c.getSystemService(android.media.AudioManager.class);
        SeekBar volume=new SeekBar(c); volume.setContentDescription(store.t("Volume media","Media volume")); volume.setMax(audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)); volume.setProgress(audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
            public void onProgressChanged(SeekBar b,int n,boolean user) { if(user) audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,n,0); }
        }); content.addView(volume);
        content.addView(Glass.button(c,store.t("Pengaturan kecerahan","Brightness settings"),false,()->{
            close(); try { c.startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch(Exception ignored) { }
        }));
        Glass.gap(content,10);
        content.addView(Glass.button(c,store.t("Tutup panel","Close panel"),false,this::close)); Glass.gap(content,8);
        content.addView(Glass.button(c,store.t("Akhiri sesi & pulihkan","End session & restore"),true,()->{ close(); end.run(); }));
        int width=c.getResources().getDisplayMetrics().widthPixels;
        int height=c.getResources().getDisplayMetrics().heightPixels;
        WindowManager.LayoutParams p=params(Math.min(dp(330),width-dp(40)),Math.max(dp(100),height-dp(32)));
        p.x=hp.x>width/2?width-p.width-dp(14):dp(14); p.y=dp(12);
        panel=scroll;
        try { wm.addView(panel,p); } catch(Exception ignored) { panel=null; metrics=null; }
    }
    private void testNetwork(LinearLayout content) {
        TextView result=Glass.text(c,store.t("Mengukur koneksi TCP…","Measuring TCP connection…"),11,Glass.MUTED,false);
        content.addView(result);
        new Thread(()->{
            String text; long start=android.os.SystemClock.elapsedRealtime();
            try(Socket socket=new Socket()) {
                socket.connect(new InetSocketAddress("1.1.1.1",443),2000);
                text="TCP 1.1.1.1:443 · "+(android.os.SystemClock.elapsedRealtime()-start)+" ms";
            } catch(Exception e) { text=store.t("Tes koneksi gagal","Connection test failed"); }
            String finalText=text; main.post(()->{ if(!hidden && panel!=null) result.setText(finalText); });
        },"spaces-network-test").start();
    }
    private void close() { if(panel!=null) try { wm.removeView(panel); } catch(Exception ignored) { } finally { panel=null; metrics=null; } }
    public void hide() { hidden=true; close(); main.removeCallbacksAndMessages(null); if(handle!=null) try { wm.removeView(handle); } catch(Exception ignored) { } finally { handle=null; } }
}
