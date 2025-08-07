package com.jubitus.millmix.utility;

import com.jubitus.millmix.MillMixModConfig;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MillMixLogCleaner {
    // Updated to match /mods/millenaire-custom/ directory
    private static final File LOG_DIR = new File("mods/millenaire-custom");

    private static final List<String> LOG_NAMES = Arrays.asList(
            "millenaire.log",
            "millenaire-server.log"
    );

    // Max allowed log size in bytes (10 MB)
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    // How many bytes to keep when trimming (e.g. last 30 MB)
    private static final long RETAIN_SIZE_BYTES = 3L * 1024 * 1024;
    //Every 30 min
    private static final int CHECK_INTERVAL_SECONDS = 60 * 30; //60sec * 30
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void start() {
        executor.submit(() -> {
            while (true) {
                try {
                    for (String name : LOG_NAMES) {
                        File logFile = new File(LOG_DIR, name);
                        trimLogFileBySize(logFile);
                    }
                    Thread.sleep(CHECK_INTERVAL_SECONDS * 1000L);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    private static void trimLogFileBySize(File file) throws IOException {
        if (!file.exists()) return;

        long length = file.length();
        if (length <= MAX_SIZE_BYTES) return;

        System.out.println("[MillenaireLogCleaner] Trimming " + file.getName() + " (Size: " + length + " bytes)");

        // Keep only the last RETAIN_SIZE_BYTES
        byte[] buffer = new byte[(int) RETAIN_SIZE_BYTES];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(Math.max(0, length - RETAIN_SIZE_BYTES));
            int readBytes = raf.read(buffer);
            if (readBytes < RETAIN_SIZE_BYTES) {
                buffer = Arrays.copyOf(buffer, readBytes);
            }
        }

        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(buffer);
        }

        System.out.println("[MillenaireLogCleaner] Trimmed " + file.getName() + " to last " + buffer.length + " bytes");
    }
}