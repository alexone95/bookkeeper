package org.apache.bookkeeper.bookie;

import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import static org.apache.bookkeeper.util.BookKeeperConstants.BOOKIE_STATUS_FILENAME;

@RunWith(Parameterized.class)
public class TestBookieStatusWriteToDirectories {

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
                {1, 1, generateListOfDirectories(1, 1, true), BookieStatus.BookieMode.READ_ONLY, "READ_ONLY" },
                {2, 1, generateListOfDirectories(1, 2, true), BookieStatus.BookieMode.READ_WRITE, "READ_WRITE" },
                // Coverage
                {3, 1, generateListOfDirectories(1, 3, false), BookieStatus.BookieMode.READ_WRITE,
                        new IndexOutOfBoundsException("Index: 0, Size: 0")},

        });
    }

    public TestBookieStatusWriteToDirectories(int index, int size, List<File> directories,
                                              BookieStatus.BookieMode bookieMode, Object testResult) {
        this.index = index;
        this.size = size;
        this.directories = directories;
        this.bookieMode = bookieMode;
        this.testResult = testResult;
    }


    @Before
    public void configureTest(){
        if (bookieMode.equals(BookieStatus.BookieMode.READ_WRITE)){
            bookieStatus.setToWritableMode();
        } else if (bookieMode.equals(BookieStatus.BookieMode.READ_ONLY)) {
            bookieStatus.setToReadOnlyMode();
        }
    }

    @After
    public void deleteTempDir() throws IOException {
        FileUtils.deleteDirectory(new File("tempdir" + index));
    }

    public static List<File> generateListOfDirectories(int size, int index, boolean writable) throws IOException {
        List<File> directories = new ArrayList<File>();
        File directory;

        File tempDirForIndex = new File("tempdir" + index);
        if (!tempDirForIndex.exists()){
            tempDirForIndex.mkdirs();
        }
        tempDirForIndex.deleteOnExit();

        for (int i=0; i < size; i++){
            String dirName = String.format("dir%s", i);
            directory = new File(tempDirForIndex.getPath() + "/" + dirName);
            if (!directory.exists()){
                directory.mkdirs();
            }
            generateBookieStatusFile(directory.getPath(), writable);
            directories.add(directory);
        }
        return directories;
    }

    public static void generateBookieStatusFile(String directoryPath, boolean writable) throws IOException {
        File tempTxtFile;
        tempTxtFile = new File(directoryPath + "/" + BOOKIE_STATUS_FILENAME);
        tempTxtFile.deleteOnExit();

        if (tempTxtFile.createNewFile()){
            System.out.println("File creato");
            tempTxtFile.setWritable(writable);
        }
        else
            System.out.println("File già esistente");
    }

    @Test
    public void testWriteToDirectories() {
        String bookieModeFromRead = null;
        try {
            bookieStatus.writeToDirectories(directories);
            for (int i=0; i < size; i++){
                File bookieStatusFile = new File(String.format("tempdir%s/dir%s/BOOKIE_STATUS", index, i));
                List<String> read = Files.readAllLines(bookieStatusFile.toPath());
                bookieModeFromRead = read.get(0).split(",")[1];
            }
            Assert.assertEquals(testResult, bookieModeFromRead);
        } catch (Exception e){
            Assert.assertEquals(testResult.toString(), e.toString());
        }
    }
}
