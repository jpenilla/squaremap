package net.pl3x.map.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import net.pl3x.map.Logger;
import net.pl3x.map.configuration.Lang;

public class JarDownloader {
    public boolean downloadJar(String url, File destination) {
        try {
            if (!destination.getParentFile().exists()) {
                if (!destination.getParentFile().mkdirs()) {
                    Logger.warn(Lang.LOG_COULD_NOT_CREATE_DIR
                            .replace("{path}", destination.getParentFile().getAbsolutePath()));
                }
            }

            if (destination.exists() && destination.isDirectory()) {
                Files.delete(destination.toPath());
            }

            if (!destination.exists()) {
                Logger.info(Lang.LOG_JARLOADER_DOWNLOADING
                        .replace("{url}", url));
                download(url, destination);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void download(String url, File file) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);

        boolean redirect;

        do {
            int status = conn.getResponseCode();
            redirect = status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER;

            if (redirect) {
                String newUrl = conn.getHeaderField("Location");
                String cookies = conn.getHeaderField("Set-Cookie");

                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("Cookie", cookies);
                conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            }
        } while (redirect);

        Progress progress = new Progress(conn.getContentLength());
        progress.start();

        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                progress.current += bytesRead;
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }

        progress.interrupt();
    }

    private static class Progress extends Thread {
        private final long total;
        private long current;

        private Progress(long total) {
            this.total = total;
        }

        @Override
        public void run() {
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    logProgress();
                    //noinspection BusyWait
                    sleep(1000);
                }
            } catch (InterruptedException ignore) {
            }
        }

        @Override
        public void interrupt() {
            logProgress();
            super.interrupt();
        }

        private void logProgress() {
            Logger.info(Lang.LOG_JARLOADER_PROGRESS
                    .replace("{percent}", Integer.toString((int) ((((double) current) / ((double) total)) * 100D)))
                    .replace("{current}", Long.toString(current))
                    .replace("{total}", Long.toString(total)));
        }
    }
}
