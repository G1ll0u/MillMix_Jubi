package vict.millmix.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * BufferedWriter that calls fsync() on flush().
 *
 * Millénaire's text saves use getWriter() and call flush() without close(),
 * so syncing only on close() would not help. Syncing on flush() ensures
 * data is on disk even if the JVM is killed immediately after flush() returns.
 */
public class SyncingBufferedWriter extends BufferedWriter {

    private final FileOutputStream fos;

    public SyncingBufferedWriter(File file) throws UnsupportedEncodingException, FileNotFoundException {
        this(new FileOutputStream(file));
    }

    private SyncingBufferedWriter(FileOutputStream fos) throws UnsupportedEncodingException {
        super(new OutputStreamWriter(fos, "UTF8"));
        this.fos = fos;
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        try {
            fos.getFD().sync();
        } catch (IOException ignored) {
        }
    }
}
