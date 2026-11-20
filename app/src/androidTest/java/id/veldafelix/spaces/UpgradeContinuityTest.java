package id.veldafelix.spaces;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Also run on the installation-gate emulator with fixtures seeded under the published old APK. */
@RunWith(AndroidJUnit4.class)
public class UpgradeContinuityTest {
    @Test public void oldHistoryActiveDraftAndProfileSurviveAndMigrateWithoutReset() throws Exception {
        Store store=new Store(InstrumentationRegistry.getInstrumentation().getTargetContext());
        boolean external="true".equals(InstrumentationRegistry.getArguments().getString("spaces_upgrade"));
        if(!external) {
            store.clearHistory();store.p.edit().clear().putString("history","[{\"id\":\"upgrade-fixture\",\"game\":\"com.tencent.igfit\",\"start\":1000,\"reason\":\"manual\",\"samples\":[{\"at\":1000,\"temp\":35.5}]}]")
                .putBoolean("active",true).putString("active_game","com.tencent.igfit").putLong("active_start",1000)
                .putString("draft_samples","[{\"at\":2000,\"temp\":35.5}]").putString("journal","[]").putString("com.tencent.igfit.mode","eco").commit();
        }
        try {
            assertTrue(store.p.getBoolean("active",false));assertEquals("com.tencent.igfit",store.p.getString("active_game",""));
            assertEquals(1000,store.p.getLong("active_start",0));assertEquals("eco",store.profile("com.tencent.igfit","mode",""));
            assertEquals("[]",store.p.getString("journal","missing"));
            assertEquals(1,store.draftSamples().length());assertEquals(2000,store.draftSamples().getJSONObject(0).getLong("at"));
            JSONArray history=store.history();assertEquals(1,history.length());JSONObject old=history.getJSONObject(0);
            assertEquals("upgrade-fixture",old.getString("id"));assertFalse(old.has("samples"));assertEquals(1,store.sessionSamples(old).length());
            assertEquals(35.5,store.sessionSamples(old).getJSONObject(0).getDouble("temp"),.01);
            assertTrue(store.p.getBoolean("active",false));assertEquals("[]",store.p.getString("journal","missing"));
        } finally { if(!external) { store.clearHistory();store.p.edit().clear().commit(); } }
    }
}
