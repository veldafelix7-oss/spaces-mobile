package id.veldafelix.spaces.core;

/** Associate Android exit evidence only with an unfinished session's main process/time window. */
public final class SessionEvidence {
    private SessionEvidence() {}
    public static boolean matchesExit(int pid,long start,long closed,boolean finished,String process,
                                      int exitPid,long at,String exitProcess) {
        return !finished && pid>0 && start>0 && at>=start && (closed<=0 || at<=closed)
            && pid==exitPid && process!=null && process.equals(exitProcess);
    }
}
