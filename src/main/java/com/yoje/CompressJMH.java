package com.yoje;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.xxhash.XXHashFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author yoje
 * @date 2018/2/28
 */
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.AverageTime)
public class CompressJMH {

    private static int byteSize = 5 * 1024 * 1024;

    /**
     * @see LZ4Factory
     * All methods from this class are very costly, so you should get an instance
     * once, and then reuse it whenever possible. This is typically done by storing
     * a {@link LZ4Factory} instance in a static field.
     */
    @State(Scope.Benchmark)
    public static class LZ4FactorySupplier {
        LZ4Factory lz4Factory;
        LZ4Compressor lz4Compressor;
        LZ4FastDecompressor lz4FastDecompressor;

        @Setup
        public void up() {
            lz4Factory = LZ4Factory.fastestInstance();
            lz4Compressor = lz4Factory.fastCompressor();
            lz4FastDecompressor = lz4Factory.fastDecompressor();
        }
    }

    /**
     * @see XXHashFactory
     * All methods from this class are very costly, so you should get an instance
     * once, and then reuse it whenever possible. This is typically done by storing
     * a {@link XXHashFactory} instance in a static field.
     */
    @State(Scope.Benchmark)
    public static class XXHashFactorySupplier {
        XXHashFactory xxHashFactory;

        @Setup
        public void up() {
            xxHashFactory = XXHashFactory.fastestInstance();
        }
    }

    @State(Scope.Thread)
    public static class ByteSupplier {
        byte[] buffer;

        @Setup
        public void up() {
            String size = System.getProperty("byte.size", String.valueOf(1024*1024));
            buffer = new byte[Integer.parseInt(size)];
            Arrays.fill(buffer, (byte) 1);
        }
    }

    @State(Scope.Benchmark)
    public static class LZ4StreamBlockSize {
        int blockSize;

        @Setup
        public void up() {
            String size = System.getProperty("lz4.blocksize", String.valueOf(1 << 16));
            blockSize = Integer.parseInt(size);
        }
    }

    @State(Scope.Benchmark)
    public static class SnappyStreamBlockSize {
        int blockSize;

        @Setup
        public void up() {
            String size = System.getProperty("snappy.blocksize", String.valueOf(32 * 1024));
            blockSize = Integer.parseInt(size);
        }
    }

    @Benchmark
    public int lz4_byte(LZ4FactorySupplier lz4FactorySupplier, ByteSupplier byteSupplier) {
        byte[] bytes = byteSupplier.buffer;
        LZ4Compressor lz4Compressor = lz4FactorySupplier.lz4Compressor;
        byte[] compress = lz4Compressor.compress(bytes);
        return compress.length;
    }

    private void writeToStream(OutputStream outputStream, byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }

    @Benchmark
    public int lz4_stream(LZ4FactorySupplier lz4FactorySupplier, XXHashFactorySupplier xxHashFactorySupplier,
            ByteSupplier byteSupplier, LZ4StreamBlockSize blockSize) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LZ4BlockOutputStream lz4BlockOutputStream = new LZ4BlockOutputStream(outputStream, blockSize.blockSize,
                lz4FactorySupplier.lz4Compressor,
                xxHashFactorySupplier.xxHashFactory.newStreamingHash32(0x9747b28c).asChecksum(), false);
        writeToStream(lz4BlockOutputStream, byteSupplier.buffer);
        return outputStream.size();
    }

    @Benchmark
    public int snappy_byte(ByteSupplier byteSupplier) throws IOException {
        byte[] bytes = byteSupplier.buffer;
        byte[] compress = Snappy.compress(bytes);
        return compress.length;
    }

    @Benchmark
    public int snappy_stream(ByteSupplier byteSupplier, SnappyStreamBlockSize blockSize) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SnappyOutputStream snappyOutputStream = new SnappyOutputStream(outputStream, blockSize.blockSize);
        writeToStream(snappyOutputStream, byteSupplier.buffer);
        return outputStream.size();
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * You can see the cold scenario is running longer, because we pay for
     * thread wakeups.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ gradle clean build
     *    $ java -jar build/libs/jmh-tests.jar CompressJMH -jvmArgs "-Dlz4.blocksize=1024 -Dsnappy.blocksize=1024 -Dbyte
     *    .size=1024"
     *
     * b) Via the Java API:
     *    (see the JMH homepage for possible caveats when running from IDE:
     *      http://openjdk.java.net/projects/code-tools/jmh/)
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(CompressJMH.class.getSimpleName())
                .jvmArgsAppend("-Dbyte.size=1024")
                .build();

        new Runner(opt).run();
    }

}
