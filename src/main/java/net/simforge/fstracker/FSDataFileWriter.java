package net.simforge.fstracker;

import net.simforge.commons.misc.Misc;
import net.simforge.fstracker.fsdata.FSDataProvider;
import net.simforge.fstracker.fsdata.FSDataRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

public class FSDataFileWriter implements Runnable {
    private FSDataProvider provider;
    private volatile boolean stopRequested = false;
    private LocalDate dateOfFile;
    private FileOutputStream fos;

    public FSDataFileWriter(FSDataProvider provider) {
        this.provider = provider;
    }

    @Override
    public void run() {
        while (!stopRequested) {
            FSDataRecord next = provider.next();
            if (next == null) {
                Misc.sleep(100);
                continue;
            }

            if (dateOfFile == null) {
                openFile();
            } else if (!dateOfFile.equals(currDate())) {
                closeFile();
                openFile();
            }

            String line = next.toString() + "\r\n";
            try {
                fos.write(line.getBytes());

                fos.flush();
            } catch (IOException e) {
                e.printStackTrace(); // todo
            }
        }

        closeFile();
    }

    public void requestStop() {
        stopRequested = true;
    }

    private void openFile() {
        dateOfFile = currDate();
        new File("fsdata").mkdirs();
        try {
            fos = new FileOutputStream("fsdata/" + dateOfFile.toString() + ".txt", true);
        } catch (FileNotFoundException e) {
            e.printStackTrace(); // todo
        }
    }

    private void closeFile() {
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace(); // todo
        }
    }

    private LocalDate currDate() {
        return LocalDate.now(ZoneId.of("UTC"));
    }
}
