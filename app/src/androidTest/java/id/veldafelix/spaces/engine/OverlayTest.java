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
        inst.runOnMainSync(()->{controller.moveTo(EdgeDock.TOP,.6f);assertTrue(controller.placement().y>=0);assertTrue(controller.placement().width>controller.placement().height);
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
    private void pointer(int action,long down,float x,float y) {
        android.view.MotionEvent e=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,x,y,0);
        e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        try { assertTrue("OS input injection rejected",inst.getUiAutomation().injectInputEvent(e,false)); }
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
    }
    private void inputWindows() {
        try(ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand("dumpsys input");
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1) bytes.write(b,0,n);
            StringBuilder diagnostic=new StringBuilder();int count=0;
            for(String line:bytes.toString("UTF-8").split("\n")) {
                if(line.contains("frame=") && count++<4) diagnostic.append(line.trim()).append(" | ");
                if(line.contains("displayId=0") && (line.contains("rotation=")||line.contains("orientation="))) diagnostic.append(line.trim()).append(" | ");
            }
            step(diagnostic.toString());
        } catch(Exception e) { step("Input diagnostic: "+e.getClass().getSimpleName()); }
    }
    private void snapshot(float x,float y) {
        android.graphics.Bitmap bitmap=inst.getUiAutomation().takeScreenshot();
        if(bitmap==null) return;
        android.graphics.Bitmap image=bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888,true);bitmap.recycle();
        android.graphics.Canvas canvas=new android.graphics.Canvas(image);android.graphics.Paint paint=new android.graphics.Paint();
        paint.setColor(android.graphics.Color.RED);paint.setStrokeWidth(5);
        canvas.drawLine(x-25,y,x+25,y,paint);canvas.drawLine(x,y-25,x,y+25,paint);
        android.graphics.Bitmap small=android.graphics.Bitmap.createScaledBitmap(image,640,360,true);image.recycle();
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();small.compress(android.graphics.Bitmap.CompressFormat.JPEG,75,bytes);small.recycle();
        android.os.Bundle status=new android.os.Bundle();status.putString("spaces_image",android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));inst.sendStatus(2,status);
    }
    private void tap(float x,float y) { input("tap "+Math.round(x)+" "+Math.round(y));settle(); }
    @Test public void actualWindowTapsOpenAndCollapsePanel() {
        step("opening by actual handle tap");
        EdgeDock.Placement p=controller.placement();tap(p.x+p.width/2f,p.y+p.height/2f);step("panel opened; locating collapse arrow");
        final int[] xy=new int[2];
        inst.runOnMainSync(()->{assertTrue(new Store(c).p.getString("overlay_error","Panel did not open after tap; "+controller.gestureStatus()),controller.isOpen());View arrow=collapse(controller.panelView());assertNotNull(arrow);assertTrue("Collapse control must be laid out",arrow.getWidth()>0 && arrow.getHeight()>0);arrow.getLocationOnScreen(xy);xy[0]+=arrow.getWidth()/2;xy[1]+=arrow.getHeight()/2;});
        step("tapping collapse arrow at "+xy[0]+","+xy[1]);tap(xy[0],xy[1]);step("collapse tap delivered");final boolean collapseFailed=controller.isOpen();if(collapseFailed) {
            snapshot(xy[0],xy[1]);inputWindows();
            final int[] origin=new int[2];inst.runOnMainSync(()->{controller.panelView().getLocationOnScreen(origin);step("panel origin="+origin[0]+","+origin[1]+" size="+controller.panelView().getWidth()+"x"+controller.panelView().getHeight());});
            tap(p.x+p.width/2f,p.y+p.height/2f);step("tap former handle region: "+controller.gestureStatus());
            tap(origin[0]+controller.panelView().getWidth()/2f,origin[1]+controller.panelView().getHeight()/2f);step("tap panel center: "+controller.gestureStatus());
            tap(xy[0]+20,xy[1]);step("tap 20px inside collapse: "+controller.gestureStatus());
        }inst.runOnMainSync(()->assertFalse("Collapse failed; "+controller.gestureStatus(),collapseFailed));
    }
    @Test public void heldDragSnapsToTopThenDownwardSwipeOpens() {
        EdgeDock.Placement p=controller.placement();
        int w=activity.getSystemService(android.view.WindowManager.class).getCurrentWindowMetrics().getBounds().width();
        step("dragging handle to top with a held OS touch stream");
        input("swipe "+(p.x+p.width/2)+" "+(p.y+p.height/2)+" "+(w/2)+" 2 600");settle();
        inst.runOnMainSync(()->{assertEquals(EdgeDock.TOP,controller.placement().edge);assertTrue(controller.placement().width>controller.placement().height);});
        p=controller.placement();int x=p.x+p.width/2,y=p.y+p.height/2;
        step("opening top dock by downward swipe");
        int inward=Math.round(60*activity.getResources().getDisplayMetrics().density);
        input("swipe "+x+" "+y+" "+x+" "+(y+inward)+" 80");settle();if(!controller.isOpen()) inputWindows();
        inst.runOnMainSync(()->assertTrue(new Store(c).p.getString("overlay_error","Panel did not open after swipe; "+controller.gestureStatus()),controller.isOpen()));
    }

    @Test public void rightInwardSwipeOpensPanel() {
        EdgeDock.Placement p=controller.placement();int x=p.x+p.width/2,y=p.y+p.height/2;
        int inward=Math.round(60*activity.getResources().getDisplayMetrics().density);
        input("swipe "+x+" "+y+" "+(x-inward)+" "+y+" 80");settle();
        inst.runOnMainSync(()->assertTrue("Right swipe failed; "+controller.gestureStatus(),controller.isOpen()));
    }

}
