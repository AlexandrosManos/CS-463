# Biomedical Information Retrieval System

A search engine built from scratch in Java for a collection of biomedical articles, developed for
CS-463 Information Retrieval Systems (University of Crete, Spring 2026).

## Features
- **Indexer** – Parses the XML articles, removes stopwords and stems English and Greek terms, then
  builds a disk-based inverted index (vocabulary, postings and document files). Large collections are
  handled by writing partial indexes and combining them with a k-way merge; postings are read
  through random-access files.
- **Query evaluator** – Ranks documents with the vector space model (TF-IDF, cosine similarity),
  returns a snippet for each result, and corrects out-of-vocabulary query terms using edit distance.
  Available as both a command-line tool and a Swing GUI.
- **IRQualityEvaluator** – Runs the medical topics through the system, compares the results with the
  relevance judgments (`qrels.txt`) and reports **bpref**, **AveP** and **nDCG** per topic, with
  comparison charts.

## Project structure
| Package | Contents |
|---|---|
| `org.example` | Document parsing, text analysis and indexing (partial indexes and merging) |
| `org.search` | Query evaluation, snippets, OOV handling and the GUI |
| `org.evaluation` | Evaluation metrics and plots |

## Running
Build the JARs from IntelliJ (*Build → Build Artifacts*), then:
```bash
java -jar PhaseA.jar              # build the index
java -jar PhaseB_CLI.jar          # search from the terminal
java -jar PhaseB_GUI.jar          # search with the GUI
java -jar IRQualityEvaluator.jar  # evaluate against qrels.txt
```
