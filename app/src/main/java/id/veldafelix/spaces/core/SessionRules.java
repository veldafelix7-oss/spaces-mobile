package id.veldafelix.spaces.core;

public final class SessionRules {
    private SessionRules() {}
    public static boolean shouldEnd(long elapsedMs, long awayMs, boolean usageGranted) {
        return usageGranted && elapsedMs>=30000 && awayMs>=20000;
    }
    public static boolean canRestore(String current, String applied) {
        return current!=null && current.trim().equals(applied);
    }
    public static float clamp(float value, float min, float max) { return Math.max(min,Math.min(max,value)); }
}
