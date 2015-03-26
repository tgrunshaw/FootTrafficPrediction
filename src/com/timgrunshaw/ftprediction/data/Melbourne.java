package com.timgrunshaw.ftprediction.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Melbourne represents the foot traffic sensor data available for the city of 
 * Melbourne.
 * 
 * @author Tim Grunshaw
 */
public class Melbourne {

    /*
     Sensors string values are stored here as it allows us to quickly identify
     if any new sensors are put in. 
     */
    private final String[] sensorNames = {
        "State Library",
        "Collins Place (South)",
        "Collins Place (North)",
        "Flagstaff Station",
        "Melbourne Central",
        "Town Hall (West)",
        "Bourke Street Mall (North)",
        "Bourke Street Mall (South)",
        "Australia on Collins",
        "Southern Cross Station",
        "Victoria Point",
        "New Quay",
        "Waterfront City",
        "Webb Bridge",
        "Princes Bridge",
        "Flinders St Station Underpass",
        "Sandridge Bridge",
        "Birrarung Marr",
        "QV Market-Elizabeth (West)",
        "Flinders St-Elizabeth St (East)",
        "Spencer St-Collins St (North)",
        "Spencer St-Collins St (South)",
        "Bourke St-Russell St (West)",
        "Convention/Exhibition Centre",
        "Chinatown-Swanston St (North)",
        "Chinatown-Lt Bourke St (South)",
        "QV Market-Peel St",
        "Vic Arts Centre",
        "Lonsdale St (South)",
        "Lygon St (West)",
        "Flinders St-Spring St (West)",
        "Flinders St-Spark Lane",
        "Alfred Place",
        "Queen Street (West)",
        "Lygon Street (East)",
        "Flinders St-Swanston St (West)",
        "Spring St-Lonsdale St (South)"
    };

    private final HashMap<String, Sensor> sensors = new HashMap<>();

    public Melbourne() {
        for (String s : sensorNames) {
            sensors.put(s, new Sensor());
        }
    }

    public Sensor getSensor(String sensor) {
        return sensors.get(sensor);
    }

    public int getCountOfAllSensors(LocalDateTime hour) {
        int count = 0;
        for (Map.Entry<String, Sensor> entry : sensors.entrySet()) {
            count += entry.getValue().getCount(hour);
        }

        return count;
    }

    
    /**
     * Write this data to CSV format. The first row contains the sensors names,
     * the first column contains the date and time of the reading. 
     * @param dest
     * @throws IOException 
     */
    public void writeData(Path dest) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dest)) {

            // Write first line
            writer.append("Sensor");
            for (String s : sensorNames) {
                writer.append(",");
                writer.append(s);
            }

            // Keep a reference to iterators of each sensor for efficient reading.
            ArrayList<Iterator<Map.Entry<LocalDateTime, Integer>>> iterators = new ArrayList<>();
            for (String s : sensorNames) {
                // Loop in order of sensorNames, not sensors (as may not be in order)
                iterators.add(sensors.get(s).getAllRecords().entrySet().iterator());
            }

            // First dateTime for first sensor. 
            LocalDateTime currentReadingTime = sensors.get(sensorNames[0]).getAllRecords().firstKey();

            // Last dateTime for first sensor.
            LocalDateTime finalReadingTime = sensors.get(sensorNames[0]).getAllRecords().lastKey();

            // Write values for every hour, including the final reading.
            while (currentReadingTime.isBefore(finalReadingTime.plusMinutes(1))) {
                
                // Each loop on a new line (new hour)
                writer.newLine();
                
                // Write dateTime
                writer.append(currentReadingTime.toString());

                // For each iterator (sensor), write the next value on this line.
                for (Iterator<Map.Entry<LocalDateTime, Integer>> it : iterators) {
                    
                    // The outer while loop should never continue past the end of any of the sensors iterators. 
                    assert it.hasNext() : "One of the iterators reached end before "
                            + "reaching the final reading of the first sensor. Time: "
                            + currentReadingTime;
                    
                    Map.Entry<LocalDateTime, Integer> reading = it.next();
                    
                    // Ensure that we are sychronised.
                    assert reading.getKey().equals(currentReadingTime) : "Sensor times "
                            + "are not sychronised, ensure folders is not missing any csv files."
                            + " Expected: " + currentReadingTime
                            + "   Time was: " + reading.getKey(); 
                    
                    // Write value
                    writer.append(",");
                    writer.append(reading.getValue().toString());
                }
                // Go to next hour
                currentReadingTime = currentReadingTime.plusHours(1);
            }
            
            // We should always have used all the values in each iterator
            for(Iterator<Map.Entry<LocalDateTime, Integer>> it : iterators){
                assert !it.hasNext() : "Reached final dateTime for the first sensor, "
                        + "but an iterator still had entries: " + it.next().getKey();
            }
            
            writer.close();
        }
    }
}
