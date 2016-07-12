prime
=====

Prime is a tool used for analyzing and extracting information from a large number of code snippets. It is intended to mine code snippets that all use the same API, and then use the mined information to answer queries about the usage of the API in those snippets.

## What Can Prime Be Used For

Prime can be used for:

* Code search, by providing a partial program as a query.
* Specification mining - to learn how the API is supposed to be used.
* Verification - to verify that a piece of code uses the API in one of the common ways it is used by other snippets.
* Completion - to complete the missing parts in a code snippet based on information from the mined snippets.

## Prime as Code Search Engine

### Input

One or more cached files.
To prepare a search query, either use a code snippet which will be analyzed by Prime, or write down the query in the code itself using EdgeHistoryBuilder.

### Output

A HistoryCollection containing the search results, each with an associated "support" (number of samples represented by it) and "score" (the path's probability in the automata).

## Examples from OOPSLA 2012 Paper

The OOPSLA 2012 paper submission contained a collection of results. We have prepared a list here of some of them along with a few others. There are also instructions on how to reproduce these results yourself, on identical or different inputs.

## Usage Instructions

### Using ready-made "Prime in a Box"
If you don't want to bother installing anything and don't mind downloading a 3GB file, just use our Prime in a Box edition, in which you download a complete VM.

### Manual Install
How to install Prime
How to get Prime running

##License
Prime is released under the Apache 2.0 License.
