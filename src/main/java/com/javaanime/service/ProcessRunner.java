package com.javaanime.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/** Bounded output and cancellation keep helper processes from hanging the UI. */
public final class ProcessRunner {
    private ProcessRunner() {}
    public static String run(List<String> command, Duration timeout, Cancellation cancellation) throws IOException {
        cancellation.check();
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var output = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> {
            try (var in = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                for (int n; (n = in.read(buffer)) != -1;) {
                    synchronized (output) { if (output.size() < 65536) output.write(buffer, 0, Math.min(n, 65536 - output.size())); }
                }
            } catch (IOException ignored) { /* Process teardown closes its stream. */ }
        });
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (!process.waitFor(100, TimeUnit.MILLISECONDS)) {
                cancellation.check();
                if (System.nanoTime() > deadline) throw new IOException("The helper took too long. Please try again.");
            }
            cancellation.check();
            reader.join(2000);
            String result;
            synchronized (output) { result = output.toString(StandardCharsets.UTF_8).strip(); }
            if (process.exitValue() != 0) throw new IOException(result.isBlank() ? "Helper process failed (" + process.exitValue() + ")." : result);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancellation.cancel(); cancellation.check();
            throw new IOException("Interrupted", e);
        } finally {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                try { process.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }
}
