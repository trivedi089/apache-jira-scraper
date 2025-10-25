# Apache Jira Scraper

A Java-based tool to scrape issues from Apache Jira projects (HADOOP, SPARK, MAVEN) and save them in JSONL format with checkpointing support.

## Features

- Scrapes issues from multiple Jira projects serially.
- Handles pagination and retries automatically.
- Saves issues in `issues.jsonl`.
- Checkpoints progress in `checkpoint.json` to resume interrupted runs.
- Uses configurable `page_size`, `user_agent`, and other settings via `config.properties`.

## Requirements

- Java 17+ (tested with JDK 17 & 21)
- Maven
- Internet access to Jira API

## Setup

Clone the repository:

```bash
git clone git@github.com:trivedi089/apache-jira-scraper.git
cd apache-jira-scraper
```

## Output

	•	issues.jsonl – Contains scraped issues in JSONL format.
	•	checkpoint.json – Stores the last scraped project and starting index to resume in case of interruptions.
	•	data.jsonl – Optional intermediate data file (used internally).



## Algorithms / Stepwise Workflow
	1.	Read Configurations
	•	Load config.properties to get projects, page_size, checkpoint_file, etc.
	2.	Initialize Checkpoint
	•	Read checkpoint.json if exists.
	•	Determine which project to resume and the startAt index.
	3.	Iterate Over Projects
	•	For each project in projects list (serially):
	•	Start from last checkpoint (or startAt = 0 if fresh).
	4.	Fetch Issues Batch-wise
	•	Construct Jira API URL using project key, startAt, and page_size.
	•	Send HTTP GET request with configured user_agent.
	•	Retry up to max_retries if request fails.
	5.	Process JSON Response
	•	Parse JSON response using Jackson.
	•	Extract issue fields: key, title, status, priority, reporter, assignee, labels, comments, etc.
	6.	Write to issues.jsonl
	•	Append each issue as a separate JSON object in JSONL format.
	7.	Update Checkpoint
	•	Save last_project and last_startAt to checkpoint.json.
	•	Ensures scraper can resume in case of interruption.
	8.	Repeat Until All Issues Are Fetched
	•	Increment startAt by page_size.
	•	Continue fetching batches until total issues for the project are completed.
	9.	Finish
	•	Scraper completes when all projects are processed.
	•	Prints summary to console.
