package id.veldafelix.spaces.engine;

import android.app.*;
import android.content.Intent;
import android.os.*;
import id.veldafelix.spaces.MainActivity;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.core.GameSpec;
import id.veldafelix.spaces.core.SessionExitMonitor;
import id.veldafelix.spaces.core.SampleJournal;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SessionService extends Service {
    public static final String START="spaces.START", STOP="spaces.STOP", READY="spaces.READY", RESTORE_OVERLAY="spaces.RESTORE_OVERLAY";
    public static volatile boolean running=false;
    private Store store;
    private String game;
    private long startWall,startElapsed,lastPoll;
    private final SessionExitMonitor exitMonitor=new SessionExitMonitor();
    private boolean autoEnd;
    private int overlayRepairAttempts=0;
    private long overlayAttachAt;
    private SampleJournal sampleJournal;
    private long memoryAt;
    private final Handler main=new Handler(Looper.getMainLooper());
    private OverlayController overlay;
    private volatile boolean ending=false,destroyed=false;
    private boolean initialized=false;
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(ending || destroyed || !initialized) return;
            final JSONObject point=Telemetry.sample(SessionService.this);
            // Queue at most one sample; the next tick is scheduled only after persistence completes.
            Engine.WORK.execute(()->{
                if(ending || destroyed) return;
                try {
                    sampleJournal.append(point.toString());
                    long wall=point.optLong("at",System.currentTimeMillis());
                    store.p.edit().putString("live_sample",point.toString()).putLong("session_heartbeat",wall).remove("session_sample_error").commit();
                    long now=SystemClock.elapsedRealtime();
                    if(memoryAt==0 || now-memoryAt>=60000) { SessionDiagnostics.sampleMemory(SessionService.this);memoryAt=now; }
                } catch(Exception error) { store.p.edit().putBoolean("session_sample_error",true).commit(); }
                main.post(()->finishTick(point));
            });
        }
    };
    private void finishTick(JSONObject point) {
            if(ending || destroyed || !initialized) return;
            long wall=System.currentTimeMillis();
            checkOverlay();
            if(overlay!=null) overlay.update(point);
            if(autoEnd) {
                boolean allowed=Telemetry.usageAllowed(SessionService.this);
                boolean observed=Telemetry.observePresence(SessionService.this,lastPoll,wall,game,exitMonitor);
                lastPoll=wall;
                PowerManager power=getSystemService(PowerManager.class);
                KeyguardManager lock=getSystemService(KeyguardManager.class);
                boolean screenUsable=power!=null && power.isInteractive() && lock!=null && !lock.isKeyguardLocked();
                long now=SystemClock.elapsedRealtime();
                if(exitMonitor.shouldEnd(now,now-startElapsed,true,allowed && observed,screenUsable,overlay!=null && overlay.isOpen())) { end("left_game");return; }
            }
            main.postDelayed(tick,10000);
    }
    @Override public void onCreate() { super.onCreate();store=new Store(this); }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent==null) { stopSelf();return START_NOT_STICKY; }
        if(STOP.equals(intent.getAction())) {
            if(running) end("manual");else stopSelf();
            return START_NOT_STICKY;
        }
        if(RESTORE_OVERLAY.equals(intent.getAction())) {
            if(running && initialized && !ending && !destroyed) {
                overlayRepairAttempts=0;attachOverlay(true);
            } else if(!running) stopSelf();
            return START_NOT_STICKY;
        }
        if(!START.equals(intent.getAction()) || running) return START_NOT_STICKY;
        String requested=intent.getStringExtra("game");
        if(GameSpec.find(requested)==null || !store.added(requested)) { stopSelf();return START_NOT_STICKY; }
        if(store.p.getBoolean("active",false) || new Engine(this).pending()) { stopSelf();return START_NOT_STICKY; }
        game=requested;autoEnd=store.p.getBoolean("auto_end",false);
        startWall=System.currentTimeMillis();startElapsed=SystemClock.elapsedRealtime();lastPoll=startWall;
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("session",store.t("Sesi permainan","Gaming session"),NotificationManager.IMPORTANCE_LOW));
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent launch=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,SessionService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"session").setSmallIcon(id.veldafelix.spaces.R.drawable.ic_status)
            .setContentTitle("spaces : mobile · "+GameSpec.find(game).title)
            .setContentText(autoEnd && Telemetry.usageAllowed(this)?store.t("Sesi aktif · deteksi keluar opsional aktif","Session active · optional exit detection enabled"):store.t("Sesi aktif · akhiri manual setelah main","Session active · end manually after playing"))
            .setContentIntent(launch).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,store.t("Akhiri & pulihkan","End & restore"),stop).build()).build();
        startForeground(41,n);running=true;
        boolean saved=store.p.edit().putBoolean("active",true).putString("active_game",game).putLong("active_start",startWall)
            .putBoolean("session_auto_end",autoEnd).putInt("session_process_pid",android.os.Process.myPid()).putLong("session_heartbeat",0)
            .putString("session_overlay_status","starting").putLong("session_overlay_at",startWall)
            .putString("session_id",java.util.UUID.randomUUID().toString()).putInt("draft_format",1).remove("draft_samples")
            .remove("overlay_error").remove("last_engine_log").remove("session_sample_error").remove("session_exit_reason").remove("session_exit_at")
            .remove("session_pss_first_kb").remove("session_pss_kb").remove("session_pss_peak_kb").remove("session_memory_at")
            .putBoolean("session_finished",false).remove("session_closed_at").commit();
        if(!saved) { running=false;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY; }
        sampleJournal=store.draftJournal();
        SessionDiagnostics.note(this,"starting");
        Engine.WORK.execute(()->{
            if(ending || destroyed) return;
            try { new Engine(this).applyProfile(game); }
            finally { main.post(()->{
                // A service stopped during profile work must not create a ghost overlay/READY event.
                if(ending || destroyed) return;
                initialized=true;attachOverlay(false);SessionDiagnostics.note(this,"running");
                sendBroadcast(new Intent(READY).setPackage(getPackageName()).putExtra("game",game));
                tick.run();
            }); }
        });
        return START_NOT_STICKY;
    }
    private void overlayStatus(String status) {
        if(!status.equals(store.p.getString("session_overlay_status",""))) store.p.edit().putString("session_overlay_status",status).putLong("session_overlay_at",System.currentTimeMillis()).apply();
    }
    private void attachOverlay(boolean userRequested) {
        if(overlay!=null) { overlay.hide();overlay=null; }
        if(!store.p.getBoolean("overlay",true) || !android.provider.Settings.canDrawOverlays(this)) {
            overlayStatus(store.p.getBoolean("overlay",true)?"permission_missing":"disabled");
            if(userRequested) SessionDiagnostics.note(this,"overlay_restore_failed");return;
        }
        overlay=new OverlayController(this,()->end("manual"));overlayAttachAt=SystemClock.elapsedRealtime();overlay.show();
        final OverlayController requested=overlay;
        if(!requested.hasWindow()) {
            overlayStatus("attach_failed");if(userRequested) SessionDiagnostics.note(this,"overlay_restore_failed");return;
        }
        overlayStatus("attaching");
        requested.whenAttached(()->{
            if(destroyed || ending || overlay!=requested) return;
            overlayStatus("attached");if(userRequested) SessionDiagnostics.note(this,"overlay_restored");
        });
    }
    private void checkOverlay() {
        if(!store.p.getBoolean("overlay",true) || !android.provider.Settings.canDrawOverlays(this)) {
            if(overlay!=null) { overlay.hide();overlay=null; }
            overlayStatus(store.p.getBoolean("overlay",true)?"permission_missing":"disabled");return;
        }
        if(overlay!=null && overlay.isAttached()) { overlayStatus("attached");return; }
        // addView can succeed before the first ViewRoot traversal attaches its View.
        if(overlay!=null && overlay.hasWindow() && SystemClock.elapsedRealtime()-overlayAttachAt<2000) return;
        // One bounded repair of an actually detached view, not an attempt to bypass
        // a game's/system's hide-overlay policy or to restart a dead process.
        if(overlayRepairAttempts<1) { overlayRepairAttempts++;attachOverlay(false); }
    }
    private void end(String reason) {
        if(ending || destroyed) return;ending=true;main.removeCallbacks(tick);
        SessionDiagnostics.note(this,reason);
        if(overlay!=null) { overlay.hide();overlay=null; }overlayStatus("session_ended");
        Engine.WORK.execute(()->{
            String recovery;
            try { recovery=new Engine(this).restore(); }
            catch(Exception error) { recovery="Recovery pending: "+error.getClass().getSimpleName(); }
            try {
                JSONObject summary=new JSONObject();summary.put("game",game);summary.put("start",startWall);
                summary.put("duration",Math.max(0,SystemClock.elapsedRealtime()-startElapsed));
                summary.put("id",store.p.getString("session_id",""));
                summary.put("reason",reason);summary.put("samples",store.draftSamples());summary.put("auto_end",autoEnd);
                store.p.edit().putBoolean("session_finished",true).putLong("session_closed_at",System.currentTimeMillis()).commit();
                summary.put("diagnostics",SessionDiagnostics.snapshot(this));
                summary.put("engine",store.p.getString("last_engine_log",""));summary.put("recovery",recovery);
                store.saveSession(summary);
                store.p.edit().putBoolean("active",false).commit();store.clearDraft();
            } catch(Exception error) {
                // Keep active/draft/journal for explicit recovery if persistence fails.
                SessionDiagnostics.note(this,"save_failed");
            }
            main.post(()->{ running=false;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(); });
        });
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        if(initialized && !ending && !destroyed) { overlayRepairAttempts=0;attachOverlay(false); }
    }
    @Override public void onTaskRemoved(Intent rootIntent) {
        if(!ending && initialized) SessionDiagnostics.note(this,"task_removed");
        super.onTaskRemoved(rootIntent);
    }
    @Override public void onDestroy() {
        destroyed=true;main.removeCallbacksAndMessages(null);
        if(overlay!=null) overlay.hide();
        if(store!=null) overlayStatus("service_stopped");
        // No sticky restart. Preserve journal/active flag after unexpected service death.
        if(!ending && store!=null && store.p.getBoolean("active",false)) SessionDiagnostics.note(this,"service_destroyed");
        running=false;super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
