ReversLogs
==========

This ReversLogs project was created to help to collect more useful information from your application logs.
Imagine one of your applications in production environment. 
To reduce size of logs you probably use **WARNING** as a default log level. 
And at one sunny day unexpected **EXCEPTION** has occurred! And all you have to debug it just a stacktrace.

The ReversLogs project handles such situations. 
In case of trouble it will provide all related log messages bounded with defined scope and levels. 
You will know what your application have done before crash.

# Installation
## Maven
You could find this library in Maven Central repo:

```
<dependency>
  <groupId>com.restmonkeys</groupId>
  <artifactId>logger</artifactId>
  <version>1.0.0</version>
</dependency>
```
## Usage
For standalone java application you don't need to do any additional setup - just start using it!
By default Logger set UncaughtExceptionHandler for each Thread it used. 

For web application you could use some kind of Filter or Interceptor like following:

```
void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    try {
        chain.doFilter(request, response);
    } catch(Exception e) {
        Logger.logger(MyFilter.class).fallback("Something bad happened", e);    
    }
}
```

Also you could use *fallback* method to flush fallback logs manually in any time(for example in catch block).

You could find some examples in test folder.