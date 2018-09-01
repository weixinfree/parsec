package xin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ParameterizedTest {

    public static <F, S, T> Tuple3<F, S, T> param(F f, S s, T t) {
        return new Tuple3<>(f, s, t);
    }

    private static void test_parsec(Parsec parsec, String input, Parsec.Value expected) {
        final Parsec.Value res = parsec._parse(input, 0);
        try {
            assertThat(expected, equalTo(res));
        } catch (AssertionError e) {
            System.out.println("input = " + input);
            throw e;
        }
    }

    @SafeVarargs
    public static void parameterized_test(Tuple3<Parsec, String, Parsec.Value>... testItems) {
        for (Tuple3<Parsec, String, Parsec.Value> tuple3 : testItems) {
            test_parsec(tuple3.first, tuple3.second, tuple3.third);
        }
    }
}
