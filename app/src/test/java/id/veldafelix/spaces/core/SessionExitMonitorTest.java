package id.veldafelix.spaces.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionExitMonitorTest {
    private SessionExitMonitor away() {
        SessionExitMonitor m=new SessionExitMonitor();m.resumed(true,"game");m.resumed(false,"home");m.stopped(true,"game");return m;
    }
    private boolean poll(SessionExitMonitor m,long at) { return m.shouldEnd(at,at,true,true,true,false); }
    @Test public void manualDefaultNeverAutoEndsDespiteAnotherApp() {
        SessionExitMonitor m=away();assertFalse(m.shouldEnd(60000,60000,false,true,true,false));assertFalse(m.shouldEnd(600000,600000,false,true,true,false));
    }
    @Test public void gameMustActuallyHaveBeenObserved() {
        SessionExitMonitor m=new SessionExitMonitor();m.resumed(false,"home");assertFalse(poll(m,60000));assertFalse(poll(m,600000));
    }
    @Test public void transientActivityDoesNotEndStillVisibleGame() {
        SessionExitMonitor m=new SessionExitMonitor();m.resumed(true,"game");m.resumed(false,"permission-or-game-tools");
        assertFalse(poll(m,60000));assertFalse(poll(m,600000));
    }
    @Test public void otherActivityStoppingDoesNotLeaveStickyForeground() {
        SessionExitMonitor m=away();m.stopped(false,"home");assertFalse(poll(m,60000));assertFalse(poll(m,600000));
    }
    @Test public void requiresFullAwayGraceAfterConfirmedStop() {
        SessionExitMonitor m=away();assertFalse(poll(m,60000));assertFalse(poll(m,79999));assertTrue(poll(m,80000));
    }
    @Test public void launchGraceStillApplies() {
        SessionExitMonitor m=away();assertFalse(poll(m,0));assertFalse(poll(m,20000));assertTrue(poll(m,30000));
    }
    @Test public void gameReturningResetsAwayTimer() {
        SessionExitMonitor m=away();assertFalse(poll(m,60000));m.resumed(true,"game");assertFalse(poll(m,90000));
        m.stopped(true,"game");assertFalse(poll(m,100000));assertTrue(poll(m,120000));
    }
    @Test public void multiWindowGameRemainsVisible() {
        SessionExitMonitor m=new SessionExitMonitor();m.resumed(true,"game-a");m.resumed(true,"game-b");m.resumed(false,"other");m.stopped(true,"game-a");
        assertFalse(poll(m,60000));assertFalse(poll(m,90000));
    }
    @Test public void queryFailureForgetsEvidenceRatherThanEndingSession() {
        SessionExitMonitor m=away();assertFalse(poll(m,60000));m.unknown();assertFalse(poll(m,120000));m.resumed(false,"other");assertFalse(poll(m,180000));
    }
    @Test public void permissionLossDoesNotAccumulateAwayTime() {
        SessionExitMonitor m=away();assertFalse(poll(m,60000));assertFalse(m.shouldEnd(90000,90000,true,false,true,false));
        assertFalse(poll(m,100000));assertTrue(poll(m,120000));
    }
    @Test public void lockOrScreenOffDoesNotAccumulateAwayTime() {
        SessionExitMonitor m=away();assertFalse(poll(m,60000));assertFalse(m.shouldEnd(90000,90000,true,true,false,false));assertFalse(poll(m,120000));assertTrue(poll(m,140000));
    }
    @Test public void openPanelDoesNotTriggerAutomaticEnd() {
        SessionExitMonitor m=away();assertFalse(poll(m,60000));assertFalse(m.shouldEnd(90000,90000,true,true,true,true));assertFalse(poll(m,120000));assertTrue(poll(m,140000));
    }
    @Test public void repeatedEventsAreIdempotent() {
        SessionExitMonitor m=new SessionExitMonitor();m.resumed(true,"game");m.resumed(true,"game");m.stopped(true,"game");m.stopped(true,"game");m.resumed(false,"other");
        assertFalse(poll(m,60000));assertTrue(poll(m,80000));
    }
    @Test public void backwardsElapsedClockFailsConservatively() {
        SessionExitMonitor m=away();assertFalse(poll(m,60000));assertFalse(poll(m,40000));assertFalse(poll(m,59999));assertTrue(poll(m,60000));
    }
    @Test public void boundedLedgerOverflowCannotTurnIntoExitProof() {
        SessionExitMonitor m=away();for(int i=0;i<100;i++) m.resumed(false,"other-"+i);
        assertFalse(poll(m,60000));assertFalse(poll(m,600000));
    }

}
