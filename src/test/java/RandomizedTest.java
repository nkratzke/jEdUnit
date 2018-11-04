import org.junit.Test;
import java.util.stream.Stream;
import static org.junit.Assert.*;

import static de.thl.jedunit.Randomized.*;

public class RandomizedTest {

    public static int N = 1000;
    
    @Test public void testGenerateString() {
        Stream.iterate(0, l -> l + 1).limit(N / 100).forEach(l -> {
            String generated = s(String.format("[a-cA-C]{0,%d}", l));
            assertTrue("Generated string should be longer than 1.", generated.length() >= 0);
            assertTrue("Generated string should be shorter than 7.", generated.length() <= l);
            assertTrue("Generated string should not contain a 'D'.", !generated.contains("D"));
            assertTrue("Generated string should not contain a 'd'.", !generated.contains("d"));

            generated = s(0, l);
            assertTrue("Generated string should be longer than " + l, generated.length() <= l);
            assertFalse("Generated string should only contain [a-zA-Z]", generated.contains("ü"));
        });
    }

    @Test public void testGenerateBool() {
        Stream.generate(() -> 1).limit(N).forEach(t -> {
            boolean generated = b();
            assertTrue("Generated boolean should return true or false.", generated == true || generated == false);
        });
    }

    @Test public void testGenerateInteger() {
        Stream.generate(() -> 1).limit(N).forEach(t -> {
            int i = i();
            assertTrue("Generated integer should work.", Integer.MIN_VALUE <= i && i <= Integer.MAX_VALUE);
            i = i(10);
            assertTrue("Generated integer should be between 0 and upper bound.", i >= 0 && i < 10);
            i = i(-10, 10);
            assertTrue("Generated integer should be in specified borders.", i >= -10 && i < 10);
        });
    }

    @Test public void testGenerateDouble() {
        Stream.iterate(1.0, d -> d + 1.0).limit(N).forEach(d -> {
            double r = d();
            assertTrue("Generate double should work.", -Double.MAX_VALUE < r && r < Double.MAX_VALUE);
            r = d(d);
            assertTrue("Generated double should be between 0 and upper bound.", r >= 0.0 && r < d);
            r = d(-d, d);
            assertTrue("Generated double should be in specified borders.", r >= -d && r < d);
        });
    }

    @Test public void testGenerateChar() {
        Stream.generate(() -> true).limit(N).forEach(b -> {
            char c = c("[a-z]");
            assertTrue("Generated char should be in [a-z]", c >= 'a' && c <= 'z');
            c = c();
            assertTrue("Generated char should be in [a-z]", c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
        });
    }
}
