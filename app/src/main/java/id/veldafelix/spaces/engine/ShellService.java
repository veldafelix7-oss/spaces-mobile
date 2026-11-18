package id.veldafelix.spaces.engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import id.veldafelix.spaces.core.ShellPolicy;

/** Runs under Shizuku's shell identity. No arbitrary commands, paths or package IDs. */
public final class ShellService extends IShell.Stub {
    public ShellService() {}
    @Override public String execute(int operation, String key, String value) {
        Process p=null;
        try {
            String[] args=ShellPolicy.command(operation,key,value);
            p=new ProcessBuilder(args).redirectErrorStream(true).start();
            final Process child=p;
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            Thread reader=new Thread(()->{
                try(InputStream in=child.getInputStream()) {
                    byte[] b=new byte[1024]; int n;
                    while((n=in.read(b))!=-1) if(out.size()<32768) out.write(b,0,Math.min(n,32768-out.size()));
                } catch(Exception ignored) { }
            });
            reader.start();
            if(!p.waitFor(5,TimeUnit.SECONDS)) { p.destroyForcibly(); reader.join(500); return "ERR:timeout"; }
            reader.join(1000);
            String text=out.toString(StandardCharsets.UTF_8.name()).trim();
            return p.exitValue()==0?text:"ERR:"+text;
        } catch(Exception e) { return "ERR:"+e.getClass().getSimpleName(); }
        finally { if(p!=null) p.destroy(); }
    }
    @Override public void destroy() { System.exit(0); }
}
