package id.veldafelix.spaces;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import id.veldafelix.spaces.engine.*;
import id.veldafelix.spaces.core.ShellPolicy;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionStorageTest {
    private Context c;private Store store;
    @Before public void setup() {
        c=InstrumentationRegistry.getInstrumentation().getTargetContext();store=new Store(c);
        store.clearHistory();store.p.edit().clear().commit();
    }
    @After public void cleanup() { if(store.p.getInt("draft_format",0)==1) store.clearDraft();store.clearHistory();store.p.edit().clear().commit(); }
    private JSONArray points() throws Exception { return new JSONArray().put(new JSONObject().put("at",1).put("temp",35.1)).put(new JSONObject().put("at",2).put("temp",36.4)); }
    private JSONObject session(String id) throws Exception { return new JSONObject().put("id",id).put("game","com.tencent.igfit").put("start",1).put("reason","manual").put("samples",points()); }
    @Test public void oldPreviewHistoryMigratesWithoutLosingPointsProfilesOrRecovery() throws Exception {
        String old=new JSONArray().put(session("old")).toString();
        store.p.edit().putString("history",old).putString("journal","pending-fixture").putString("draft_samples",points().toString()).putString("com.tencent.igfit.mode","eco").commit();
        JSONObject saved=store.history().getJSONObject(0);
        assertFalse(saved.has("samples"));assertEquals(2,saved.getInt("sample_count"));assertEquals(36.4,saved.getDouble("peak_temp"),.01);
        assertEquals(points().toString(),store.sessionSamples(saved).toString());
        assertEquals("pending-fixture",store.p.getString("journal",""));assertEquals(2,store.draftSamples().length());assertEquals("eco",store.profile("com.tencent.igfit","mode",""));
        assertEquals(saved.toString(),store.history().getJSONObject(0).toString());
    }
    @Test public void savingMetadataDoesNotRetainAllSampleGraphsInPreferences() throws Exception {
        for(int i=0;i<32;i++) store.saveSession(session("session-"+i));
        JSONArray history=store.history();assertEquals(30,history.length());assertEquals("session-31",history.getJSONObject(0).getString("id"));assertEquals("session-2",history.getJSONObject(29).getString("id"));
        assertFalse(store.p.getString("history","").contains("\"samples\":"));
        int files=0;for(File f:c.getFilesDir().listFiles()) if(f.getName().matches("samples-[a-f0-9-]+\\.json")) files++;
        assertEquals(30,files);assertEquals(2,store.sessionSamples(history.getJSONObject(29)).length());
        store.saveSession(session("session-31"));assertEquals(30,store.history().length()); // Idempotent recovery retry.
    }
    @Test public void deletingHistoryDoesNotDeleteActiveDraftOrJournal() throws Exception {
        store.saveSession(session("saved"));
        store.p.edit().putString("session_id",java.util.UUID.randomUUID().toString()).putInt("draft_format",1).putString("journal","keep").commit();
        store.draftJournal().append(points().getJSONObject(0).toString());store.clearHistory();
        assertEquals(0,store.history().length());assertEquals(1,store.draftSamples().length());assertEquals("keep",store.p.getString("journal",""));
    }
    @Test public void corruptHistoryIsPreservedNotOverwrittenBySave() throws Exception {
        store.p.edit().putString("history","unreadable-fixture").commit();
        try { store.saveSession(session("new"));fail("corrupt history overwritten"); } catch(IllegalStateException expected) { }
        assertEquals("unreadable-fixture",store.p.getString("history",""));
    }
    @Test public void invalidSampleReferenceCannotReadOutsideArchive() throws Exception {
        JSONObject bad=new JSONObject().put("sample_file","../shared_prefs/spaces.xml");assertEquals(0,store.sessionSamples(bad).length());
    }
    @Test public void historicalReportKeepsItsGameAndLabelsOldAttachmentAsHistorical() throws Exception {
        store.p.edit().putString("active_game","com.netease.newspike").commit();
        JSONObject evidence=new JSONObject().put("game","com.tencent.igfit").put("overlay","attached").put("exit_reason",android.app.ApplicationExitInfo.REASON_LOW_MEMORY);
        String report=SessionDiagnostics.format(store,evidence);
        assertTrue(report.contains("PUBGM FLASH"));assertFalse(report.contains("Blood Strike"));
        assertTrue(report.contains("pemeriksaan terakhir"));assertTrue(report.contains("proses spaces karena memori rendah"));assertFalse(report.contains("attached"));
        assertFalse(report.contains("FPS meningkat"));
    }
    @Test public void engineTextSeparatesVerificationFromPerformanceAndExplainsRecovery() {
        String raw="com.tencent.igfit: setting verified (not performance proof)\npeak_refresh_rate: changed elsewhere, preserved\nfocus: recovery pending\nShizuku unavailable; advanced settings skipped";
        String report=SessionDiagnostics.engineReport(store,raw,true);
        assertTrue(report.contains("bukan bukti FPS meningkat"));assertTrue(report.contains("dipertahankan"));assertTrue(report.contains("Pemulihan belum selesai"));assertTrue(report.contains("dilewati"));
        assertFalse(report.contains("setting verified"));assertFalse(report.contains("recovery pending"));
        store.p.edit().putString("language","en").commit();assertTrue(SessionDiagnostics.engineReport(store,raw,true).contains("not proof of increased FPS"));
    }
    @Test public void shizukuExecutionNeverBlocksMainThread() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->assertEquals("ERR:main thread",PrivilegeBridge.exec(c,ShellPolicy.READ_SETTING,"peak_refresh_rate","")));
    }
}
