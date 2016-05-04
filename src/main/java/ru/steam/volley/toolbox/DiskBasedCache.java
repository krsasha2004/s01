package ru.steam.volley.toolbox;

import android.os.SystemClock;
import com.android.volley.Cache;
import com.android.volley.Cache.Entry;
import com.android.volley.VolleyLog;
import ru.steam.volley.Cache;
import ru.steam.volley.VolleyLog;

import java.io.*;
import java.util.*;

public class DiskBasedCache implements Cache {
    private final Map<String, CacheHeader> mEntries;
    private final int mMaxCacheSizeInBytes;
    private final File mRootDirectory;
    private long mTotalSize;

    static class CacheHeader {
        public String etag;
        public String key;
        public Map<String, String> responseHeaders;
        public long serverDate;
        public long size;
        public long softTtl;
        public long ttl;

        private CacheHeader() {
        }

        public CacheHeader(String key, Entry entry) {
            this.key = key;
            this.size = (long) entry.data.length;
            this.etag = entry.etag;
            this.serverDate = entry.serverDate;
            this.ttl = entry.ttl;
            this.softTtl = entry.softTtl;
            this.responseHeaders = entry.responseHeaders;
        }

        public static CacheHeader readHeader(InputStream is) throws IOException {
            CacheHeader entry = new CacheHeader();
            if (DiskBasedCache.readInt(is) != 538183203) {
                throw new IOException();
            }
            entry.key = DiskBasedCache.readString(is);
            entry.etag = DiskBasedCache.readString(is);
            if (entry.etag.equals("")) {
                entry.etag = null;
            }
            entry.serverDate = DiskBasedCache.readLong(is);
            entry.ttl = DiskBasedCache.readLong(is);
            entry.softTtl = DiskBasedCache.readLong(is);
            entry.responseHeaders = DiskBasedCache.readStringStringMap(is);
            return entry;
        }

        public Entry toCacheEntry(byte[] data) {
            Entry e = new Entry();
            e.data = data;
            e.etag = this.etag;
            e.serverDate = this.serverDate;
            e.ttl = this.ttl;
            e.softTtl = this.softTtl;
            e.responseHeaders = this.responseHeaders;
            return e;
        }

        public boolean writeHeader(OutputStream os) {
            try {
                DiskBasedCache.writeInt(os, 538183203);
                DiskBasedCache.writeString(os, this.key);
                DiskBasedCache.writeString(os, this.etag == null ? "" : this.etag);
                DiskBasedCache.writeLong(os, this.serverDate);
                DiskBasedCache.writeLong(os, this.ttl);
                DiskBasedCache.writeLong(os, this.softTtl);
                DiskBasedCache.writeStringStringMap(this.responseHeaders, os);
                os.flush();
                return true;
            } catch (IOException e) {
                VolleyLog.m0d("%s", e.toString());
                return false;
            }
        }
    }

    private static class CountingInputStream extends FilterInputStream {
        private int bytesRead;

        private CountingInputStream(InputStream in) {
            super(in);
            this.bytesRead = 0;
        }

        public int read() throws IOException {
            int result = super.read();
            if (result != -1) {
                this.bytesRead++;
            }
            return result;
        }

        public int read(byte[] buffer, int offset, int count) throws IOException {
            int result = super.read(buffer, offset, count);
            if (result != -1) {
                this.bytesRead += result;
            }
            return result;
        }
    }

    public DiskBasedCache(File rootDirectory, int maxCacheSizeInBytes) {
        this.mEntries = new LinkedHashMap(16, 0.75f, true);
        this.mTotalSize = 0;
        this.mRootDirectory = rootDirectory;
        this.mMaxCacheSizeInBytes = maxCacheSizeInBytes;
    }

    public DiskBasedCache(File rootDirectory) {
        this(rootDirectory, 5242880);
    }

    public synchronized Cache.Entry get(String key) {
        IOException e;
        Throwable th;
        Entry entry = null;
        synchronized (this) {
            CacheHeader entry2 = (CacheHeader) this.mEntries.get(key);
            if (entry2 != null) {
                File file = getFileForKey(key);
                CountingInputStream cis = null;
                try {
                    CountingInputStream cis2 = new CountingInputStream(null);
                    try {
                        CacheHeader.readHeader(cis2);
                        Entry toCacheEntry = entry2.toCacheEntry(streamToBytes(cis2, (int) (file.length() - ((long) cis2.bytesRead))));
                        if (cis2 != null) {
                            try {
                                cis2.close();
                            } catch (IOException e2) {
                            }
                        }
                        entry = toCacheEntry;
                    } catch (IOException e3) {
                        e = e3;
                        cis = cis2;
                        try {
                            VolleyLog.m0d("%s: %s", file.getAbsolutePath(), e.toString());
                            remove(key);
                            if (cis != null) {
                                try {
                                    cis.close();
                                } catch (IOException e4) {
                                }
                            }
                            return entry;
                        } catch (Throwable th2) {
                            th = th2;
                            if (cis != null) {
                                try {
                                    cis.close();
                                } catch (IOException e5) {
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        cis = cis2;
                        if (cis != null) {
                            cis.close();
                        }
                        throw th;
                    }
                } catch (IOException e6) {
                    e = e6;
                    VolleyLog.m0d("%s: %s", file.getAbsolutePath(), e.toString());
                    remove(key);
                    if (cis != null) {
                        cis.close();
                    }
                    return entry;
                }
            }
        }
        return entry;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void initialize() {
        /*
        r10 = this;
        r5 = 0;
        monitor-enter(r10);
        r6 = r10.mRootDirectory;	 Catch:{ all -> 0x006d }
        r6 = r6.exists();	 Catch:{ all -> 0x006d }
        if (r6 != 0) goto L_0x0025;
    L_0x000a:
        r5 = r10.mRootDirectory;	 Catch:{ all -> 0x006d }
        r5 = r5.mkdirs();	 Catch:{ all -> 0x006d }
        if (r5 != 0) goto L_0x0023;
    L_0x0012:
        r5 = "Unable to create cache dir %s";
        r6 = 1;
        r6 = new java.lang.Object[r6];	 Catch:{ all -> 0x006d }
        r7 = 0;
        r8 = r10.mRootDirectory;	 Catch:{ all -> 0x006d }
        r8 = r8.getAbsolutePath();	 Catch:{ all -> 0x006d }
        r6[r7] = r8;	 Catch:{ all -> 0x006d }
        com.android.volley.VolleyLog.m1e(r5, r6);	 Catch:{ all -> 0x006d }
    L_0x0023:
        monitor-exit(r10);
        return;
    L_0x0025:
        r6 = r10.mRootDirectory;	 Catch:{ all -> 0x006d }
        r2 = r6.listFiles();	 Catch:{ all -> 0x006d }
        if (r2 == 0) goto L_0x0023;
    L_0x002d:
        r6 = r2.length;	 Catch:{ all -> 0x006d }
    L_0x002e:
        if (r5 >= r6) goto L_0x0023;
    L_0x0030:
        r1 = r2[r5];	 Catch:{ all -> 0x006d }
        r3 = 0;
        r4 = new java.io.BufferedInputStream;	 Catch:{ IOException -> 0x0058 }
        r7 = new java.io.FileInputStream;	 Catch:{ IOException -> 0x0058 }
        r7.<init>(r1);	 Catch:{ IOException -> 0x0058 }
        r4.<init>(r7);	 Catch:{ IOException -> 0x0058 }
        r0 = com.android.volley.toolbox.DiskBasedCache.CacheHeader.readHeader(r4);	 Catch:{ IOException -> 0x0075, all -> 0x0072 }
        r8 = r1.length();	 Catch:{ IOException -> 0x0075, all -> 0x0072 }
        r0.size = r8;	 Catch:{ IOException -> 0x0075, all -> 0x0072 }
        r7 = r0.key;	 Catch:{ IOException -> 0x0075, all -> 0x0072 }
        r10.putEntry(r7, r0);	 Catch:{ IOException -> 0x0075, all -> 0x0072 }
        if (r4 == 0) goto L_0x0051;
    L_0x004e:
        r4.close();	 Catch:{ IOException -> 0x0055 }
    L_0x0051:
        r3 = r4;
    L_0x0052:
        r5 = r5 + 1;
        goto L_0x002e;
    L_0x0055:
        r7 = move-exception;
        r3 = r4;
        goto L_0x0052;
    L_0x0058:
        r7 = move-exception;
    L_0x0059:
        if (r1 == 0) goto L_0x005e;
    L_0x005b:
        r1.delete();	 Catch:{ all -> 0x0066 }
    L_0x005e:
        if (r3 == 0) goto L_0x0052;
    L_0x0060:
        r3.close();	 Catch:{ IOException -> 0x0064 }
        goto L_0x0052;
    L_0x0064:
        r7 = move-exception;
        goto L_0x0052;
    L_0x0066:
        r5 = move-exception;
    L_0x0067:
        if (r3 == 0) goto L_0x006c;
    L_0x0069:
        r3.close();	 Catch:{ IOException -> 0x0070 }
    L_0x006c:
        throw r5;	 Catch:{ all -> 0x006d }
    L_0x006d:
        r5 = move-exception;
        monitor-exit(r10);
        throw r5;
    L_0x0070:
        r6 = move-exception;
        goto L_0x006c;
    L_0x0072:
        r5 = move-exception;
        r3 = r4;
        goto L_0x0067;
    L_0x0075:
        r7 = move-exception;
        r3 = r4;
        goto L_0x0059;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.volley.toolbox.DiskBasedCache.initialize():void");
    }

    public synchronized void put(String key, Entry entry) {
        pruneIfNeeded(entry.data.length);
        File file = getFileForKey(key);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            CacheHeader e = new CacheHeader(key, entry);
            if (e.writeHeader(fos)) {
                fos.write(entry.data);
                fos.close();
                putEntry(key, e);
            } else {
                fos.close();
                VolleyLog.m0d("Failed to write header for %s", file.getAbsolutePath());
                throw new IOException();
            }
        } catch (IOException e2) {
            if (!file.delete()) {
                VolleyLog.m0d("Could not clean up file %s", file.getAbsolutePath());
            }
        }
    }

    public synchronized void remove(String key) {
        boolean deleted = getFileForKey(key).delete();
        removeEntry(key);
        if (!deleted) {
            VolleyLog.m0d("Could not delete cache entry for key=%s, filename=%s", key, getFilenameForKey(key));
        }
    }

    private String getFilenameForKey(String key) {
        int firstHalfLength = key.length() / 2;
        return String.valueOf(key.substring(0, firstHalfLength).hashCode()) + String.valueOf(key.substring(firstHalfLength).hashCode());
    }

    public File getFileForKey(String key) {
        return new File(this.mRootDirectory, getFilenameForKey(key));
    }

    private void pruneIfNeeded(int neededSpace) {
        if (this.mTotalSize + ((long) neededSpace) >= ((long) this.mMaxCacheSizeInBytes)) {
            if (VolleyLog.DEBUG) {
                VolleyLog.m3v("Pruning old cache entries.", new Object[0]);
            }
            long before = this.mTotalSize;
            int prunedFiles = 0;
            long startTime = SystemClock.elapsedRealtime();
            Iterator<Map.Entry<String, CacheHeader>> iterator = this.mEntries.entrySet().iterator();
            while (iterator.hasNext()) {
                CacheHeader e = (CacheHeader) ((Map.Entry) iterator.next()).getValue();
                if (getFileForKey(e.key).delete()) {
                    this.mTotalSize -= e.size;
                } else {
                    VolleyLog.m0d("Could not delete cache entry for key=%s, filename=%s", e.key, getFilenameForKey(e.key));
                }
                iterator.remove();
                prunedFiles++;
                if (((float) (this.mTotalSize + ((long) neededSpace))) < ((float) this.mMaxCacheSizeInBytes) * 0.9f) {
                    break;
                }
            }
            if (VolleyLog.DEBUG) {
                VolleyLog.m3v("pruned %d files, %d bytes, %d ms", Integer.valueOf(prunedFiles), Long.valueOf(this.mTotalSize - before), Long.valueOf(SystemClock.elapsedRealtime() - startTime));
            }
        }
    }

    private void putEntry(String key, CacheHeader entry) {
        if (this.mEntries.containsKey(key)) {
            this.mTotalSize += entry.size - ((CacheHeader) this.mEntries.get(key)).size;
        } else {
            this.mTotalSize += entry.size;
        }
        this.mEntries.put(key, entry);
    }

    private void removeEntry(String key) {
        CacheHeader entry = (CacheHeader) this.mEntries.get(key);
        if (entry != null) {
            this.mTotalSize -= entry.size;
            this.mEntries.remove(key);
        }
    }

    private static byte[] streamToBytes(InputStream in, int length) throws IOException {
        byte[] bytes = new byte[length];
        int pos = 0;
        while (pos < length) {
            int count = in.read(bytes, pos, length - pos);
            if (count == -1) {
                break;
            }
            pos += count;
        }
        if (pos == length) {
            return bytes;
        }
        throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
    }

    private static int read(InputStream is) throws IOException {
        int b = is.read();
        if (b != -1) {
            return b;
        }
        throw new EOFException();
    }

    static void writeInt(OutputStream os, int n) throws IOException {
        os.write((n >> 0) & 255);
        os.write((n >> 8) & 255);
        os.write((n >> 16) & 255);
        os.write((n >> 24) & 255);
    }

    static int readInt(InputStream is) throws IOException {
        return (((0 | (read(is) << 0)) | (read(is) << 8)) | (read(is) << 16)) | (read(is) << 24);
    }

    static void writeLong(OutputStream os, long n) throws IOException {
        os.write((byte) ((int) (n >>> null)));
        os.write((byte) ((int) (n >>> 8)));
        os.write((byte) ((int) (n >>> 16)));
        os.write((byte) ((int) (n >>> 24)));
        os.write((byte) ((int) (n >>> 32)));
        os.write((byte) ((int) (n >>> 40)));
        os.write((byte) ((int) (n >>> 48)));
        os.write((byte) ((int) (n >>> 56)));
    }

    static long readLong(InputStream is) throws IOException {
        return (((((((0 | ((((long) read(is)) & 255) << null)) | ((((long) read(is)) & 255) << 8)) | ((((long) read(is)) & 255) << 16)) | ((((long) read(is)) & 255) << 24)) | ((((long) read(is)) & 255) << 32)) | ((((long) read(is)) & 255) << 40)) | ((((long) read(is)) & 255) << 48)) | ((((long) read(is)) & 255) << 56);
    }

    static void writeString(OutputStream os, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        writeLong(os, (long) b.length);
        os.write(b, 0, b.length);
    }

    static String readString(InputStream is) throws IOException {
        return new String(streamToBytes(is, (int) readLong(is)), "UTF-8");
    }

    static void writeStringStringMap(Map<String, String> map, OutputStream os) throws IOException {
        if (map != null) {
            writeInt(os, map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writeString(os, (String) entry.getKey());
                writeString(os, (String) entry.getValue());
            }
            return;
        }
        writeInt(os, 0);
    }

    static Map<String, String> readStringStringMap(InputStream is) throws IOException {
        int size = readInt(is);
        Map<String, String> result = size == 0 ? Collections.emptyMap() : new HashMap(size);
        for (int i = 0; i < size; i++) {
            result.put(readString(is).intern(), readString(is).intern());
        }
        return result;
    }
}
