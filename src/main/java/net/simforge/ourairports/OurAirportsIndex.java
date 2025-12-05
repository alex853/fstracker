package net.simforge.ourairports;

import net.simforge.commons.misc.Geo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class OurAirportsIndex {
    private Airport[] data = new Airport[0];
    private Airport[][] index = new Airport[0][];

    public static OurAirportsIndex loadFromResources() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(OurAirportsIndex.class.getResourceAsStream("/airports.dat"))) {
            OurAirportsIndex index = new OurAirportsIndex();
            index.data = (Airport[]) in.readObject();
            index.rebuildIndex();
            return index;
        }
    }

    public void add(String icao, float lat, float lon) {
        Airport airport = new Airport(lat, lon, icao);

        Airport[] newData = new Airport[data.length + 1];
        System.arraycopy(data, 0, newData, 0, data.length);
        newData[newData.length-1] = airport;

        data = newData;
        rebuildIndex();
    }

    public void save(String filename) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(data);
        }
    }

    private void rebuildIndex() {
        List<Airport>[] raw = (List<Airport>[]) new List[360];

        for (Airport airport : data) {
            int index = (int) (airport.lon + 180);
            if (index == 360) {
                throw new RuntimeException("oooppsss");
            }

            List<Airport> each = raw[index];
            if (each == null) {
                each = new ArrayList<>();
                raw[index] = each;
            }
            each.add(airport);
        }

        Airport[][] newIndex = new Airport[360][];

        for (int i = 0; i < 360; i++) {
            List<Airport> airports = raw[i];
            if (airports == null) {
                continue;
            }
            newIndex[i] = airports.toArray(new Airport[0]);
        }

        index = newIndex;
    }

    public String findNearestIcao(double lat, double lon) {
        String nearestIcao = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Airport airport : data) {
            double distance = Geo.distance(Geo.coords(lat, lon), Geo.coords(airport.lat, airport.lon));
            if (nearestIcao == null || distance < nearestDistance) {
                nearestIcao = airport.icao;
                nearestDistance = distance;
            }
        }
        return nearestIcao;
    }

    public AirportAndDistance findNearestIcaoIndexed(double lat, double lon) {
        int startingIndex = (int) (lon + 180);

        AirportAndDistance nearest = findNearestInIndex(lat, lon, startingIndex);

        int indexDistance = 1;
        while (indexDistance < 180) {
            AirportAndDistance nearestLeft = findNearestInIndex(lat, lon, startingIndex - indexDistance);
            AirportAndDistance nearestRight = findNearestInIndex(lat, lon, startingIndex + indexDistance);

            nearest = AirportAndDistance.chooseNearest(nearest, nearestRight);
            nearest = AirportAndDistance.chooseNearest(nearest, nearestLeft);

            if (nearest != null) {
                return nearest;
            }

            indexDistance++;
        }

        return null;
    }

    private AirportAndDistance findNearestInIndex(double lat, double lon, int index) {
        if (index < 0 || index >= 360) {
            return null;
        }

        Airport[] data = this.index[index];
        if (data == null) {
            return null;
        }

        AirportAndDistance nearest = null;
        for (Airport airport : data) {
            double distance = Geo.distance(Geo.coords(lat, lon), Geo.coords(airport.lat, airport.lon));
            nearest = AirportAndDistance.chooseNearest(nearest, new AirportAndDistance(airport, distance));
        }

        return nearest;
    }

    public static class AirportAndDistance {
        public final Airport airport;
        public final double distance;

        public AirportAndDistance(Airport airport, double distance) {

            this.airport = airport;
            this.distance = distance;
        }

        public static AirportAndDistance chooseNearest(AirportAndDistance ad1, AirportAndDistance ad2) {
            if (ad1 == null && ad2 == null) {
                return null;
            } else if (ad1 == null) {
                return ad2;
            } else if (ad2 == null) {
                return ad1;
            } else {
                return ad1.distance < ad2.distance ? ad1 : ad2;
            }
        }
    }

    public static class Airport implements Serializable {
        public final float lat;
        public final float lon;
        public final String icao;

        public Airport(float lat, float lon, String icao) {
            this.lat = lat;
            this.lon = lon;
            this.icao = icao;
        }
    }
}
