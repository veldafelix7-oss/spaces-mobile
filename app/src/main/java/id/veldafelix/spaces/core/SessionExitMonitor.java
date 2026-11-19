package id.veldafelix.spaces.core;

import java.util.HashSet;
import java.util.Set;

/** Conservative opt-in exit detection. Only explicit STOPPED events mean no longer visible. */
public final class SessionExitMonitor {
    private final Set<String> games=new HashSet<>(),others=new HashSet<>();
    private boolean sawGame=false,ambiguous=false;
    private long awaySince=-1;
    public void resumed(boolean game,String activity) {
        if(activity==null || activity.isEmpty()) return;
        if(games.size()+others.size()>=64) unknown();
        // Public UsageEvents identifies classes, not individual activity instances.
        // A repeat while still visible may be another instance: never infer exit
        // from its later STOPPED event. Falling back to manual is safer.
        if(game && games.contains(activity)) ambiguous=true;
        (game?games:others).add(activity);
        if(game) { sawGame=true;awaySince=-1; }
    }
    public void stopped(boolean game,String activity) { (game?games:others).remove(activity); }
    public void unknown() { games.clear();others.clear();sawGame=false;ambiguous=false;awaySince=-1; }
    public boolean confirmedAway() { return sawGame && !ambiguous && games.isEmpty() && !others.isEmpty(); }
    public boolean shouldEnd(long now,long elapsed,boolean enabled,boolean usageAllowed,boolean screenUsable,boolean panelOpen) {
        if(!enabled || !usageAllowed || !screenUsable || panelOpen || !confirmedAway()) { awaySince=-1;return false; }
        if(awaySince<0 || now<awaySince) awaySince=now;
        return SessionRules.shouldEnd(elapsed,now-awaySince,true);
    }
}
