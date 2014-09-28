package com.restmonkeys.logger;

import com.restmonkeys.logger.collections.LimitedSizeList;
import com.restmonkeys.logger.collections.LimitedSizeMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Logger {
    private static Map<String, List<LogEntity>> logStack = Collections.synchronizedMap(new LimitedSizeMap<>());
    private static Map<StackTraceElement, Log> logCache = Collections.synchronizedMap(new LimitedSizeMap<>());
    private static org.slf4j.Logger loggersLogger = LoggerFactory.getLogger(Logger.class);

    org.slf4j.Logger backLogger;
    private Class clazz;

    private Logger(Class clazz) {
        this.clazz = clazz;
        this.backLogger = LoggerFactory.getLogger(clazz);
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler eh = new LoggerExceptionHandler(uncaughtExceptionHandler);
        Thread.currentThread().setUncaughtExceptionHandler(eh);
    }

    /**
     * Factory method to get Logger instance
     *
     * @param clazz Type of object that will use logger
     */
    public static Logger logger(Class clazz) {
        return new Logger(clazz);
    }

    private static Log getLog(StackTraceElement[] stackTrace) {
        for (StackTraceElement stackTraceElement : stackTrace) {
            Log log = logCache.get(stackTraceElement);
            if (log != null) {
                return log;
            }
            try {
                Method method = getMethod(stackTraceElement);
                if (method == null) {
                    return null;
                }
                Log annotation = method.getAnnotation(Log.class);
                if (annotation != null) {
                    logCache.put(stackTraceElement, annotation);
                    return annotation;
                }
            } catch (Exception e1) {
                loggersLogger.error("Unexpected error happened", e1);
            }
        }
        return null;
    }

    private static Method getMethod(final StackTraceElement stackTraceElement) throws Exception {
        final String stackTraceClassName = stackTraceElement.getClassName();
        final String stackTraceMethodName = stackTraceElement.getMethodName();
        final int stackTraceLineNumber = stackTraceElement.getLineNumber();
        Class<?> stackTraceClass = Class.forName(stackTraceClassName);

        final AtomicReference<String> methodDescriptorReference = new AtomicReference<>();
        String classFileResourceName = "/" + stackTraceClassName.replaceAll("\\.", "/") + ".class";
        InputStream classFileStream = stackTraceClass.getResourceAsStream(classFileResourceName);

        if (classFileStream == null) {
            throw new RuntimeException("Could not acquire the class file containing for the calling class");
        }

        try {
            ClassReader classReader = new ClassReader(classFileStream);
            classReader.accept(
                    new ClassVisitor(Opcodes.ASM5) {
                        @Override
                        public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
                            if (!name.equals(stackTraceMethodName)) {
                                return null;
                            }

                            return new MethodVisitor(Opcodes.ASM5) {

                                @Override
                                public void visitLineNumber(int i, org.objectweb.asm.Label label) {
                                    if (i == stackTraceLineNumber) {
                                        methodDescriptorReference.set(desc);
                                    }
                                }

                            };
                        }
                    },
                    0
            );

        } finally {
            classFileStream.close();
        }

        String methodDescriptor = methodDescriptorReference.get();
        if (methodDescriptor == null) {
            return null;
        }
        for (Method method : stackTraceClass.getDeclaredMethods()) {
            if (stackTraceMethodName.equals(method.getName()) && methodDescriptor.equals(Type.getMethodDescriptor(method))) {
                return method;
            }
        }
        return null;
    }


    private void addLogMessage(String message, LogLevel level, Throwable e) {
        Log log = getLog(Thread.currentThread().getStackTrace());
        if (log == null) {
            level.log(backLogger, new LogEntity(message, level), e);
        } else if (log.minLevel().compare(level) != 1) {
            level.log(backLogger, new LogEntity(message, level), e);
        } else if (log.fallback().compare(level) != 1) {
            if (!logStack.containsKey(log.name())) {
                logStack.put(log.name(), Collections.synchronizedList(new LimitedSizeList<>()));
            }
            logStack.get(log.name()).add(new LogEntity(message, level));
        }
    }

    /**
     * Log message with info level
     *
     * @param message String with message
     */
    public void info(String message) {
        addLogMessage(message, LogLevel.INFO, null);
    }

    /**
     * Log message with info level
     *
     * @param message String with message
     * @param e       passed exception
     */
    public void info(String message, Throwable e) {
        addLogMessage(message, LogLevel.INFO, e);
    }

    /**
     * Log message with debug level
     *
     * @param message String with message
     */
    public void debug(String message) {
        addLogMessage(message, LogLevel.DEBUG, null);
    }

    /**
     * Log message with debug level
     *
     * @param message String with message
     * @param e       passed exception
     */
    public void debug(String message, Throwable e) {
        addLogMessage(message, LogLevel.DEBUG, e);
    }

    /**
     * Log message with warn level
     *
     * @param message String with message
     */
    public void warn(String message) {
        addLogMessage(message, LogLevel.WARN, null);
    }

    /**
     * Log message with warn level
     *
     * @param message String with message
     * @param e       passed exception
     */
    public void warn(String message, Throwable e) {
        addLogMessage(message, LogLevel.WARN, e);
    }

    /**
     * Log message with error level
     *
     * @param message String with message
     */
    public void error(String message) {
        addLogMessage(message, LogLevel.ERROR, null);
    }

    /**
     * Log message with error level
     *
     * @param message String with message
     * @param e       passed exception
     */
    public void error(String message, Throwable e) {
        addLogMessage(message, LogLevel.ERROR, e);
    }

    /**
     * This method invokes manual flushing fallback logs to file
     *
     * @param message String with message
     */
    public void fallback(String message) {
        logFallback(message, null);
    }

    public void fallback(String message, Throwable e) {
        logFallback(message, e);
    }

    private void logFallback(String message, Throwable e) {
        StackTraceElement[] stackTrace;
        if (e != null) {
            stackTrace = e.getStackTrace();
        } else {
            stackTrace = Thread.currentThread().getStackTrace();
        }
        Log log = getLog(stackTrace);
        if (log == null && e != null) {
            loggersLogger.error("Logger not found but exception happened", e);
        } else if (log != null) {
            List<LogEntity> logEntities = logStack.get(log.name());
            LogLevel.FALLBACK.log(backLogger, new LogEntity(message, LogLevel.FALLBACK), null);

            for (LogEntity logEntity : logEntities) {
                LogLevel.FALLBACK.log(backLogger, logEntity, null);
            }
            logStack.remove(log.name());
        }
    }

    public static class LogEntity {
        private String message;
        private LogLevel level;

        public LogEntity(String message, LogLevel level) {
            this.message = message;
            this.level = level;
        }

        @Override
        public String toString() {
            return level.name() + " " + message;
        }
    }

    private class LoggerExceptionHandler implements Thread.UncaughtExceptionHandler {

        private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        public LoggerExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            if (uncaughtExceptionHandler != null && !(uncaughtExceptionHandler instanceof LoggerExceptionHandler)) {
                this.uncaughtExceptionHandler = uncaughtExceptionHandler;
            }
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logFallback("Fallback Logs start: ", e);
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(t, e);
            }
        }
    }
}
