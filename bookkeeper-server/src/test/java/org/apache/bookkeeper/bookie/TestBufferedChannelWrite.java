package org.apache.bookkeeper.bookie;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@RunWith(Parameterized.class)
public class TestBufferedChannelWrite {

    private final ByteBuf source;
    private final long byteCapacity;
    private final Object testResult;

    private BufferedChannel bufferedChannel;
    private final static long HEADER_SIZE = 32L;

    @Parameterized.Parameters
    public static Collection BufferedChannelParameters() {
        return Arrays.asList(new Object[][] {
                // Suite di test
                {null, 0, new NullPointerException()},
                {generateByteBuf(0, false), 1, 0L},
                {generateByteBuf(1, false), 1, 1L + HEADER_SIZE},
                // Coverage
                {generateByteBuf(1, false), -33, (long)0}
        });

    }

    public TestBufferedChannelWrite(ByteBuf byteBuf, int byteCapacity, Object testResult){
        this.source = byteBuf;
        this.byteCapacity = byteCapacity + HEADER_SIZE;
        this.testResult = testResult;
    }

    public static ByteBuf generateByteBuf(int length, boolean resetIndex) {
        Random random = new Random();
        byte[] data = new byte[length];
        ByteBuf byteBuffer;
        try {
            random.nextBytes(data);
            byteBuffer = Unpooled.buffer(1024);
        }
        catch (IllegalArgumentException e){
            return null;
        }
        byteBuffer.writeLong(0);
        byteBuffer.writeLong(1);
        byteBuffer.writeLong(2);
        byteBuffer.writeLong(length);
        byteBuffer.writeBytes(data);
        if (resetIndex){
            byteBuffer.resetWriterIndex();
        }
        return byteBuffer;
    }

    @Test
    public void testWrite() throws Exception {
        try {
            bufferedChannel.write(source);
            // Assert that the size of the filechannel is equal to the expected value
            Assert.assertEquals((long) testResult, bufferedChannel.fileChannel.size());
        } catch (Exception e){
            Assert.assertEquals(testResult.toString(), e.toString());
        }
    }

    @Before
    public void initializeBufferedChannel() throws IOException {
        File tempLogFile = File.createTempFile("test", "log");
        tempLogFile.deleteOnExit();
        FileChannel fileChannel = new RandomAccessFile(tempLogFile, "rw").getChannel();
        bufferedChannel = new BufferedChannel(UnpooledByteBufAllocator.DEFAULT, fileChannel, (int) byteCapacity);
    }

    @After
    public void close() throws IOException {
        bufferedChannel.close();
    }
}
