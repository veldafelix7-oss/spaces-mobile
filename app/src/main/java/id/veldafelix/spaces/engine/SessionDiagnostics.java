package id.veldafelix.spaces.engine;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Debug;
import android.os.PowerManager;
import id.veldafelix.spaces.Store;
import id.veldafelix.spaces.core.GameSpec;
import id.veldafelix.spaces.core.SessionEvidence;
import org.json.JSONObject;
import java.util.Locale;

/** Small local evidence, attributed to a session. Historical attachment is never live visibility. */
public final class SessionDiagnostics {
    private SessionDiagnostics() {}
    static void note(Context c,String event) {
        new Store(c).p.edit().putString("session_last_event",event).putLong("session_event_at",System.currentTimeMillis()).commit();
    }
    static void sampleMemory(Context c) {
        Store s=new Store(c);long pss=Debug.getPss();if(pss<=0) return;
        s.p.edit().putLong("session_pss_first_kb",s.p.getLong("session_pss_first_kb",pss)).putLong("session_pss_kb",pss).putLong("session_pss_peak_kb",Math.max(pss,s.p.getLong("session_pss_peak_kb",0)))
            .putLong("session_memory_at",System.currentTimeMillis()).commit();
    }
    public static void noteRecovery(Context c) {
        snapshot(c); // Cache Android evidence before closing the interrupted session's time window.
        Store s=new Store(c);
        if(s.p.getLong("session_closed_at",0)==0) s.p.edit().putLong("session_closed_at",System.currentTimeMillis()).commit();
        note(c,"interrupted");
    }
    public static String eventLabel(Store s,String event) {
        switch(event) {
            case "starting": return s.t("Sesi sedang dimulai","Session starting");
            case "running": return s.t("Sesi berjalan","Session running");
            case "manual": return s.t("Diakhiri manual","Ended manually");
            case "left_game": return s.t("Diakhiri deteksi keluar otomatis","Ended by automatic exit detection");
            case "interrupted": return s.t("Sesi terputus","Session interrupted");
            case "save_failed": return s.t("Penyimpanan sesi belum selesai; buka pemulihan","Session save incomplete; open recovery");
            case "service_destroyed": return s.t("Layanan berhenti tanpa akhir sesi normal","Service stopped without normal session end");
            case "task_removed": return s.t("Jendela aplikasi dihapus dari daftar terbaru","App task removed from recents");
            case "overlay_restored": return s.t("Sidebar dipasang ulang atas permintaanmu","Sidebar reattached on your request");
            case "overlay_restore_failed": return s.t("Sidebar belum dapat dipasang ulang","Sidebar could not be reattached");
            default: return s.t("Belum ada catatan sesi","No session evidence yet");
        }
    }
    private static String time(long at) { return at<=0?"—":android.text.format.DateFormat.format("dd MMM yyyy · HH:mm:ss",at).toString(); }
    private static String mb(long kb) { return String.format(Locale.getDefault(),"%.1f MB",kb/1024.0); }
    private static String overlayLabel(Store s,String state) {
        switch(state) {
            case "attached": return s.t("Terpasang pada pemeriksaan terakhir","Attached at last check");
            case "attaching": case "starting": return s.t("Sedang dipasang","Attaching");
            case "disabled": return s.t("Dinonaktifkan di pengaturan","Disabled in settings");
            case "permission_missing": return s.t("Izin tampil di atas aplikasi belum tersedia","Overlay permission unavailable");
            case "attach_failed": return s.t("Gagal dipasang","Attachment failed");
            case "service_stopped": return s.t("Layanan sudah berhenti","Service has stopped");
            case "session_ended": return s.t("Dilepas saat sesi diakhiri","Removed when session ended");
            default: return s.t("Belum diketahui","Unknown");
        }
    }
    public static String shizukuLabel(Store s) { return shizukuLabel(s,PrivilegeBridge.state()); }
    public static String shizukuLabel(Store s,String state) { return shizukuLabel(s,state,""); }
    public static String shizukuLabel(Store s,String state,String failure) {
        failure=failure==null?"":failure;
        switch(state) {
            case "ready": return s.t("Shizuku tersambung · izin siap","Shizuku connected · authorized");
            case "connecting": return s.t("Shizuku diizinkan · menyambungkan layanan…","Shizuku authorized · connecting service…");
            case "error":
                String failure=PrivilegeBridge.lastFailure();
                if(failure.length()>72) failure=failure.substring(0,69)+"…";
                return s.t("Shizuku diizinkan · layanan gagal tersambung","Shizuku authorized · service connection failed")+
                    (failure.isEmpty()?"":" · "+failure);
            case "authorized": return s.t("Shizuku diizinkan · layanan belum tersambung","Shizuku authorized · service not connected");
            case "permission": return s.t("Shizuku memerlukan izin","Shizuku needs permission");
            default: return s.t("Shizuku belum terhubung","Shizuku not connected");
        }
    }
    /** Call before archiving an interruption. Never borrow an exit from another process/session. */
    public static JSONObject snapshot(Context c) {
        Store s=new Store(c);JSONObject data=new JSONObject();
        try {
            long start=s.p.getLong("active_start",0),closed=s.p.getLong("session_closed_at",0);
            int pid=s.p.getInt("session_process_pid",0);
            if(!SessionService.running && !s.p.getBoolean("session_finished",false) && start>0 && pid>0 && !s.p.contains("session_exit_reason")) {
                ActivityManager manager=c.getSystemService(ActivityManager.class);
                if(manager!=null) for(ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(null,pid,16)) {
                    if(!SessionEvidence.matchesExit(pid,start,closed,s.p.getBoolean("session_finished",false),c.getApplicationInfo().processName,
                            exit.getPid(),exit.getTimestamp(),exit.getProcessName())) continue;
                    s.p.edit().putInt("session_exit_reason",exit.getReason()).putLong("session_exit_at",exit.getTimestamp()).commit();break;
                }
            }
            data.put("id",s.p.getString("session_id","")).put("game",s.p.getString("active_game",""))
                .put("start",start).put("pid",pid).put("event",s.p.getString("session_last_event",""))
                .put("event_at",s.p.getLong("session_event_at",0)).put("heartbeat",s.p.getLong("session_heartbeat",0))
                .put("auto_end",s.p.getBoolean("session_auto_end",false)).put("overlay",s.p.getString("session_overlay_status",""))
                .put("overlay_at",s.p.getLong("session_overlay_at",0)).put("pss",s.p.getLong("session_pss_kb",0))
                .put("pss_first",s.p.getLong("session_pss_first_kb",0)).put("pss_peak",s.p.getLong("session_pss_peak_kb",0)).put("memory_at",s.p.getLong("session_memory_at",0))
                .put("sample_error",s.p.getBoolean("session_sample_error",false));
            if(s.p.contains("session_exit_reason")) data.put("exit_reason",s.p.getInt("session_exit_reason",0)).put("exit_at",s.p.getLong("session_exit_at",0));
        } catch(Exception ignored) { /* Missing Android evidence is not a fabricated cause. */ }
        return data;
    }
    public static String report(Context c) {
        Store s=new Store(c);JSONObject data=snapshot(c);
        String status=SessionService.running?s.t("Layanan berjalan","Service running"):s.t("Layanan tidak berjalan","Service not running");
        String recovery=(!SessionService.running && (s.p.getBoolean("active",false) || new Engine(c).pending()))?
            s.t("Periksa & pulihkan sebelum memulai sesi baru.","Check & recover before starting a new session."):
            s.t("Tidak ada sesi terputus yang menunggu pemeriksaan.","No interrupted session awaiting review.");
        PowerManager power=c.getSystemService(PowerManager.class);
        String battery=power!=null && power.isIgnoringBatteryOptimizations(c.getPackageName())?s.t("Dikecualikan","Exempt"):s.t("Tidak dikecualikan","Not exempt");
        return format(s,data)+"\n\n"+s.t("KONDISI SEKARANG","CURRENT STATE")+"\n"+status+"\n"+recovery+"\n"+shizukuLabel(s)
            +"\n"+s.t("Optimasi baterai: ","Battery optimization: ")+battery+"\n\n"
            +s.t("Alasan Android di atas berlaku untuk proses spaces, bukan game. Catatan sidebar bukan jaminan tampilan masih terlihat. Pengecualian baterai tidak mencegah semua penghentian karena memori rendah. Data hanya lokal.",
                 "Android's reason above concerns spaces, not the game. Sidebar evidence does not guarantee current visibility. Battery exemption cannot prevent every low-memory termination. Data stays local.");
    }
    public static String format(Store s,JSONObject data) {
        GameSpec game=GameSpec.find(data.optString("game"));String id=data.optString("id");
        StringBuilder out=new StringBuilder(s.t("SESI YANG DIPERIKSA","SESSION UNDER REVIEW"));
        out.append("\nGame: ").append(game==null?s.t("Belum tercatat","Not recorded"):game.title)
            .append("\n").append(s.t("Mulai: ","Started: ")).append(time(data.optLong("start")));
        if(!id.isEmpty()) out.append("\n").append(s.t("Kode sesi: ","Session ID: ")).append(id);
        out.append("\n").append(s.t("Mode pengakhiran: ","Session end mode: ")).append(data.optBoolean("auto_end")?s.t("Deteksi otomatis (opsional)","Automatic detection (opt-in)"):s.t("Manual","Manual"));
        out.append("\n\n").append(s.t("BUKTI TERSIMPAN TERAKHIR","LAST SAVED EVIDENCE"))
            .append("\n").append(eventLabel(s,data.optString("event"))).append("\n").append(time(data.optLong("event_at")))
            .append("\n").append(s.t("Heartbeat tersimpan: ","Saved heartbeat: ")).append(time(data.optLong("heartbeat")))
            .append("\n").append(s.t("Sidebar: ","Sidebar: ")).append(overlayLabel(s,data.optString("overlay")))
            .append("\n").append(s.t("Diperiksa: ","Checked: ")).append(time(data.optLong("overlay_at")));
        if(data.optBoolean("sample_error")) out.append("\n").append(s.t("Sampel terakhir gagal disimpan; grafik mungkin tidak lengkap.","Latest sample could not be saved; chart may be incomplete."));
        out.append("\n\n").append(s.t("LAPORAN ANDROID · PROSES SPACES","ANDROID REPORT · SPACES PROCESS"));
        if(data.has("exit_reason")) out.append("\n").append(exitReason(s,data.optInt("exit_reason"))).append("\n").append(time(data.optLong("exit_at")));
        else out.append("\n").append(s.t("Belum ada laporan akhir proses yang cocok dengan sesi ini.","No matching process-exit report for this session."));
        out.append("\n\n").append(s.t("MEMORI SPACES · SAMPEL BERKALA","SPACES MEMORY · PERIODIC SAMPLES"));
        if(data.optLong("pss")>0) out.append("\n").append(s.t("PSS awal: ","First PSS: ")).append(mb(data.optLong("pss_first")))
            .append("\n").append(s.t("PSS terakhir: ","Last PSS: ")).append(mb(data.optLong("pss")))
            .append("\n").append(s.t("Puncak sampel: ","Sampled peak: ")).append(mb(data.optLong("pss_peak")))
            .append("\n").append(time(data.optLong("memory_at")))
            .append("\n").append(s.t("Perkiraan jatah RAM proses; bukan total RAM HP atau bukti kebocoran.","Estimated process RAM share; not total device RAM or proof of a leak."));
        else out.append("\n").append(s.t("Belum ada sampel memori pada sesi ini.","No memory samples in this session."));
        return out.toString();
    }
    public static String engineReport(Store s,String raw,boolean recovery) {
        StringBuilder out=new StringBuilder(recovery?s.t("PEMULIHAN PENGATURAN","SETTINGS RECOVERY"):s.t("HASIL PENERAPAN PROFIL","PROFILE APPLICATION RESULTS"));
        if(raw==null || raw.trim().isEmpty()) return out+"\n"+s.t("Tidak ada perubahan pengaturan yang tercatat.","No setting changes recorded.");
        for(String line:raw.split("\n")) {
            if(line.isEmpty()) continue;
            if(line.startsWith("No game files")) { out.append("\n• ").append(s.t("File game, proses aplikasi lain, dan batas suhu tidak diubah.","Game files, other app processes and thermal limits were not modified."));continue; }
            if(line.startsWith("Shizuku unavailable")) { out.append("\n• ").append(s.t("Shizuku tidak tersedia; pengaturan lanjutan dilewati.","Shizuku unavailable; advanced settings skipped."));continue; }
            String[] parts=line.split(":",2);String key=parts[0],value=parts.length>1?parts[1].trim():line;
            GameSpec game=GameSpec.find(key);
            String title=game!=null?game.title+" · GameManager":key.equals("focus")?s.t("Mode Fokus","Focus mode"):key.equals("peak_refresh_rate")?s.t("Batas refresh maksimum","Maximum refresh preference"):key.equals("min_refresh_rate")?s.t("Batas refresh minimum","Minimum refresh preference"):s.t("Pengaturan sesi","Session settings");
            String result;
            if(value.equals("restored")) result=s.t("Sudah dipulihkan.","Restored.");
            else if(value.equals("already restored")) result=s.t("Sudah sesuai nilai awal.","Already at the original value.");
            else if(value.contains("changed elsewhere")) result=s.t("Diubah dari luar spaces; nilai tersebut dipertahankan.","Changed outside spaces; that value was preserved.");
            else if(value.contains("recovery pending") || line.contains("Recovery pending") || line.contains("unreadable")) result=s.t("Pemulihan belum selesai. Periksa izin/koneksi; jangan hapus data aplikasi.","Recovery incomplete. Check permission/connection; do not clear app data.");
            else if(value.equals("unchanged")) result=s.t("Sudah sesuai; tidak diubah.","Already matched; unchanged.");
            else if(value.contains("setting verified")) result=s.t("Nilai pengaturan terbaca sesuai permintaan; bukan bukti FPS meningkat.","Setting readback matched; not proof of increased FPS.");
            else if(value.contains("permission")) result=s.t("Izin belum tersedia atau dicabut.","Permission unavailable or revoked.");
            else result=s.t("Belum terverifikasi. Periksa dukungan perangkat dan koneksi Shizuku.","Not verified. Check device support and Shizuku connection.");
            out.append("\n• ").append(title).append(": ").append(result);
        }
        return out.toString();
    }
    private static String exitReason(Store s,int reason) {
        switch(reason) {
            case ApplicationExitInfo.REASON_LOW_MEMORY: return s.t("Android menghentikan proses spaces karena memori rendah.","Android terminated spaces due to low memory.");
            case ApplicationExitInfo.REASON_CRASH: case ApplicationExitInfo.REASON_CRASH_NATIVE: return s.t("Android melaporkan crash pada proses spaces.","Android reported a spaces process crash.");
            case ApplicationExitInfo.REASON_ANR: return s.t("Android melaporkan proses spaces tidak merespons (ANR).","Android reported spaces was not responding (ANR).");
            case ApplicationExitInfo.REASON_USER_REQUESTED: return s.t("Android melaporkan penghentian atas permintaan pengguna/sistem.","Android reported a user/system-requested termination.");
            default: return s.t("Alasan Android lainnya, kode ","Other Android reason, code ")+reason;
        }
    }
}
