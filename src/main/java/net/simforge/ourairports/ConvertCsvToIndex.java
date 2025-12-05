package net.simforge.ourairports;

import net.simforge.commons.io.Csv;
import net.simforge.commons.io.IOHelper;

import java.io.File;
import java.io.IOException;

public class ConvertCsvToIndex {
    public static void main(String[] args) throws IOException {
        String content = IOHelper.loadFile(new File("data/airports.csv"));
        Csv csv = Csv.fromContent(content);

        OurAirportsIndex index = new OurAirportsIndex();

        for (int row = 0; row < csv.rowCount(); row++) {
            String icao = csv.value(row, 1);
            String latStr = csv.value(row, 4);
            String lonStr = csv.value(row, 5);

            if (icao.contains("-")) {
                System.out.println(icao + " SKIPPED!");
                continue;
            }

            System.out.println(icao);
            index.add(icao, Double.valueOf(latStr).floatValue(), Double.valueOf(lonStr).floatValue());
        }

        index.save("src/main/resources/airports.dat");
    }
}
