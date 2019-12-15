package net.simforge.fstracker;

public class TrackToFile {
    public static void main(String[] args) throws InterruptedException {
        FSDataSimReader provider = new FSDataSimReader();
        FSDataFileWriter writer = new FSDataFileWriter(provider);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    provider.requestStop();
                    writer.requestStop();
                })
        );

        Thread providerThread = new Thread(provider);
        providerThread.start();

        Thread writerThread = new Thread(writer);
        writerThread.start();

        providerThread.join();
        writerThread.join();
    }
}
