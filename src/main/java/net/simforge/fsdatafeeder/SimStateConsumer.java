package net.simforge.fsdatafeeder;

public interface SimStateConsumer {
    void consume(SimState currentSimState);
}
