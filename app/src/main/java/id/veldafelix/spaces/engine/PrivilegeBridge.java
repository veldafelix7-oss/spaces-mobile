package id.veldafelix.spaces.engine;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import rikka.shizuku.Shizuku;

public final class PrivilegeBridge {
    private static volatile IShell shell;
    private static boolean binding;
    private static long generation;
    private static ServiceConnection activeConnection;
    private static Shizuku.UserServiceArgs activeArgs;
    private static CountDownLatch ready=new CountDownLatch(1);
    static { Shizuku.addBinderDeadListener(PrivilegeBridge::reset); }
    private static synchronized void reset() {
        shell=null;binding=false;generation++;ready.countDown();ready=new CountDownLatch(1);
        ServiceConnection old=activeConnection;Shizuku.UserServiceArgs args=activeArgs;activeConnection=null;activeArgs=null;
        if(old!=null && args!=null) try { Shizuku.unbindUserService(args,old,false); } catch(Exception ignored) { }
    }
    private static boolean connected() { return shell!=null && shell.asBinder().isBinderAlive(); }
    public static boolean authorized() {
        try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED; }
        catch(Exception e) { return false; }
    }
    public static synchronized String state() {
        try {
            if(!Shizuku.pingBinder()) return "offline";
            if(!authorized()) return "permission";
            return connected()?"ready":"authorized";
        } catch(Exception e) { return "offline"; }
    }
    public static synchronized void bind(Context c) {
        if(!authorized() || connected() || binding) return;
        binding=true;final long attempt=++generation;
        final CountDownLatch latch=new CountDownLatch(1);ready=latch;
        ServiceConnection connection=new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName n,IBinder binder) {
                synchronized(PrivilegeBridge.class) {
                    if(attempt!=generation) return;
                    shell=IShell.Stub.asInterface(binder);binding=false;latch.countDown();
                }
            }
            @Override public void onServiceDisconnected(ComponentName n) {
                synchronized(PrivilegeBridge.class) { if(attempt==generation) reset(); }
            }
        };
        try {
            activeArgs=new Shizuku.UserServiceArgs(new ComponentName(c.getApplicationContext(),ShellService.class))
                .daemon(false).processNameSuffix("spaces_shell").version(1);
            activeConnection=connection;Shizuku.bindUserService(activeArgs,connection);
        } catch(Exception e) { if(attempt==generation) reset(); }
    }
    /** Never block the UI. Each timeout/death invalidates late connection callbacks. */
    public static String exec(Context c,int op,String key,String value) {
        if(Looper.myLooper()==Looper.getMainLooper()) return "ERR:main thread";
        if(!authorized()) return "ERR:Shizuku unavailable";
        bind(c);
        final long attempt;final CountDownLatch latch;
        synchronized(PrivilegeBridge.class) { attempt=generation;latch=ready; }
        try {
            if(!connected()) latch.await(6,TimeUnit.SECONDS);
            final IShell selected;
            synchronized(PrivilegeBridge.class) {
                if(attempt!=generation || !connected()) { if(attempt==generation) reset();return "ERR:Shizuku connection timeout"; }
                selected=shell;
            }
            return selected.execute(op,key,value);
        } catch(Exception e) {
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            synchronized(PrivilegeBridge.class) { if(attempt==generation) reset(); }
            return "ERR:"+e.getClass().getSimpleName();
        }
    }
}
