package id.veldafelix.spaces.engine;

import android.app.*;
import android.content.Intent;
import android.os.*;
import id.veldafelix.spaces.MainActivity;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.core.GameSpec;
import id.veldafelix.spaces.core.SessionExitMonitor;
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
    private JSONArray samples=new JSONArray();
    private final Handler main=new Handler(Looper.getMainLooper());
    private OverlayController overlay;
    private volatile boolean ending=false,destroyed=false;
    private boolean initialized=false;
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(ending || destroyed || !initialized) return;
            JSONObject point=Telemetry.sample(SessionService.this);
            if(samples.length()>=1080) { JSONArray next=new JSONArray(); for(int i=1;i<samples.length();i++) next.put(samples.optJSONObject(i)); samples=next; }
            samples.put(point);
            long wall=System.currentTimeMillis();
            store.p.edit().putString("live_sample",point.toString()).putString("draft_samples",samples.toString()).putLong("session_heartbeat",wall).apply();
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
            main.postDelayed(this,10000);
        }
    };
    @Override public void onCreate() { super.onCreate();store=new Store(this); }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent==null) { stopSelf();return START_NOT_STICKY; }
        if(STOP.equals(intent.getAction())) {
            if(running) end("manual");else stopSelf();
            return START_NOT_STICKY;
        }
        if(RESTORE_OVERLAY.equals(intent.getAction())) {
            if(running && initialized && !ending && !destroyed) {
                overlayRepairAttempts=0;attachOverlay();SessionDiagnostics.note(this,overlay!=null && overlay.isAttached()?"overlay_restored":"overlay_restore_failed");
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
            .putString("session_overlay_status","starting").putString("draft_samples","[]").commit();
        if(!saved) { running=false;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY; }
        SessionDiagnostics.note(this,"starting");
        Engine.WORK.execute(()->{
            if(ending || destroyed) return;
            try { new Engine(this).applyProfile(game); }
            finally { main.post(()->{
                // A service stopped during profile work must not create a ghost overlay/READY event.
                if(ending || destroyed) return;
                initialized=true;attachOverlay();SessionDiagnostics.note(this,"running");
                sendBroadcast(new Intent(READY).setPackage(getPackageName()).putExtra("game",game));
                tick.run();
            }); }
        });
        return START_NOT_STICKY;
    }
    private void overlayStatus(String status) {
        if(!status.equals(store.p.getString("session_overlay_status",""))) store.p.edit().putString("session_overlay_status",status).apply();
    }
    private void attachOverlay() {
        if(overlay!=null) { overlay.hide();overlay=null; }
        if(!store.p.getBoolean("overlay",true)) { overlayStatus("disabled");return; }
        if(!android.provider.Settings.canDrawOverlays(this)) { overlayStatus("permission_missing");return; }
        overlay=new OverlayController(this,()->end("manual"));overlay.show();
        overlayStatus(overlay.isAttached()?"attached":"attach_failed");
    }
    private void checkOverlay() {
        if(!store.p.getBoolean("overlay",true) || !android.provider.Settings.canDrawOverlays(this)) {
            if(overlay!=null) { overlay.hide();overlay=null; }
            overlayStatus(store.p.getBoolean("overlay",true)?"permission_missing":"disabled");return;
        }
        if(overlay!=null && overlay.isAttached()) { overlayStatus("attached");return; }
        // One bounded repair of an actually detached view, not an attempt to bypass
        // a game's/system's hide-overlay policy or to restart a dead process.
        if(overlayRepairAttempts<1) { overlayRepairAttempts++;attachOverlay(); }
    }
    private void end(String reason) {
        if(ending || destroyed) return;ending=true;main.removeCallbacks(tick);
        SessionDiagnostics.note(this,reason);
        if(overlay!=null) overlay.hide();
        final JSONArray collected=samples;
        Engine.WORK.execute(()->{
            String recovery;
            try { recovery=new Engine(this).restore(); }
            catch(Exception error) { recovery="Recovery pending: "+error.getClass().getSimpleName(); }
            try {
                JSONObject summary=new JSONObject();summary.put("game",game);summary.put("start",startWall);
                summary.put("duration",Math.max(0,SystemClock.elapsedRealtime()-startElapsed));
                summary.put("reason",reason);summary.put("samples",collected);summary.put("auto_end",autoEnd);
                summary.put("engine",store.p.getString("last_engine_log",""));summary.put("recovery",recovery);
                store.saveSession(summary);
            } catch(Exception ignored) { }
            store.p.edit().putBoolean("active",false).remove("draft_samples").commit();
            main.post(()->{ running=false;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(); });
        });
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        if(initialized && !ending && !destroyed) { overlayRepairAttempts=0;attachOverlay(); }
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
