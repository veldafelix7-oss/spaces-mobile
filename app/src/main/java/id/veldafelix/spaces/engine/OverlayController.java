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
    private FrameLayout window;
    private WindowManager.LayoutParams hp;
    private EdgeDock.Placement dock;
    private TextView temperature,ram,battery,networkResult;
    private JSONObject sample=new JSONObject();
    private String gestureState="idle";
    private final java.util.ArrayDeque<String> gestureTrace=new java.util.ArrayDeque<>();
    private void note(String message) {
        gestureState=message;if(gestureTrace.size()>=18) gestureTrace.removeFirst();gestureTrace.addLast(message);
    }
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean hidden=false,testing=false,windowAttached=false;
    private static final int MOVE_LEFT=0x01010001,MOVE_RIGHT=0x01010002,MOVE_TOP=0x01010003;
    public OverlayController(Context context,Runnable end) {
        Context app=context.getApplicationContext();
        Display display=app.getSystemService(android.hardware.display.DisplayManager.class).getDisplay(Display.DEFAULT_DISPLAY);
        // Keep screen geometry tied to the display, not a window context whose
        // configuration can change to the small overlay's own bounds.
        this.c=app.createDisplayContext(display);
        this.end=end;store=new Store(app);wm=this.c.getSystemService(WindowManager.class);
    }
    private int dp(float n) { return Glass.dp(c,n); }
    private Rect bounds() { return wm.getMaximumWindowMetrics().getBounds(); }
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
    private EdgeDock.Placement place(String edge,float offset) {
        WindowInsets insets=wm.getMaximumWindowMetrics().getWindowInsets();
        int safeTop=Math.max(insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout()).top,
            insets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures()).top);
        return EdgeDock.place(edge,offset,width(),height(),dp(18),dp(52),safeTop>0?safeTop+1:0);
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
        dock=place(edge,offset);
        hp=params(dock.width,dock.height);hp.setTitle("spaces-handle");hp.x=dock.x;hp.y=dock.y;
        handle.direction(dock.edge,false);
        handle.setOnClickListener(v->{note("click");open();});
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
                        note("down");
                        sx=e.getRawX();sy=e.getRawY();ox=hp.x;oy=hp.y;down=e.getEventTime();dragging=false;v.setPressed(true);return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx,dy=e.getRawY()-sy;
                        if(Math.hypot(dx,dy)>slop && (e.getEventTime()-down>=280 || !EdgeDock.inwardSwipe(dock.edge,dx,dy,slop))) dragging=true;
                        if(dragging) {
                            hp.x=Math.max(0,Math.min(width()-hp.width,ox+(int)dx));hp.y=Math.max(0,Math.min(height()-hp.height,oy+(int)dy));updateHandle();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        note("up drag="+dragging+" dx="+(e.getRawX()-sx)+" dy="+(e.getRawY()-sy));
                        v.setPressed(false);
                        if(dragging) snap(hp.x+hp.width/2f,hp.y+hp.height/2f);
                        else if(Math.hypot(e.getRawX()-sx,e.getRawY()-sy)<=slop || EdgeDock.inwardSwipe(dock.edge,e.getRawX()-sx,e.getRawY()-sy,dp(12))) {
                            // Finish ACTION_UP before replacing its overlay window/input channel.
                            v.postOnAnimation(()->{if(!hidden && handle==v) v.performClick();});
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        note("cancelled by window/system");
                        v.setPressed(false);applyDock(dock,false);return true;
                    default:return true;
                }
            }
        });
        // One input channel for both states: replacing an overlay ViewRoot can leave
        // input geometry behind during rotation or an in-flight touch transaction.
        window=new FrameLayout(c) {
            @Override public void onWindowFocusChanged(boolean focused) {
                super.onWindowFocusChanged(focused);note("focus="+focused);
            }
            @Override public boolean dispatchKeyEvent(KeyEvent e) {
                if(panel!=null && e.getKeyCode()==KeyEvent.KEYCODE_BACK) {
                    if(e.getAction()==KeyEvent.ACTION_UP) postOnAnimation(OverlayController.this::close);
                    return true;
                }
                return super.dispatchKeyEvent(e);
            }
            @Override public boolean dispatchTouchEvent(MotionEvent e) {
                note("window "+e.getActionMasked()+" local="+e.getX()+","+e.getY()+" raw="+e.getRawX()+","+e.getRawY());
                return super.dispatchTouchEvent(e);
            }
        };
        window.setFocusableInTouchMode(true);
        window.addView(handle,new FrameLayout.LayoutParams(-1,-1));
        try { wm.addView(window,hp);windowAttached=true;saveDock(); }
        catch(Exception error) { recordError(error);handle=null;window=null; }
    }
    private void saveDock() { store.p.edit().putString("dock_edge",dock.edge).putFloat("dock_offset",dock.fraction).remove("handle_x").remove("handle_y").apply(); }
    private void updateHandle() { if(handle!=null && windowAttached) try { wm.updateViewLayout(window,hp); } catch(Exception ignored) { } }
    private void applyDock(EdgeDock.Placement p,boolean save) {
        dock=p;hp.x=p.x;hp.y=p.y;hp.width=p.width;hp.height=p.height;
        if(handle!=null) handle.direction(p.edge,false);updateHandle();if(save) saveDock();
    }
    void moveTo(String edge,float offset) { if(handle!=null && panel==null) applyDock(place(edge,offset),true); }
    private void snap(float x,float y) {
        EdgeDock.Placement p=EdgeDock.snap(x,y,width(),height(),dp(18),dp(52));
        applyDock(place(p.edge,p.fraction),true);
    }
    private void recordError(Exception error) {
        store.p.edit().putString("overlay_error",error.getClass().getSimpleName()+": "+error.getMessage()).apply();
    }
    @androidx.annotation.MainThread
    EdgeDock.Placement placement() { return dock; }
    @androidx.annotation.MainThread
    View handleView() { return handle; }
    @androidx.annotation.MainThread
    View panelView() { return panel; }
    @androidx.annotation.MainThread
    String gestureStatus() { return gestureState+"; trace="+String.join(" > ",gestureTrace); }
    @androidx.annotation.MainThread
    public boolean isOpen() { return panel!=null; }
    @androidx.annotation.MainThread
    boolean inputReady() { return panel!=null && window!=null && window.hasWindowFocus() && panel.isShown() && panel.getWidth()>0 && panel.getHeight()>0; }
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
        note("open");
        final String edge=dock.edge;
        boolean top=EdgeDock.TOP.equals(edge),left=EdgeDock.LEFT.equals(edge);
        int panelY=top?dock.y:0;
        int pw=Math.min(dp(top?400:280),width()),ph=Math.min(height()-panelY,top?dp(300):height());
        FrameLayout frame=new FrameLayout(c) {
            private boolean closingTouch=false;
            @Override public boolean dispatchTouchEvent(MotionEvent event) {
                int action=event.getActionMasked();
                note("panel action="+action+" x="+event.getX()+" y="+event.getY());
                if(action==MotionEvent.ACTION_DOWN) {
                    closingTouch=top?event.getY()>=getHeight()-dp(48):left?event.getX()>=getWidth()-dp(48):event.getX()<=dp(48);
                }
                if(closingTouch) {
                    if(action==MotionEvent.ACTION_UP) { closingTouch=false;window.postOnAnimation(OverlayController.this::close); }
                    else if(action==MotionEvent.ACTION_CANCEL) closingTouch=false;
                    return true;
                }
                return super.dispatchTouchEvent(event);
            }
        };
        frame.setContentDescription("spaces-session-panel");frame.setClickable(true);
        frame.setBackground(Glass.surface(0xF2FFFFFF,dp(20)));
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,-1);
        // Keep the collapse control outside the scroller so it is always reachable.
        sp.setMargins(left?dp(12):dp(52),dp(12),left?dp(52):dp(12),top?dp(52):dp(12));frame.addView(scroll,sp);
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
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(top?dp(52):dp(12),top?dp(12):dp(52),top?Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL:(left?Gravity.RIGHT:Gravity.LEFT)|Gravity.CENTER_VERTICAL);
        // For the collapse strip, render the glass on the panel's inner edge.
        collapse.direction(top?EdgeDock.TOP:left?EdgeDock.RIGHT:EdgeDock.LEFT,top);
        // Keep the narrow chevron inside its own panel, away from the window boundary.
        if(top) cp.bottomMargin=dp(16);
        else if(left) cp.rightMargin=dp(16);
        else cp.leftMargin=dp(16);
        frame.addView(collapse,cp);
        WindowManager.LayoutParams p=params(pw,ph);p.setTitle("spaces-panel");p.x=top?Math.max(0,Math.min(width()-pw,dock.x+dock.width/2-pw/2)):left?0:width()-pw;p.y=panelY;
        // The open panel is an explicit interaction surface; the closed tab never
        // takes focus. Keep the keyboard out and return focus when it collapses.
        p.flags=(p.flags & ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        panel=frame;
        try {
            window.removeAllViews();window.addView(panel,new FrameLayout.LayoutParams(-1,-1));
            wm.updateViewLayout(window,p);window.requestFocus();
            store.p.edit().remove("overlay_error").apply();
        } catch(Exception error) {
            recordError(error);note("open failed: "+error.getClass().getSimpleName());
            panel=null;clearMetrics();restoreHandle();
        }
    }
    private void restoreHandle() {
        if(window==null || handle==null || !windowAttached || hidden) return;
        try {
            window.removeAllViews();window.addView(handle,new FrameLayout.LayoutParams(-1,-1));
            window.clearFocus();wm.updateViewLayout(window,hp);
        } catch(Exception error) { recordError(error); }
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
        if(panel==null) return;
        note("close");panel=null;clearMetrics();restoreHandle();
    }
    public void hide() {
        hidden=true;main.removeCallbacksAndMessages(null);
        if(window!=null && windowAttached) try { wm.removeViewImmediate(window); } catch(Exception error) { recordError(error); }
        finally { windowAttached=false; }
        window=null;handle=null;panel=null;clearMetrics();
    }
}
