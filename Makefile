compile:
	javac -Werror -Xdiags:verbose -Xlint:unchecked Calang.java

clean:
	rm *.class

test:
	cd examples && java -ea -cp .. Example.java minimal/ prog X "Hello world, how is it going?"
	cd examples/filesum && java -ea -cp ../.. FileSumExample.java
	cd examples && java -ea -cp .. TranspileJs.java minimal/ prog subprog tower
