package id.veldafelix.spaces.core;

public final class SessionRules {
    private SessionRules() {}
    public static boolean shouldEnd(long elapsedMs, long awayMs, boolean usageGranted) {
        return usageGranted && elapsedMs>=30000 && awayMs>=20000;
    }
    public static boolean canRestore(String current, String applied) {
        return current!=null && current.trim().equals(applied);
    }
    public static boolean equivalent(String kind, String a, String b) {
        if (a==null || b==null) return false;
        if (a.trim().equals(b.trim())) return true;
        if (!"refresh".equals(kind)) return false;
        try { return Float.compare(Float.parseFloat(a.trim()),Float.parseFloat(b.trim()))==0; }
        catch(NumberFormatException e) { return false; }
    }
    public static float clamp(float value, float min, float max) { return Math.max(min,Math.min(max,value)); }
}
