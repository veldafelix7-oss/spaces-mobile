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
    private void shell(String command) throws Exception {
        try(ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand(command);
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) { byte[] b=new byte[512];while(in.read(b)!=-1) {} }
    }
    @Before public void start() throws Exception {
        inst=InstrumentationRegistry.getInstrumentation();c=inst.getTargetContext();
        c.getSharedPreferences("spaces",0).edit().clear().commit();
        activity=(MainActivity)inst.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));inst.waitForIdleSync();
        shell("appops set "+c.getPackageName()+" SYSTEM_ALERT_WINDOW allow");
        assertTrue(android.provider.Settings.canDrawOverlays(c));
        inst.runOnMainSync(()->{controller=new OverlayController(activity,()->{});controller.show();});settle();
    }
    @After public void finish() throws Exception {
        inst.runOnMainSync(()->{controller.hide();activity.finish();});inst.waitForIdleSync();shell("appops set "+c.getPackageName()+" SYSTEM_ALERT_WINDOW default");
    }
    @Test public void initialHandleIsFlushWithRightEdge() {
        inst.runOnMainSync(()->{assertNotNull(controller.handleView());assertEquals(EdgeDock.RIGHT,controller.placement().edge);
            int width=activity.getSystemService(android.view.WindowManager.class).getCurrentWindowMetrics().getBounds().width();
            assertEquals(width,controller.placement().x+controller.placement().width);assertTrue(controller.placement().height>controller.placement().width);});
    }
    @Test public void topDockRotatesAndRestoresPreference() {
        inst.runOnMainSync(()->{controller.moveTo(EdgeDock.TOP,.6f);assertEquals(0,controller.placement().y);assertTrue(controller.placement().width>controller.placement().height);
            controller.hide();controller=new OverlayController(activity,()->{});controller.show();assertEquals(EdgeDock.TOP,controller.placement().edge);assertEquals(.6f,controller.placement().fraction,.001f);});
    }
    private View collapse(View v) {
        if("Tutup panel sesi".contentEquals(v.getContentDescription()==null?"":v.getContentDescription())) return v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) {View found=collapse(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    @Test public void edgeArrowClosesWithoutScrollingAndRestoresHandle() {
        inst.runOnMainSync(()->{controller.handleView().performClick();assertTrue(controller.isOpen());assertFalse(controller.handleView().isAttachedToWindow());
            View arrow=collapse(controller.panelView());assertNotNull(arrow);assertTrue(arrow.isShown());arrow.performClick();assertFalse(controller.isOpen());assertEquals(View.VISIBLE,controller.handleView().getVisibility());});
    }
    @Test public void leftAndTopPanelsAlsoCloseFromTheirEdge() {
        inst.runOnMainSync(()->{for(String edge:new String[]{EdgeDock.LEFT,EdgeDock.TOP}) {controller.moveTo(edge,.5f);controller.handleView().performClick();assertTrue(controller.isOpen());assertNotNull(collapse(controller.panelView()));collapse(controller.panelView()).performClick();assertFalse(controller.isOpen());}});
    }
    @Test public void legacyPositionMigratesToDockAndStaysWithinScreen() {
        inst.runOnMainSync(()->{controller.hide();new Store(c).p.edit().remove("dock_edge").remove("dock_offset").putInt("handle_x",99999).putInt("handle_y",99999).commit();
            controller=new OverlayController(activity,()->{});controller.show();assertNotNull(controller.handleView());assertTrue(controller.placement().x>=0);assertTrue(controller.placement().y>=0);assertTrue(new Store(c).p.contains("dock_edge"));assertFalse(new Store(c).p.contains("handle_x"));});
    }
    private void step(String message) {
        android.os.Bundle status=new android.os.Bundle();status.putString("spaces_step",message);inst.sendStatus(2,status);
    }
    private void input(String args) {
        try { shell("input touchscreen "+args); }
        catch(Exception e) { throw new AssertionError("Could not deliver shell touch input",e); }
    }
    private void settle() {
        // A queued UI-frame latch is bounded; waiting for a globally idle accessibility stream
        // can stall when an overlay itself keeps producing accessibility/window events.
        java.util.concurrent.CountDownLatch frame=new java.util.concurrent.CountDownLatch(1);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(frame::countDown,1000);
        try { assertTrue("Overlay UI frame timed out",frame.await(4,java.util.concurrent.TimeUnit.SECONDS)); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new AssertionError(e); }
    }
    private void inputWindows() {
        try(ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("dumpsys input");
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1) bytes.write(b,0,n);
            for(String line:bytes.toString("UTF-8").split("\n")) if(line.contains("spaces-handle")||line.contains("spaces-panel")) step(line.trim().substring(0,Math.min(1000,line.trim().length())));
        } catch(Exception e) { step("Input diagnostic: "+e.getClass().getSimpleName()); }
    }
    private void tap(float x,float y) { input("tap "+Math.round(x)+" "+Math.round(y));settle(); }
    @Test public void actualWindowTapsOpenAndCollapsePanel() {
        step("opening by actual handle tap");
        EdgeDock.Placement p=controller.placement();tap(p.x+p.width/2f,p.y+p.height/2f);step("panel opened; locating collapse arrow");
        final int[] xy=new int[2];
        inst.runOnMainSync(()->{assertTrue(new Store(c).p.getString("overlay_error","Panel did not open after tap; "+controller.gestureStatus()),controller.isOpen());View arrow=collapse(controller.panelView());assertNotNull(arrow);assertTrue("Collapse control must be laid out",arrow.getWidth()>0 && arrow.getHeight()>0);arrow.getLocationOnScreen(xy);xy[0]+=arrow.getWidth()/2;xy[1]+=arrow.getHeight()/2;});
        step("tapping collapse arrow at "+xy[0]+","+xy[1]);tap(xy[0],xy[1]);step("collapse tap delivered");if(controller.isOpen()) inputWindows();inst.runOnMainSync(()->assertFalse("Collapse failed; "+controller.gestureStatus(),controller.isOpen()));
    }
    @Test public void heldDragSnapsToTopThenTapOpens() {
        EdgeDock.Placement p=controller.placement();
        int w=activity.getSystemService(android.view.WindowManager.class).getCurrentWindowMetrics().getBounds().width();
        step("dragging handle to top with a held shell swipe");
        input("swipe "+(p.x+p.width/2)+" "+(p.y+p.height/2)+" "+(w/2)+" 2 600");settle();
        inst.runOnMainSync(()->{assertEquals(EdgeDock.TOP,controller.placement().edge);assertTrue(controller.placement().width>controller.placement().height);});
        p=controller.placement();int x=p.x+p.width/2,y=p.height/2;
        step("opening top dock by tap; downward system-bar swipes are OS-reserved");
        tap(x,y);if(!controller.isOpen()) inputWindows();
        inst.runOnMainSync(()->assertTrue(new Store(c).p.getString("overlay_error","Panel did not open after swipe; "+controller.gestureStatus()),controller.isOpen()));
    }

    @Test public void rightInwardSwipeOpensPanel() {
        EdgeDock.Placement p=controller.placement();int x=p.x+p.width/2,y=p.y+p.height/2;
        int inward=Math.round(60*activity.getResources().getDisplayMetrics().density);
        input("swipe "+x+" "+y+" "+(x-inward)+" "+y+" 80");settle();
        inst.runOnMainSync(()->assertTrue("Right swipe failed; "+controller.gestureStatus(),controller.isOpen()));
    }

}
