package org.example.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

public class JiraFetcher {

    private final String baseUrl;
    private final String[] projects;
    private final int pageSize;
    private final String outputFile;
    private final String checkpointFile;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public JiraFetcher(Properties config) {
        this.baseUrl = config.getProperty("base_url");
        this.projects = config.getProperty("projects").split(",");
        this.pageSize = Integer.parseInt(config.getProperty("page_size", "100"));
        this.outputFile = config.getProperty("output_file", "issues.jsonl");
        this.checkpointFile = config.getProperty("checkpoint_file", "checkpoint.json");
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    public void fetchIssues() throws IOException {
        try (FileWriter writer = new FileWriter(outputFile, true)) {
            for (String project : projects) {
                int startAt = 0;
                boolean moreIssues = true;

                while (moreIssues) {
                    moreIssues = fetchIssuesBatch(project.trim(), startAt, writer);
                    startAt += pageSize;
                }
            }
        }
    }

    private boolean fetchIssuesBatch(String project, int startAt, FileWriter writer) throws IOException {
        String url = String.format("%s/rest/api/2/search?jql=project=%s&startAt=%d&maxResults=%d",
                baseUrl, project, startAt, pageSize);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "apache-jira-scraper-java/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to fetch " + project + " batch at " + startAt + ", status: " + response.code());
                return false;
            }

            JsonNode root = mapper.readTree(response.body().string());
            JsonNode issues = root.get("issues");
            if (issues == null || !issues.isArray() || issues.size() == 0) {
                return false;
            }

            Iterator<JsonNode> it = issues.elements();
            while (it.hasNext()) {
                JsonNode issue = it.next();
                writer.write(mapper.writeValueAsString(transformIssue(issue)) + "\n");
            }

            writer.flush();
            System.out.printf("Fetched %d issues from project %s starting at %d%n", issues.size(), project, startAt);

            return issues.size() == pageSize; // if fewer issues returned, we reached the last batch
        }
    }

    private JsonNode transformIssue(JsonNode issue) {
        // Transform raw Jira issue to structured JSON
        return mapper.createObjectNode()
                .put("project", issue.path("fields").path("project").path("key").asText())
                .put("key", issue.path("key").asText())
                .put("title", issue.path("fields").path("summary").asText())
                .put("status", issue.path("fields").path("status").path("name").asText())
                .put("priority", issue.path("fields").path("priority").path("name").asText(""))
                .put("reporter", issue.path("fields").path("reporter").path("displayName").asText(""))
                .put("assignee", issue.path("fields").path("assignee").path("displayName").asText(""))
                .put("created", issue.path("fields").path("created").asText())
                .put("updated", issue.path("fields").path("updated").asText())
                .put("description", issue.path("fields").path("description").asText(""));
    }
}