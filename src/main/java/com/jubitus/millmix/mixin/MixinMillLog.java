package com.jubitus.millmix.mixin;

import com.jubitus.millmix.MillMixModConfig;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

@Mixin(MillLog.class)
public class MixinMillLog {
    /**
     * @author Jubitus
     * @reason kill 100MB millenaire.log
     */
    @Overwrite
    public static void initLogFileWriter() {
        try {
            // Base directory: millenaire-custom inside mods folder
            File baseDir = MillCommonUtilities.getMillenaireCustomContentDir();

            // Create logs/ subfolder for log sessions
            File logDir = new File(baseDir, "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // Clean up old log files (keep X most recent)
            File[] logFiles = logDir.listFiles((dir, name) -> name.endsWith(".log"));
            int maxLogs = MillMixModConfig.logsToKeep;

            if (logFiles != null && logFiles.length > maxLogs) {
                // Sort descending by last modified time (newest first)
                Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified).reversed());

                // Delete files beyond the most recent `maxLogs`
                for (int i = maxLogs; i < logFiles.length; i++) {
                    logFiles[i].delete();
                }
            }

            // Create a new log file with timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File logFile = new File(logDir, "millenaire_" + timestamp + ".log");

            // Reflectively set the private static writer field
            Field writerField = MillLog.class.getDeclaredField("writer");
            writerField.setAccessible(true);
            writerField.set(null, new FileWriter(logFile, false));  // false = overwrite

        } catch (Exception e) {
            System.out.println("Failed to initialize Millénaire log file writer: " + e);
            e.printStackTrace();
        }
    }
}

