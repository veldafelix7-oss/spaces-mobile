package id.veldafelix.spaces.engine;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Process;
import org.json.JSONObject;

public final class Telemetry {
    private Telemetry() {}
    public static JSONObject sample(Context c) {
        JSONObject o=new JSONObject();
        try {
            Intent b=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            o.put("at",System.currentTimeMillis());
            if(b!=null) {
                int level=b.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=b.getIntExtra(BatteryManager.EXTRA_SCALE,100);
                if(level>=0 && scale>0) o.put("battery",Math.round(level*100f/scale));
                int temp=b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);
                if(temp!=Integer.MIN_VALUE) o.put("temp",temp/10.0);
                o.put("charging",b.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0);
            }
            ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo(); c.getSystemService(ActivityManager.class).getMemoryInfo(m);
            o.put("ramMb",m.availMem/1048576); o.put("totalMb",m.totalMem/1048576);
        } catch(Exception ignored) { }
        return o;
    }
    public static boolean usageAllowed(Context c) {
        try {
            AppOpsManager m=c.getSystemService(AppOpsManager.class);
            return m!=null && m.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;
        } catch(Exception ignored) { return false; }
    }
    /** Only explicit activity visibility transitions; a RESUMED other app alone is not exit proof. */
    public static boolean observePresence(Context c,long since,long until,String game,id.veldafelix.spaces.core.SessionExitMonitor monitor) {
        if(!usageAllowed(c) || since>until) { monitor.unknown();return false; }
        try {
            UsageEvents events=c.getSystemService(UsageStatsManager.class).queryEvents(since,until);
            if(events==null) { monitor.unknown();return false; }
            UsageEvents.Event e=new UsageEvents.Event();
            while(events.hasNextEvent()) {
                events.getNextEvent(e);
                String pkg=e.getPackageName();if(pkg==null) continue;
                String activity=pkg+"/"+e.getClassName()+"#"+e.getInstanceId();
                if(e.getEventType()==UsageEvents.Event.ACTIVITY_RESUMED) monitor.resumed(game.equals(pkg),activity);
                else if(e.getEventType()==UsageEvents.Event.ACTIVITY_STOPPED) monitor.stopped(game.equals(pkg),activity);
                // PAUSED activities can still be visible under permission/game-tool windows or in multi-window.
            }
            return true;
        } catch(Exception error) { monitor.unknown();return false; }
    }
}
