package id.veldafelix.spaces.core;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShellPolicyTest {
    @Test public void readsOnlyKnownRefreshSetting() {
        assertArrayEquals(new String[]{"settings","get","system","peak_refresh_rate"},ShellPolicy.command(1,"peak_refresh_rate",""));
    }
    @Test public void writesRefreshWithoutShell() {
        assertArrayEquals(new String[]{"settings","put","system","min_refresh_rate","60"},ShellPolicy.command(2,"min_refresh_rate","60"));
    }
    @Test public void restoresMissingSettingByDeleting() {
        assertArrayEquals(new String[]{"settings","delete","system","peak_refresh_rate"},ShellPolicy.command(2,"peak_refresh_rate","null"));
    }
    @Test public void acceptsOemDecimalSnapshot() { assertEquals("90.001",ShellPolicy.command(2,"peak_refresh_rate","90.001")[4]); }
    @Test(expected=SecurityException.class) public void rejectsOtherSettings() { ShellPolicy.command(2,"thermal_limit","0"); }
    @Test(expected=SecurityException.class) public void rejectsInjection() { ShellPolicy.command(2,"peak_refresh_rate","60; reboot"); }
    @Test(expected=SecurityException.class) public void rejectsNegative() { ShellPolicy.command(2,"peak_refresh_rate","-1"); }
    @Test(expected=SecurityException.class) public void rejectsExcessive() { ShellPolicy.command(2,"peak_refresh_rate","999"); }
    @Test(expected=SecurityException.class) public void rejectsNullInput() { ShellPolicy.command(2,"peak_refresh_rate",null); }
    @Test(expected=SecurityException.class) public void rejectsUnknownOperation() { ShellPolicy.command(99,"com.tencent.igfit","2"); }
    @Test(expected=SecurityException.class) public void rejectsOtherGame() { ShellPolicy.command(4,"com.tencent.ig","2"); }
    @Test(expected=SecurityException.class) public void rejectsArbitraryPackage() { ShellPolicy.command(4,"com.android.systemui","2"); }
    @Test(expected=SecurityException.class) public void rejectsInvalidGameMode() { ShellPolicy.command(4,"com.tencent.igfit","999"); }
    @Test public void correctGameModeCommand() { assertArrayEquals(new String[]{"cmd","game","mode","2","com.tencent.igfit"},ShellPolicy.command(4,"com.tencent.igfit","2")); }
    @Test public void parsesAndroidGameMode() { assertEquals("1",ShellPolicy.parseGameMode("com.tencent.igfit current mode: 1, available game modes: [1, 2, 3]")); }
    @Test public void parsesAlternativeLabel() { assertEquals("2",ShellPolicy.parseGameMode("current game mode: 2")); }
    @Test public void unknownModeNeverAssumed() { assertNull(ShellPolicy.parseGameMode("Unknown command")); assertNull(ShellPolicy.parseGameMode(null)); }
}
