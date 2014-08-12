import com.restmonkeys.logger.Log;
import com.restmonkeys.logger.LogLevel;
import com.restmonkeys.logger.Logger;
import org.junit.Test;

public class Example {
    private Logger logger = null;

    public static void main(String[] args) throws Exception {
        new Example().test();
    }

    @Test
    public void test() throws Exception {
        logger = Logger.logger(Example.class);
//        logger.error("SOME ERROR");
        method1();
    }

    @Log(name = "Method1", minLevel = LogLevel.WARN, fallback = LogLevel.DEBUG)
    public void method1() throws Exception {
        logger.debug("method2 debug");
        logger.info("method2 info");
        method2();
    }

    public void method2() throws Exception {
        method3();
        throw new Exception("a");
    }

    public void method3() {
        logger.warn("method3");
    }
}
