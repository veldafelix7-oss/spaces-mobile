package id.veldafelix.spaces;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import id.veldafelix.spaces.core.GameSpec;
import java.util.HashSet;
import java.util.Set;

public final class Store {
    public final SharedPreferences p;
    public Store(Context c) { p=c.getSharedPreferences("spaces",Context.MODE_PRIVATE); }
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
    public JSONArray history() {
        try { return new JSONArray(p.getString("history","[]")); } catch (Exception e) { return new JSONArray(); }
    }
    public synchronized void saveSession(JSONObject session) {
        JSONArray old=history(), next=new JSONArray(); next.put(session);
        for(int i=0;i<Math.min(old.length(),29);i++) next.put(old.optJSONObject(i));
        p.edit().putString("history",next.toString()).commit();
    }
}
