package net.simforge.fsdatafeeder;

import flightsim.simconnect.SimConnect;

public interface SimStateConsumer {
    void consume(SimState currentSimState, SimConnect simConnect);
}
