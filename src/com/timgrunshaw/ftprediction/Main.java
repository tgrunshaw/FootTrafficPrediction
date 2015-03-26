package com.timgrunshaw.ftprediction;

import com.timgrunshaw.ftprediction.dataretrieval.MelbourneDataSource;
import java.io.IOException;

/**
 * Entry class for foot traffic prediction program. 
 * 
 * @author Tim Grunshaw
 */
public class Main {
    
    public static void main(String[] args) throws IOException{
        MelbourneDataSource melbourne = new MelbourneDataSource();
        melbourne.createMelbourne();
        
        /*
        int numUpdates = melbourne.update();
        System.out.println("Added " + numUpdates + " new days.");
        
        int numConvert = melbourne.convertAllCSVFilesInFolder(Paths.get("convertedOutput"));
        System.out.println("Converted " + numConvert);
                */
        
    }
}
