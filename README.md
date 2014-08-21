reverslogs
==========

This ReversLogs project was created to help to collect more useful information from your application logs.
Imagine one of your applications in production environment. To reduce size of logs you probably use WARNING as a default log level. And at one sunny day unexpected EXCEPTION has occured! And all you have to debug it just a stacktrace.

The ReversLogs project handles such situations. In case of trouble it will provide all related log messages bounded with defined scope and levels. You will know what your application have done before crash.

## Maven
```
<dependency>
  <groupId>com.restmonkeys</groupId>
  <artifactId>logger</artifactId>
  <version>1.0.0</version>
</dependency>
```
