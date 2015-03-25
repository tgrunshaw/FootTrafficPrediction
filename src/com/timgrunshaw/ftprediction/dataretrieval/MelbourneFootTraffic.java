package com.timgrunshaw.ftprediction.dataretrieval;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 *
 * @author Tim Grunshaw
 */
public class MelbourneFootTraffic {

    public enum SensorName {
        State_Library ("State Library"),
        Collins_Place_South ("Collins Place (South)");
        
        private final String actualString;
        
        SensorName(String str){
            this.actualString = str;
        }
    }

    public int getCount(String sensor, LocalDateTime datetime) {
        return 0;
    }

    public int getCount(LocalDateTime dateTime) {
        return 0;
    }

}
