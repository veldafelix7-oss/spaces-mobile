package id.veldafelix.spaces.core;

/** Only typed, allowlisted operations cross the privilege boundary. Never uses sh -c. */
public final class ShellPolicy {
    public static final int READ_SETTING=1, WRITE_SETTING=2, GAME_GET=3, GAME_SET=4;
    private ShellPolicy() {}
    public static String[] command(int op, String key, String value) {
        if (op==READ_SETTING || op==WRITE_SETTING) {
            if (!"peak_refresh_rate".equals(key) && !"min_refresh_rate".equals(key)) throw new SecurityException("Setting not allowed");
            if (op==READ_SETTING) return new String[]{"settings","get","system",key};
            if ("null".equals(value)) return new String[]{"settings","delete","system",key};
            if (value==null || !value.matches("[0-9]{1,3}(\\.[0-9]{1,3})?")) throw new SecurityException("Invalid refresh value");
            float hz=Float.parseFloat(value);
            if (hz<0 || hz>240) throw new SecurityException("Invalid refresh range");
            return new String[]{"settings","put","system",key,value};
        }
        if (GameSpec.find(key)==null) throw new SecurityException("Game not allowed");
        if (op==GAME_GET) return new String[]{"cmd","game","list-modes",key};
        if (op==GAME_SET && ("1".equals(value)||"2".equals(value)||"3".equals(value)))
            return new String[]{"cmd","game","mode",value,key};
        throw new SecurityException("Operation not allowed");
    }
    public static String parseGameMode(String output) {
        if (output==null) return null;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("current(?: game)? mode\\s*:\\s*([123])", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(output);
        return m.find()?m.group(1):null;
    }
}
