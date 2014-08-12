package com.restmonkeys.logger;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Logger {
    // TODO add caching for methods by stack trace;
    org.slf4j.Logger backLogger;
    private Class clazz;

    private static Map<String, List<LogEntity>> logStack = new HashMap<String, List<LogEntity>>();

    public static Logger logger(Class clazz) {
        Logger logger = new Logger(clazz);
        logger.clazz = clazz;
        return logger;
    }

    private Logger(Class clazz) {
        this.backLogger = LoggerFactory.getLogger(clazz);
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler eh = new LoggerExceptionHandler(uncaughtExceptionHandler);

        Thread.currentThread().setUncaughtExceptionHandler(eh);
    }

    public class LoggerExceptionHandler implements Thread.UncaughtExceptionHandler {

        private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        public LoggerExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            if (uncaughtExceptionHandler != null && !(uncaughtExceptionHandler instanceof LoggerExceptionHandler)) {
                this.uncaughtExceptionHandler = uncaughtExceptionHandler;
            }
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            StackTraceElement[] stackTrace = e.getStackTrace();

            Log log = getLog(stackTrace);
            if (log == null) {
                e.printStackTrace();
            } else {
                List<LogEntity> logEntities = logStack.get(log.name());
                LogLevel.FALLBACK.log(backLogger, new LogEntity("Fallback Logs start: ", LogLevel.FALLBACK));

                for (LogEntity logEntity : logEntities) {
                    LogLevel.FALLBACK.log(backLogger, logEntity);
                }
                logStack.remove(log.name());
            }
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(t, e);
            }
        }


    }

    public void info(String message) { // todo do not add if level less then fallback
        addLogMessage(message, LogLevel.INFO);
    }

    public void addLogMessage(String message, LogLevel level) {
        Log log = getLog(Thread.currentThread().getStackTrace());
        if (log == null) {
            level.log(backLogger, new LogEntity(message, level));
        } else if (log.minLevel().compare(level) != 1) {
            level.log(backLogger, new LogEntity(message, level));
        } else if (log.fallback().compare(level) != 1) {
            if (!logStack.containsKey(log.name())) {
                logStack.put(log.name(), new ArrayList<LogEntity>());
            }
            logStack.get(log.name()).add(new LogEntity(message, level));
        }
    }

    public void debug(String message) {
        addLogMessage(message, LogLevel.DEBUG);

    }

    public void warn(String message) {
        addLogMessage(message, LogLevel.WARN);

    }

    public void error(String message) {
        addLogMessage(message, LogLevel.ERROR);

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

    private static Log getLog(StackTraceElement[] stackTrace) {
        for (StackTraceElement stackTraceElement : stackTrace) {
            try {
                Method method = getMethod(stackTraceElement);
                if (method == null) {
                    return null;
                }
                Log annotation = method.getAnnotation(Log.class);
                if (annotation != null) {
                    return annotation;
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public static Method getMethod(final StackTraceElement stackTraceElement) throws Exception {
        final String stackTraceClassName = stackTraceElement.getClassName();
        final String stackTraceMethodName = stackTraceElement.getMethodName();
        final int stackTraceLineNumber = stackTraceElement.getLineNumber();
        Class<?> stackTraceClass = Class.forName(stackTraceClassName);

        // I am only using AtomicReference as a container to dump a String into, feel free to ignore it for now
        final AtomicReference<String> methodDescriptorReference = new AtomicReference<String>();

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
                                public void visitLineNumber(int i, jdk.internal.org.objectweb.asm.Label label) {
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
//            throw new RuntimeException("Could not find line " + stackTraceLineNumber);
            return null;
        }

        for (Method method : stackTraceClass.getDeclaredMethods()) {
            if (stackTraceMethodName.equals(method.getName()) && methodDescriptor.equals(Type.getMethodDescriptor(method))) {
                return method;
            }
        }

//        throw new RuntimeException("Could not find the calling method");
        return null;
    }
}
