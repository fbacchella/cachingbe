package org.rrd4j.caching;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedByInterruptException;

import org.rrd4j.core.RrdFileBackend;

/** 
 * A backend which is used to store RRD data to ordinary disk files 
 * by using fast java.nio.* package enhanced with custom caching functionalities.
 * <p>
 * When available it use directio for file access, all caching is done with a embedded page cache.
 * <p>
 * The file state on disk is kept consistent, as all or no modifications are committed.
 * @author Fabrice Bacchella
 */
public class RrdCachedFileBackend extends RrdFileBackend {

    private final File file;
    private final PageCache pagecache;

    /**
     * Creates RrdFileBackend object for the given file path.
     *
     * @param path     Path to a file
     * @param readOnly True, if file should be open in a read-only mode. False otherwise
     * @throws IOException Thrown in case of I/O error
     */
    protected RrdCachedFileBackend(String path, boolean readOnly, PageCache pagecache) throws IOException {
        super(path, readOnly);
        this.file = new File(path);
        this.pagecache = pagecache;
    }

    /**
     * Writes bytes to the underlying RRD file on the disk
     * @param offset Starting file offset
     * @param b Bytes to be written.
     * @throws IOException Thrown in case of I/O error
     */
    public void write(long offset, byte[] b) throws IOException {
        if(readOnly) {
            throw new IOException("read only file");
        }
        
        if( Thread.currentThread().isInterrupted()) {
            close();
            throw new ClosedByInterruptException();
        }
        else {
            try {
                pagecache.write(file, offset, b);
            } catch (InterruptedException e) {
                throw new ClosedByInterruptException();
            }
        }
    }

    /**
     * Reads a number of bytes from the RRD file on the disk
     * @param offset Starting file offset
     * @param b Buffer which receives bytes read from the file.
     * @throws IOException Thrown in case of I/O error.
     */
    public void read(long offset, byte[] b) throws IOException {
        if( Thread.currentThread().isInterrupted()) {
            close();
            throw new ClosedByInterruptException();
        }
        else {
            try {
                pagecache.read(file, offset, b);
            } catch (InterruptedException e) {
                throw new ClosedByInterruptException();
            }
        }
    }

    /** 
     * Closes the underlying RRD file. 
     * @throws IOException Thrown in case of I/O error 
     */
    public void close() throws IOException {
    }

    /**
     * Returns RRD file length.
     *
     * @return File length.
     * @throws IOException Thrown in case of I/O error.
     */
    public long getLength() throws IOException {
        return file.length();
    }

    /**
     * Sets length of the underlying RRD file. This method is called only once, immediately
     * after a new RRD file gets created.
     *
     * @param length Length of the RRD file
     * @throws IOException Thrown in case of I/O error.
     */
    protected void setLength(long length) throws IOException {
        RandomAccessFile fd = new RandomAccessFile(file, "rw");
        fd.setLength(length);
    }

}