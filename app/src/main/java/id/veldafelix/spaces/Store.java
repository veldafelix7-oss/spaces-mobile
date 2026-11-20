package id.veldafelix.spaces;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import id.veldafelix.spaces.core.GameSpec;
import id.veldafelix.spaces.core.SampleJournal;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Store {
    public final SharedPreferences p;
    private final File files;
    private static final Object HISTORY_LOCK=new Object();
    public Store(Context c) { p=c.getSharedPreferences("spaces",Context.MODE_PRIVATE);files=c.getFilesDir(); }
    public boolean en() { return p.getString("language","id").equals("en"); }
    public String t(String id, String en) { return en()?en:id; }
    public boolean added(String id) { return p.getStringSet("games",new HashSet<>()).contains(id); }
    public void add(String id, boolean add) {
        if (GameSpec.find(id)==null) throw new IllegalArgumentException("Unsupported game");
        Set<String> games=new HashSet<>(p.getStringSet("games",new HashSet<>()));
        if (add) games.add(id); else games.remove(id);
        p.edit().putStringSet("games",games).apply();
    }
    public String profile(String game, String field, String fallback) { return p.getString(game+"."+field,fallback); }
    public void setProfile(String game, String field, String value) { p.edit().putString(game+"."+field,value).apply(); }
    private File sampleFile(String name) {
        if(!name.matches("samples-[a-f0-9-]+\\.json")) throw new IllegalArgumentException("Invalid sample reference");
        return new File(files,name);
    }
    private void archive(JSONObject session) throws Exception {
        JSONArray points=session.optJSONArray("samples");if(points==null) return;
        // At most one session's points is materialized. Index/history cards never retain all graphs.
        JSONArray bounded=new JSONArray();for(int i=Math.max(0,points.length()-SampleJournal.LIMIT);i<points.length();i++) bounded.put(points.optJSONObject(i));
        String name="samples-"+UUID.randomUUID()+".json";AtomicFile target=new AtomicFile(sampleFile(name));
        FileOutputStream out=null;
        try {
            out=target.startWrite();out.write(bounded.toString().getBytes(StandardCharsets.UTF_8));target.finishWrite(out);
        } catch(Exception e) { target.failWrite(out);throw e; }
        session.put("sample_file",name).put("sample_count",bounded.length());
        double peak=-999;for(int i=0;i<bounded.length();i++) { JSONObject point=bounded.optJSONObject(i);if(point!=null && point.has("temp")) peak=Math.max(peak,point.optDouble("temp")); }
        if(peak>-999) session.put("peak_temp",peak);
        session.remove("samples");
    }
    /** Worker-thread API. Old preview histories are migrated losslessly before changing preferences. */
    public JSONArray history() {
        synchronized(HISTORY_LOCK) {
            try {
                JSONArray old=new JSONArray(p.getString("history","[]"));boolean migrated=false;
                for(int i=0;i<old.length();i++) { JSONObject session=old.optJSONObject(i);if(session!=null && session.has("samples")) { archive(session);migrated=true; } }
                if(migrated && !p.edit().putString("history",old.toString()).commit()) throw new IOException("History migration not saved");
                if(migrated) prune(old);
                return old;
            } catch(Exception e) { throw new IllegalStateException("History remains available for recovery",e); }
        }
    }
    public JSONArray sessionSamples(JSONObject session) {
        synchronized(HISTORY_LOCK) {
            JSONArray legacy=session.optJSONArray("samples");if(legacy!=null) return legacy;
            try(FileInputStream in=new AtomicFile(sampleFile(session.getString("sample_file"))).openRead()) {
                return new JSONArray(new String(in.readAllBytes(),StandardCharsets.UTF_8));
            } catch(Exception ignored) { return new JSONArray(); }
        }
    }
    public void saveSession(JSONObject session) {
        synchronized(HISTORY_LOCK) {
            try {
                JSONArray old=history(),next=new JSONArray();
                JSONObject saved=new JSONObject(session.toString());archive(saved);next.put(saved);
                String id=saved.optString("id");
                for(int i=0;i<old.length() && next.length()<30;i++) {
                    JSONObject prior=old.optJSONObject(i);if(prior!=null && (id.isEmpty() || !id.equals(prior.optString("id")))) next.put(prior);
                }
                if(!p.edit().putString("history",next.toString()).commit()) throw new IOException("History not saved");
                prune(next);
            } catch(Exception e) { throw new IllegalStateException("Session could not be saved",e); }
        }
    }
    private void prune(JSONArray index) {
        Set<String> kept=new HashSet<>();for(int i=0;i<index.length();i++) { JSONObject entry=index.optJSONObject(i);if(entry!=null) kept.add(entry.optString("sample_file")); }
        File[] all=files.listFiles();if(all==null) return;
        for(File file:all) if(file.getName().matches("samples-[a-f0-9-]+\\.json") && !kept.contains(file.getName())) new AtomicFile(file).delete();
    }
    public void clearHistory() {
        synchronized(HISTORY_LOCK) { if(p.edit().remove("history").commit()) prune(new JSONArray()); }
    }
    public SampleJournal draftJournal() {
        String id=p.getString("session_id","");
        if(!id.matches("[a-f0-9-]{36}")) throw new IllegalStateException("Session ID missing");
        return new SampleJournal(new File(files,"draft-"+id+".jsonl"));
    }
    public JSONArray draftSamples() throws Exception {
        if(p.getInt("draft_format",0)!=1) return new JSONArray(p.getString("draft_samples","[]"));
        JSONArray samples=new JSONArray();for(String line:draftJournal().readLatest()) { try { samples.put(new JSONObject(line)); } catch(Exception ignored) { /* torn/bad record is not invented data */ } }
        return samples;
    }
    public void clearDraft() {
        if(p.getInt("draft_format",0)==1) draftJournal().delete();
        p.edit().remove("draft_format").remove("draft_samples").commit();
    }
}
