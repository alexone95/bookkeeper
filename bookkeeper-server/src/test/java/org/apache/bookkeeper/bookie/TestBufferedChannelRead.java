package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import static org.apache.bookkeeper.bookie.TestBufferedChannelWrite.generateByteBuf;

@RunWith(Parameterized.class)
public class TestBufferedChannelRead {

    private final int bufferedChannelCapacity;
    private final ByteBuf dstBuffer;
    private final long bufferedChannelPos;
    private final int bufferedChannelLength;
    private final boolean resetIndex;
    private final int writeBufferStartPosition;
    private final Object testResult;
    private BufferedChannel bufferedChannel;
    private static FileChannel bufferedChannelFileChannel;

    @Parameterized.Parameters
    public static Collection BufferedChannelParameters() {
        return Arrays.asList(new Object[][] {

                // Suite Test
                {0, null, -1, 0, true, 0, 0},
                {1, generateByteBuf(1024, false), 1, 1, false, 0, 1},
                {1, generateByteBuf(1024, false), 2, 2, false, 0, 2},

                // Coverage
                {1, generateByteBuf(1024, true), 35, 45, true, 10,
                        new IllegalArgumentException("minWritableBytes : -25 (expected: >= 0)")},
                //prova a scrivere un numero negativo di byte
                {1, generateByteBuf(1024, false), 35, 45, false, 10,
                        new IOException("Read past EOF")},

                // Mutation
                {1, generateByteBuf(0, false), 35, 45, false, 10, new IOException("Read past EOF")}
        });
    }

    public TestBufferedChannelRead(int capacity, ByteBuf dst, long pos, int length, boolean resetIndex,
                                   int writeBufferStartPosition, Object result){
        this.dstBuffer = dst;
        this.bufferedChannelPos = pos;
        this.bufferedChannelLength = length;
        this.bufferedChannelCapacity = capacity;
        this.resetIndex = resetIndex;
        this.writeBufferStartPosition = writeBufferStartPosition;
        this.testResult = result;
    }

    @Before
    public void configureTest() throws IOException {

        ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        ByteBuf source;
        File newLogFile = File.createTempFile("test", "log");
        newLogFile.deleteOnExit();
        bufferedChannelFileChannel = new RandomAccessFile(newLogFile, "rw").getChannel();

        if (resetIndex) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(writeBufferStartPosition);
            bufferedChannelFileChannel.write(byteBuffer);
        }
        source = generateByteBuf(8, resetIndex);

        int bufferedChannelUnpersistedBytesBound = 10;
        bufferedChannel = new BufferedChannel(allocator, bufferedChannelFileChannel,
                bufferedChannelCapacity, bufferedChannelUnpersistedBytesBound);

        bufferedChannel.write(source);
    }

    @After
    public void close() throws IOException {
        bufferedChannelFileChannel.close();
    }

    @Test
    public void testRead() {
        try {
            // Assert that the number of readed bytes is equal to the expected value
            Assert.assertEquals(testResult, bufferedChannel.read(dstBuffer, bufferedChannelPos, bufferedChannelLength));
        } catch (Exception e) {
            Assert.assertEquals(testResult.toString(), e.toString());
        }
    }

}
