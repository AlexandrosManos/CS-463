# CS-463 Information Retrieval System

Java implementation of the HY-463 Information Retrieval Systems project
(Spring 2026). The system indexes biomedical NXML documents, answers queries
with a vector-space model, and evaluates retrieval quality.

## Features

Recursive NXML indexing, UTF-8 English/Greek processing, stop-word removal,
stemming, disk-backed inverted indexes, partial-index merging, TF-IDF cosine
search, edit-distance OOV handling, CLI/GUI search, snippets, and Bpref/AveP/
NDCG evaluation.

## Requirements

- Java 21+
- Maven 3.8+
- The local libraries in `libs/` (`Stemmer.jar` and `BioReader.jar`)
Run commands from the repository root.

## Structure

```text
src/main/java/                    Java source
src/main/resources/stopwords/    Stop-word lists
dataset/                          Collections, topics.xml, and qrels.txt
results/                          Runs, metrics, and charts
libs/                             Local runtime libraries
```

`CollectionIndex/` and Maven build output are generated files.

## Build

```bash
mvn clean package
```

The assembly plugin creates `target/queryevaluator.jar`.

## Usage

### Build the index

`org.example.WordCounter` is configured to index the small collection:

```bash
java -cp target/queryevaluator.jar org.example.WordCounter
```

For the full collection, change `dataset/MiniCollection` to
`dataset/MedicalCollection` in `WordCounter.java`, rebuild, and run again.

### Search

```bash
java -cp target/queryevaluator.jar org.search.Searcher
java -cp target/queryevaluator.jar org.search.SearcherGUI
```

The CLI supports simple searches and topic retrieval. The GUI displays paths,
snippets, and scores.

### Evaluate

Generate topic runs from the CLI, then ensure they are named
`results/results_summary.txt` and `results/results_description.txt`.

```bash
java -cp target/queryevaluator.jar org.evaluation.IRQualityEvaluator
java -cp target/queryevaluator.jar org.evaluation.Plotter
```

Evaluation files are written to `results/eval_*.txt`; charts are written to
`results/`.

## Retrieval model

```text
tf     = 1 + log2(freq)
idf    = log2(N / df) + 0.1
weight = tf * idf
score  = dot(query, document) / (||query|| * ||document||)
```

Unknown terms are reported and matched against close vocabulary terms using
bounded Levenshtein distance.

## Included results

The checked-in evaluation covers 30 topics:

| Query | Bpref | AveP | NDCG |
|---|---:|---:|---:|
| Summary | 0.5529 | 0.5320 | 0.7047 |
| Description | 0.5434 | 0.5329 | 0.7073 |

See the [general project brief](./project_2026_A-GeneralSteps_V1.pdf) and
[Phase C brief](./ΗΥ-463–PhaseC.pdf) for the assignment requirements.
