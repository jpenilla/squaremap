package net.pl3x.map.plugin.util;

import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.task.Progress;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class JarDownloader {
    public boolean downloadJar(String url, File destination) {
        try {
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
                progress.add(bytesRead);
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }

        progress.interrupt();
    }
}
