package id.veldafelix.spaces.engine;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import rikka.shizuku.Shizuku;

/** Owns one generation-checked Shizuku UserService binding. Never executes shell on the UI thread. */
public final class PrivilegeBridge {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final long CONNECT_TIMEOUT_MS=7000;
    private static volatile IShell shell;
    private static volatile Runnable stateListener;
    private static boolean binding;
    private static long generation;
    private static String lastFailure;
    private static ServiceConnection activeConnection;
    private static Shizuku.UserServiceArgs activeArgs;
    private static CountDownLatch ready=new CountDownLatch(1);

    static { Shizuku.addBinderDeadListener(()->reset("Shizuku stopped")); }

    private static void changed() {
        Runnable listener=stateListener;
        if(listener!=null) MAIN.post(listener);
    }
    /** Activity listener is detached on pause so it cannot retain a stopped UI. */
    public static void setStateListener(Runnable listener) {
        stateListener=listener;
    }
    private static void reset(String failure) {
        resetIfCurrent(-1,failure);
    }
    /** Invalidate one binding generation without allowing an old callback to tear down its replacement. */
    private static boolean resetIfCurrent(long expectedGeneration,String failure) {
        return resetGeneration(expectedGeneration,failure,false);
    }
    /** Timeout invalidation is conditional on that exact generation still being in the binding phase. */
    private static boolean resetIfBindingTimedOut(long expectedGeneration,String failure) {
        return resetGeneration(expectedGeneration,failure,true);
    }
    private static boolean resetGeneration(long expectedGeneration,String failure,boolean requireBinding) {
        ServiceConnection old;
        Shizuku.UserServiceArgs args;
        synchronized(PrivilegeBridge.class) {
            if(expectedGeneration>=0 && (generation!=expectedGeneration || (requireBinding && !binding))) return false;
            shell=null;binding=false;generation++;lastFailure=failure;
            ready.countDown();ready=new CountDownLatch(1);
            old=activeConnection;args=activeArgs;activeConnection=null;activeArgs=null;
        }
        if(old!=null && args!=null) try { Shizuku.unbindUserService(args,old,false); } catch(Exception ignored) { }
        changed();return true;
    }
    private static boolean connected() {
        IShell current=shell;
        return current!=null && current.asBinder()!=null && current.asBinder().isBinderAlive();
    }
    public static boolean authorized() {
        try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED; }
        catch(Exception e) { return false; }
    }
    public static synchronized String state() {
        try {
            if(!Shizuku.pingBinder()) return "offline";
            if(!authorized()) return "permission";
            if(connected()) return "ready";
            if(binding) return "connecting";
            return lastFailure==null?"authorized":"error";
        } catch(Exception e) { return "offline"; }
    }
    public static synchronized String lastFailure() { return lastFailure==null?"":lastFailure; }

    public static void bind(Context context) {
        final Context appContext=context.getApplicationContext();
        final long attempt;
        final CountDownLatch latch;
        final ServiceConnection connection;
        final Shizuku.UserServiceArgs args;
        final ServiceConnection staleConnection;
        final Shizuku.UserServiceArgs staleArgs;
        synchronized(PrivilegeBridge.class) {
            if(!authorized() || connected() || binding) { changed(); return; }
            // A previous disconnected binding is stale. Invalidate its callbacks before retrying.
            staleConnection=activeConnection;staleArgs=activeArgs;
            if(staleConnection!=null) { activeConnection=null;activeArgs=null;generation++; }
            binding=true;lastFailure=null;attempt=++generation;
            latch=new CountDownLatch(1);ready=latch;
            connection=new ServiceConnection() {
                @Override public void onServiceConnected(ComponentName name,IBinder binder) {
                    synchronized(PrivilegeBridge.class) {
                        if(attempt!=generation) return;
                        IShell connected=binder==null?null:IShell.Stub.asInterface(binder);
                        if(connected==null || !binder.isBinderAlive()) {
                            shell=null;binding=false;lastFailure="UserService returned no live binder";
                        } else {
                            shell=connected;binding=false;lastFailure=null;
                        }
                        latch.countDown();
                    }
                    changed();
                }
                @Override public void onServiceDisconnected(ComponentName name) {
                    if(resetIfCurrent(attempt,"UserService disconnected")) {
                        // One bounded reconnect while the user is in Spaces; otherwise retry on next resume.
                        MAIN.postDelayed(()->{
                            if(stateListener!=null && authorized()) bind(appContext);
                        },800);
                    }
                }
                @Override public void onBindingDied(ComponentName name) {
                    resetIfCurrent(attempt,"Android reported UserService binding died");
                }
                @Override public void onNullBinding(ComponentName name) {
                    resetIfCurrent(attempt,"Android returned a null UserService binder");
                }
            };
            args=new Shizuku.UserServiceArgs(new ComponentName(context.getApplicationContext(),ShellService.class))
                .daemon(false).processNameSuffix("spaces_shell").tag("spaces_shell_v1").version(1);
            activeArgs=args;activeConnection=connection;
        }
        if(staleConnection!=null && staleArgs!=null) try { Shizuku.unbindUserService(staleArgs,staleConnection,false); } catch(Exception ignored) { }
        changed();
        try {
            Shizuku.bindUserService(args,connection);
            MAIN.postDelayed(()->{
                synchronized(PrivilegeBridge.class) {
                    if(attempt!=generation || !binding) return;
                }
                resetIfBindingTimedOut(attempt,"UserService connection timed out");
            },CONNECT_TIMEOUT_MS);
        } catch(Exception error) {
            resetIfCurrent(attempt,error.getClass().getSimpleName()+": "+String.valueOf(error.getMessage()));
        }
    }

    /** Never block the UI. Each timeout/death invalidates late connection callbacks. */
    public static String exec(Context context,int operation,String key,String value) {
        if(Looper.myLooper()==Looper.getMainLooper()) return "ERR:main thread";
        if(!authorized()) return "ERR:Shizuku unavailable";
        bind(context);
        final long attempt;final CountDownLatch latch;
        synchronized(PrivilegeBridge.class) { attempt=generation;latch=ready; }
        try {
            if(!connected() && !latch.await(CONNECT_TIMEOUT_MS,TimeUnit.MILLISECONDS)) {
                final IShell lateConnection;
                synchronized(PrivilegeBridge.class) {
                    if(attempt!=generation) return "ERR:stale connection";
                    lateConnection=connected()?shell:null;
                }
                // A callback that won the timeout race is allowed to finish normally.
                if(lateConnection!=null) return lateConnection.execute(operation,key,value);
                if(!resetIfBindingTimedOut(attempt,"UserService connection timed out")) {
                    final IShell racedConnection;
                    synchronized(PrivilegeBridge.class) {
                        if(attempt!=generation) return "ERR:stale connection";
                        racedConnection=connected()?shell:null;
                    }
                    if(racedConnection!=null) return racedConnection.execute(operation,key,value);
                }
                return "ERR:Shizuku connection timeout";
            }
            final IShell selected;
            synchronized(PrivilegeBridge.class) {
                if(attempt!=generation || !connected()) return "ERR:Shizuku service unavailable";
                selected=shell;
            }
            return selected.execute(operation,key,value);
        } catch(Exception error) {
            if(error instanceof InterruptedException) Thread.currentThread().interrupt();
            resetIfCurrent(attempt,error.getClass().getSimpleName()+": "+String.valueOf(error.getMessage()));
            return "ERR:"+error.getClass().getSimpleName();
        }
    }
}
