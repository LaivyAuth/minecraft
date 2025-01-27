package com.laivyauth.utilities.timeout;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class Timeout implements Delayed {

    // Static initializers

    private static final @NotNull Object queueLock = new Object();
    private static final @NotNull DelayQueue<@NotNull Timeout> queue = new DelayQueue<>();
    private static @Nullable TimeoutThread thread;

    private static synchronized void initialize() {
        thread = new TimeoutThread();
        thread.start();
    }
    private static synchronized void interrupt() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    static {
        // Initialize
        initialize();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(Timeout::interrupt, "Timeouts Shutdown Hook"));
    }

    // Object

    private final @NotNull LinkedList<@NotNull Consumer<@Nullable TimeoutException>> consumers = new LinkedList<>();
    private final @NotNull ReentrantLock reentrant = new ReentrantLock();

    private final long executionTime;
    private volatile boolean cancelled = false;

    public Timeout(@NotNull Duration delay) {
        this.executionTime = System.currentTimeMillis() + delay.toMillis();

        if (!queue.offer(this)) {
            throw new IllegalStateException("cannot offer timeout to queue");
        }
    }

    // Getters

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(executionTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public @NotNull Duration getRemaining() {
        return Duration.ofMillis(executionTime - System.currentTimeMillis());
    }

    public void cancel() {
        reentrant.lock();

        try {
            this.cancelled = true;

            for (@NotNull Consumer<@Nullable TimeoutException> consumer : consumers) {
                consumer.accept(null);
            }
        } finally {
            reentrant.unlock();
        }
    }

    @Contract(value = "_->this", pure = true)
    public @NotNull Timeout whenComplete(@NotNull Consumer<@Nullable TimeoutException> consumer) {
        reentrant.lock();

        try {
            consumers.addFirst(consumer);
        } finally {
            reentrant.unlock();
        }

        return this;
    }

    // Modules

    @Override
    public int compareTo(@NotNull Delayed delayed) {
        return Long.compare(this.executionTime, delayed.getDelay(TimeUnit.MILLISECONDS));
    }

    // Implementations

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Timeout)) return false;
        @NotNull Timeout that = (Timeout) object;

        return cancelled == that.cancelled && Objects.equals(executionTime, that.executionTime);
    }
    @Override
    public int hashCode() {
        return Objects.hash(executionTime, cancelled);
    }

    @Override
    public @NotNull String toString() {
        return "Timeout{" + "delay=" + executionTime + '}';
    }

    // Classes

    private static final class TimeoutThread extends Thread {

        // Object

        private volatile boolean running = true;

        public TimeoutThread() {
            super("Timeouts Manager");
            setDaemon(false); // Mark as not daemon
        }

        @Override
        public synchronized void start() {
            running = true;
            super.start();
        }
        @Override
        public void interrupt() {
            running = false;
            super.interrupt();
        }

        // Getters

        public boolean isRunning() {
            return running;
        }

        // Modules

        @Override
        public void run() {
            while (isRunning()) {
                try {
                    // Retrieve expired timeout
                    @NotNull Timeout task = queue.take();

                    // Start executing
                    task.reentrant.lock();

                    try {
                        for (@NotNull Consumer<@Nullable TimeoutException> consumer : task.consumers) {
                            consumer.accept(new TimeoutException());
                        }
                    } finally {
                        task.reentrant.unlock();
                    }
                } catch (@NotNull InterruptedException e) {
                    // todo: interrupted exception
                    break;
                }
            }
        }
    }

}
