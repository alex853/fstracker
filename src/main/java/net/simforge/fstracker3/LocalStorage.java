package net.simforge.fstracker3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.simforge.commons.io.IOHelper;

import java.io.File;
import java.io.IOException;

public class LocalStorage {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static class Record {
        private final String recordId;
        private final Type type;
        private final Status status;

        private Record(final String recordId, final Status status) {
            this.recordId = recordId;
            this.type = Type.Flight;
            this.status = status;
        }

        public String getRecordId() {
            return recordId;
        }

        public Type getType() {
            return type;
        }

        public Status getStatus() {
            return status;
        }
    }

    public static Record load(final String recordId) {
        final File file = makeFile(recordId);
        if (!file.exists()) {
            return null;
        }
        try {
            return gson.fromJson(IOHelper.loadFile(file), Record.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveAsLogged(final String recordId) {
        save(new Record(recordId, Status.Logged));
    }

    public static void saveAsSkipped(final String recordId) {
        save(new Record(recordId, Status.Skipped));
    }

    private static void save(final Record record) {
        final File file = makeFile(record.getRecordId());
        file.getParentFile().mkdirs();
        try {
            IOHelper.saveFile(file, gson.toJson(record));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Status {
        Logged,
        Skipped
    }

    public enum Type {
        Flight
    }

    private static File makeFile(final String flightRecordId) {
        return new File(System.getenv("LOCALAPPDATA") + "/simforge.net/FSLog/record-cache/" + flightRecordId.replace('\'', '-').replace(':', '-') + ".json");
    }

}
