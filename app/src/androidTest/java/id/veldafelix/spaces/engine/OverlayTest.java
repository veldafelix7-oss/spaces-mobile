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
        inst.runOnMainSync(()->{controller=new OverlayController(activity,()->{});controller.show();});inst.waitForIdleSync();
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
        inst.runOnMainSync(()->{controller.handleView().performClick();assertTrue(controller.isOpen());assertEquals(View.INVISIBLE,controller.handleView().getVisibility());
            View arrow=collapse(controller.panelView());assertNotNull(arrow);assertTrue(arrow.isShown());arrow.performClick();assertFalse(controller.isOpen());assertEquals(View.VISIBLE,controller.handleView().getVisibility());});
    }
    @Test public void leftAndTopPanelsAlsoCloseFromTheirEdge() {
        inst.runOnMainSync(()->{for(String edge:new String[]{EdgeDock.LEFT,EdgeDock.TOP}) {controller.moveTo(edge,.5f);controller.handleView().performClick();assertTrue(controller.isOpen());assertNotNull(collapse(controller.panelView()));collapse(controller.panelView()).performClick();assertFalse(controller.isOpen());}});
    }
    @Test public void legacyPositionMigratesToDockAndStaysWithinScreen() {
        inst.runOnMainSync(()->{controller.hide();new Store(c).p.edit().remove("dock_edge").remove("dock_offset").putInt("handle_x",99999).putInt("handle_y",99999).commit();
            controller=new OverlayController(activity,()->{});controller.show();assertNotNull(controller.handleView());assertTrue(controller.placement().x>=0);assertTrue(controller.placement().y>=0);assertTrue(new Store(c).p.contains("dock_edge"));assertFalse(new Store(c).p.contains("handle_x"));});
    }
}
