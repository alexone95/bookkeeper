package org.apache.bookkeeper.bookie;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.apache.bookkeeper.util.BookKeeperConstants.BOOKIE_STATUS_FILENAME;

@RunWith(Parameterized.class)
public class TestBookieStatusParse {

    private BookieStatus bookieStatus = new BookieStatus();

    private Object testResult;

    private int index;

    private int size;

    private BufferedReader bufferedReader;

    @Parameterized.Parameters
    public static Collection BookieStatusParameters() throws IOException {
        return Arrays.asList(new Object[][] {

                // Suite test
                {0, null, new NullPointerException() },
                {1, generateBufferedReader(0, 1, new String[] {"1", "READ_ONLY", "12345"}),
                        new NullPointerException()},
                {2, generateBufferedReader(8192, 2, new String[] {"1", "READ_ONLY", "12345"}),
                        new String[] {"1", "READ_ONLY", "12345"}},
                // Coverage
                {3, generateBufferedReader(1, 3, null), new NullPointerException() },
                {4, generateBufferedReader(1, 4, new String[] {",", ","}), new NullPointerException() },
        });
    }

    public TestBookieStatusParse(int index, BufferedReader bufferedReader, Object testResult) {
        this.testResult = testResult;
        this.index = index;
        this.bufferedReader = bufferedReader;
    }

    public static BufferedReader generateBufferedReader(int size, int index, String[] content) throws IOException {
        File directory = new File(String.format("tempdir%s", index));
        BufferedReader bufferedReader = null;
        if (!directory.exists()){
            directory.mkdirs();
        }

        File tempTxtFile = new File(directory.getPath() + "/" + BOOKIE_STATUS_FILENAME);
        tempTxtFile.deleteOnExit();

        if (tempTxtFile.createNewFile()) {
            if (content != null && content.length == 3)
                Files.write(tempTxtFile.toPath(), Collections.singleton(String.format("%s,%s,%s", content[0], content[1], content[2])));
            else if (content == null) {
                Files.write(tempTxtFile.toPath(), Collections.singleton(""));
            }
            else {
                Files.write(tempTxtFile.toPath(), Collections.singleton(String.format("%s%s", content[0], content[1])));
            }
            System.out.println("File creato");
        }
        else
            System.out.println("File già esistente");
        // instantiate BufferedReader object
        try{
            bufferedReader = new BufferedReader(new FileReader((tempTxtFile.getPath())), size);
        }
        catch (IllegalArgumentException e){
            return null;
        }
        return bufferedReader;
    }

    @After
    public void deleteTempDir() throws IOException {
        FileUtils.deleteDirectory(new File("tempdir" + index));
    }

    @Test
    public void testParse() {
        String[] bookieModeFromRead;
        try {
            BookieStatus bookieStatusTest = bookieStatus.parse(bufferedReader);

            bookieModeFromRead = bookieStatusTest.toString().split(",");

            String[] testResultToArray = (String[]) testResult;
            Assert.assertEquals(testResultToArray[0], bookieModeFromRead[0]);
            Assert.assertEquals(testResultToArray[1], bookieModeFromRead[1]);
        } catch (Exception e){
            Assert.assertEquals(testResult.toString(), e.toString());
        }
    }
}
