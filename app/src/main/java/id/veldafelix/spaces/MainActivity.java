package id.veldafelix.spaces;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import id.veldafelix.spaces.core.GameSpec;
import id.veldafelix.spaces.engine.*;
import id.veldafelix.spaces.ui.*;
import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
    private Store store;
    private LinearLayout page;
    private int tab=0;
    private ScrollView pageScroll;
    private final NavItem[] navigation=new NavItem[3];
    private final Map<String,Boolean> installationCache=new HashMap<>();
    private final Map<String,android.graphics.drawable.Drawable> iconCache=new HashMap<>();
    private boolean registered=false,launching=false,resumed=false;
    private String pendingLaunch;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final BroadcastReceiver sessionReady=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent intent) {
            launching=false;
            if(resumed) launchGame(intent.getStringExtra("game"));
        }
    };
    private final Shizuku.OnRequestPermissionResultListener shizukuResult=(request,grant)->runOnUiThread(()->{
        if(grant==PackageManager.PERMISSION_GRANTED) PrivilegeBridge.bind(this);
        render();
    });
    private final Shizuku.OnBinderReceivedListener binderReceived=()->runOnUiThread(()->{ PrivilegeBridge.bind(this); if(resumed) render(); });
    private final Shizuku.OnBinderDeadListener binderDead=()->runOnUiThread(()->{ if(resumed) render(); });
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store=new Store(this);
        if(state!=null) tab=state.getInt("tab",0);
        WindowManager.LayoutParams window=getWindow().getAttributes();
        window.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        getWindow().setAttributes(window);
        getWindow().setDecorFitsSystemWindows(false);
        immersive();
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        Shizuku.addRequestPermissionResultListener(shizukuResult);
        Shizuku.addBinderReceivedListenerSticky(binderReceived);
        Shizuku.addBinderDeadListener(binderDead);
        IntentFilter f=new IntentFilter(SessionService.READY);
        registerReceiver(sessionReady,f,Context.RECEIVER_NOT_EXPORTED);
        registered=true;
    }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused);if(focused) immersive(); }
    @Override protected void onResume() { super.onResume(); resumed=true; installationCache.clear();iconCache.clear();render(); }
    @Override protected void onPause() { resumed=false; super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); b.putInt("tab",tab); }
    @Override protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        if(registered) unregisterReceiver(sessionReady);
        Shizuku.removeRequestPermissionResultListener(shizukuResult);
        Shizuku.removeBinderReceivedListener(binderReceived);
        Shizuku.removeBinderDeadListener(binderDead);
        super.onDestroy();
    }
    private int dp(float n) { return Glass.dp(this,n); }
    private String t(String a,String b) { return store.t(a,b); }
    private TextView text(String title,int size,int color,boolean bold) { return Glass.text(this,title,size,color,bold); }
    private TextView button(String s,boolean primary,Runnable action) { return Glass.button(this,s,primary,action); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private void info(String title,String body) { new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton(t("Mengerti","Got it"),null).show(); }
    private void open(Intent intent) {
        try { startActivity(intent); } catch(Exception e) { toast(t("Halaman tidak tersedia di HP ini.","This page is unavailable on this device.")); }
    }
    private boolean installed(String id) {
        Boolean cached=installationCache.get(id);if(cached!=null) return cached;
        boolean found;
        try { getPackageManager().getPackageInfo(id,0);found=true; } catch(PackageManager.NameNotFoundException e) { found=false; }
        installationCache.put(id,found);return found;
    }
    private android.graphics.drawable.Drawable appIcon(String id) throws PackageManager.NameNotFoundException {
        android.graphics.drawable.Drawable icon=iconCache.get(id);
        if(icon==null) { icon=getPackageManager().getApplicationIcon(id);iconCache.put(id,icon); }
        return icon;
    }
    private String version(String id) {
        try { return getPackageManager().getPackageInfo(id,0).versionName; } catch(Exception e) { return "—"; }
    }
    private void render() {
        if(isFinishing() || store==null) return;
        FrameLayout root=new FrameLayout(this); root.addView(new Glass.Background(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout shell=Glass.row(this); shell.setGravity(Gravity.TOP); shell.setPadding(dp(14),dp(14),dp(20),dp(10));
        root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout rail=Glass.column(this); rail.setGravity(Gravity.CENTER_HORIZONTAL); rail.setPadding(dp(8),dp(8),dp(8),dp(8));
        rail.setBackground(Glass.surface(0x88FFFFFF,dp(26)));
        shell.addView(rail,new LinearLayout.LayoutParams(dp(78),-1));
        ImageView lion=new ImageView(this); lion.setImageResource(R.drawable.ic_lion); lion.setContentDescription("spaces : mobile");
        rail.addView(lion,new LinearLayout.LayoutParams(dp(36),dp(36))); Glass.gap(rail,10);
        String[] symbols={"◈","≋","◎"}; String[] titles={t("Main","Play"),t("Performa","Engine"),t("Perangkat","Device")};
        for(int i=0;i<3;i++) {
            final int index=i;
            NavItem nav=new NavItem(this,symbols[i],titles[i],()->selectTab(index));
            nav.setTag("nav_"+index);nav.active(i==tab);navigation[i]=nav;
            rail.addView(nav,new LinearLayout.LayoutParams(-1,dp(58)));Glass.gap(rail,3);
        }
        View spacer=new View(this); rail.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        TextView language=text(store.en()?"EN":"ID",11,Glass.ACCENT,true); language.setGravity(Gravity.CENTER);
        language.setMinHeight(dp(44)); language.setContentDescription(t("Ganti bahasa","Change language"));
        language.setOnClickListener(v->{ store.p.edit().putString("language",store.en()?"id":"en").apply(); render(); }); rail.addView(language,new LinearLayout.LayoutParams(-1,dp(44)));
        Glass.gap(shell,20);
        LinearLayout body=Glass.column(this); shell.addView(body,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout header=Glass.row(this);
        LinearLayout heading=Glass.column(this); heading.addView(text("spaces : mobile",26,Glass.INK,true)); Glass.gap(heading,3);
        heading.addView(text(t("RUANG BERMAIN PRIBADI","YOUR PERSONAL GAME SPACE"),9,Glass.MUTED,true));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        String state=PrivilegeBridge.state();
        TextView status=text("●  "+(state.equals("ready")?t("Shizuku terhubung","Shizuku connected"):state.equals("permission")?t("Shizuku perlu izin","Shizuku needs permission"):t("Shizuku belum terhubung","Shizuku not connected")),10,Glass.ACCENT,true);
        status.setPadding(dp(12),dp(9),dp(12),dp(9)); status.setBackground(Glass.surface(0x99FFFFFF,dp(20)));
        status.setMinHeight(dp(44));status.setGravity(Gravity.CENTER);status.setOnClickListener(v->selectTab(2)); header.addView(status);
        body.addView(header); Glass.gap(body,17);
        ScrollView scroll=new ScrollView(this);pageScroll=scroll; scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        page=Glass.column(this); page.setPadding(0,0,0,dp(12)); scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(tab==0) home(); else if(tab==1) engine(); else device();
        root.setOnApplyWindowInsetsListener((v,insets)->{
            DisplayCutout cutout=insets.getDisplayCutout();
            int left=cutout==null?0:cutout.getSafeInsetLeft(),right=cutout==null?0:cutout.getSafeInsetRight();
            shell.setPadding(Math.max(dp(14),left+dp(6)),dp(14),Math.max(dp(20),right+dp(6)),dp(10));
            return insets;
        });
        setContentView(root);root.requestApplyInsets();
    }
    private void selectTab(int next) {
        if(next<0 || next>2 || page==null) return;
        if(tab==next) return;
        tab=next;
        for(int i=0;i<navigation.length;i++) if(navigation[i]!=null) navigation[i].active(i==tab);
        page.removeAllViews();
        if(tab==0) home(); else if(tab==1) engine(); else device();
        pageScroll.scrollTo(0,0);
    }
    private void titleRow(String title,String subtitle,String action,Runnable click) {
        LinearLayout row=Glass.row(this), left=Glass.column(this);
        left.addView(text(title,20,Glass.INK,true)); Glass.gap(left,4); left.addView(text(subtitle,11,Glass.MUTED,false));
        row.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        if(action!=null) row.addView(button(action,false,click));
        page.addView(row); Glass.gap(page,14);
    }
    private boolean needsRecovery() { return !SessionService.running && (store.p.getBoolean("active",false) || new Engine(this).pending()); }
    private void home() {
        if(needsRecovery()) {
            LinearLayout warning=Glass.card(this); warning.addView(text(t("Sesi sebelumnya perlu diperiksa","Previous session needs attention"),14,Glass.INK,true)); Glass.gap(warning,8);
            warning.addView(button(t("Periksa & pulihkan","Check & recover"),true,this::recover)); page.addView(warning); Glass.gap(page,12);
        }
        if(SessionService.running) {
            LinearLayout active=Glass.card(this); active.addView(text(t("Sesi sedang berjalan","Session in progress"),15,Glass.INK,true)); Glass.gap(active,8);
            LinearLayout controls=Glass.row(this);
            controls.addView(button(t("Kembali ke game","Return to game"),true,()->launchGame(store.p.getString("active_game","")))); Glass.gap(controls,8);
            controls.addView(button(t("Akhiri & pulihkan","End & restore"),false,this::endSession)); active.addView(controls);
            if(store.p.getBoolean("overlay",true)) {
                Glass.gap(active,8);active.addView(button(t("Tampilkan ulang sidebar","Restore sidebar"),false,()->{
                    startService(new Intent(this,SessionService.class).setAction(SessionService.RESTORE_OVERLAY));
                    toast(t("Permintaan sidebar dikirim. Jika tetap hilang, periksa Detail sesi terakhir.","Sidebar requested. If still missing, check Last session details."));
                }));
            }
            page.addView(active); Glass.gap(page,12);
        }
        titleRow(t("Koleksi game","Your collection"),t("Pilih game untuk memulai sesi.","Choose a game to start a session."),t("＋ Tambah game","＋ Add game"),this::addGames);
        int count=0; for(GameSpec g:GameSpec.ALL) if(store.added(g.id)) count++;
        if(count==0) {
            LinearLayout empty=Glass.card(this); empty.setGravity(Gravity.CENTER); empty.setMinimumHeight(dp(190));
            empty.addView(text(t("Tambahkan game untuk mulai","Add a game to get started"),19,Glass.INK,true)); Glass.gap(empty,9);
            TextView note=text(t("Tambahkan PUBG Flash atau Blood Strike yang sudah terpasang.","Add an installed copy of PUBG Flash or Blood Strike."),12,Glass.MUTED,false); note.setGravity(Gravity.CENTER); empty.addView(note); Glass.gap(empty,16);
            empty.addView(button(t("Pilih game terpasang","Choose installed games"),true,this::addGames)); page.addView(empty);
        } else {
            LinearLayout games=Glass.row(this); games.setGravity(Gravity.TOP);
            for(GameSpec g:GameSpec.ALL) if(store.added(g.id)) {
                if(games.getChildCount()>0) Glass.gap(games,14);
                games.addView(gameCard(g),new LinearLayout.LayoutParams(0,dp(212),1));
            }
            page.addView(games);
        }
        Glass.gap(page,14);
        JSONObject sample=Telemetry.sample(this);
        LinearLayout bar=Glass.row(this); bar.setPadding(dp(18),dp(14),dp(18),dp(14)); bar.setBackground(Glass.surface(0x99FFFFFF,dp(20)));
        metric(bar,"TECNO SPARK 20C",t("PERANGKAT TARGET","TARGET DEVICE"));
        metric(bar,sample.has("temp")?String.format(Locale.getDefault(),"%.1f °C",sample.optDouble("temp")):"—",t("SUHU BATERAI","BATTERY TEMP"));
        metric(bar,String.format(Locale.getDefault(),"%.1f GB",sample.optLong("ramMb")/1024f),t("RAM TERSEDIA","AVAILABLE RAM"));
        metric(bar,sample.has("battery")?sample.optInt("battery")+"%":"—",t("BATERAI","BATTERY"));
        page.addView(bar); Glass.gap(page,10);
        page.addView(text(t("Optimasi aktif hanya saat game dimulai dari sini.","Optimizations start only when you launch a game here."),11,Glass.MUTED,false));
    }
    private void metric(LinearLayout row,String value,String label) {
        LinearLayout block=Glass.column(this); block.addView(text(value,14,Glass.INK,true)); Glass.gap(block,5); block.addView(text(label,8,Glass.MUTED,true));
        row.addView(block,new LinearLayout.LayoutParams(0,-2,1));
    }
    private View gameCard(GameSpec game) {
        FrameLayout card=new FrameLayout(this); card.addView(new GameArt(this,game==GameSpec.ALL[0]),new FrameLayout.LayoutParams(-1,-1));
        TextView badge=text(installed(game.id)?t("TERPASANG","INSTALLED"):t("TIDAK TERPASANG","NOT INSTALLED"),9,Color.WHITE,true);
        badge.setPadding(dp(10),dp(6),dp(10),dp(6)); badge.setBackground(Glass.surface(0x25516676,dp(12)));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT); bp.setMargins(dp(installed(game.id)?72:16),dp(22),dp(44),0); card.addView(badge,bp);
        try {
            ImageView appIcon=new ImageView(this);appIcon.setImageDrawable(appIcon(game.id));
            appIcon.setContentDescription(t("Ikon ","Icon ")+game.title);appIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            appIcon.setBackground(Glass.surface(0xDFFFFFFF,dp(11)));appIcon.setClipToOutline(true);
            FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.LEFT);ip.setMargins(dp(16),dp(15),0,0);card.addView(appIcon,ip);
        } catch(PackageManager.NameNotFoundException ignored) { }
        TextView edit=text("···",24,Color.WHITE,true); edit.setGravity(Gravity.CENTER); edit.setContentDescription(t("Profil dan koleksi ","Profile and collection ")+game.title);
        FrameLayout.LayoutParams ep=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.RIGHT); card.addView(edit,ep);
        edit.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(game.title).setItems(new String[]{t("Atur profil","Edit profile"),t("Hapus dari koleksi","Remove from collection")},(d,which)->{
            if(which==0) { store.p.edit().putString("selected_game",game.id).apply(); tab=1; render(); }
            else { store.add(game.id,false); render(); }
        }).show());
        LinearLayout bottom=Glass.column(this); bottom.setPadding(dp(18),0,dp(18),dp(16));
        bottom.addView(text(game.subtitle,9,0xCCDDE7EE,true)); Glass.gap(bottom,6); bottom.addView(text(game.title,23,Color.WHITE,true)); Glass.gap(bottom,12);
        LinearLayout row=Glass.row(this); String mode=store.profile(game.id,"mode","balanced");
        TextView profile=text(mode.equals("boost")?t("Performa","Performance"):mode.equals("eco")?t("Hemat","Eco"):t("Seimbang","Balanced"),11,0xDDEBF1F6,false);
        row.addView(profile,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(button(launching?t("Menyiapkan…","Preparing…"):t("Main  ↗","Play  ↗"),false,()->startSession(game)));
        bottom.addView(row);
        card.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM)); return card;
    }
    private void addGames() {
        String[] labels=new String[GameSpec.ALL.length]; boolean[] checks=new boolean[labels.length];
        for(int i=0;i<labels.length;i++) { GameSpec g=GameSpec.ALL[i]; labels[i]=g.title+" · "+(installed(g.id)?t("terpasang","installed"):t("belum terpasang","not installed")); checks[i]=store.added(g.id); }
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(t("Pilih game dari HP","Choose installed games"))
            .setMultiChoiceItems(labels,checks,(d,which,checked)->{
                GameSpec g=GameSpec.ALL[which];
                if(!installed(g.id)) {
                    ((AlertDialog)d).getListView().setItemChecked(which,false); checks[which]=false;
                    toast(t("Game ini belum terdeteksi. Aplikasi lain tidak bisa ditambahkan.","This game was not detected. Other apps cannot be added."));
                } else checks[which]=checked;
            }).setPositiveButton(t("Simpan","Save"),(d,w)->{
                for(int i=0;i<checks.length;i++) store.add(GameSpec.ALL[i].id,checks[i]&&installed(GameSpec.ALL[i].id)); render();
            }).setNegativeButton(t("Batal","Cancel"),null).create(); dialog.show();
    }
    private void startSession(GameSpec game) {
        if(launching) return;
        if(!installed(game.id)) { toast(t("Game belum terpasang atau variannya tidak didukung.","Game is missing or this variant is unsupported.")); return; }
        if(SessionService.running) { toast(t("Akhiri sesi yang aktif dulu.","End the current session first.")); return; }
        if(needsRecovery()) { recover(); return; }
        if(!store.p.getBoolean("session_consent",false)) {
            new AlertDialog.Builder(this).setTitle(t("Sebelum mulai","Before you play"))
                .setMessage(t("Profil hanya mengubah pengaturan yang didukung. FPS tidak dijamin naik. Mode Fokus membisukan panggilan/pesan, alarm tetap diizinkan. Pengaturan dicoba pulihkan saat sesi berakhir. Notifikasi menyediakan tombol berhenti. Akhiri sesi manual secara bawaan; deteksi keluar otomatis hanya jika kamu aktifkan.","Profiles change supported settings only. Higher FPS is not guaranteed. Focus silences calls/messages while allowing alarms. Settings are restored where possible after the session. Use the notification to stop. Sessions end manually by default; automatic exit detection is opt-in."))
                .setPositiveButton(t("Setuju & mulai","Agree & start"),(d,w)->{ store.p.edit().putBoolean("session_consent",true).apply(); startSession(game); })
                .setNegativeButton(t("Batal","Cancel"),null).show(); return;
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
                && !store.p.getBoolean("notification_prompted",false)) {
            pendingLaunch=game.id; store.p.edit().putBoolean("notification_prompted",true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},31); return;
        }
        launching=true;
        try { startForegroundService(new Intent(this,SessionService.class).setAction(SessionService.START).putExtra("game",game.id)); }
        catch(Exception e) { launching=false; toast(t("Tidak bisa memulai sesi: ","Unable to start session: ")+e.getClass().getSimpleName()); }
        main.postDelayed(()->{ launching=false; if(resumed) render(); },20000);
        render();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==31 && pendingLaunch!=null) {
            GameSpec game=GameSpec.find(pendingLaunch); pendingLaunch=null;
            if(game!=null) main.post(()->startSession(game));
        } else render();
    }
    private void launchGame(String id) {
        if(GameSpec.find(id)==null) return;
        Intent launch=getPackageManager().getLaunchIntentForPackage(id);
        if(launch==null) { toast(t("Game tidak dapat dibuka. Akhiri sesi untuk memulihkan.","Unable to launch game. End the session to restore settings.")); render(); return; }
        try { startActivity(launch); } catch(Exception e) { toast(t("Game gagal dibuka.","Game could not be launched.")); }
    }
    private void endSession() {
        if(!SessionService.running) return;
        startService(new Intent(this,SessionService.class).setAction(SessionService.STOP));
        toast(t("Mengakhiri dan memulihkan…","Ending and restoring…")); main.postDelayed(this::render,1000);
    }
    private void recover() {
        if(SessionService.running) { toast(t("Akhiri sesi aktif terlebih dahulu.","End the active session first.")); return; }
        toast(t("Memeriksa jurnal pemulihan…","Checking recovery journal…"));
        Engine.WORK.execute(()->{
            Engine engine=new Engine(this); String log=engine.restore();
            // Preserve interrupted sessions as explicitly incomplete, never invent missing samples.
            if(store.p.getBoolean("active",false)) {
                try {
                    JSONObject summary=new JSONObject(); summary.put("game",store.p.getString("active_game",""));
                    long start=store.p.getLong("active_start",System.currentTimeMillis());
                    JSONArray samples=new JSONArray(store.p.getString("draft_samples","[]"));
                    long last=samples.length()==0?start:samples.getJSONObject(samples.length()-1).optLong("at",start);
                    summary.put("start",start); summary.put("duration",Math.max(0,last-start)); summary.put("samples",samples);
                    summary.put("reason","interrupted"); summary.put("engine",store.p.getString("last_engine_log","")); summary.put("recovery",log);
                    store.saveSession(summary);
                } catch(Exception ignored) { }
                store.p.edit().putBoolean("active",false).remove("draft_samples").commit();
            }
            boolean pending=engine.pending();
            runOnUiThread(()->{ if(!isFinishing()) { render(); info(t("Pemulihan","Recovery"),
                (pending?t("Masih ada perubahan yang menunggu. Sambungkan Shizuku/periksa izin, lalu ulangi.\n\n","Some changes are pending. Reconnect Shizuku/check permissions, then retry.\n\n"):t("Pemeriksaan selesai.\n\n","Check complete.\n\n"))+log); } });
        });
    }
    private void section(String title,String subtitle) {
        Glass.gap(page,16); page.addView(text(title,16,Glass.INK,true)); Glass.gap(page,4);
        page.addView(text(subtitle,11,Glass.MUTED,false)); Glass.gap(page,11);
    }
    private void engine() {
        titleRow(t("Profil performa","Performance profiles"),t("Pengaturan berlaku pada sesi berikutnya.","Settings apply on the next session."),null,null);
        String selected=store.p.getString("selected_game",GameSpec.ALL[0].id);
        if(GameSpec.find(selected)==null) selected=GameSpec.ALL[0].id;
        final String gameId=selected;
        LinearLayout picker=Glass.row(this);
        for(GameSpec game:GameSpec.ALL) {
            if(picker.getChildCount()>0) Glass.gap(picker,8);
            TextView b=button(game.title,game.id.equals(gameId),()->{ store.p.edit().putString("selected_game",game.id).apply(); render(); });
            picker.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        page.addView(picker);
        section(t("Profil sesi","Session profile"),t("Pilihan GameManager; efek tergantung dukungan game dan HiOS.","GameManager preferences; effects depend on the game and HiOS."));
        LinearLayout modes=Glass.row(this);
        String[] ids={"eco","balanced","boost"}; String[] labels={t("Hemat","Eco"),t("Seimbang","Balanced"),t("Performa","Performance")};
        for(int i=0;i<ids.length;i++) { if(i>0) Glass.gap(modes,8); final String mode=ids[i];
            modes.addView(button(labels[i],store.profile(gameId,"mode","balanced").equals(mode),()->{ store.setProfile(gameId,"mode",mode); render(); }),new LinearLayout.LayoutParams(0,-2,1)); }
        page.addView(modes);
        section(t("Refresh rate layar","Display refresh rate"),t("Bukan pembuka batas FPS game. Pilihan mengikuti mode layar perangkat.","Does not unlock game FPS. Options follow the device display modes."));
        LinearLayout refresh=Glass.row(this);
        addHz(refresh,gameId,"system",t("Ikuti sistem","System"));
        TreeSet<Integer> supported=new TreeSet<>();
        for(Display.Mode m:getWindowManager().getDefaultDisplay().getSupportedModes()) {
            int hz=Math.round(m.getRefreshRate()); if(hz==60 || hz==90) supported.add(hz);
        }
        for(int hz:supported) { Glass.gap(refresh,8); addHz(refresh,gameId,String.valueOf(hz),hz+" Hz"); }
        page.addView(refresh);
        Glass.gap(page,14);
        LinearLayout focus=Glass.card(this);
        toggle(focus,t("Mode Fokus","Focus mode"),t("Senyapkan pesan/panggilan; alarm tetap diizinkan. Perlu izin Jangan Ganggu.","Silence calls/messages; allow alarms. Requires Do Not Disturb access."),"focus",true);
        Glass.gap(focus,12); toggle(focus,t("Sidebar tarik","Pull-out sidebar"),t("Tarik untuk membuka. Tahan dan geser untuk memindah tepi.","Swipe to open. Hold and drag to another edge."),"overlay",true);
        Glass.gap(focus,12);toggle(focus,t("Akhiri otomatis saat keluar","End automatically after leaving"),t("Mati secara bawaan: akhiri lewat sidebar, notifikasi, atau aplikasi. Opsional dengan akses penggunaan; berlaku mulai sesi berikutnya.","Off by default: end via sidebar, notification or app. Optional with usage access; applies to the next session."),"auto_end",false);
        page.addView(focus);
        section(t("Pengecualian aplikasi","App exclusions"),t("WhatsApp dilindungi secara default. Tidak ada force-stop otomatis di versi ini.","WhatsApp is protected by default. This version does not automatically force-stop apps."));
        page.addView(button(t("Atur daftar pengecualian","Manage exclusions"),false,this::exclusions));
        section(t("Batas kemampuan versi ini","This version's capability limits"),t("Fitur yang belum tervalidasi tetap dinonaktifkan.","Unverified features remain disabled."));
        LinearLayout limits=Glass.card(this);
        limits.addView(text(t("• Downscale render: belum diaktifkan sebelum validasi HiOS.\n• FPS/1% low: belum tersedia; tidak disimulasikan.\n• Driver GPU, EQ lintas aplikasi, dan tuning sentuh: tidak diklaim didukung.\n• Tidak ada debloat, kompilasi AOT otomatis, atau pemutusan internet aplikasi lain.\n• Proteksi suhu dan file game tidak disentuh.","• Render downscale: disabled pending HiOS validation.\n• FPS/1% low: unavailable; never simulated.\n• GPU drivers, global EQ and touch tuning: not claimed as supported.\n• No debloat, automatic AOT compilation or app network blocking.\n• Thermal protections and game files remain untouched."),12,Glass.MUTED,false)); page.addView(limits);
        Glass.gap(page,12); page.addView(button(t("Lihat hasil engine terakhir","View last engine results"),false,()->info("Engine",store.p.getString("last_engine_log",t("Belum ada sesi.","No session yet.")))));
    }
    private void addHz(LinearLayout row,String game,String value,String label) {
        row.addView(button(label,store.profile(game,"refresh","system").equals(value),()->{ store.setProfile(game,"refresh",value); render(); }),new LinearLayout.LayoutParams(0,-2,1));
    }
    private void toggle(LinearLayout parent,String title,String description,String key,boolean fallback) {
        LinearLayout row=Glass.row(this), words=Glass.column(this);
        words.addView(text(title,14,Glass.INK,true)); Glass.gap(words,4); words.addView(text(description,11,Glass.MUTED,false));
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        Switch s=new Switch(this); s.setContentDescription(title); s.setChecked(store.p.getBoolean(key,fallback)); s.setHapticFeedbackEnabled(false);
        s.setOnCheckedChangeListener((v,checked)->store.p.edit().putBoolean(key,checked).apply()); row.addView(s); parent.addView(row);
    }
    private void exclusions() {
        List<android.content.pm.ResolveInfo> apps=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
        TreeMap<String,String> names=new TreeMap<>();
        for(android.content.pm.ResolveInfo a:apps) {
            String id=a.activityInfo.packageName;
            if(GameSpec.find(id)==null && !id.equals(getPackageName())) names.put(id,a.loadLabel(getPackageManager()).toString());
        }
        names.put("com.whatsapp","WhatsApp");
        String[] packages=names.keySet().toArray(new String[0]),labels=new String[packages.length]; boolean[] checks=new boolean[packages.length];
        Set<String> selected=new HashSet<>(store.p.getStringSet("exclusions",new HashSet<>(Collections.singleton("com.whatsapp"))));
        for(int i=0;i<packages.length;i++) { labels[i]=names.get(packages[i]); checks[i]=selected.contains(packages[i]); }
        new AlertDialog.Builder(this).setTitle(t("Pengecualian · disimpan untuk profil","Exclusions · saved for profiles"))
            .setMultiChoiceItems(labels,checks,(d,w,checked)->checks[w]=checked)
            .setPositiveButton(t("Simpan","Save"),(d,w)->{
                Set<String> save=new HashSet<>(); for(int i=0;i<checks.length;i++) if(checks[i]) save.add(packages[i]);
                store.p.edit().putStringSet("exclusions",save).apply();
            }).setNegativeButton(t("Batal","Cancel"),null).show();
    }
    private void device() {
        titleRow(t("Perangkat & sesi","Device & sessions"),android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" · Android "+Build.VERSION.RELEASE,null,null);
        LinearLayout permissions=Glass.card(this);
        String state=PrivilegeBridge.state();
        permission(permissions,"Shizuku",state.equals("ready")?t("Terhubung · izin diberikan","Connected · authorized"):state.equals("permission")?t("Terhubung · butuh izin","Connected · permission needed"):t("Belum terhubung","Not connected"),this::shizuku);
        permission(permissions,t("Panel di atas game","Display over games"),Settings.canDrawOverlays(this)?t("Diizinkan","Allowed"):t("Opsional · belum diizinkan","Optional · not allowed"),()->open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        NotificationManager n=getSystemService(NotificationManager.class);
        permission(permissions,t("Jangan Ganggu","Do Not Disturb"),n.isNotificationPolicyAccessGranted()?t("Diizinkan","Allowed"):t("Dibutuhkan untuk Mode Fokus","Required for Focus mode"),()->open(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        permission(permissions,t("Akses penggunaan","Usage access"),Telemetry.usageAllowed(this)?t("Diizinkan · deteksi keluar tetap pilihan pengguna","Allowed · exit detection remains opt-in"):t("Opsional · sesi diakhiri manual","Optional · end session manually"),()->open(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        permission(permissions,t("Notifikasi sesi","Session notifications"),n.areNotificationsEnabled()?t("Diizinkan","Allowed"):t("Aktifkan agar tombol berhenti terlihat","Enable to see the stop action"),()->{
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},31);
            else open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
        });
        page.addView(permissions);
        Glass.gap(page,12);page.addView(button(t("Detail sesi terakhir / sidebar hilang","Last session / missing sidebar details"),false,()->info(t("Pemeriksaan sesi","Session evidence"),SessionDiagnostics.report(this))));
        Glass.gap(page,8);page.addView(button(t("Pengaturan baterai/latar belakang aplikasi","App battery/background settings"),false,()->{
            new AlertDialog.Builder(this).setTitle(t("Aktivitas latar belakang","Background activity"))
                .setMessage(t("Periksa pengaturan baterai/aktivitas latar belakang spaces di HiOS. Pembatasan dapat menghentikan layanan; pengaturan ini tidak menjamin aplikasi selalu hidup. Aplikasi tidak mengubahnya otomatis.","Review spaces battery/background activity settings in HiOS. Restrictions can stop services; settings cannot guarantee survival. The app does not change them automatically."))
                .setPositiveButton(t("Buka info aplikasi","Open app info"),(d,w)->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))))
                .setNegativeButton(t("Batal","Cancel"),null).show();
        }));
        Glass.gap(page,12); page.addView(button(t("Periksa & pulihkan pengaturan","Check & restore settings"),false,this::recover));
        section(t("Riwayat sesi","Session history"),t("Maksimal 30 sesi di HP ini. Grafik adalah suhu baterai, bukan FPS.","Up to 30 local sessions. Charts show battery temperature, not FPS."));
        JSONArray history=store.history();
        if(history.length()==0) page.addView(text(t("Belum ada sesi. Riwayat muncul setelah selesai main.","No sessions yet. History appears after you finish playing."),12,Glass.MUTED,false));
        for(int i=0;i<history.length();i++) {
            JSONObject session=history.optJSONObject(i); if(session==null) continue;
            GameSpec g=GameSpec.find(session.optString("game"));
            LinearLayout item=Glass.card(this);
            item.addView(text((g==null?"Game":g.title)+" · "+session.optLong("duration")/60000+" min",15,Glass.INK,true)); Glass.gap(item,5);
            String when=new SimpleDateFormat("dd MMM yyyy · HH:mm",Locale.getDefault()).format(new Date(session.optLong("start")));
            item.addView(text(when+" · "+SessionDiagnostics.eventLabel(store,session.optString("reason")),11,Glass.MUTED,false));
            JSONArray points=session.optJSONArray("samples");
            if(points!=null && points.length()>1) {
                Glass.gap(item,10); item.addView(new SessionChart(this,points),new LinearLayout.LayoutParams(-1,dp(120)));
                double peak=-999; for(int j=0;j<points.length();j++) { JSONObject p=points.optJSONObject(j); if(p!=null && p.has("temp")) peak=Math.max(peak,p.optDouble("temp")); }
                if(peak>-999) item.addView(text(t("Suhu baterai puncak: ","Peak battery temperature: ")+String.format(Locale.getDefault(),"%.1f °C",peak),11,Glass.MUTED,false));
            } else { Glass.gap(item,8); item.addView(text(t("Belum cukup sampel untuk grafik.","Not enough samples for a chart."),11,Glass.MUTED,false)); }
            Glass.gap(item,8); item.addView(button(t("Detail pengaturan & pemulihan","Settings & recovery details"),false,()->info("Engine",session.optString("engine")+"\n\n"+session.optString("recovery"))));
            page.addView(item); Glass.gap(page,10);
        }
        if(history.length()>0) page.addView(button(t("Hapus riwayat lokal","Delete local history"),false,()->new AlertDialog.Builder(this)
            .setTitle(t("Hapus seluruh riwayat?","Delete all history?"))
            .setPositiveButton(t("Hapus","Delete"),(d,w)->{ store.p.edit().remove("history").apply(); render(); }).setNegativeButton(t("Batal","Cancel"),null).show()));
        section(t("Tentang spaces","About spaces"),"v"+BuildConfig.VERSION_NAME+" · "+t("Tanpa akun, iklan, atau pelacak.","No accounts, ads or trackers."));
        page.addView(button(t("Buka halaman unduhan pembaruan","Open update downloads"),false,()->open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/veldafelix7-oss/spaces-mobile/releases")))));
        Glass.gap(page,10); page.addView(button(t("Privasi & batasan","Privacy & limitations"),false,()->info(t("Privasi & batasan","Privacy & limitations"),t(
            "Data sesi disimpan hanya di HP. Tidak ada analitik atau server aplikasi. Tes koneksi opsional menghubungi 1.1.1.1:443; itu bukan ping server game. Tombol pembaruan membuka browser.\n\nGameManager/refresh rate memerlukan Shizuku dan belum tentu meningkatkan FPS. Tidak ada jaminan anti-ban. Jangan matikan proteksi suhu.\n\nFusiMem adalah RAM virtual. Ubah lewat Pengaturan HiOS dan uji terpisah setelah restart. Aplikasi ini tidak mengubahnya otomatis.\n\nJika proses mati/restart, buka aplikasi lalu gunakan pemulihan. Jika Shizuku mati, sambungkan lagi. Tidak ada jaminan pemulihan saat aplikasi tidak berjalan.",
            "Session data stays on your phone. No analytics or app servers. Optional connectivity tests contact 1.1.1.1:443, not a game server. Updates open your browser.\n\nGameManager/refresh settings require Shizuku and may not improve FPS. No anti-ban guarantee. Keep thermal protections enabled.\n\nFusiMem is virtual RAM. Change it in HiOS settings and test separately after reboot. This app does not change it automatically.\n\nAfter process death/reboot, open the app and use recovery. Reconnect Shizuku if necessary. Recovery cannot be guaranteed while the app is not running."))));
    }
    private void permission(LinearLayout parent,String title,String state,Runnable action) {
        LinearLayout row=Glass.row(this); row.setPadding(0,dp(7),0,dp(7)); LinearLayout words=Glass.column(this);
        words.addView(text(title,14,Glass.INK,true)); Glass.gap(words,4); words.addView(text(state,11,Glass.MUTED,false));
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1)); row.addView(button(t("Atur","Set up"),false,action)); parent.addView(row);
    }
    private void shizuku() {
        if(PrivilegeBridge.authorized()) { PrivilegeBridge.bind(this); info("Shizuku",t("Izin sudah diberikan. Dukungan setiap pengaturan tetap diperiksa saat sesi dimulai.","Permission granted. Individual settings are still checked when a session starts.")); return; }
        if(PrivilegeBridge.state().equals("permission")) {
            try { Shizuku.requestPermission(19); } catch(Exception e) { toast(t("Tidak dapat meminta izin Shizuku.","Unable to request Shizuku permission.")); } return;
        }
        new AlertDialog.Builder(this).setTitle(t("Hubungkan Shizuku · tanpa root","Connect Shizuku · no root"))
            .setMessage(t("1. Pasang Shizuku dari sumber resminya.\n2. Buka Pengaturan → Tentang ponsel, ketuk Nomor bentukan 7× untuk membuka Opsi Pengembang.\n3. Di Opsi Pengembang, aktifkan Debugging nirkabel saat tersambung Wi-Fi.\n4. Di Shizuku, ikuti panduan pemasangan/pairing lalu Mulai.\n5. Kembali ke sini, ketuk Atur dan izinkan akses.\n\nSetelah HP restart, Shizuku biasanya perlu dimulai lagi. Jangan membagikan kode pairing. Matikan debugging saat tidak diperlukan.",
            "1. Install Shizuku from its official source.\n2. Settings → About phone: tap Build number 7 times to enable Developer options.\n3. Enable Wireless debugging on Wi-Fi.\n4. Follow Shizuku's pairing guide, then Start.\n5. Return here, tap Set up and grant access.\n\nAfter a reboot, Shizuku usually needs restarting. Never share pairing codes. Disable debugging when not needed."))
            .setPositiveButton(t("Buka Shizuku","Open Shizuku"),(d,w)->{
                Intent i=getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                if(i!=null) open(i); else open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/RikkaApps/Shizuku/releases")));
            }).setNeutralButton(t("Opsi pengembang","Developer options"),(d,w)->open(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)))
            .setNegativeButton(t("Tutup","Close"),null).show();
    }
}
