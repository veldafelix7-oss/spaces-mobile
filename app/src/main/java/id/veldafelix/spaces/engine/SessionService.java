package id.veldafelix.spaces.engine;

import android.app.*;
import android.content.Intent;
import android.os.*;
import id.veldafelix.spaces.MainActivity;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.core.GameSpec;
import id.veldafelix.spaces.core.SessionRules;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SessionService extends Service {
    public static final String START="spaces.START", STOP="spaces.STOP", READY="spaces.READY";
    public static volatile boolean running=false;
    private Store store;
    private String game;
    private long startWall,startElapsed,lastPoll,awaySince=0;
    private String foreground="";
    private JSONArray samples=new JSONArray();
    private final Handler main=new Handler(Looper.getMainLooper());
    private OverlayController overlay;
    private boolean ending=false, initialized=false;
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(ending || !initialized) return;
            JSONObject point=Telemetry.sample(SessionService.this);
            if(samples.length()>=1080) { JSONArray next=new JSONArray(); for(int i=1;i<samples.length();i++) next.put(samples.optJSONObject(i)); samples=next; }
            samples.put(point);
            store.p.edit().putString("live_sample",point.toString()).putString("draft_samples",samples.toString()).apply();
            if(overlay!=null) overlay.update(point);
            boolean allowed=Telemetry.usageAllowed(SessionService.this);
            String recent=Telemetry.foreground(SessionService.this,lastPoll);
            lastPoll=System.currentTimeMillis()-1000;
            if(!recent.isEmpty()) foreground=recent;
            boolean inGame=game.equals(foreground);
            long now=SystemClock.elapsedRealtime();
            if(inGame) awaySince=0;
            else if(!foreground.isEmpty() && awaySince==0) awaySince=now;
            if(SessionRules.shouldEnd(now-startElapsed,awaySince==0?0:now-awaySince,allowed)) { end("left_game"); return; }
            main.postDelayed(this,10000);
        }
    };
    @Override public void onCreate() { super.onCreate(); store=new Store(this); }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent==null) { stopSelf(); return START_NOT_STICKY; }
        if(STOP.equals(intent.getAction())) {
            if(running) end("manual"); else stopSelf();
            return START_NOT_STICKY;
        }
        if(!START.equals(intent.getAction()) || running) return START_NOT_STICKY;
        String requested=intent.getStringExtra("game");
        if(GameSpec.find(requested)==null || !store.added(requested)) { stopSelf(); return START_NOT_STICKY; }
        if(store.p.getBoolean("active",false) || new Engine(this).pending()) { stopSelf(); return START_NOT_STICKY; }
        game=requested;
        startWall=System.currentTimeMillis(); startElapsed=SystemClock.elapsedRealtime(); lastPoll=startWall;
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("session",store.t("Sesi permainan","Gaming session"),NotificationManager.IMPORTANCE_LOW));
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent launch=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,SessionService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"session").setSmallIcon(id.veldafelix.spaces.R.drawable.ic_status)
            .setContentTitle("spaces : mobile · "+GameSpec.find(game).title)
            .setContentText(store.t("Sesi aktif · ketuk untuk kembali","Session active · tap to return"))
            .setContentIntent(launch).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,store.t("Akhiri & pulihkan","End & restore"),stop).build()).build();
        startForeground(41,n);
        running=true;
        boolean saved=store.p.edit().putBoolean("active",true).putString("active_game",game).putLong("active_start",startWall)
            .putString("draft_samples","[]").commit();
        if(!saved) { running=false; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY; }
        Engine.WORK.execute(()->{
            try { new Engine(this).applyProfile(game); }
            finally { main.post(()->{
                if(ending) return;
                initialized=true;
                if(store.p.getBoolean("overlay",true) && android.provider.Settings.canDrawOverlays(this)) {
                    overlay=new OverlayController(this,()->end("manual")); overlay.show();
                }
                sendBroadcast(new Intent(READY).setPackage(getPackageName()).putExtra("game",game));
                tick.run();
            }); }
        });
        return START_NOT_STICKY;
    }
    private void end(String reason) {
        if(ending) return; ending=true; main.removeCallbacks(tick);
        if(overlay!=null) overlay.hide();
        final JSONArray collected=samples;
        Engine.WORK.execute(()->{
            String recovery=new Engine(this).restore();
            try {
                JSONObject summary=new JSONObject(); summary.put("game",game); summary.put("start",startWall);
                summary.put("duration",Math.max(0,SystemClock.elapsedRealtime()-startElapsed));
                summary.put("reason",reason); summary.put("samples",collected);
                summary.put("engine",store.p.getString("last_engine_log","")); summary.put("recovery",recovery);
                store.saveSession(summary);
            } catch(Exception ignored) { }
            store.p.edit().putBoolean("active",false).remove("draft_samples").commit();
            main.post(()->{ running=false; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); });
        });
    }
    @Override public void onDestroy() {
        main.removeCallbacksAndMessages(null);
        if(overlay!=null) overlay.hide();
        // If killed unexpectedly, leave journal + active flag intact for explicit recovery.
        running=false;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
