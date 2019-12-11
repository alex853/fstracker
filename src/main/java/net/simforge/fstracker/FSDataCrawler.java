package net.simforge.fstracker;

import java.util.concurrent.ConcurrentLinkedQueue;

public class FSDataCrawler {
    public static void main(String[] args) {
        final Queue<ReadResult> readingQueue = new ConcurrentLinkedQueue<>();

        Thread fsuipcThread = new Thread() {
            // if stop requested - disconnect if connected and exit
            // if not connected - try to connect
            // if connected - read record
            // plus read some checking fields before and after (year?)
            // plus calculate elapsed time
            // provide data to outer queue

            @Override
            public void run() {
            }
        };



    }
}
