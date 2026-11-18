package id.veldafelix.spaces.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class SessionRulesTest {
    @Test public void launchGracePeriod() { assertFalse(SessionRules.shouldEnd(29000,25000,true)); }
    @Test public void shortAppSwitchDoesNotEndSession() { assertFalse(SessionRules.shouldEnd(60000,19000,true)); }
    @Test public void manualOnlyWithoutUsagePermission() { assertFalse(SessionRules.shouldEnd(600000,600000,false)); }
    @Test public void endsAfterLeaving() { assertTrue(SessionRules.shouldEnd(60000,20000,true)); }
    @Test public void neverOverwriteUserChanges() { assertFalse(SessionRules.canRestore("60","90")); }
    @Test public void restoresOwnValue() { assertTrue(SessionRules.canRestore("90.0\n","90.0")); }
    @Test public void unknownCurrentNeverRestored() { assertFalse(SessionRules.canRestore(null,"90")); }
    @Test public void numericRefreshSnapshotsAreEquivalent() { assertTrue(SessionRules.equivalent("refresh","60.0","60")); }
    @Test public void nullSettingIsNotZero() { assertFalse(SessionRules.equivalent("refresh","null","0")); }
    @Test public void modeComparisonIsStrict() { assertFalse(SessionRules.equivalent("game","2.0","2")); }
    @Test public void changedRefreshIsPreserved() { assertFalse(SessionRules.equivalent("refresh","90","60")); }
    @Test public void catalogIsExactlyTwoGames() { assertEquals(2,GameSpec.ALL.length); assertNotNull(GameSpec.find("com.tencent.igfit")); assertNotNull(GameSpec.find("com.netease.newspike")); }
    @Test public void unsupportedGameIsRejected() { assertNull(GameSpec.find("com.whatsapp")); assertNull(GameSpec.find(null)); }
}
