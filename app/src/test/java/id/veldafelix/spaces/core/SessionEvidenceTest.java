package id.veldafelix.spaces.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionEvidenceTest {
    private boolean matches(int pid,long start,long closed,boolean finished,String process,int exitPid,long at,String exitProcess) {
        return SessionEvidence.matchesExit(pid,start,closed,finished,process,exitPid,at,exitProcess);
    }
    @Test public void matchingMainProcessExitBelongsToSession() { assertTrue(matches(41,100,200,false,"spaces",41,150,"spaces")); }
    @Test public void anEarlierExitCannotBecomeTheCurrentCause() { assertFalse(matches(41,100,0,false,"spaces",41,99,"spaces")); }
    @Test public void laterExitCannotBeAssignedToClosedSession() { assertFalse(matches(41,100,200,false,"spaces",41,201,"spaces")); }
    @Test public void secondaryProcessAndOtherPidsAreRejected() {
        assertFalse(matches(41,100,0,false,"spaces",41,150,"spaces:shell"));assertFalse(matches(41,100,0,false,"spaces",42,150,"spaces"));
    }
    @Test public void normalFinishDoesNotBorrowAnExitOnNextAppClose() { assertFalse(matches(41,100,200,true,"spaces",41,150,"spaces")); }
    @Test public void absentSessionMetadataCannotIdentifyACause() {
        assertFalse(matches(0,100,0,false,"spaces",0,150,"spaces"));assertFalse(matches(41,0,0,false,"spaces",41,150,"spaces"));
    }
}
