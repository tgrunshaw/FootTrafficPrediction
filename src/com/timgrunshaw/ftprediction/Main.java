package com.timgrunshaw.ftprediction;

import com.timgrunshaw.ftprediction.dataretrieval.Melbourne;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Entry class for foot traffic prediction program. 
 * 
 * @author Tim Grunshaw
 */
public class Main {
    
    public static void main(String[] args) throws IOException{
        Melbourne melbourne = new Melbourne();
        int numUpdates = melbourne.update();
        System.out.println("Added " + numUpdates + " new days.");
        
        int numConvert = melbourne.convertAllCSVFilesInFolder(Paths.get("convertedOutput"));
        System.out.println("Converted " + numConvert);
    }
}
