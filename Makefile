compile:
	javac -Werror -Xdiags:verbose -Xlint:unchecked Calang.java
	javac -Werror -Xdiags:verbose -Xlint:unchecked -cp . TranspileJs.java 

clean:
	rm *.class

test:
	cd examples && java -ea -cp .. Example.java minimal/ prog X "Hello world, how is it going?"
	cd examples/filesum && java -ea -cp ../.. FileSumExample.java
	java -ea -cp . TranspileJs examples/minimal/ prog subprog tower
