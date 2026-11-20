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
        inst.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Tambahkan game untuk mulai")));
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
    private <T extends View> T findType(View root,Class<T> kind) {
        if(kind.isInstance(root)) return kind.cast(root);
        if(root instanceof ViewGroup) for(int i=0;i<((ViewGroup)root).getChildCount();i++) { T found=findType(((ViewGroup)root).getChildAt(i),kind);if(found!=null) return found; }
        return null;
    }
    private android.app.AlertDialog reportWindow() throws Exception {
        java.lang.reflect.Field field=MainActivity.class.getDeclaredField("reportWindow");field.setAccessible(true);
        java.util.concurrent.atomic.AtomicReference<android.app.AlertDialog> result=new java.util.concurrent.atomic.AtomicReference<>();
        inst.runOnMainSync(()->{ try { result.set((android.app.AlertDialog)field.get(activity)); } catch(IllegalAccessException error) { throw new AssertionError(error); } });
        return result.get();
    }
    @Test public void sessionReportIsCompactScrollableAndClearlyAttributesTheGame() throws Exception {
        Store store=new Store(context);
        store.p.edit().putString("active_game","com.tencent.igfit").putString("session_id",java.util.UUID.randomUUID().toString())
            .putLong("active_start",1000).putString("session_last_event","running").putString("session_overlay_status","attached")
            .putBoolean("session_finished",true).putInt("session_exit_reason",android.app.ApplicationExitInfo.REASON_LOW_MEMORY).putLong("session_exit_at",5000)
            .putLong("session_pss_kb",20000).putLong("session_pss_first_kb",19000).putLong("session_pss_peak_kb",22000).commit();
        click("Perangkat");click("Detail sesi terakhir / sidebar hilang");
        android.app.AlertDialog dialog=reportWindow();assertNotNull(dialog);inst.waitForIdleSync();
        inst.runOnMainSync(()->{
            android.widget.ScrollView scroll=dialog.getWindow().getDecorView().findViewWithTag("session_report_scroll");assertNotNull(scroll);
            TextView body=dialog.getWindow().getDecorView().findViewWithTag("session_report_body");assertNotNull(body);assertTrue(body.isShown());
            assertTrue(body.getText().toString(),body.getText().toString().contains("PUBGM FLASH"));assertTrue(body.getText().toString(),body.getText().toString().contains("proses spaces karena memori rendah"));
            assertFalse(body.getText().toString().contains("attached"));assertTrue(body.isTextSelectable());
            assertEquals(13f,body.getTextSize()/context.getResources().getDisplayMetrics().scaledDensity,.1f);
            assertTrue("Long evidence must scroll rather than hide below buttons",scroll.canScrollVertically(1));
        });
        screenshot("05-session-report");
    }
    @Test public void historyLoadsOnlyTheRequestedChart() throws Exception {
        Store store=new Store(context);store.clearHistory();
        for(int j=0;j<3;j++) {
            org.json.JSONArray points=new org.json.JSONArray();for(int i=0;i<100;i++) points.put(new org.json.JSONObject().put("at",i*10000).put("temp",35+i*.01));
            store.saveSession(new org.json.JSONObject().put("id","history-"+j).put("game","com.tencent.igfit").put("reason","manual").put("samples",points));
        }
        click("Perangkat");long deadline=android.os.SystemClock.elapsedRealtime()+10000;
        java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
        while(!ready.get() && android.os.SystemClock.elapsedRealtime()<deadline) {
            inst.runOnMainSync(()->ready.set(find(activity.getWindow().getDecorView(),"Grafik & laporan sesi")!=null));android.os.SystemClock.sleep(40);
        }
        assertTrue("History finished loading",ready.get());
        inst.runOnMainSync(()->assertNull("Cards must not materialize all graphs",findType(activity.getWindow().getDecorView(),id.veldafelix.spaces.ui.SessionChart.class)));
        click("Grafik & laporan sesi");
        while(reportWindow()==null && android.os.SystemClock.elapsedRealtime()<deadline) android.os.SystemClock.sleep(40);
        android.app.AlertDialog dialog=reportWindow();assertNotNull(dialog);inst.waitForIdleSync();
        inst.runOnMainSync(()->assertNotNull(findType(dialog.getWindow().getDecorView(),id.veldafelix.spaces.ui.SessionChart.class)));
        screenshot("06-history-report");store.clearHistory();
    }
    private void shell(String command) throws Exception {
        try(android.os.ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand(command);
            java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            byte[] buffer=new byte[1024]; while(in.read(buffer)!=-1) { }
        }
    }
    private void setDnd(android.app.NotificationManager n,int mode) throws Exception {
        n.setInterruptionFilter(mode);
        long deadline=android.os.SystemClock.elapsedRealtime()+3000;
        while(n.getCurrentInterruptionFilter()!=mode && android.os.SystemClock.elapsedRealtime()<deadline) Thread.sleep(25);
        assertEquals("Wait for Android DND transition",mode,n.getCurrentInterruptionFilter());
    }
    @Test public void focusJournalAppliesAndRestoresRealAndroidDnd() throws Exception {
        android.app.NotificationManager n=context.getSystemService(android.app.NotificationManager.class);
        shell("cmd notification allow_dnd "+context.getPackageName());
        assertTrue(n.isNotificationPolicyAccessGranted()); int old=n.getCurrentInterruptionFilter();
        try {
            setDnd(n,android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
            Engine e=new Engine(context); e.applyProfile("com.tencent.igfit");
            assertTrue(e.pending());
            assertEquals(android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS,n.getCurrentInterruptionFilter());
            Engine recreated=new Engine(context); assertTrue(recreated.pending()); recreated.restore();
            assertFalse(recreated.pending());
            assertEquals(android.app.NotificationManager.INTERRUPTION_FILTER_ALL,n.getCurrentInterruptionFilter());
        } finally { setDnd(n,old); shell("cmd notification disallow_dnd "+context.getPackageName()); }
    }
    @Test public void recoveryPreservesAnExternalDndChange() throws Exception {
        android.app.NotificationManager n=context.getSystemService(android.app.NotificationManager.class);
        shell("cmd notification allow_dnd "+context.getPackageName()); int old=n.getCurrentInterruptionFilter();
        try {
            setDnd(n,android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
            Engine e=new Engine(context); e.applyProfile("com.tencent.igfit");
            setDnd(n,android.app.NotificationManager.INTERRUPTION_FILTER_NONE);
            e.restore(); assertFalse(e.pending());
            assertEquals(android.app.NotificationManager.INTERRUPTION_FILTER_NONE,n.getCurrentInterruptionFilter());
        } finally { setDnd(n,old); shell("cmd notification disallow_dnd "+context.getPackageName()); }
    }
    @Test public void revokedDndAccessKeepsJournalForRetry() throws Exception {
        android.app.NotificationManager n=context.getSystemService(android.app.NotificationManager.class);
        shell("cmd notification allow_dnd "+context.getPackageName()); int old=n.getCurrentInterruptionFilter();
        try {
            setDnd(n,android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
            Engine e=new Engine(context); e.applyProfile("com.tencent.igfit");
            shell("cmd notification disallow_dnd "+context.getPackageName());
            e.restore(); assertTrue(e.pending());
            shell("cmd notification allow_dnd "+context.getPackageName());
            e.restore(); assertFalse(e.pending());
        } finally { shell("cmd notification allow_dnd "+context.getPackageName()); setDnd(n,old); shell("cmd notification disallow_dnd "+context.getPackageName()); }
    }
    @Test public void foregroundSessionStartsOnceAndEndsWithHistory() throws Exception {
        Store store=new Store(context);
        store.add("com.tencent.igfit",true);
        store.p.edit().putBoolean("focus",false).putBoolean("overlay",false).commit();
        java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);
        android.content.BroadcastReceiver receiver=new android.content.BroadcastReceiver() {
            public void onReceive(Context c,Intent i) { ready.countDown(); }
        };
        context.registerReceiver(receiver,new android.content.IntentFilter(id.veldafelix.spaces.engine.SessionService.READY),Context.RECEIVER_NOT_EXPORTED);
        java.util.concurrent.CountDownLatch ended=new java.util.concurrent.CountDownLatch(1);
        android.content.SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{
            if(key.equals("active") && !p.getBoolean("active",false)) ended.countDown();
        };
        store.p.registerOnSharedPreferenceChangeListener(listener);
        try {
            Intent start=new Intent(context,id.veldafelix.spaces.engine.SessionService.class).setAction(id.veldafelix.spaces.engine.SessionService.START).putExtra("game","com.tencent.igfit");
            context.startForegroundService(start);
            assertTrue("Session should become ready",ready.await(15,java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(store.p.getBoolean("active",false));
            context.startForegroundService(start); // Duplicate must not create another session.
            context.startService(new Intent(context,id.veldafelix.spaces.engine.SessionService.class).setAction(id.veldafelix.spaces.engine.SessionService.STOP));
            assertTrue("Session should stop",ended.await(15,java.util.concurrent.TimeUnit.SECONDS));
            inst.waitForIdleSync(); assertFalse(store.p.getBoolean("active",true));
            assertEquals(1,store.history().length()); assertFalse(new Engine(context).pending());
        } finally {
            context.unregisterReceiver(receiver); store.p.unregisterOnSharedPreferenceChangeListener(listener);
            context.startService(new Intent(context,id.veldafelix.spaces.engine.SessionService.class).setAction(id.veldafelix.spaces.engine.SessionService.STOP));
        }
    }
    private void tapNav(int index,boolean corner) {
        int[] xy=new int[2];
        inst.runOnMainSync(()->{
            View nav=activity.getWindow().getDecorView().findViewWithTag("nav_"+index);assertNotNull(nav);
            nav.getLocationOnScreen(xy);xy[0]+=corner?3:nav.getWidth()/2;xy[1]+=corner?3:nav.getHeight()-3;
        });
        long at=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(at,at,android.view.MotionEvent.ACTION_DOWN,xy[0],xy[1],0);
        android.view.MotionEvent up=android.view.MotionEvent.obtain(at,at+40,android.view.MotionEvent.ACTION_UP,xy[0],xy[1],0);
        inst.sendPointerSync(down);inst.sendPointerSync(up);down.recycle();up.recycle();inst.waitForIdleSync();
    }
    @Test public void navigationRespondsAtPaddingAndKeepsTheSameRail() {
        View before=activity.getWindow().getDecorView().findViewWithTag("nav_1");
        tapNav(1,true);
        inst.runOnMainSync(()->{
            assertNotNull(find(activity.getWindow().getDecorView(),"Profil performa"));
            assertSame(before,activity.getWindow().getDecorView().findViewWithTag("nav_1"));
        });
        tapNav(2,false);inst.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Perangkat & sesi")));
        tapNav(0,false);inst.runOnMainSync(()->assertNotNull(find(activity.getWindow().getDecorView(),"Koleksi game")));
    }
    private void screenshot(String name) {
        try {
            Bitmap bitmap=inst.getUiAutomation().takeScreenshot();
            File dir=new File(context.getExternalFilesDir(null),"screenshots"); dir.mkdirs();
            try(FileOutputStream f=new FileOutputStream(new File(dir,name+".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG,100,f); }
            if(name.equals("05-session-report")) {
                Bitmap preview=Bitmap.createScaledBitmap(bitmap,720,Math.max(1,bitmap.getHeight()*720/bitmap.getWidth()),true);
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();preview.compress(Bitmap.CompressFormat.JPEG,75,bytes);
                android.os.Bundle evidence=new android.os.Bundle();evidence.putString("spaces_image",android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));inst.sendStatus(0,evidence);
                if(preview!=bitmap) preview.recycle();
            }
            bitmap.recycle();
        } catch(Exception e) { throw new AssertionError(e); }
    }
}
