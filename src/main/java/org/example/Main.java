package org.example;

import org.example.fetcher.JiraFetcher;

import java.util.Properties;

public class Main {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        config.load(Main.class.getResourceAsStream("/config.properties"));

        JiraFetcher fetcher = new JiraFetcher(config);
        fetcher.fetchIssues();

        System.out.println("Scraping completed successfully! Check data.jsonl for output.");
    }
}