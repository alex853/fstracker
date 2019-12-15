package net.simforge.fstracker;

import net.simforge.fstracker.fsdata.FSDataProvider;
import net.simforge.fstracker.fsdata.FSDataRecord;

public class FSDataSimReader implements Runnable, FSDataProvider {

    @Override
    public void run() {

    }

    public void requestStop() {
        stopRequested = true;
    }

    @Override
    public FSDataRecord next() {
        return null;
    }
}
