package id.veldafelix.spaces.engine;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import rikka.shizuku.Shizuku;

public final class PrivilegeBridge {
    private static volatile IShell shell;
    private static boolean binding;
    private static volatile CountDownLatch ready=new CountDownLatch(1);
    private static final ServiceConnection CONNECTION=new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder binder) { shell=IShell.Stub.asInterface(binder); ready.countDown(); }
        @Override public void onServiceDisconnected(ComponentName n) { shell=null; synchronized(PrivilegeBridge.class) { binding=false; ready.countDown(); ready=new CountDownLatch(1); } }
    };
    public static boolean authorized() {
        try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED; }
        catch(Exception e) { return false; }
    }
    public static String state() {
        try { if(!Shizuku.pingBinder()) return "offline"; return authorized()?"ready":"permission"; }
        catch(Exception e) { return "offline"; }
    }
    public static synchronized void bind(Context c) {
        if(!authorized() || (shell!=null && shell.asBinder().isBinderAlive()) || binding) return;
        binding=true;
        try {
            Shizuku.bindUserService(new Shizuku.UserServiceArgs(new ComponentName(c,ShellService.class))
                .daemon(false).processNameSuffix("spaces_shell").version(1),CONNECTION);
        } catch(Exception e) { binding=false; ready.countDown(); }
    }
    /** Must only be called off the main thread. */
    public static String exec(Context c,int op,String key,String value) {
        if(!authorized()) return "ERR:Shizuku unavailable";
        bind(c);
        try {
            if(shell==null) ready.await(6,TimeUnit.SECONDS);
            IShell s=shell;
            if(s==null) return "ERR:Shizuku connection timeout";
            return s.execute(op,key,value);
        } catch(Exception e) { shell=null; synchronized(PrivilegeBridge.class) { binding=false; ready=new CountDownLatch(1); } return "ERR:"+e.getClass().getSimpleName(); }
    }
}
