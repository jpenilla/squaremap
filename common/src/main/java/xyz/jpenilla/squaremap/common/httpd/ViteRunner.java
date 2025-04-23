package xyz.jpenilla.squaremap.common.httpd;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.util.Util;

public final class ViteRunner extends Thread {
    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private final ThreadFactory threadFactory;
    private final Path dir;
    private final CompletableFuture<String> url = new CompletableFuture<>();
    private volatile boolean running = true;

    public ViteRunner(final String frontendPath) {
        this.dir = Path.of(frontendPath);
        this.setName("squaremap-vite-runner-" + COUNT.getAndIncrement());
        this.threadFactory = Thread.ofVirtual()
            .name(this.getName() + "-", 0)
            .factory();
    }

    @Override
    public void run() {
        Logging.logger().info("Starting Vite dev server...");

        try {
            final Process process = new ProcessBuilder()
                .directory(this.dir.toFile())
                .command("bun", "run", "dev")
                .redirectErrorStream(true)
                .start();

            // wait for http://localhost:<port>
            while (true) {
                final String line = readLine(process);
                if (line == null) {
                    throw new RuntimeException("Vite dev server process exited before URL was found");
                }
                Logging.info(line);
                final int idx = line.indexOf("http://localhost:");
                if (idx != -1) {
                    String foundUrl = line.substring(idx).trim();
                    if (foundUrl.endsWith("/")) {
                        foundUrl = foundUrl.substring(0, foundUrl.length() - 1);
                    }
                    this.url.complete(foundUrl);
                    break;
                }
            }

            while (this.running) {
                final String line = readLine(process);
                if (line == null) {
                    break;
                }
                Logging.info(line);
            }

            process.destroy();
            try {
                if (!process.waitFor(4, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (final InterruptedException e) {
                process.destroyForcibly();
            }
        } catch (final IOException e) {
            throw new RuntimeException("Exception running Vite dev server", e);
        }
    }

    /**
     * Reads a line from the process's input stream. Uses a virtual thread to allow interruption.
     *
     * @param process the process to read from
     * @return line read from the process
     */
    private String readLine(final Process process) {
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return process.inputReader().readLine();
            } catch (final IOException e) {
                throw Util.rethrow(e);
            }
        }, task -> this.threadFactory.newThread(task).start());
        while (!future.isDone()) {
            final boolean interrupted = Thread.interrupted();
            Thread.yield();
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
            if (interrupted) {
                return null;
            }
        }
        return future.join();
    }

    void shutdown() {
        Logging.logger().info("Shutting down Vite dev server...");
        this.running = false;
        this.interrupt();
        try {
            if (this.join(Duration.ofSeconds(5))) {
                Logging.info("Vite dev server shut down");
            } else {
                Logging.logger().error("Vite dev server did not shut down in time, you may need to kill it manually");
            }
        } catch (final InterruptedException e) {
            Logging.error("Error shutting down Vite dev server", e);
        }
    }

    CompletableFuture<String> getUrl() {
        return this.url;
    }
}
