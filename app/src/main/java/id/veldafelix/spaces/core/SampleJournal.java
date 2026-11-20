package id.veldafelix.spaces.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;

/** Disk-backed samples. No growing JSON graph or whole-session rewrite on each tick. */
public final class SampleJournal {
    public static final int LIMIT=1080, MAX_LINE=2048;
    private final File file;
    private int records=-1;
    public SampleJournal(File file) { this.file=file; }
    public synchronized void append(String point) throws IOException {
        if(point==null || point.isEmpty() || point.length()>MAX_LINE || point.indexOf('\n')>=0 || point.indexOf('\r')>=0)
            throw new IOException("Invalid sample record");
        File parent=file.getParentFile();
        if(parent!=null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Sample directory unavailable");
        // If compaction previously failed, do not keep appending an unbounded disk backlog.
        if(records>=LIMIT*2) compact();
        // A process can die halfway through the last append. Never join its partial JSON to a new sample.
        try(RandomAccessFile out=new RandomAccessFile(file,"rw")) {
            long length=out.length();
            if(length>0) {
                out.seek(length-1);
                if(out.read()!='\n') {
                    long at=length-1;
                    while(at>=0) { out.seek(at);if(out.read()=='\n') break;at--; }
                    length=at+1;out.setLength(length);
                }
            }
            out.seek(length);out.write((point+"\n").getBytes(StandardCharsets.UTF_8));out.getFD().sync();
        }
        if(records<0) { records=0;try(BufferedReader in=reader()) { while(in.readLine()!=null) records++; } }
        else records++;
        if(records>=LIMIT*2) compact();
    }
    private BufferedReader reader() throws IOException { return new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)); }
    public synchronized List<String> readLatest() throws IOException {
        ArrayDeque<String> points=new ArrayDeque<>(LIMIT);
        if(!file.exists()) return new ArrayList<>();
        boolean terminated;
        try(RandomAccessFile in=new RandomAccessFile(file,"r")) { if(in.length()==0) return new ArrayList<>();in.seek(in.length()-1);terminated=in.read()=='\n'; }
        try(BufferedReader in=reader()) {
            String line=in.readLine();
            while(line!=null) {
                String next=in.readLine();
                if(next==null && !terminated) break;
                if(!line.isEmpty() && line.length()<=MAX_LINE) {
                    if(points.size()==LIMIT) points.removeFirst();points.addLast(line);
                }
                line=next;
            }
        }
        return new ArrayList<>(points);
    }
    private void compact() throws IOException {
        List<String> latest=readLatest();File temp=new File(file.getPath()+".tmp");
        try(FileOutputStream out=new FileOutputStream(temp)) {
            for(String line:latest) out.write((line+"\n").getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        Files.move(temp.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        records=latest.size();
    }
    public synchronized void delete() { file.delete();new File(file.getPath()+".tmp").delete();records=0; }
}
