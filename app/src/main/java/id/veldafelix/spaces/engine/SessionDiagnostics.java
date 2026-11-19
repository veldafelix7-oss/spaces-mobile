package id.veldafelix.spaces.engine;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.PowerManager;
import id.veldafelix.spaces.Store;

/** Bounded, local-only last-session evidence. Never infers an OEM kill without Android evidence. */
public final class SessionDiagnostics {
    private SessionDiagnostics() {}
    static void note(Context c,String event) {
        new Store(c).p.edit().putString("session_last_event",event).putLong("session_event_at",System.currentTimeMillis()).commit();
    }
    public static String eventLabel(Store s,String event) {
        switch(event) {
            case "starting": return s.t("Sesi sedang dimulai","Session starting");
            case "running": return s.t("Sesi berjalan","Session running");
            case "manual": return s.t("Diakhiri manual","Ended manually");
            case "left_game": return s.t("Diakhiri deteksi keluar otomatis","Ended by automatic exit detection");
            case "interrupted": return s.t("Sesi terputus","Session interrupted");
            case "service_destroyed": return s.t("Layanan berhenti tanpa akhir sesi normal; penyebab belum diketahui","Service stopped without normal session end; cause unknown");
            case "task_removed": return s.t("Jendela aplikasi dihapus dari aplikasi terbaru; bukan bukti layanan mati","App task removed from recents; not proof of service death");
            case "overlay_restored": return s.t("Sidebar dipasang ulang atas permintaan pengguna","Sidebar reattached by user request");
            case "overlay_restore_failed": return s.t("Sidebar belum dapat dipasang; periksa izin/status lampiran","Sidebar could not be attached; check permission/attachment status");
            default: return s.t("Belum ada catatan sesi","No session evidence yet");
        }
    }
    private static String time(long at) {
        return at<=0?"—":android.text.format.DateFormat.format("dd MMM HH:mm:ss",at).toString();
    }
    public static String report(Context c) {
        Store s=new Store(c);StringBuilder out=new StringBuilder();
        out.append(s.t("Layanan di proses ini: ","Service in this process: ")).append(SessionService.running?s.t("berjalan","running"):s.t("tidak berjalan","not running"));
        out.append("\n").append(s.t("Catatan terakhir: ","Last event: ")).append(eventLabel(s,s.p.getString("session_last_event",""))).append(" · ").append(time(s.p.getLong("session_event_at",0)));
        out.append("\n").append(s.t("Sampel/heartbeat terakhir: ","Last sample/heartbeat: ")).append(time(s.p.getLong("session_heartbeat",0)));
        out.append("\n").append(s.t("Deteksi keluar pada sesi terakhir: ","Exit detection in last session: ")).append(s.p.getBoolean("session_auto_end",false)?s.t("otomatis (pilihan pengguna)","automatic (opt-in)"):s.t("manual","manual"));
        out.append("\n").append(s.t("Status lampiran sidebar: ","Sidebar attachment status: ")).append(s.p.getString("session_overlay_status","—"));
        String error=s.p.getString("overlay_error","");if(!error.isEmpty()) out.append("\nOverlay: ").append(error);
        PowerManager power=c.getSystemService(PowerManager.class);
        out.append("\n").append(s.t("Pengecualian optimasi baterai Android: ","Android battery-optimization exemption: ")).append(power!=null && power.isIgnoringBatteryOptimizations(c.getPackageName())?s.t("ya","yes"):s.t("tidak","no"));
        long start=s.p.getLong("active_start",0);int pid=s.p.getInt("session_process_pid",0);
        if(!SessionService.running && start>0 && pid>0) {
            try {
                for(ApplicationExitInfo exit:c.getSystemService(ActivityManager.class).getHistoricalProcessExitReasons(null,pid,3)) {
                    if(exit.getTimestamp()<start) continue;
                    out.append("\n").append(s.t("Laporan akhir proses Android: ","Android process-exit report: ")).append(exitReason(s,exit.getReason())).append(" · ").append(time(exit.getTimestamp()));
                    break;
                }
            } catch(Exception ignored) { /* Absent evidence must never become an invented kill reason. */ }
        }
        out.append("\n\n").append(s.t("Lampiran tidak membuktikan panel terlihat: game/sistem dapat menyembunyikan overlay. Heartbeat terhenti saja tidak membuktikan penyebab proses mati. Catatan ini hanya lokal; tidak dikirim otomatis. Tidak ada jaminan anti-mati di HiOS.","Attachment does not prove visibility: games/system can hide overlays. A stopped heartbeat alone does not prove why a process ended. Evidence stays local; no automatic upload. No HiOS keep-alive guarantee."));
        return out.toString();
    }
    private static String exitReason(Store s,int reason) {
        switch(reason) {
            case ApplicationExitInfo.REASON_LOW_MEMORY: return s.t("memori rendah","low memory");
            case ApplicationExitInfo.REASON_CRASH: case ApplicationExitInfo.REASON_CRASH_NATIVE: return s.t("crash","crash");
            case ApplicationExitInfo.REASON_ANR: return "ANR";
            case ApplicationExitInfo.REASON_USER_REQUESTED: return s.t("permintaan pengguna/sistem (Android)","user/system request (Android)");
            default: return s.t("kode alasan ","reason code ")+reason;
        }
    }
}
