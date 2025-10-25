package org.example.utils;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

import org.jsoup.Jsoup;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpUtils {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static String safeGet(String url, int maxRetries) throws IOException, InterruptedException {
        int retries = 0;

        while (retries <= maxRetries) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "apache-jira-scraper-java/1.0")
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return response.body();
                } else if (response.statusCode() == 429 || response.statusCode() >= 500) {
                    long sleepMs = (long) Math.pow(2, retries) * 1000 + (long)(Math.random()*500);
                    Thread.sleep(sleepMs);
                    retries++;
                } else {
                    throw new IOException("HTTP Error: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                retries++;
                if (retries > maxRetries) throw e;
                long sleepMs = (long) Math.pow(2,retries) * 1000;
                Thread.sleep(sleepMs);
            }
        }
        throw new IOException("Max retries reached for URL: " + url);
    }

    public static String htmlToText(String html) {
        if(html==null) return "";
        return Jsoup.parse(html).text();
    }

    public static void saveCheckpoint(Map<String, Object> checkpoint, String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File temp = new File(path + ".tmp");
        mapper.writeValue(temp, checkpoint);
        temp.renameTo(new File(path));
    }

    public static Map<String, Object> loadCheckpoint(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(path);
        if (!file.exists()) return null;
        return mapper.readValue(file, Map.class);
    }
}