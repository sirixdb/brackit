We have to build the grammars manually until the maven plugin
for antlr3 works properly with m2eclipse. The grammars already
reside in the default directory src/main/antlr3

The antlr3 support has been removed from pom.xml to avoid problems
during the build.

To compile the grammars, go to src/main/antlr3 and type 'ant'.
The files will be generated directly into the java source tree.