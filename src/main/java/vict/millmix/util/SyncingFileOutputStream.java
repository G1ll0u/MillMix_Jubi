package vict.millmix.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * FileOutputStream that calls fsync() before close().
 * Used for binary files written directly without temp+rename.
 */
public class SyncingFileOutputStream extends FileOutputStream {

    public SyncingFileOutputStream(File file) throws FileNotFoundException {
        super(file);
    }

    @Override
    public void close() throws IOException {
        try {
            getFD().sync();
        } catch (IOException ignored) {
        }
        super.close();
    }
}
