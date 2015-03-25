package com.timgrunshaw.ftprediction.dataretrieval;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

/**
 *
 * @author Tim Grunshaw
 */
public class Sensor{
        private HashMap<LocalDateTime, Integer> sensorCount = new HashMap<>();
        
        public int getCount(LocalDateTime hour){
            if(!hour.isEqual(hour.truncatedTo(ChronoUnit.HOURS))){
                throw new IllegalArgumentException("LocalDateTime must be exactly to the hour (00 mins, 00 seconds): " + hour);
            }
            return sensorCount.get(hour);
        }
        
        public void setCount(LocalDateTime hour, int count){
            if(!hour.isEqual(hour.truncatedTo(ChronoUnit.HOURS))){
                throw new IllegalArgumentException("LocalDateTime must be exactly to the hour (00 mins, 00 seconds): " + hour);
            }
            sensorCount.put(hour, count);
        }
    }
