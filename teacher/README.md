# Priority and JMP study desk

A browser course for two subjects. **JMP** (SAS) is the first course, and the one the desk opens on. **Priority** (Priority Software, the Israeli ERP) is the second course, and the longer one.

This directory is separate from the Kotlin app at the repository root. It does not use Gradle.

## Run

From this directory:

```bash
python3 -m http.server 8765
```

Open http://127.0.0.1:8765

There is no login. Checks and the lesson you last opened are stored in this browser (`localStorage`).

## What you study

JMP, first: Statistical Thinking for Industrial Problem Solving, modules 0 through 7, in that order. Frame a problem, explore the table, separate signal from noise, decide with intervals and tests, fit a relationship, design an experiment, then predict and read text.

Priority, second and longer: how a company is organized in the system, how a form actually behaves, customers and the part card, the sales chain, the purchasing chain, warehouse balances, planning, and what finalizing posts to the books.
