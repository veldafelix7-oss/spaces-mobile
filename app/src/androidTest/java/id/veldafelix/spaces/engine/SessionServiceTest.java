package id.veldafelix.spaces.engine;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import id.veldafelix.spaces.MainActivity;
import id.veldafelix.spaces.Store;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Real service/overlay lifecycle; fixture package is allowlisted, not an installed/running game. */
@RunWith(AndroidJUnit4.class)
public class SessionServiceTest {
    private Instrumentation inst;
    private Context c;
    private Store store;
    private MainActivity activity;
    private CountDownLatch ready;
    private BroadcastReceiver receiver;
    private Intent command(String action) { return new Intent(c,SessionService.class).setAction(action).putExtra("game","com.tencent.igfit"); }
    private void await(String message,BooleanSupplier condition,long timeout) {
        long deadline=SystemClock.elapsedRealtime()+timeout;
        while(SystemClock.elapsedRealtime()<deadline) { if(condition.getAsBoolean()) return;SystemClock.sleep(40); }
        fail(message+"; "+SessionDiagnostics.report(c));
    }
    private void shell(String command) throws Exception {
        try(ParcelFileDescriptor fd=inst.getUiAutomation().executeShellCommand(command);
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            byte[] bytes=new byte[1024];while(in.read(bytes)!=-1) { }
        }
    }
    @Before public void setup() throws Exception {
        inst=InstrumentationRegistry.getInstrumentation();c=inst.getTargetContext();store=new Store(c);
        c.stopService(new Intent(c,SessionService.class));
        await("Previous service must stop",()->!SessionService.running,5000);
        Engine.WORK.submit(()->{}).get(15,TimeUnit.SECONDS);inst.runOnMainSync(()->{});
        store.clearHistory();
        store.p.edit().clear().putBoolean("focus",false).putBoolean("overlay",true).commit();
        store.add("com.tencent.igfit",true);
        assertTrue(inst.getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_FREEZE_90));
        activity=(MainActivity)inst.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        shell("appops set "+c.getPackageName()+" SYSTEM_ALERT_WINDOW allow");
        shell("appops set "+c.getPackageName()+" GET_USAGE_STATS allow");
        assertTrue(android.provider.Settings.canDrawOverlays(c));assertTrue(Telemetry.usageAllowed(c));
        ready=new CountDownLatch(1);
        receiver=new BroadcastReceiver() { @Override public void onReceive(Context context,Intent intent) { ready.countDown(); } };
        c.registerReceiver(receiver,new IntentFilter(SessionService.READY),Context.RECEIVER_NOT_EXPORTED);
    }
    private void start() throws Exception {
        c.startForegroundService(command(SessionService.START));
        assertTrue("Service readiness",ready.await(15,TimeUnit.SECONDS));
        await("First heartbeat",()->store.p.getLong("session_heartbeat",0)>0,5000);
    }
    private void stopNormally() {
        c.startService(command(SessionService.STOP));
        await("Manual stop",()->!SessionService.running && !store.p.getBoolean("active",false),15000);
    }
    @After public void cleanup() throws Exception {
        try {
            if(SessionService.running) stopNormally();
            c.stopService(new Intent(c,SessionService.class));
            Engine.WORK.submit(()->{}).get(15,TimeUnit.SECONDS);
        } finally {
            if(receiver!=null) c.unregisterReceiver(receiver);
            if(activity!=null) inst.runOnMainSync(()->activity.finish());
            shell("appops set "+c.getPackageName()+" SYSTEM_ALERT_WINDOW default");
            shell("appops set "+c.getPackageName()+" GET_USAGE_STATS default");
            // Fixtures never modify system settings (Focus is off); clear synthetic recovery data.
            store.p.edit().clear().commit();
        }
    }
    @AfterClass public static void rotation() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);
    }
    @Test public void manualDefaultKeepsServiceAndAttachedSidebarPastExitGraceInBackground() throws Exception {
        assertFalse(store.p.contains("auto_end"));start();
        assertFalse(store.p.getBoolean("session_auto_end",true));
        long first=store.p.getLong("session_heartbeat",0);
        await("First process-memory sample",()->store.p.getLong("session_memory_at",0)>0,5000);
        long firstMemory=store.p.getLong("session_memory_at",0);
        shell("input keyevent 3"); // Real launcher switch; do not confuse this with gameplay certification.
        await("Service must keep sampling past the old 30-second exit grace",()->store.p.getLong("session_heartbeat",0)>=first+61000,80000);
        assertTrue(SessionService.running);assertTrue(store.p.getBoolean("active",false));
        java.lang.reflect.Field page=MainActivity.class.getDeclaredField("page");page.setAccessible(true);
        inst.runOnMainSync(()->{ try { assertNull("Background activity must release history/UI",page.get(activity)); } catch(IllegalAccessException e) { throw new AssertionError(e); } });
        assertFalse("No whole-session JSON in preferences",store.p.contains("draft_samples"));
        assertTrue(store.draftSamples().length()>=4);
        await("A second periodic process-memory sample",()->store.p.getLong("session_memory_at",0)>=firstMemory+60000,5000);
        assertTrue(store.p.getLong("session_pss_kb",0)>0);
        android.os.Bundle evidence=new android.os.Bundle();
        evidence.putString("spaces_diagnostic","Background fixture PSS KB: first="+store.p.getLong("session_pss_first_kb",0)+", last="+store.p.getLong("session_pss_kb",0)+", peak="+store.p.getLong("session_pss_peak_kb",0)+"; not PUBG/HiOS certification");inst.sendStatus(0,evidence);
        assertEquals("attached",store.p.getString("session_overlay_status",""));
        assertEquals(0,store.history().length());
        stopNormally();assertEquals(1,store.history().length());
        assertEquals("manual",store.history().getJSONObject(0).getString("reason"));
        assertEquals("com.tencent.igfit",store.history().getJSONObject(0).getJSONObject("diagnostics").getString("game"));
        assertFalse(store.history().getJSONObject(0).has("samples"));
        assertTrue(store.sessionSamples(store.history().getJSONObject(0)).length()>=4);
    }
    @Test public void interruptedServicePreservesJournalDraftAndLocalEvidence() throws Exception {
        start();store.p.edit().putString("journal","fixture-corrupt-journal").commit();
        c.stopService(new Intent(c,SessionService.class));
        await("Service destroyed",()->!SessionService.running,5000);
        assertTrue(store.p.getBoolean("active",false));
        assertEquals("fixture-corrupt-journal",store.p.getString("journal",""));
        assertTrue(store.draftSamples().length()>0);assertFalse(store.p.contains("draft_samples"));assertTrue(new Engine(c).pending());
        assertEquals("service_destroyed",store.p.getString("session_last_event",""));
        assertEquals("service_stopped",store.p.getString("session_overlay_status",""));
        assertEquals(0,store.history().length());assertFalse(SessionDiagnostics.report(c).isEmpty());
    }
    @Test public void userCanReattachSidebarWithoutEndingTheActiveSession() throws Exception {
        start();c.startService(command(SessionService.RESTORE_OVERLAY));
        await("Explicit overlay repair",()->"overlay_restored".equals(store.p.getString("session_last_event","")),5000);
        assertEquals("attached",store.p.getString("session_overlay_status",""));
        assertTrue(SessionService.running);assertTrue(store.p.getBoolean("active",false));assertEquals(0,store.history().length());
        stopNormally();
    }
    @Test public void failedHistorySaveDoesNotDiscardActiveDraftOrClaimCompletion() throws Exception {
        start();store.p.edit().putString("history","corrupt-fixture").commit();
        c.startService(command(SessionService.STOP));
        await("Service ends safely after save failure",()->!SessionService.running,15000);
        assertTrue(store.p.getBoolean("active",false));assertTrue(store.draftSamples().length()>0);
        assertEquals("corrupt-fixture",store.p.getString("history",""));assertEquals("save_failed",store.p.getString("session_last_event",""));
    }
    @Test public void stopDuringQueuedProfileWorkCannotCreateGhostOverlayOrApplyLater() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        Engine.WORK.execute(()->{ entered.countDown();try { release.await(15,TimeUnit.SECONDS); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } });
        assertTrue(entered.await(5,TimeUnit.SECONDS));
        try {
            c.startForegroundService(command(SessionService.START));
            await("Starting state persisted",()->store.p.getBoolean("active",false),5000);
            c.stopService(new Intent(c,SessionService.class));await("Stopped during initialization",()->!SessionService.running,5000);
        } finally { release.countDown(); }
        Engine.WORK.submit(()->{}).get(15,TimeUnit.SECONDS);inst.runOnMainSync(()->{});
        assertEquals(1,ready.getCount());assertTrue(store.p.getBoolean("active",false));
        assertFalse(store.p.contains("last_engine_log"));
        assertEquals("service_stopped",store.p.getString("session_overlay_status",""));
    }
}
