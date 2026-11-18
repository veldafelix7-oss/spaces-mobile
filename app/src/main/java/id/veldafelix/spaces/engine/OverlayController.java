package id.veldafelix.spaces.engine;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.core.EdgeDock;
import id.veldafelix.spaces.ui.DockHandle;
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
    private DockHandle handle;
    private View panel;
    private WindowManager.LayoutParams hp;
    private EdgeDock.Placement dock;
    private TextView temperature,ram,battery,networkResult;
    private JSONObject sample=new JSONObject();
    private String gestureState="idle";
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean hidden=false,testing=false,handleAttached=false;
    private static final int MOVE_LEFT=0x01010001,MOVE_RIGHT=0x01010002,MOVE_TOP=0x01010003;
    public OverlayController(Context context,Runnable end) {
        Context app=context.getApplicationContext();
        Display display=app.getSystemService(android.hardware.display.DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        this.c=app.createWindowContext(display,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null);
        this.end=end;store=new Store(app);wm=this.c.getSystemService(WindowManager.class);
    }
    private int dp(float n) { return Glass.dp(c,n); }
    private Rect bounds() { return wm.getCurrentWindowMetrics().getBounds(); }
    private int width() { return Math.max(1,bounds().width()); }
    private int height() { return Math.max(1,bounds().height()); }
    private WindowManager.LayoutParams params(int w,int h) {
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;
        p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        p.setFitInsetsTypes(0);
        return p;
    }
    @SuppressWarnings("ClickableViewAccessibility")
    public void show() {
        if(hidden || handle!=null) return;
        handle=new DockHandle(c);
        handle.setContentDescription(store.t("Buka panel sesi. Tahan lalu geser untuk pindah tepi.","Open session panel. Hold and drag to another edge."));
        String edge=store.p.getString("dock_edge",EdgeDock.RIGHT);
        float offset=store.p.getFloat("dock_offset",.2f);
        // Migrate legacy free-floating coordinates once; never change the active session/profile.
        if(!store.p.contains("dock_edge") && store.p.contains("handle_x")) {
            EdgeDock.Placement migrated=EdgeDock.snap(store.p.getInt("handle_x",width())+dp(12),store.p.getInt("handle_y",dp(85))+dp(35),width(),height(),dp(18),dp(52));
            edge=migrated.edge;offset=migrated.fraction;
        }
        dock=EdgeDock.place(edge,offset,width(),height(),dp(18),dp(52));
        hp=params(dock.width,dock.height);hp.x=dock.x;hp.y=dock.y;
        handle.direction(dock.edge,false);
        handle.setOnClickListener(v->{gestureState="click";open();});
        handle.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host,info);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(MOVE_LEFT,store.t("Pindah ke kiri","Dock left")));
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(MOVE_RIGHT,store.t("Pindah ke kanan","Dock right")));
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(MOVE_TOP,store.t("Pindah ke atas","Dock top")));
            }
            @Override public boolean performAccessibilityAction(View host,int action,android.os.Bundle args) {
                if(action==MOVE_LEFT||action==MOVE_RIGHT||action==MOVE_TOP) { moveTo(action==MOVE_LEFT?EdgeDock.LEFT:action==MOVE_TOP?EdgeDock.TOP:EdgeDock.RIGHT,.35f);return true; }
                return super.performAccessibilityAction(host,action,args);
            }
        });
        handle.setOnTouchListener(new View.OnTouchListener() {
            float sx,sy;int ox,oy;long down;boolean dragging;
            final int slop=ViewConfiguration.get(c).getScaledTouchSlop();
            @Override public boolean onTouch(View v,android.view.MotionEvent e) {
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        gestureState="down";
                        sx=e.getRawX();sy=e.getRawY();ox=hp.x;oy=hp.y;down=e.getEventTime();dragging=false;v.setPressed(true);return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx,dy=e.getRawY()-sy;
                        if(Math.hypot(dx,dy)>slop && (e.getEventTime()-down>=280 || !EdgeDock.inwardSwipe(dock.edge,dx,dy,slop))) dragging=true;
                        if(dragging) {
                            hp.x=Math.max(0,Math.min(width()-hp.width,ox+(int)dx));hp.y=Math.max(0,Math.min(height()-hp.height,oy+(int)dy));updateHandle();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        gestureState="up drag="+dragging+" dx="+(e.getRawX()-sx)+" dy="+(e.getRawY()-sy);
                        v.setPressed(false);
                        if(dragging) snap(hp.x+hp.width/2f,hp.y+hp.height/2f);
                        else if(Math.hypot(e.getRawX()-sx,e.getRawY()-sy)<=slop || EdgeDock.inwardSwipe(dock.edge,e.getRawX()-sx,e.getRawY()-sy,dp(12))) {
                            // Finish ACTION_UP before replacing its overlay window/input channel.
                            main.post(()->{if(!hidden && handle==v) v.performClick();});
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        gestureState="cancelled by window/system";
                        v.setPressed(false);applyDock(dock,false);return true;
                    default:return true;
                }
            }
        });
        try { wm.addView(handle,hp);handleAttached=true;saveDock(); } catch(Exception ignored) { handle=null; }
    }
    private void saveDock() { store.p.edit().putString("dock_edge",dock.edge).putFloat("dock_offset",dock.fraction).remove("handle_x").remove("handle_y").apply(); }
    private void updateHandle() { if(handle!=null && handleAttached) try { wm.updateViewLayout(handle,hp); } catch(Exception ignored) { } }
    private void applyDock(EdgeDock.Placement p,boolean save) {
        dock=p;hp.x=p.x;hp.y=p.y;hp.width=p.width;hp.height=p.height;
        if(handle!=null) handle.direction(p.edge,false);updateHandle();if(save) saveDock();
    }
    void moveTo(String edge,float offset) { if(handle!=null && panel==null) applyDock(EdgeDock.place(edge,offset,width(),height(),dp(18),dp(52)),true); }
    private void snap(float x,float y) { applyDock(EdgeDock.snap(x,y,width(),height(),dp(18),dp(52)),true); }
    EdgeDock.Placement placement() { return dock; }
    View handleView() { return handle; }
    View panelView() { return panel; }
    String gestureStatus() { return gestureState; }
    public boolean isOpen() { return panel!=null; }
    public void update(JSONObject point) { sample=point;renderMetrics(); }
    private void renderMetrics() {
        if(temperature==null) return;
        temperature.setText(sample.has("temp")?String.format(Locale.getDefault(),"%.1f °C",sample.optDouble("temp")):"—");
        ram.setText(sample.has("ramMb")?sample.optLong("ramMb")+" MB":"—");
        battery.setText(sample.has("battery")?sample.optInt("battery")+"%":"—");
    }
    private TextView metric(LinearLayout row,String label,String value) {
        LinearLayout box=Glass.column(c);box.setPadding(dp(10),dp(9),dp(6),dp(9));box.setBackground(Glass.surface(0xAAFFFFFF,dp(12)));
        box.addView(Glass.text(c,label,10,Glass.MUTED,false));Glass.gap(box,4);
        TextView text=Glass.text(c,value,15,Glass.INK,true);box.addView(text);
        row.addView(box,new LinearLayout.LayoutParams(0,-2,1));return text;
    }
    private void open() {
        if(panel!=null || hidden || handle==null) return;
        final String edge=dock.edge;
        boolean top=EdgeDock.TOP.equals(edge),left=EdgeDock.LEFT.equals(edge);
        int pw=Math.min(dp(top?400:280),width()),ph=Math.min(height(),top?dp(300):height());
        FrameLayout frame=new FrameLayout(c);frame.setContentDescription("spaces-session-panel");
        frame.setBackground(Glass.surface(0xF2FFFFFF,dp(20)));
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,-1);
        // Keep the collapse control outside the scroller so it is always reachable.
        sp.setMargins(left?dp(12):dp(27),dp(12),left?dp(27):dp(12),top?dp(27):dp(12));frame.addView(scroll,sp);
        LinearLayout content=Glass.column(c);scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        content.addView(Glass.text(c,"spaces : mobile",18,Glass.INK,true));Glass.gap(content,3);
        content.addView(Glass.text(c,store.t("SESI PERMAINAN","GAME SESSION"),9,Glass.MUTED,true));Glass.gap(content,12);
        LinearLayout first=Glass.row(c);temperature=metric(first,store.t("Suhu baterai","Battery temp"),"—");Glass.gap(first,7);ram=metric(first,store.t("RAM tersedia","Available RAM"),"—");content.addView(first);
        Glass.gap(content,7);LinearLayout second=Glass.row(c);battery=metric(second,store.t("Baterai","Battery"),"—");Glass.gap(second,7);metric(second,"FPS",store.t("Belum tersedia","Unavailable"));content.addView(second);renderMetrics();
        Glass.gap(content,10);content.addView(Glass.text(c,store.t("Mode Fokus mengikuti profil sesi.","Focus follows the session profile."),10,Glass.MUTED,false));Glass.gap(content,10);
        content.addView(Glass.text(c,store.t("Volume media","Media volume"),11,Glass.INK,true));
        android.media.AudioManager audio=c.getSystemService(android.media.AudioManager.class);
        SeekBar volume=new SeekBar(c);volume.setContentDescription(store.t("Volume media","Media volume"));volume.setMax(audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC));volume.setProgress(audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar b) {}public void onStopTrackingTouch(SeekBar b) {}
            public void onProgressChanged(SeekBar b,int n,boolean user) { if(user) audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,n,0); }
        });content.addView(volume,new LinearLayout.LayoutParams(-1,dp(36)));
        content.addView(Glass.button(c,store.t("Kecerahan layar","Display brightness"),false,()->{
            close();try { c.startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch(Exception ignored) { }
        }));Glass.gap(content,7);
        content.addView(Glass.button(c,store.t("Uji koneksi","Test connection"),false,this::testNetwork));Glass.gap(content,4);
        networkResult=Glass.text(c,store.t("TCP ke 1.1.1.1 · bukan ping pertandingan","TCP to 1.1.1.1 · not in-match ping"),9,Glass.MUTED,false);content.addView(networkResult);Glass.gap(content,9);
        content.addView(Glass.button(c,store.t("Akhiri sesi & pulihkan","End session & restore"),true,()->{close();end.run();}));
        DockHandle collapse=new DockHandle(c);collapse.direction(edge,true);collapse.setContentDescription(store.t("Tutup panel sesi","Collapse session panel"));collapse.setOnClickListener(v->close());
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(top?dp(52):dp(24),top?dp(24):dp(52),top?Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL:(left?Gravity.RIGHT:Gravity.LEFT)|Gravity.CENTER_VERTICAL);
        // For the collapse strip, render the glass on the panel's inner edge.
        collapse.direction(top?EdgeDock.TOP:left?EdgeDock.RIGHT:EdgeDock.LEFT,top);
        frame.addView(collapse,cp);
        WindowManager.LayoutParams p=params(pw,ph);p.x=top?Math.max(0,Math.min(width()-pw,dock.x+dock.width/2-pw/2)):left?0:width()-pw;p.y=0;
        panel=frame;
        try {
            wm.addView(panel,p);
            // Detach the tab window instead of leaving an invisible overlay that may retain
            // an input/obscuring region on OEM window managers.
            if(handleAttached) { wm.removeViewImmediate(handle);handleAttached=false; }
        } catch(Exception error) {
            store.p.edit().putString("overlay_error",error.getClass().getSimpleName()+": "+error.getMessage()).apply();
            if(panel!=null) try { wm.removeViewImmediate(panel); } catch(Exception ignoredAgain) { }
            gestureState="open failed: "+error.getClass().getSimpleName();panel=null;clearMetrics();
        }
    }
    private void testNetwork() {
        if(testing || networkResult==null) return;testing=true;
        TextView result=networkResult;result.setText(store.t("Mengukur koneksi TCP…","Measuring TCP connection…"));
        new Thread(()->{
            String text;long start=android.os.SystemClock.elapsedRealtime();
            try(Socket socket=new Socket()) { socket.connect(new InetSocketAddress("1.1.1.1",443),2000);text="TCP · "+(android.os.SystemClock.elapsedRealtime()-start)+" ms · "+store.t("bukan ping game","not game ping"); }
            catch(Exception e) { text=store.t("Uji koneksi gagal","Connection test failed"); }
            String finalText=text;main.post(()->{testing=false;if(!hidden && panel!=null && networkResult==result) result.setText(finalText);});
        },"spaces-network-test").start();
    }
    private void clearMetrics() { temperature=null;ram=null;battery=null;networkResult=null; }
    void close() {
        if(panel!=null) try { wm.removeView(panel); } catch(Exception ignored) { } finally { panel=null;clearMetrics(); }
        if(handle!=null && !hidden && !handleAttached) try { wm.addView(handle,hp);handleAttached=true; } catch(Exception ignored) { }
    }
    public void hide() {
        hidden=true;close();main.removeCallbacksAndMessages(null);
        if(handle!=null) try { if(handleAttached) wm.removeViewImmediate(handle); } catch(Exception ignored) { } finally { handle=null;handleAttached=false; }
    }
}
