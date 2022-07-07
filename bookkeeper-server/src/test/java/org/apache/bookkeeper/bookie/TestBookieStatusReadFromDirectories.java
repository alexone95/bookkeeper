package org.apache.bookkeeper.bookie;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.apache.bookkeeper.util.BookKeeperConstants.BOOKIE_STATUS_FILENAME;

@RunWith(Parameterized.class)
public class TestBookieStatusReadFromDirectories {

    private BookieStatus bookieStatus = new BookieStatus();

    private List<File> directories;

    private volatile BookieStatus.BookieMode bookieMode;

    private Object testResult;

    private int index;

    private int size;


    @Parameterized.Parameters
    public static Collection BookieStatusParameters() throws IOException {
        return Arrays.asList(new Object[][] {

                // Suite test
                {0, 0, null, BookieStatus.BookieMode.READ_ONLY, new NullPointerException() },
                {1, 1, generateListOfDirectoriesAndWriteBookieStatus(1, 1,
                        new String[] {"1", "READ_ONLY", "12345"}),
                        BookieStatus.BookieMode.READ_ONLY, new String[] {"1", "READ_ONLY","12345"} },
                {2, 1, generateListOfDirectoriesAndWriteBookieStatus(1, 2,
                        new String[] {"1", "READ_WRITE", "654321"}),
                        BookieStatus.BookieMode.READ_WRITE, new String[] {"1", "READ_WRITE", "654321"}},
        });
    }

    public TestBookieStatusReadFromDirectories(int index, int size, List<File> directories,
                                              BookieStatus.BookieMode bookieMode, Object testResult) {
        this.index = index;
        this.size = size;
        this.directories = directories;
        this.bookieMode = bookieMode;
        this.testResult = testResult;
    }

    public static List<File> generateListOfDirectoriesAndWriteBookieStatus(int size, int index, String[] content) throws IOException {
        List<File> directories = new ArrayList<File>();
        File directory;

        File tempDirForIndex = new File("tempdir" + index);
        if (!tempDirForIndex.exists()){
            tempDirForIndex.mkdirs();
        }

        for (int i=0; i < size; i++){
            String dirName = String.format("dir%s", i);
            directory = new File(tempDirForIndex.getPath() + "/" + dirName);
            if (!directory.exists()){
                directory.mkdirs();
            }
            generateBookieStatusFile(directory.getPath(), content);
            directories.add(directory);
        }
        return directories;
    }

    public static void generateBookieStatusFile(String directoryPath, String[] content) throws IOException {
        File tempTxtFile;
        tempTxtFile = new File(directoryPath + "/" + BOOKIE_STATUS_FILENAME);
        tempTxtFile.deleteOnExit();

        if (tempTxtFile.createNewFile()) {
            Files.write(tempTxtFile.toPath(), Collections.singleton(String.format("%s,%s,%s", content[0], content[1], content[2])));
            System.out.println("File creato");
        }
        else
            System.out.println("File già esistente");
    }

    @After
    public void deleteTempDir() throws IOException {
        FileUtils.deleteDirectory(new File("tempdir" + index));
    }

    @Test
    public void testReadFromDirectories() {
        String[] bookieModeFromRead = null;
        try {
            bookieStatus.readFromDirectories(directories);
            for (int i=0; i < size; i++){
                File bookieStatusFile = new File(String.format("tempdir%s/dir%s/BOOKIE_STATUS", index, i));
                List<String> read = Files.readAllLines(bookieStatusFile.toPath());
                bookieModeFromRead = read.get(0).split(",");
            }
            String[] testResultToArray = (String[]) testResult;
            assert bookieModeFromRead != null;
            Assert.assertEquals(testResultToArray[0], bookieModeFromRead[0]);
            Assert.assertEquals(testResultToArray[1], bookieModeFromRead[1]);
            Assert.assertEquals(testResultToArray[2], bookieModeFromRead[2]);
        } catch (Exception e){
            Assert.assertEquals(testResult.toString(), e.toString());
        }
    }

}
