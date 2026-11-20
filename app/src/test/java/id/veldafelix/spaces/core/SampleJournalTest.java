package id.veldafelix.spaces.core;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import static org.junit.Assert.*;

public class SampleJournalTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private File file() { return new File(temp.getRoot(),"samples.jsonl"); }
    @Test public void emptySessionHasNoInventedSamples() throws Exception { assertTrue(new SampleJournal(file()).readLatest().isEmpty()); }
    @Test public void longSessionStaysBoundedAndRetainsLatestWindow() throws Exception {
        File file=file();SampleJournal journal=new SampleJournal(file());int count=SampleJournal.LIMIT*3+17;
        for(int i=0;i<count;i++) journal.append("{\"at\":"+i+"}");
        List<String> latest=journal.readLatest();assertEquals(SampleJournal.LIMIT,latest.size());
        assertEquals("{\"at\":"+(count-SampleJournal.LIMIT)+"}",latest.get(0));
        assertEquals("{\"at\":"+(count-1)+"}",latest.get(latest.size()-1));
        assertTrue("Disk journal must compact, not grow for hours",file.length()<SampleJournal.LIMIT*2*32);
    }
    @Test public void processDeathPartialTailIsNotAValidRecord() throws Exception {
        Files.write(file().toPath(),"{\"at\":1}\n{\"at\":".getBytes(StandardCharsets.UTF_8));
        List<String> latest=new SampleJournal(file()).readLatest();assertEquals(1,latest.size());assertEquals("{\"at\":1}",latest.get(0));
    }
    @Test public void nextAppendRepairsTornTailWithoutJoiningRecords() throws Exception {
        Files.write(file().toPath(),"one\npartial".getBytes(StandardCharsets.UTF_8));
        SampleJournal journal=new SampleJournal(file());journal.append("two");assertEquals(java.util.Arrays.asList("one","two"),journal.readLatest());
    }
    @Test public void reopenRetainsSamplesAndCompactionLimit() throws Exception {
        SampleJournal journal=new SampleJournal(file());journal.append("first");
        journal=new SampleJournal(file());journal.append("second");assertEquals(java.util.Arrays.asList("first","second"),journal.readLatest());
    }
    @Test public void invalidRecordsCannotGrowOrBreakTheLog() throws Exception {
        for(String point:new String[]{"","a\nb","a\rb",new String(new char[SampleJournal.MAX_LINE+1]).replace('\0','x')}) {
            try { new SampleJournal(file()).append(point);fail("invalid record accepted"); } catch(IOException expected) { }
        }
        assertFalse(file().exists());
    }
    @Test public void writeFailureIsSurfacedRatherThanDiscardingEvidence() throws Exception {
        File directory=temp.newFolder("not-a-file");
        try { new SampleJournal(directory).append("point");fail("write failure ignored"); } catch(IOException expected) { }
    }
    @Test public void failedCompactionDoesNotAllowUnboundedFurtherAppends() throws Exception {
        SampleJournal journal=new SampleJournal(file());
        File blocked=new File(file().getPath()+".tmp");assertTrue(blocked.mkdir());
        for(int i=0;i<SampleJournal.LIMIT*2-1;i++) journal.append("point");
        try { journal.append("point");fail("Compaction failure hidden"); } catch(IOException expected) { }
        long length=file().length();
        for(int i=0;i<5;i++) { try { journal.append("point");fail("Backlog grew after failed compaction"); } catch(IOException expected) { } }
        assertEquals(length,file().length());assertEquals(SampleJournal.LIMIT,journal.readLatest().size());
    }
    @Test public void completedSessionCanClearItsDraft() throws Exception {
        SampleJournal journal=new SampleJournal(file());journal.append("point");journal.delete();assertFalse(file().exists());assertTrue(journal.readLatest().isEmpty());
    }
}
