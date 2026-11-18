package id.veldafelix.spaces.engine;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import id.veldafelix.spaces.MainActivity;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.core.EdgeDock;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OverlayTest {
    private Instrumentation inst;
    private Context c;
    private MainActivity activity;
    private OverlayController controller;
    /** Read View/controller state only on its owning thread, including failure snapshots. */
    private <T> T ui(java.util.concurrent.Callable<T> read) {
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()) {
            try { return read.call(); } catch(Exception e) { throw new AssertionError(e); }
        }
        java.util.concurrent.FutureTask<T> task=new java.util.concurrent.FutureTask<>(read);
        inst.runOnMainSync(task);
        try { return task.get(); } catch(Exception e) { throw new AssertionError(e); }
    }
    private void shell(String command) throws Exception {
        try(ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand(command);
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) { byte[] b=new byte[512];while(in.read(b)!=-1) {} }
    }
    @Before public void start() throws Exception {
        inst=InstrumentationRegistry.getInstrumentation();c=inst.getTargetContext();
        // Each test otherwise returns to the portrait launcher and immediately rotates
        // again. Freeze the test display before constructing any overlay input surface.
        assertTrue(inst.getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_FREEZE_90));
        c.getSharedPreferences("spaces",0).edit().clear().commit();
        activity=(MainActivity)inst.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));inst.waitForIdleSync();
        shell("appops set "+c.getPackageName()+" SYSTEM_ALERT_WINDOW allow");
        assertTrue(android.provider.Settings.canDrawOverlays(c));
        awaitActivityFocus();
        inst.runOnMainSync(()->{controller=new OverlayController(activity,()->{});controller.show();});settle();
    }
    @After public void finish() throws Exception {
        inst.runOnMainSync(()->{controller.hide();activity.finish();});inst.waitForIdleSync();shell("appops set "+c.getPackageName()+" SYSTEM_ALERT_WINDOW default");
    }
    @AfterClass public static void restoreDisplayRotation() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);
    }
    private void awaitActivityFocus() {
        long deadline=android.os.SystemClock.uptimeMillis()+4000;final boolean[] focused={false};
        do {
            inst.runOnMainSync(()->focused[0]=activity.hasWindowFocus());
            if(focused[0]) return;
            android.os.SystemClock.sleep(40);
        } while(android.os.SystemClock.uptimeMillis()<deadline);
        fail("Underlying activity did not acquire input focus");
    }
    @Test public void initialHandleIsFlushWithRightEdge() {
        inst.runOnMainSync(()->{assertNotNull(controller.handleView());assertEquals(EdgeDock.RIGHT,ui(controller::placement).edge);
            int width=activity.getSystemService(android.view.WindowManager.class).getCurrentWindowMetrics().getBounds().width();
            assertEquals(width,ui(controller::placement).x+ui(controller::placement).width);assertTrue(ui(controller::placement).height>ui(controller::placement).width);});
    }
    @Test public void topDockRotatesAndRestoresPreference() {
        inst.runOnMainSync(()->{controller.moveTo(EdgeDock.TOP,.6f);assertTrue(ui(controller::placement).y>=0);assertTrue(ui(controller::placement).width>ui(controller::placement).height);
            controller.hide();controller=new OverlayController(activity,()->{});controller.show();assertEquals(EdgeDock.TOP,ui(controller::placement).edge);assertEquals(.6f,ui(controller::placement).fraction,.001f);});
    }
    private View collapse(View v) {
        if("Tutup panel sesi".contentEquals(v.getContentDescription()==null?"":v.getContentDescription())) return v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) {View found=collapse(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    @Test public void edgeArrowClosesWithoutScrollingAndRestoresHandle() {
        inst.runOnMainSync(()->{controller.handleView().performClick();assertTrue(ui(controller::isOpen));assertFalse(controller.handleView().isAttachedToWindow());
            View arrow=collapse(controller.panelView());assertNotNull(arrow);assertTrue(arrow.isShown());arrow.performClick();assertFalse(ui(controller::isOpen));assertEquals(View.VISIBLE,controller.handleView().getVisibility());});
    }
    @Test public void leftAndTopPanelsAlsoCloseFromTheirEdge() {
        inst.runOnMainSync(()->{for(String edge:new String[]{EdgeDock.LEFT,EdgeDock.TOP}) {controller.moveTo(edge,.5f);controller.handleView().performClick();assertTrue(ui(controller::isOpen));assertNotNull(collapse(controller.panelView()));collapse(controller.panelView()).performClick();assertFalse(ui(controller::isOpen));}});
    }
    @Test public void legacyPositionMigratesToDockAndStaysWithinScreen() {
        inst.runOnMainSync(()->{controller.hide();new Store(c).p.edit().remove("dock_edge").remove("dock_offset").putInt("handle_x",99999).putInt("handle_y",99999).commit();
            controller=new OverlayController(activity,()->{});controller.show();assertNotNull(controller.handleView());assertTrue(ui(controller::placement).x>=0);assertTrue(ui(controller::placement).y>=0);assertTrue(new Store(c).p.contains("dock_edge"));assertFalse(new Store(c).p.contains("handle_x"));});
    }
    private void step(String message) {
        android.os.Bundle status=new android.os.Bundle();status.putString("spaces_step",message);inst.sendStatus(2,status);
    }
    private void pointer(int action,long down,float x,float y) {
        android.view.MotionEvent e=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,x,y,0);
        e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        try { assertTrue("OS input injection rejected",inst.getUiAutomation().injectInputEvent(e,action==android.view.MotionEvent.ACTION_UP)); }
        finally { e.recycle(); }
    }
    private void input(String args) {
        String[] parts=args.split(" ");float x=Float.parseFloat(parts[1]),y=Float.parseFloat(parts[2]);
        long down=android.os.SystemClock.uptimeMillis();pointer(android.view.MotionEvent.ACTION_DOWN,down,x,y);
        if("swipe".equals(parts[0])) {
            float tx=Float.parseFloat(parts[3]),ty=Float.parseFloat(parts[4]);int duration=Integer.parseInt(parts[5]);
            int steps=Math.max(2,duration/16);
            for(int i=1;i<=steps;i++) {
                android.os.SystemClock.sleep(Math.max(1,duration/steps));
                pointer(android.view.MotionEvent.ACTION_MOVE,down,x+(tx-x)*i/steps,y+(ty-y)*i/steps);
            }
            x=tx;y=ty;
        } else android.os.SystemClock.sleep(40);
        pointer(android.view.MotionEvent.ACTION_UP,down,x,y);
    }
    private void settle() {
        // A queued UI-frame latch is bounded; waiting for a globally idle accessibility stream
        // can stall when an overlay itself keeps producing accessibility/window events.
        java.util.concurrent.CountDownLatch frame=new java.util.concurrent.CountDownLatch(1);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(frame::countDown,1000);
        try { assertTrue("Overlay UI frame timed out",frame.await(4,java.util.concurrent.TimeUnit.SECONDS)); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new AssertionError(e); }
        // A Handler latch only observes the UI thread, not the emulator compositor's
        // input-window transaction. Screen capture is a public UiAutomation barrier
        // for the rendered state; never retry a missed tap or dispatch directly to a View.
        android.graphics.Bitmap rendered=inst.getUiAutomation().takeScreenshot();
        assertNotNull("Compositor screenshot unavailable",rendered);rendered.recycle();
        if(ui(controller::isOpen)) {
            long deadline=android.os.SystemClock.uptimeMillis()+4000;
            while(!ui(controller::inputReady) && android.os.SystemClock.uptimeMillis()<deadline) android.os.SystemClock.sleep(25);
            assertTrue("Panel never acquired its input window: "+ui(controller::gestureStatus),ui(controller::inputReady));
        }
    }
    private void inputWindows() {
        try(ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("logcat -d -t 80 -s InputDispatcher");
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1) bytes.write(b,0,n);
            String log=bytes.toString("UTF-8").replace('\n','|');step("input log="+log.substring(Math.max(0,log.length()-2400)));
        } catch(Exception e) { step("Input diagnostic: "+e.getClass().getSimpleName()); }
    }
    private void snapshot(float x,float y) {
        android.graphics.Bitmap bitmap=inst.getUiAutomation().takeScreenshot();
        if(bitmap==null) return;
        android.graphics.Bitmap image=bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888,true);bitmap.recycle();
        android.graphics.Canvas canvas=new android.graphics.Canvas(image);android.graphics.Paint paint=new android.graphics.Paint();
        paint.setColor(android.graphics.Color.RED);paint.setStrokeWidth(5);
        canvas.drawLine(x-25,y,x+25,y,paint);canvas.drawLine(x,y-25,x,y+25,paint);
        android.graphics.Bitmap small=android.graphics.Bitmap.createScaledBitmap(image,480,270,true);image.recycle();
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();small.compress(android.graphics.Bitmap.CompressFormat.JPEG,40,bytes);small.recycle();
        android.os.Bundle status=new android.os.Bundle();status.putString("spaces_image",android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));inst.sendStatus(2,status);
    }
    private void tap(float x,float y) { input("tap "+Math.round(x)+" "+Math.round(y));settle(); }
    @Test public void actualWindowTapsOpenAndCollapsePanel() {
        step("opening by actual handle tap");
        EdgeDock.Placement p=ui(controller::placement);tap(p.x+p.width/2f,p.y+p.height/2f);step("panel opened; locating collapse arrow");
        final int[] xy=new int[2];
        inst.runOnMainSync(()->{assertTrue(new Store(c).p.getString("overlay_error","Panel did not open after tap; "+ui(controller::gestureStatus)),ui(controller::isOpen));View arrow=collapse(controller.panelView());assertNotNull(arrow);assertTrue("Collapse control must be laid out",arrow.getWidth()>0 && arrow.getHeight()>0);arrow.getLocationOnScreen(xy);xy[0]+=arrow.getWidth()/2;xy[1]+=arrow.getHeight()/2;});
        step("tapping collapse arrow at "+xy[0]+","+xy[1]);tap(xy[0],xy[1]);step("collapse tap delivered");final boolean collapseFailed=ui(controller::isOpen);if(collapseFailed) {
            snapshot(xy[0],xy[1]);inputWindows();
            step("original trace="+ui(controller::gestureStatus));
        }inst.runOnMainSync(()->assertFalse("Collapse failed; "+ui(controller::gestureStatus),collapseFailed));
    }
    @Test public void heldDragSnapsToTopThenDownwardSwipeOpens() {
        EdgeDock.Placement p=ui(controller::placement);
        int w=activity.getSystemService(android.view.WindowManager.class).getCurrentWindowMetrics().getBounds().width();
        step("dragging handle to top with a held OS touch stream");
        input("swipe "+(p.x+p.width/2)+" "+(p.y+p.height/2)+" "+(w/2)+" 2 600");settle();
        inst.runOnMainSync(()->{assertEquals(ui(controller::gestureStatus),EdgeDock.TOP,ui(controller::placement).edge);assertTrue(ui(controller::placement).width>ui(controller::placement).height);});
        p=ui(controller::placement);int x=p.x+p.width/2,y=p.y+p.height/2;
        step("opening top dock by downward swipe");
        int inward=Math.round(60*activity.getResources().getDisplayMetrics().density);
        input("swipe "+x+" "+y+" "+x+" "+(y+inward)+" 80");settle();if(!ui(controller::isOpen)) inputWindows();
        inst.runOnMainSync(()->assertTrue(new Store(c).p.getString("overlay_error","Panel did not open after swipe; "+ui(controller::gestureStatus)),ui(controller::isOpen)));
    }

    @Test public void rightInwardSwipeOpensPanel() {
        EdgeDock.Placement p=ui(controller::placement);int x=p.x+p.width/2,y=p.y+p.height/2;
        int inward=Math.round(60*activity.getResources().getDisplayMetrics().density);
        input("swipe "+x+" "+y+" "+(x-inward)+" "+y+" 80");settle();
        inst.runOnMainSync(()->assertTrue("Right swipe failed; "+ui(controller::gestureStatus),ui(controller::isOpen)));
    }

    private void tapCollapseGlyph(String edge) {
        final int[] xy=new int[2];int six=Math.round(6*activity.getResources().getDisplayMetrics().density);
        inst.runOnMainSync(()->{
            View arrow=collapse(controller.panelView());assertNotNull(arrow);arrow.getLocationOnScreen(xy);
            xy[0]+=EdgeDock.TOP.equals(edge)?arrow.getWidth()/2:EdgeDock.LEFT.equals(edge)?arrow.getWidth()-six:six;
            xy[1]+=EdgeDock.TOP.equals(edge)?arrow.getHeight()-six:arrow.getHeight()/2;
        });
        tap(xy[0],xy[1]);
    }
    @Test public void everyDockOpensAndClosesRepeatedlyAtVisibleChevron() {
        for(String edge:new String[]{EdgeDock.RIGHT,EdgeDock.LEFT,EdgeDock.TOP}) for(int cycle=0;cycle<2;cycle++) {
            inst.runOnMainSync(()->controller.moveTo(edge,.55f));settle();
            EdgeDock.Placement p=ui(controller::placement);tap(p.x+p.width/2f,p.y+p.height/2f);
            inst.runOnMainSync(()->assertTrue("Open "+edge+": "+ui(controller::gestureStatus),ui(controller::isOpen)));
            tapCollapseGlyph(edge);
            inst.runOnMainSync(()->assertFalse("Close "+edge+": "+ui(controller::gestureStatus),ui(controller::isOpen)));
            awaitActivityFocus();
        }
    }
    private android.widget.SeekBar seekBar(View view) {
        if(view instanceof android.widget.SeekBar) return (android.widget.SeekBar)view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            android.widget.SeekBar found=seekBar(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
    @Test public void actualPanelTouchChangesMediaVolumeAndRestoresFocus() {
        android.media.AudioManager audio=c.getSystemService(android.media.AudioManager.class);int stream=android.media.AudioManager.STREAM_MUSIC;
        int old=audio.getStreamVolume(stream);
        try {
            audio.setStreamVolume(stream,0,0);
            EdgeDock.Placement p=ui(controller::placement);tap(p.x+p.width/2f,p.y+p.height/2f);
            final int[] xy=new int[2];
            inst.runOnMainSync(()->{
                assertTrue(ui(controller::isOpen));android.widget.SeekBar bar=seekBar(controller.panelView());assertNotNull(bar);assertTrue(bar.isShown());
                bar.getLocationOnScreen(xy);xy[0]+=bar.getWidth()*3/4;xy[1]+=bar.getHeight()/2;
            });
            tap(xy[0],xy[1]);assertTrue("Media control must receive OS touches",audio.getStreamVolume(stream)>0);
            tapCollapseGlyph(EdgeDock.RIGHT);inst.runOnMainSync(()->assertFalse(ui(controller::isOpen)));awaitActivityFocus();
        } finally { audio.setStreamVolume(stream,old,0); }
    }

    @Test public void backClosesInteractivePanelAndReturnsFocus() throws Exception {
        EdgeDock.Placement p=ui(controller::placement);tap(p.x+p.width/2f,p.y+p.height/2f);
        inst.runOnMainSync(()->assertTrue(ui(controller::isOpen)));
        shell("input keyevent 4");
        settle();inst.runOnMainSync(()->assertFalse(ui(controller::isOpen)));awaitActivityFocus();
    }

}
