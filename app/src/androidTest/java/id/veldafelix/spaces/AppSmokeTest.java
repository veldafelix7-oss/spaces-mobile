package id.veldafelix.spaces;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
import id.veldafelix.spaces.engine.Engine;
import id.veldafelix.spaces.engine.Telemetry;

@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {
    private Instrumentation inst;
    private Context context;
    private MainActivity activity;
    @Before public void launch() {
        inst=InstrumentationRegistry.getInstrumentation(); context=inst.getTargetContext();
        context.getSharedPreferences("spaces",0).edit().clear().commit();
        activity=(MainActivity)inst.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        inst.waitForIdleSync();
    }
    @After public void finish() { inst.runOnMainSync(()->activity.finish()); inst.waitForIdleSync(); }
    private TextView find(View v,String value) {
        if(v instanceof TextView && ((TextView)v).getText().toString().equals(value)) return (TextView)v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) { TextView r=find(((ViewGroup)v).getChildAt(i),value); if(r!=null) return r; }
        return null;
    }
    private void click(String value) {
        inst.runOnMainSync(()->{ TextView v=find(activity.getWindow().getDecorView(),value); assertNotNull(value,v); if(v.isClickable()) v.performClick(); else ((View)v.getParent()).performClick(); }); inst.waitForIdleSync();
    }
    @Test public void startsLandscapeAndEmpty() {
        assertEquals(android.content.res.Configuration.ORIENTATION_LANDSCAPE,activity.getResources().getConfiguration().orientation);
        assertFalse(new Store(context).added("com.tencent.igfit"));
        inst.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Ruangnya siap. Game-nya pilih dulu.")));
        screenshot("01-home");
    }
    @Test public void engineAndDeviceRenderWithoutPermissions() { click("Performa"); screenshot("02-engine"); click("Perangkat"); screenshot("03-device"); }
    @Test public void switchesLanguage() { click("ID"); inst.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Your collection"))); }
    @Test public void catalogCannotAddOtherApps() {
        try { new Store(context).add("com.whatsapp",true); fail("Should reject"); } catch(IllegalArgumentException expected) { }
    }
    @Test public void settingsPersist() {
        Store store=new Store(context); store.setProfile("com.tencent.igfit","refresh","60");
        assertEquals("60",new Store(context).profile("com.tencent.igfit","refresh","system"));
    }
    @Test public void telemetryUsesRealBatteryAndMemory() {
        org.json.JSONObject point=Telemetry.sample(context); assertTrue(point.has("at")); assertTrue(point.optLong("totalMb")>0);
    }
    @Test public void unreadableRecoveryJournalBlocksMutation() {
        new Store(context).p.edit().putString("journal","not-json").commit();
        Engine e=new Engine(context); assertTrue(e.pending()); assertTrue(e.restore().contains("unreadable"));
        assertEquals("not-json",new Store(context).p.getString("journal",""));
    }
    @Test public void supportedCardsRenderAndRemainUnavailableIfMissing() {
        new Store(context).add("com.tencent.igfit",true); new Store(context).add("com.netease.newspike",true);
        click("Performa"); click("Main");
        inst.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"PUBGM FLASH")));
        screenshot("04-collection-test-only");
    }
    @Test public void emptyRecoveryIsSafe() { Engine e=new Engine(context); assertFalse(e.pending()); assertEquals("",e.restore()); }
    private void shell(String command) throws Exception {
        try(android.os.ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand(command);
            java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            byte[] buffer=new byte[1024]; while(in.read(buffer)!=-1) { }
        }
    }
    @Test public void focusJournalAppliesAndRestoresRealAndroidDnd() throws Exception {
        android.app.NotificationManager n=context.getSystemService(android.app.NotificationManager.class);
        shell("cmd notification allow_dnd "+context.getPackageName());
        assertTrue(n.isNotificationPolicyAccessGranted()); int old=n.getCurrentInterruptionFilter();
        try {
            n.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
            Engine e=new Engine(context); e.applyProfile("com.tencent.igfit");
            assertTrue(e.pending());
            assertEquals(android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS,n.getCurrentInterruptionFilter());
            Engine recreated=new Engine(context); assertTrue(recreated.pending()); recreated.restore();
            assertFalse(recreated.pending());
            assertEquals(android.app.NotificationManager.INTERRUPTION_FILTER_ALL,n.getCurrentInterruptionFilter());
        } finally { n.setInterruptionFilter(old); shell("cmd notification disallow_dnd "+context.getPackageName()); }
    }
    @Test public void recoveryPreservesAnExternalDndChange() throws Exception {
        android.app.NotificationManager n=context.getSystemService(android.app.NotificationManager.class);
        shell("cmd notification allow_dnd "+context.getPackageName()); int old=n.getCurrentInterruptionFilter();
        try {
            n.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
            Engine e=new Engine(context); e.applyProfile("com.tencent.igfit");
            n.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_NONE);
            e.restore(); assertFalse(e.pending());
            assertEquals(android.app.NotificationManager.INTERRUPTION_FILTER_NONE,n.getCurrentInterruptionFilter());
        } finally { n.setInterruptionFilter(old); shell("cmd notification disallow_dnd "+context.getPackageName()); }
    }
    @Test public void revokedDndAccessKeepsJournalForRetry() throws Exception {
        android.app.NotificationManager n=context.getSystemService(android.app.NotificationManager.class);
        shell("cmd notification allow_dnd "+context.getPackageName()); int old=n.getCurrentInterruptionFilter();
        try {
            n.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
            Engine e=new Engine(context); e.applyProfile("com.tencent.igfit");
            shell("cmd notification disallow_dnd "+context.getPackageName());
            e.restore(); assertTrue(e.pending());
            shell("cmd notification allow_dnd "+context.getPackageName());
            e.restore(); assertFalse(e.pending());
        } finally { shell("cmd notification allow_dnd "+context.getPackageName()); n.setInterruptionFilter(old); shell("cmd notification disallow_dnd "+context.getPackageName()); }
    }
    private void screenshot(String name) {
        try {
            Bitmap bitmap=inst.getUiAutomation().takeScreenshot();
            File dir=new File(context.getExternalFilesDir(null),"screenshots"); dir.mkdirs();
            try(FileOutputStream f=new FileOutputStream(new File(dir,name+".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG,100,f); }
            bitmap.recycle();
        } catch(Exception e) { throw new AssertionError(e); }
    }
}
