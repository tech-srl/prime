How to export a product?
========================
You need to "export an eclipse product". The configuration is already prepared so the process is pretty self-explanatory, but here are the detailed instructions in any case:

1. Select the Prime project.
2. Select "export" (either from right-click menu or from the file menu).
3. Select "eclipse product" (it's in the plug-in development category).
   * "Product Configuration" should be "Prime.PrimeProduct.product"
   * Leave "synchronize before exporting" unchecked.
   * Select a new directory as a destination. It seems that exporting over a previous export doesn't work.
   * Check "export for multiple platforms" and "allow for binary cycles in target platform".
   * Leave "generate metadata repository" unchecked.
4. Click "Next".
   * Check all the platforms you want to support.
5. Click "Finish". The process takes a few minutes.

In the designated destination folder there will be a different folder for each of the requested platforms. The folders are independent.

Note about command-line execution
---------------------------------
On Windows, trying to run the generated eclipse.exe file will open a new command-line console instead of reusing the existing one.
To circumvent that, it's possible to copy the eclipsec.exe file from the real eclipse program folder into generated folder, and running it instead.

How to install?
===============
To "install" on a specific platform, just copy the folder generated for the platform to the target machine.
The only dependency required is a JRE.

How to run?
===========
1. Run the eclipsec.exe executable. If you don't provide command-line arguments it will give you an explanation of the possible parameters. Don't forget to specify "-o" to tell it where to put the output!
2. It supports JVM arguments such as -Xmx1400m (that's for 1400 MB heap).
3. It will pollute the current directory with temporary data. If you don't want that, tell it to use a different folder by specifying it with -t. For instance "-t %TEMP%/prime/temp".
   * If you run multiple instances in parallel, you should use different "-t" and "-o" folders.

For more command-line options, see the PrimeMain class.

What is the output format?
==========================
After each run, the output folder will contain:
* A .cached file, caching the result of all the stages except clustering. You can load a cache file for re-run by using the -f command-line parameter.
* A bunch of "layer" folders, each representing the mined histories after the specific layer. Each layer folder contains:
  * A "dot" folder containing dot files of the clustered results.
  * An "xml" folder containing xml files of the clustered results.

It will also print debug information and general progress information to stdout and stderr.