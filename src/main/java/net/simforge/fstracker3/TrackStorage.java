package net.simforge.fstracker3;

import net.simforge.commons.io.IOHelper;
import net.simforge.commons.misc.JavaTime;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class TrackStorage {
    private static final String storageRootPath = "Segments";
    private static final File storageRootFile = new File(storageRootPath);

    public static List<FileInfo> listFiles() {
        final File[] dateFolders = storageRootFile.listFiles(file -> file.isDirectory() && isDate(file.getName()));
        if (dateFolders == null) {
            return new ArrayList<>();
        }

        final List<FileInfo> result = new ArrayList<>();

        Arrays.sort(dateFolders, Comparator.comparing(File::getName));
        Arrays.stream(dateFolders).forEach(dateFolder -> {
            final File trackFolder = new File(dateFolder, "track");
            final File[] trackFiles = trackFolder.listFiles(file -> file.isFile() && file.getName().startsWith("track_") && file.getName().endsWith(".json"));
            if (trackFiles == null) {
                return;
            }

            Arrays.sort(trackFiles, Comparator.comparing(File::getName));
            Arrays.stream(trackFiles).forEach(trackFile -> {
                final String name = trackFile.getName();
                final String maskedDateStr = name.substring(6, 25);
                final String dateStr = maskedDateStr.substring(0, 10) + " " + maskedDateStr.substring(11).replace('-', ':');
                final LocalDateTime date = LocalDateTime.parse(dateStr, JavaTime.yMdHms);
                result.add(new FileInfo(date, trackFile));
            });
        });

        return result;
    }

    public static boolean isFlightRecordSkipped(String flightRecordId) {
        File skippedMarkerFile = new File(storageRootFile, "_skipped/" + flightRecordId.replace(':', '-') + ".txt");
        return skippedMarkerFile.exists();
    }

    public static void markFlightRecordSkipped(String flightRecordId) {
        File skippedMarkerFile = new File(storageRootFile, "_skipped/" + flightRecordId.replace(':', '-') + ".txt");
        try {
            IOHelper.saveFile(skippedMarkerFile, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isDate(final String name) {
        try {
            JavaTime.yMd.parse(name);
            return true;
        } catch (final RuntimeException ignored) {
            return false;
        }
    }

    public static class FileInfo {
        private final LocalDateTime date;
        private final File file;

        public FileInfo(final LocalDateTime date, final File file) {
            this.date = date;
            this.file = file;
        }

        public LocalDateTime getDate() {
            return date;
        }

        public File getFile() {
            return file;
        }
    }
}
