package net.simforge.fstracker3;

import lombok.Data;

@Data
public class SimState {
    private String title; //
    private String atcType;
    private String atcModel;
    private double latitude; //
    private double longitude; //
    private double altitude; //
    private int onGround; //
    private double groundVelocity; //
    private int isUserSim;
    private int planeInParkingState;
    private int brakeParkingPosition; //

    public boolean isOnGround() {
        return onGround == 1;
    }

    public boolean isInAir() {
        return onGround == 0;
    }

    public boolean isStationary() {
        return isOnGround() && (groundVelocity <= 0.01);
    }
}
