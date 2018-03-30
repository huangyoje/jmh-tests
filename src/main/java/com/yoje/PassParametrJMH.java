package com.yoje;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author yoje
 * @date 2018/3/30
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
public class PassParametrJMH {

    private Object obj1;
    private Object obj2;
    private Object obj3;

    @Setup
    public void setUp() {
        obj1 = new Object();
        obj2 = new Object();
        obj3 = new Object();
    }

    @Benchmark
    public int base() {
        return invoke_0_parameter();
    }

    @Benchmark
    public int one_parameter() {
        return invoke_1_parameter(obj1);
    }

    @Benchmark
    public int three_parameter() {
        return invoke_3_parameter(obj1, obj2, obj3);
    }

    @Benchmark
    public int six_parameter() {
        return invoke_6_parameter(obj1, obj2, obj3, obj1, obj2, obj3);
    }

    private int invoke_0_parameter() {
        return 1;
    }

    private int invoke_1_parameter(Object obj1) {
        return 1;
    }

    private int invoke_3_parameter(Object obj1, Object obj2, Object obj3) {
        return 1;
    }

    private int invoke_6_parameter(Object obj1, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
        return 1;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(PassParametrJMH.class.getSimpleName()).warmupIterations(5)
                //                .addProfiler(LinuxPerfProfiler.class)
                .measurementIterations(3).forks(1).build();

        new Runner(opt).run();
    }
}
