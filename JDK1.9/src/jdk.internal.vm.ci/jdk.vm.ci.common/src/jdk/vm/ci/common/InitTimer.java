/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package jdk.vm.ci.common;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A facility for timing a step in the runtime initialization sequence. This is independent from all
 * other JVMCI code so as to not perturb the initialization sequence. It is enabled by setting the
 * {@code "jvmci.inittimer"} system property to {@code "true"}.
 */
public final class InitTimer implements AutoCloseable {
    private final String name;
    private final long start;

    private InitTimer(String name) {
        int n = nesting.getAndIncrement();
        if (n == 0) {
            initializingThread = Thread.currentThread();
            System.out.println("INITIALIZING THREAD: " + initializingThread);
        } else {
            assert Thread.currentThread() == initializingThread : Thread.currentThread() + " != " + initializingThread;
        }
        this.name = name;
        this.start = System.currentTimeMillis();
        System.out.println("START: " + SPACES.substring(0, n * 2) + name);
    }

    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", justification = "only the initializing thread accesses this field")
    public void close() {
        final long end = System.currentTimeMillis();
        int n = nesting.decrementAndGet();
        System.out.println(" DONE: " + SPACES.substring(0, n * 2) + name + " [" + (end - start) + " ms]");
        if (n == 0) {
            initializingThread = null;
        }
    }

    public static InitTimer timer(String name) {
        return ENABLED ? new InitTimer(name) : null;
    }

    public static InitTimer timer(String name, Object suffix) {
        return ENABLED ? new InitTimer(name + suffix) : null;
    }

    /**
     * Specifies if initialization timing is enabled. Note: This property cannot use
     * {@code HotSpotJVMCIRuntime.Option} since that class is not visible from this package.
     */
    private static final boolean ENABLED = Boolean.getBoolean("jvmci.InitTimer");

    public static final AtomicInteger nesting = ENABLED ? new AtomicInteger() : null;
    public static final String SPACES = "                                            ";

    /**
     * Used to assert the invariant that all related initialization happens on the same thread.
     */
    static Thread initializingThread;
}
