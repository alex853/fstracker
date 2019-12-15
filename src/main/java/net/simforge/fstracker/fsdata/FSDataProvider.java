package net.simforge.fstracker.fsdata;

public interface FSDataProvider {
    /**
     * @return next record or null if no more records have been read till the moment
     */
    FSDataRecord next();
}
