package com.yoje;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.LinuxPerfProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@link ThreadPoolExecutor} vs {@link ForkJoinPool}
 *
 * @author yoje
 * @date 2018/2/12
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
public class ExecutorServiceJMH {

    private List<Runnable> tasks;

    private static class Task implements Runnable {

        @Override
        public void run() {

        }
    }

    @Setup
    public void createTasks() {
        int size = 1000000;
        tasks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tasks.add(new Task());
        }
    }

    private void execute(ExecutorService executorService) {
        for (Runnable task : tasks) {
            executorService.submit(task);
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Benchmark
    public void threadPoolExecutor() {
        execute(Executors.newFixedThreadPool(4));
    }

    @Benchmark
    public void forkJoinPool() {
        execute(new ForkJoinPool(4));
    }

    // java ThreadPoolJMH -wi 5 -i 5 -f 1
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(ExecutorServiceJMH.class.getSimpleName()).warmupIterations(5)
//                .addProfiler(LinuxPerfProfiler.class)
                .measurementIterations(5).forks(1).build();

        new Runner(opt).run();
    }
}
