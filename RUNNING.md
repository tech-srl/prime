How to Run Prime
================

Prime can be ran in two modes; GUI and command-line. In both modes you should supply, as a regex, the predicate Prime should use to mark types as API types.

GUI mode
========
Prime is implemented as an Eclipse plugin. Using a PDE-enabled version of Eclipse, it can be invoked inside a new Eclipse instance, in which there will be a new Prime menu item and a new Prime preference page. The preferences should be changes to control the output folder. Prime should then be ran from the menu.

The two options are

* Local mode - in which source or class files are analyzed, or alternatively all the contents of a local .cached file(s) is displayed. If no .cached files are used in local mode, Prime will save a new .cached file to the output folder.
* Search mode - in which the selected code becomes the query, and the user should choose the .cached files to compare against.

Command-line mode
-----------------
The command-line mode can do everything the GUI version can do, except for compiling partial snippets - which also means that it cannot handle queries submitted as text, but it can run a pre-made hard-coded query.

All the command-line arguments could be seen by running the command-line tool (technion.prime.PrimeMain) with no arguments.
