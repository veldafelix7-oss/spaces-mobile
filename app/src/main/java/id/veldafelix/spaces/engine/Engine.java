package id.veldafelix.spaces.engine;

import android.app.NotificationManager;
import android.content.Context;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.core.ShellPolicy;
import id.veldafelix.spaces.core.SessionRules;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Durable write-ahead journal. Entries are committed BEFORE touching device settings. */
public final class Engine {
    public static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private final Context c;
    private final Store store;
    private JSONArray journal;
    public Engine(Context c) {
        this.c=c.getApplicationContext(); store=new Store(c);
        try { journal=new JSONArray(store.p.getString("journal","[]")); }
        catch(Exception e) { journal=new JSONArray(); }
    }
    public boolean pending() { return journal.length()>0; }
    private void persist() {
        if(!store.p.edit().putString("journal",journal.toString()).commit()) throw new IllegalStateException("Cannot persist recovery journal");
    }
    private void record(String kind,String key,String old,String applied) throws Exception {
        JSONObject e=new JSONObject(); e.put("kind",kind); e.put("key",key); e.put("old",old); e.put("applied",applied);
        journal.put(e); persist();
    }
    private String read(String kind,String key) {
        if(kind.equals("dnd")) {
            NotificationManager n=c.getSystemService(NotificationManager.class);
            return n.isNotificationPolicyAccessGranted()?String.valueOf(n.getCurrentInterruptionFilter()):"ERR:permission";
        }
        String out=PrivilegeBridge.exec(c,kind.equals("refresh")?ShellPolicy.READ_SETTING:ShellPolicy.GAME_GET,key,"");
        if(out.startsWith("ERR:")) return out;
        if(kind.equals("game")) {
            String mode=ShellPolicy.parseGameMode(out);
            return mode==null?"ERR:unrecognized GameManager output":mode;
        }
        return out;
    }
    private String write(String kind,String key,String value) {
        if(kind.equals("dnd")) {
            try {
                NotificationManager n=c.getSystemService(NotificationManager.class);
                if(!n.isNotificationPolicyAccessGranted()) return "ERR:permission";
                n.setInterruptionFilter(Integer.parseInt(value)); return "";
            } catch(Exception e) { return "ERR:"+e.getClass().getSimpleName(); }
        }
        return PrivilegeBridge.exec(c,kind.equals("refresh")?ShellPolicy.WRITE_SETTING:ShellPolicy.GAME_SET,key,value);
    }
    private void apply(String kind,String key,String value,StringBuilder log) throws Exception {
        String old=read(kind,key);
        if(old.startsWith("ERR:") || old.isEmpty()) { log.append(key).append(": ").append(old).append("\n"); return; }
        if(old.equals(value)) { log.append(key).append(": unchanged\n"); return; }
        record(kind,key,old,value);
        String result=write(kind,key,value);
        String actual=read(kind,key);
        boolean same=actual.equals(value);
        if(kind.equals("refresh")) {
            try { same=Float.compare(Float.parseFloat(actual),Float.parseFloat(value))==0; } catch(Exception ignored) { }
        }
        // Store the normalized readback, not an invented proof of effective FPS/refresh.
        if(same && !result.startsWith("ERR:")) {
            journal.getJSONObject(journal.length()-1).put("applied",actual); persist();
            log.append(key).append(": setting verified (not performance proof)\n");
        } else log.append(key).append(": not verified; ").append(result).append(" ").append(actual).append("\n");
    }
    public String applyProfile(String game) {
        if(pending()) throw new IllegalStateException("Recovery required");
        StringBuilder log=new StringBuilder();
        try {
            if(store.p.getBoolean("focus",true)) {
                // ALARMS silences calls/messages but keeps alarms. Existing stricter DND is preserved.
                NotificationManager n=c.getSystemService(NotificationManager.class);
                if(n.isNotificationPolicyAccessGranted()) {
                    int old=n.getCurrentInterruptionFilter();
                    if(old!=NotificationManager.INTERRUPTION_FILTER_NONE && old!=NotificationManager.INTERRUPTION_FILTER_ALARMS)
                        apply("dnd","focus",String.valueOf(NotificationManager.INTERRUPTION_FILTER_ALARMS),log);
                } else log.append("focus: permission missing\n");
            }
            String hz=store.profile(game,"refresh","system");
            String mode=store.profile(game,"mode","balanced");
            if(!hz.equals("system") || !mode.equals("balanced")) {
                if(!PrivilegeBridge.authorized()) log.append("Shizuku unavailable; advanced settings skipped\n");
                else {
                    if(!hz.equals("system")) {
                        apply("refresh","peak_refresh_rate",hz,log);
                        apply("refresh","min_refresh_rate",hz,log);
                    }
                    if(!mode.equals("balanced")) apply("game",game,mode.equals("eco")?"3":"2",log);
                }
            }
            log.append("No game files, app processes or thermal limits modified.");
        } catch(Exception e) { log.append("\nERR:").append(e.getMessage()); }
        store.p.edit().putString("last_engine_log",log.toString()).commit();
        return log.toString();
    }
    public String restore() {
        JSONArray left=new JSONArray(); StringBuilder log=new StringBuilder();
        for(int i=journal.length()-1;i>=0;i--) {
            JSONObject e=journal.optJSONObject(i);
            if(e==null) continue;
            try {
                String kind=e.getString("kind"),key=e.getString("key"),old=e.getString("old"),applied=e.getString("applied");
                String current=read(kind,key);
                if(current.startsWith("ERR:")) { left.put(e); log.append(key).append(": recovery pending\n"); continue; }
                if(current.equals(old)) { log.append(key).append(": already restored\n"); continue; }
                if(!SessionRules.canRestore(current,applied)) {
                    // Never overwrite changes made by the user/OEM after we applied ours.
                    log.append(key).append(": changed elsewhere, preserved\n"); continue;
                }
                String result=write(kind,key,old);
                if(result.startsWith("ERR:") || !read(kind,key).equals(old)) { left.put(e); log.append(key).append(": recovery pending\n"); }
                else log.append(key).append(": restored\n");
            } catch(Exception error) { left.put(e); }
        }
        journal=left; persist();
        store.p.edit().putString("recovery_log",log.toString()).commit();
        return log.toString();
    }
}
