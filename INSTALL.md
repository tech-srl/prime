#Installing Prime

Prime is implemented as an Eclipse plugin. It can either be ran from within Eclipse, or as a stand-alone command-line tool.

##Prerequisites

* Eclipse 3.6 with PDE or Eclipse 3.7 PDE.
* Some sort of Subversion client, e.g. Eclipse's [Subclipse plugin](subclipse.tigris.org/).

##Download Prime
Check out the "State" branch of Prime: svn://svn.code.sf.net/p/priming/code-0/branches/State

##Download the PPA plugin
###How to obtain the PPA plugin projects###
The PPA projects can be downloaded from https://bitbucket.org/barthe/ppa/downloads
In addition, in order to build it, a log4j bundle must be downloaded. One is provided alongside the Prime repository.

###Fix for Eclipse 3.7
There was a typo in a class and interface in Eclipse 3.6, which was fixed in 3.7. PPA uses that class, so in order for it to work with Eclipse 3.7 you need to fix all occurrences of that class/interface accordingly:

    INameEnviromentWithProgress  ->  INameEnvironmentWithProgress
    NameEnviromentWithProgress   ->  NameEnvironmentWithProgress

These occurrences appear in the class `org.eclipse.jdt.core.dom.PPACompilationUnitResolver`, in the project `ca.mcgill.cs.swevo.ppa`. This fix is not required when using Eclipse 3.6.

###Fix to enable timeouts / cancel requests
PPA will not respond to timeouts or cancel requests; to enable it to respond to them, the following modification should be made:
File: `org.eclipse.jdt.core.dom.PPABindingUtil`
Method: `public static boolean isUnknownType(ITypeBinding typeBinding)` - it appears around line 1050  
Insert at start of method:
```
    if (Thread.interrupted()) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(new InterruptedException());
    }
```
