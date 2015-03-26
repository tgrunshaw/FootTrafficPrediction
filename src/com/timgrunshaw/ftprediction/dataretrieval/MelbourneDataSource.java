package com.timgrunshaw.ftprediction.dataretrieval;

import com.timgrunshaw.ftprediction.data.Melbourne;
import com.timgrunshaw.ftprediction.data.Sensor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents the Melbourne data source. The most typical usage will
 * be: MelbourneDateSource melbourneDS = new MelbourneDateSource(); melbourneDS.update();
 *
 * Which will simply fill the specified output folder (default: output/) with
 * all new CSV files from the Melbourne foot traffic data source.
 * 
 * To use the data, run:
 * Melbourne melbourne = melbourneDS.createMelbourne();
 * 
 * And retrieve the data from the melbourne object.
 *
 * @author Tim Grunshaw
 */
public class MelbourneDataSource {

    final static String URL_PREFIX = "http://uioomcomcall.jit.su/api/bydatecsv/";
    private String outputDirectory = "output/";

    public static class MelbourneCSVFile {

        /*
         dd-mm-yyyy.csv
        
         Not the strongest checking, but easy to read. It does check that the format
         doesn't change to mm-dd-yyyy.csv. 
         */
        static final String FILENAME_REGEX = "^([0123][0-9])-(0[1-9]|1[012])-(\\d\\d\\d\\d)\\.csv$";
        static final DateTimeFormatter FILENAME_TO_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        // What row each field starts on. 
        static final int HEADINGS_ROW = 8;
        static final int DATA_START_ROW = 9;
        static final int DATA_FINAL_ROW = 45; // Exclusive
        static final int TOTAL_ROW = 46;

        // For validation of CSV file.
        static final String EXPECTED_FIRST_LINE = "CITY OF MELBOURNE";
        static final String EXPECTED_TOTAL_NAME = "Total";
        static final String EXPECTED_HEADING_NAME = "Sensor";
        static final String EXPECTED_ROW_30_NAME = "Spencer St-Collins St (South)";
    }

    /**
     * The first date available on the website is the 1st July 2009.
     *
     * Actually: 1st Feb 2014. Update: 9th October 2013.
     */
    final static LocalDate EARLIEST_DATE = LocalDate.of(2013, Month.OCTOBER, 9); //LocalDate.of(2009, Month.JULY, 1);

    public void setOutputDirectory(String folderName) {
        outputDirectory = folderName + "/";
    }

    /**
     * Download all CSV files from the Melbourne data source for the date range
     * specified.
     * @param from - inclusive
     * @param to - exclusive
     */
    public void downloadCSVFilesInRange(LocalDate from, LocalDate to) throws IOException {
        if (from.isBefore(EARLIEST_DATE)) {
            throw new IllegalArgumentException("From date must be equal to or "
                    + "after the EARLIEST_DATE");
        }
        if (from.isAfter(LocalDate.now()) || to.isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("fromDate must be before LocalDate.now(), "
                    + "toDate must be no more than 1 day from LocalDate.now()");
        }
        if (from.isAfter(to.minusDays(1))) {
            throw new IllegalArgumentException("fromDate must be less than toDate");
        }

        LocalDate dayToDownload = from;
        while (dayToDownload.isBefore(to)) {
            downloadDataForDay(dayToDownload);
            dayToDownload = dayToDownload.plusDays(1);
        }

    }

    /**
     * Download the CSV file from the Melbourne data source for the specified day.
     * @param day
     * @return
     * @throws IOException 
     */
    Path downloadDataForDay(LocalDate day) throws IOException {
        String destinationPath = outputDirectory
                + day.format(MelbourneCSVFile.FILENAME_TO_DATE) + ".csv";
        return downloadFile(generateCSVUrl(day), destinationPath);
    }

    Path downloadFile(String urlString, String destinationPath) throws MalformedURLException, IOException {
        Path file = Paths.get(destinationPath);
        return downloadFile(urlString, file);
    }

    Path downloadFile(String urlString, Path file) throws MalformedURLException, IOException {
        final int CONNECTION_TIMEOUT = 30000; // 30 seconds
        final int READ_TIMEOUT = 30000; // 30 seconds

        URL url = new URL(urlString);

        org.apache.commons.io.FileUtils.copyURLToFile(url, file.toFile(), CONNECTION_TIMEOUT, READ_TIMEOUT);

        return file;
    }

    String generateCSVUrl(LocalDate date) {
        String dateString = date.format(MelbourneCSVFile.FILENAME_TO_DATE);

        return URL_PREFIX + dateString;
    }

    /**
     * Update the set output directory with any new files available. Note: it
     * will not overwrite any files in the directory.
     * @return 
     * @throws java.io.IOException
     */
    public int update() throws IOException {
        ArrayList<LocalDate> currentFiles = new ArrayList<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(outputDirectory))) {
            Iterator<Path> it = dirStream.iterator();
            while (it.hasNext()) {
                Path file = it.next();
                try {
                    currentFiles.add(parseDateFromFilename(file));
                } catch (DateTimeParseException pEx) {
                    // Don't add, just continue, may have been another file in the folder. 
                }
            }
        }
        int numFilesDownloaded = 0;
        if (currentFiles.isEmpty()) {
            numFilesDownloaded = EARLIEST_DATE.until(LocalDate.now()).getDays();
            downloadCSVFilesInRange(EARLIEST_DATE, LocalDate.now());
        } else {
            LocalDate latest = getLatestDate(currentFiles);
            assert latest.isBefore(LocalDate.now()) : "During update, a file was found that is more recent or equal to today!";
            if (latest.plusDays(1).isBefore(LocalDate.now())) {
                numFilesDownloaded = latest.plusDays(1).until(LocalDate.now()).getDays();
                downloadCSVFilesInRange(latest.plusDays(1), LocalDate.now());
            } // Else do nothing, already up to date. 
        }

        return numFilesDownloaded;
    }

    /**
     * Returns the latest date in an unordered list of dates. 
     * @param dates
     * @return 
     */
    LocalDate getLatestDate(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            throw new IllegalArgumentException("Date list cannot be empty!");
        } else {
            LocalDate latestDate = dates.get(0);
            for (LocalDate date : dates) {
                if (date.isAfter(latestDate)) {
                    latestDate = date;
                }
            }
            return latestDate;
        }
    }

    /**
     * Extracts only the useful data from all original CSV files in the
     * output directory and writes to files of the same name in the specified
     * destination
     * @see convertCSVFile
     *
     * @param destDir
     * @return
     */
    public int convertAllCSVFilesInFolder(Path destDir) throws IOException {
        Files.createDirectories(destDir);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(outputDirectory))) {
            Iterator<Path> it = dirStream.iterator();
            int count = 0;
            while (it.hasNext()) {
                Path input = it.next();
                Path output = Paths.get(destDir.toString(), input.getFileName().toString());
                convertCSVFile(input, output);
                count++;
            }
            return count;
        }
    }

    /**
     * Extracts only the useful data from an original CSV file. Specifically,
     * it creates a CSV file with the first row being midnight..11pm and the 
     * first column being the sensor names. 
     * 
     * @param source
     * @param dest
     * @throws IOException
     */
    void convertCSVFile(Path source, Path dest) throws IOException {
        if (!Files.isReadable(source)) {
            throw new IOException("File does not exist / is not readable: " + source.toString());
        }

        try (BufferedReader reader = Files.newBufferedReader(source)) {
            try (BufferedWriter writer = Files.newBufferedWriter(dest)) {
                String line = null;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    if (!isValidCsvContent(line, lineNumber)) {
                        throw new IllegalArgumentException("Not a valid Melbourne CSV file or format has changed: " + source + "\n"
                                + "Line: " + lineNumber + "\n"
                                + "Line content: " + line);
                    }
                    if (lineNumber > MelbourneCSVFile.TOTAL_ROW) {
                        writer.close();
                        return;
                    }
                    if (lineNumber >= MelbourneCSVFile.HEADINGS_ROW
                            && lineNumber <= MelbourneCSVFile.DATA_FINAL_ROW) {
                        writer.append(line);
                        writer.newLine();
                    }
                    lineNumber++;
                }
            }
        }

    }

    /**
     * Checks if the CSV file appears to be valid and will return true if so.
     *
     * @param line - a line of the csv file.
     * @param lineNum - the line number of this line (zero indexed)
     * @return true if valid, false otherwise.
     */
    boolean isValidCsvContent(String line, int lineNum) {
        if (lineNum == 0 && !line.equals(MelbourneCSVFile.EXPECTED_FIRST_LINE)) {
            return false;
        }

        if (lineNum == MelbourneCSVFile.HEADINGS_ROW && !line.startsWith(MelbourneCSVFile.EXPECTED_HEADING_NAME)) {
            return false;
        }

        if (lineNum == 30 && !line.startsWith(MelbourneCSVFile.EXPECTED_ROW_30_NAME)) {
            return false;
        }

        if (lineNum == MelbourneCSVFile.TOTAL_ROW && !line.startsWith(MelbourneCSVFile.EXPECTED_TOTAL_NAME)) {
            return false;
        }

        // All checks passed.
        return true;
    }

    /**
     * For a file with a name dd-mm-yyyy.csv, this method will return the date.
     *
     * @param file
     * @return
     */
    LocalDate parseDateFromFilename(Path file) {
        String fileName = file.getFileName().toString();
        if (!fileName.matches(MelbourneDataSource.MelbourneCSVFile.FILENAME_REGEX)) {
            throw new IllegalArgumentException("File is invalid: " + fileName);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate date = LocalDate.parse(fileName.substring(0, 10), df);

        return date;
    }

    /**
     * Reads all CSV files in the output folder and creates a Melbourne foot
     * traffic object which contains all the sensors and all their readings.
     * 'N/A' readings are converted to the value of zero.
     *
     * @return
     * @throws IOException
     */
    public Melbourne createMelbourne() throws IOException {

        Melbourne melbourne = new Melbourne();

        // Read directory
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(outputDirectory))) {
            Iterator<Path> it = dirStream.iterator();

            // For each file
            while (it.hasNext()) {
                Path input = it.next();

                // Ignore files that are not a data file. 
                if (input.getFileName().toString().matches(MelbourneCSVFile.FILENAME_REGEX)) {

                    LocalDate fileDate = parseDateFromFilename(input);

                    // Open the file
                    try (BufferedReader reader = Files.newBufferedReader(input)) {
                        int lineNumber = 0;
                        String line = null;
                        String lineContent[] = null;

                        // Read file lines
                        while ((line = reader.readLine()) != null) {

                            // Ensure file content is valid.
                            if (!isValidCsvContent(line, lineNumber)) {
                                throw new IllegalArgumentException("Not a valid Melbourne CSV file or format has changed: " + input + "\n"
                                        + "Line: " + lineNumber + "\n"
                                        + "Line content: " + line);
                            }

                            // Only read data rows
                            if (lineNumber >= (MelbourneCSVFile.HEADINGS_ROW + 1)
                                    && lineNumber <= MelbourneCSVFile.DATA_FINAL_ROW) {
                                lineContent = line.split(",");

                                // Get the sensor associated with this line
                                Sensor s = melbourne.getSensor(lineContent[0]);

                                LocalTime hour = LocalTime.of(0, 0); // Midnight
                                LocalDateTime dateTime = LocalDateTime.of(fileDate, hour);
                                for (String val : lineContent) {
                                    // Skip first
                                    if (val.equals(lineContent[0])) {
                                        continue;
                                    }

                                    // Set count
                                    int count;
                                    if (val.equals("N/A")) {
                                        count = 0;
                                    } else {
                                        count = Integer.parseInt(val);
                                    }
                                    s.setCount(dateTime, count);

                                    dateTime = dateTime.plusHours(1);
                                }
                            }

                            // Break once we have finished reading data rows
                            if (lineNumber > MelbourneCSVFile.DATA_FINAL_ROW) {
                                break;
                            }

                            lineNumber++;
                        }
                    }
                }
            }
        }
        return melbourne;
    }
}
