import com.restmonkeys.logger.Log;
import com.restmonkeys.logger.LogLevel;
import com.restmonkeys.logger.Logger;

public class Example {
    private Logger logger = null;

    public static void main(String[] args) throws Exception {
        new Example().test();
    }

    public void test() throws Exception {
        logger = Logger.logger(Example.class);
        logger.error("SOME ERROR Message - that message is out of scope.");
        method11();
    }

    @Log(name = "Method1", minLevel = LogLevel.WARN, fallback = LogLevel.DEBUG)
    public void method1() throws Exception {
        logger.debug("method2 debug");
        for (int i = 0; i < 150; i++) {
            logger.debug("method2 info i = " + i);
        }
        method2();
    }

    @Log(name = "Method1-1", minLevel = LogLevel.WARN, fallback = LogLevel.DEBUG)
    public void method11() throws Exception {
        logger.debug("method11 debug message - should be skipped, belongs to different log scope");
        method1();
    }

    public void method2() throws Exception {
        method3();
        try {
            throw new Exception("a");
        } catch (Exception e) {
            logger.fallback("Fallback in method2", e);
        }
    }

    public void method3() {
        logger.debug("method3 debug - 1 First debug message from method3");
        logger.warn("method3 - that is warning message that should normally be in logs");
        logger.debug("method3 debug - 2 Second debug message from message3");
    }
}
