package com.timgrunshaw.ftprediction.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;

/**
 * A sensor keeps the counts of readings, in order, and mapped by the hour of
 * the reading. 
 * @author Tim Grunshaw
 */
public class Sensor {

    // log(n) performance for get,set. Allows us to quickly (n) iterate over time in order.
    private final TreeMap<LocalDateTime, Integer> recording = new TreeMap<>();

    
    public int getCount(LocalDateTime hour) {
        if (!hour.isEqual(hour.truncatedTo(ChronoUnit.HOURS))) {
            throw new IllegalArgumentException("LocalDateTime must be exactly to the hour (00 mins, 00 seconds): " + hour);
        }
        return recording.get(hour);
    }

    public void setCount(LocalDateTime hour, int count) {
        if (!hour.isEqual(hour.truncatedTo(ChronoUnit.HOURS))) {
            throw new IllegalArgumentException("LocalDateTime must be exactly to the hour (00 mins, 00 seconds): " + hour);
        }
        recording.put(hour, count);
    }
    
    /**
     * Retrieve the underlying TreeMap storing the data for iteration etc. 
     * @return 
     */
    public TreeMap<LocalDateTime, Integer> getAllRecords(){
        return recording;
    }
}
