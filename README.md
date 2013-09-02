java-lar-serializable
=====================

A simple Java CLI module that performs sparse matrix many vectors (SpMMV) multiplication through OpenCL/OpenGL for the [LAR](https://github.com/cvdlab/larpy)/[LAR.js](https://github.com/cvdlab/lar-demo) project

## Requirements

* OpenCL 1.1 (or greater) supported platform(s)
* JDK 1.6 (or greater)
* Maven 3 (or greater)

###### Java Libs:

* com.nativelibs4java: javacl
* org.codehaus.jackson: jackson-core-asl, jackson-mapper-asl, jackson-jaxrs
* com.google.guava: guava
* org.slf4j: slf4j-log4j12
* commons-io
* commons-lang
* commons-cli
* org.javolution: javolution-core-java
* jcuda.driver: jcudadriver

## Installing

1. Clone this repository
2. Enter the repository directory
3. `mvn clean package shade:shade`

## Startup

###### On Windows:
1. Enter the repository directory
2. `cd target`
3. `java -d64 -Xcheck:jni -Xmx14G -XX:MaxPermSize=4G -XX:PermSize=512M -jar lar-serializable-0.0.1-SNAPSHOT.jar`

###### On Linux/MacOsx:
1. Enter the maven repository directory
2. `cd target`
3. `export LD_PRELOAD=$JAVA_HOME/jre/lib/amd64/libjsig.so` (`$JAVA_HOME` should point to your JDK installation directory)
4. `java -d64 -Xcheck:jni -Xmx14G -XX:MaxPermSize=4G -XX:PermSize=512M -jar lar-serializable-0.0.1-SNAPSHOT.jar`

## JVM Options

You might want to give more/less RAM to the JVM. You can do so by fiddling with:

1. `-Xmx14G`
2. `-XX:MaxPermSize=4G`
3. `-XX:PermSize=512M`

## Software Options

You can change the software behaviour from command line. Use `-h` switch to know how.

## License

The MIT License (MIT)

Copyright (c) 2013 Francesco Furiani

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.