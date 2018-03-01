package com.yoje;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
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

    @State(Scope.Thread)
    public static class ByteSupplier {
        byte[] buffer;

        @Setup
        public void up() {
            buffer = new byte[byteSize];
            Arrays.fill(buffer, (byte) 1);
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
    public int lz4_stream(LZ4FactorySupplier lz4FactorySupplier, ByteSupplier byteSupplier) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LZ4BlockOutputStream lz4BlockOutputStream = new LZ4BlockOutputStream(outputStream, 1024 * 1024,
                lz4FactorySupplier.lz4Compressor);
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
    public int snappy_stream(ByteSupplier byteSupplier) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SnappyOutputStream snappyOutputStream = new SnappyOutputStream(outputStream, 32 * 1024);
        writeToStream(snappyOutputStream, byteSupplier.buffer);
        return outputStream.size();
    }


    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CompressJMH.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }


}
