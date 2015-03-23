/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timgrunshaw.ftprediction.dataretrieval;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * Note 18/03/15 - Running the test seems to hit a limit on the server and it
 * starts returning 400's by the later tests, these are just logged, but may be
 * worth exploring if you're having issues. To test an individual method, right
 * click, 'run focused test method'.
 *
 * @author Tim Grunshaw
 */
public class MelbourneTest {

    final long FULL_FILE_SIZE_THRESHOLD = 10000; // Test was 13933
    final long EMPTY_FILE_SIZE_THRESHOLD = 1000; // Test was 453

    // Server often becomes unavailable.
    final String ACCEPTABLE_ERROR = "Server returned HTTP response code: 400";
    final String RESOURCE_DIRECTORY = "test/resources/melbourneTest/csv_files";

    private Melbourne melbourne;
    private LocalDate validDay;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    public MelbourneTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws IOException {
        melbourne = new Melbourne();
        melbourne.setOutputDirectory(tempFolder.getRoot().getCanonicalPath());
        validDay = LocalDate.now();
    }

    @After
    public void tearDown() {
    }

    private void log(String message) {
        System.err.println(message);
        System.err.println("See comments for more information.");
    }

    @Test
    public void testGenerateCSVUrl() {
        LocalDate date = LocalDate.of(2014, Month.MARCH, 17);
        String generatedUrl = melbourne.generateCSVUrl(date);
        String expectedUrl = Melbourne.URL_PREFIX + "17-03-2014";
        assert generatedUrl.equals(expectedUrl);
    }

    @Test
    public void testDownloadFile() throws IOException {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String url = melbourne.generateCSVUrl(yesterday);
            String fileDestination = "output/test.csv";
            File expectedFile = Paths.get(fileDestination).toFile();
            Files.deleteIfExists(Paths.get(fileDestination));

            assert expectedFile.length() == 0 : "File not properly deleted";

            melbourne.downloadFile(url, fileDestination);

            assert expectedFile.length() > 0 : "File did not get created";
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testDownloadFilesInRange() throws IOException {
        // See other tests for range checks. //
        try {
            tempFolder.newFolder();
            int numberOfFiles = 3;

            melbourne.downloadCSVFilesInRange(validDay.minusDays(numberOfFiles), validDay);

            int count = 0;
            for (final File file : tempFolder.getRoot().listFiles()) {
                if (file.isFile()) {
                    assert file.length() > 0;
                    count++;
                }
            }
            assert count == numberOfFiles;
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testDownloadFilesInRange_FromDate() throws IOException {
        try {
            exception.expect(IllegalArgumentException.class);
            exception.expectMessage("From date must be equal to or "
                    + "after the EARLIEST_DATE");
            LocalDate beforeEarliestDate = Melbourne.EARLIEST_DATE.minusDays(1);

            melbourne.downloadCSVFilesInRange(beforeEarliestDate, validDay);
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testDownloadFilesInRange_ToDate() throws IOException {
        try {
            exception.expect(IllegalArgumentException.class);
            exception.expectMessage("fromDate must be before LocalDate.now(), "
                    + "toDate must be no more than 1 day from LocalDate.now()");
            LocalDate twoDaysFromNow = LocalDate.now().plusDays(2);

            melbourne.downloadCSVFilesInRange(validDay, twoDaysFromNow);
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testDownloadFilesInRange_FromToDate() throws IOException {
        try {
            exception.expect(IllegalArgumentException.class);
            exception.expectMessage("fromDate must be less than toDate");
            LocalDate anotherValidDay = validDay.minusDays(1);

            melbourne.downloadCSVFilesInRange(validDay, anotherValidDay);
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testUpdateDirectory() throws IOException {
        try {
            final int numDaysToGoBack = 5;
            // Create initial date to start with
            LocalDate initialDay = LocalDate.now().minusDays(numDaysToGoBack);
            melbourne.downloadDataForDay(initialDay);
            // Add the next day so that we test the getLatestDate code
            melbourne.downloadDataForDay(initialDay.plusDays(1));

            // Ensure it gets files for all days between start and now and that not empty.
            melbourne.update();
            int count = 0;
            for (final File file : tempFolder.getRoot().listFiles()) {
                if (file.isFile()) {
                    assert file.length() > 0;
                    count++;
                }
            }
            assert count == numDaysToGoBack : "Update only got " + count + " files, should have got " + numDaysToGoBack;
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testUpdateWhenUpToDate() throws IOException {
        try {
        // Assert that update() does not add any files when already at latest day.
            // Assert that upate does not overwrite latest day.
            LocalDate latestDay = LocalDate.now().minusDays(1);
            melbourne.setOutputDirectory(tempFolder.getRoot().getCanonicalPath());
            Path file = melbourne.downloadDataForDay(latestDay);
            FileTime lastModified = Files.getLastModifiedTime(file);

            int numUpdates = melbourne.update();
            Assert.assertEquals(0, numUpdates);

            FileTime latestModified = Files.getLastModifiedTime(file);
            Assert.assertEquals(lastModified, latestModified);
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    /**
     * We expect the earliest date to be the oldest valid date.
     */
    @Test
    public void testEarliestDate() throws IOException {
        try {
            LocalDate earliestDate = Melbourne.EARLIEST_DATE;
            Path file = melbourne.downloadDataForDay(earliestDate);
            assert Files.size(file) > FULL_FILE_SIZE_THRESHOLD : "File for the earliest date appears to be empty!";

            LocalDate beforeEarliestDate = Melbourne.EARLIEST_DATE.minusDays(1);
            file = melbourne.downloadDataForDay(beforeEarliestDate);
            assert Files.size(file) < EMPTY_FILE_SIZE_THRESHOLD : "File before earliest date appears to have data!";
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testLatestDate() throws IOException {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Path file = melbourne.downloadDataForDay(yesterday);
            assert Files.size(file) > FULL_FILE_SIZE_THRESHOLD : "Latest file appears to be empty!";
        } catch (IOException IOEx) {
            if (IOEx.getMessage().startsWith(ACCEPTABLE_ERROR)) {
                log(ACCEPTABLE_ERROR);
            } else {
                throw IOEx;
            }
        }
    }

    @Test
    public void testParseDateFromFileName() {
        Path file = Paths.get(RESOURCE_DIRECTORY + "/15-03-2015.csv");
        LocalDate date = melbourne.parseDateFromFilename(file);
        Assert.assertEquals(date, LocalDate.of(2015, Month.MARCH, 15));
    }

    @Test
    public void testGetLatestDate() throws IOException {
        ArrayList<LocalDate> currentFiles = new ArrayList<>();
        DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(RESOURCE_DIRECTORY));
        Iterator<Path> it = dirStream.iterator();
        while (it.hasNext()) {
            Path file = it.next();
            try {
                currentFiles.add(melbourne.parseDateFromFilename(file));
            } catch (DateTimeParseException pEx) {
                // Don't add, just continue, may have been another file in the folder. 
            }
        }

        LocalDate date = melbourne.getLatestDate(currentFiles);
        Assert.assertEquals(LocalDate.of(2015, Month.MARCH, 18), date);
    }
    
    @Test
    public void testConvertCSVFile() throws IOException{
        // There is significant error checking in the Melbourne class itself.
        Path file = Paths.get(RESOURCE_DIRECTORY + "/18-03-2015.csv");
        Path dest = Paths.get(RESOURCE_DIRECTORY + "/testOut.csv");
        melbourne.convertCSVFile(file, dest);
    }

}
