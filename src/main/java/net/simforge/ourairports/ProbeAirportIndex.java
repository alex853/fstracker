package net.simforge.ourairports;

import java.io.IOException;

public class ProbeAirportIndex {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        OurAirportsIndex index = OurAirportsIndex.loadFromResources();

        System.out.println(index.findNearestIcao(53.0, 51.0));
        System.out.println(index.findNearestIcaoIndexed(53.0, 51.0));

        long started = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            index.findNearestIcao(Math.random()*180 - 90, Math.random()*360 - 180);
        }
        long finished = System.currentTimeMillis();

        System.out.println((finished - started) + "ms per 1k calculations");

        started = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            index.findNearestIcaoIndexed(Math.random()*180 - 90, Math.random()*360 - 180);
        }
        finished = System.currentTimeMillis();

        System.out.println((finished - started) + "ms per 1k calculations 2");


        int skippedNull = 0;
        int skippedTooFar = 0;
        int success = 0;
        int failure = 0;
        for (int i = 0; i < 1000; i++) {
            double lat = Math.random() * 180 - 90;
            double lon = Math.random() * 360 - 180;
            String nearestIcao = index.findNearestIcao(lat, lon);
            OurAirportsIndex.AirportAndDistance indexed = index.findNearestIcaoIndexed(lat, lon);
            if (indexed == null) {
                skippedNull++;
                continue;
            }
            if (indexed.distance > 100) {
                skippedTooFar++;
                continue;
            }
            if (nearestIcao.equals(indexed.airport.icao)) {
                success++;
            } else {
                failure++;
            }
        }

        System.out.println("success = " + success);
        System.out.println("failure = " + failure);
        System.out.println("skippedNull = " + skippedNull);
        System.out.println("skippedTooFar = " + skippedTooFar);
    }
}
