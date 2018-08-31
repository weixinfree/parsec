package xin;

import org.junit.Test;

import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static xin.Parsec.*;

public class ParsecTest {

    @SuppressWarnings("WeakerAccess")
    public static class Tuple3<F, S, T> {
        final F first;
        final S second;
        final T third;

        public Tuple3(F first, S second, T third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
            return Objects.equals(first, tuple3.first) &&
                    Objects.equals(second, tuple3.second) &&
                    Objects.equals(third, tuple3.third);
        }

        @Override
        public int hashCode() {

            return Objects.hash(first, second, third);
        }

        @Override
        public String toString() {
            return "Tuple3{" +
                    "first=" + first +
                    ", second=" + second +
                    ", third=" + third +
                    '}';
        }
    }

    private static <F, S, T> Tuple3<F, S, T> param(F f, S s, T t) {
        return new Tuple3<>(f, s, t);
    }

    private static void test_parsec(Parsec parsec, String input, Value expected) {
        final Value res = parsec._parse(input, 0);
        try {
            assertThat(expected, equalTo(res));
        } catch (AssertionError e) {
            System.out.println("input = " + input);
            throw e;
        }
    }

    @SafeVarargs
    private static void parameterized_test(Tuple3<Parsec, String, Value>... testItems) {
        for (Tuple3<Parsec, String, Value> tuple3 : testItems) {
            test_parsec(tuple3.first, tuple3.second, tuple3.third);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_common() {
        parameterized_test(
                // char
                param(char_('h'), "hello", Value.success(1, 'h')),
                param(char_('h'), "hhh", Value.success(1, 'h')),
                param(char_('h'), "xiaoming", Value.failure(0, 'h')),

                // space
                param(space(), " ", Value.success(1, ' ')),
                param(space(), "\t", Value.success(1, '\t')),
                param(space(), "\r", Value.success(1, '\r')),
                param(space(), "\b", Value.failure(0, "a space")),
                param(space(), "a", Value.failure(0, "a space")),
                param(space(), "1", Value.failure(0, "a space")),
                param(space(), "\n", Value.success(1, '\n')),

                // letter
                param(letter(), "abc", Value.success(1, 'a')),
                param(letter(), "小明", Value.success(1, '小')),

                // digit
                param(digit(), "0", Value.success(1, '0')),
                param(digit(), "1", Value.success(1, '1')),
                param(digit(), ".", Value.failure(0, "a digit")),
                param(digit(), "a", Value.failure(0, "a digit")),

                // oneOf
                param(oneOf("1a$"), "1a$", Value.success(1, '1')),
                param(oneOf("1a$"), "a$", Value.success(1, 'a')),
                param(oneOf("1a$"), "$", Value.success(1, '$')),
                param(oneOf("1a$"), "%", Value.failure(0, "one of 1a$")),
                param(oneOf("1a$"), "23456", Value.failure(0, "one of 1a$")),

                // noneOf
                param(noneOf("1a$"), "1a$", Value.failure(0, "none of 1a$")),
                param(noneOf("1a$"), "a$", Value.failure(0, "none of 1a$")),
                param(noneOf("1a$"), "$", Value.failure(0, "none of 1a$")),
                param(noneOf("1a$"), "%", Value.success(1, '%')),


                // string
                param(string("hello"), "hello world", Value.success(5, "hello")),
                param(string("hello"), "helllo", Value.failure(4, "hello")),
                param(string("{[("), "{[()]}", Value.success(3, "{[(")),
                param(string("hello"), "xxhello", Value.failure(0, "hello")),

                // spaces
                param(spaces(), "   ", Value.success(3, "   ")),
                param(spaces(), " \t\n ", Value.success(4, " \t\n ")),
                param(spaces(), " \t\r\n ", Value.success(5, " \t\r\n ")),
                param(spaces(), "   1  ", Value.success(3, "   ")),
                param(spaces(), "1  ", Value.success(0, "")),

                // regex
                param(regex("\\d+"), "123", Value.success(3, "123")),
                param(regex("\\d+"), "123abc", Value.success(3, "123")),
                param(regex("\\d+"), "1", Value.success(1, "1")),
                param(regex("abc\\d+"), "abc1", Value.success(4, "abc1")),
                param(regex("\\d+abc"), "123abc", Value.success(6, "123abc")),
                param(regex("abc\\d+"), "abc123", Value.success(6, "abc123")),
                param(regex("\\d+"), "abc213", Value.failure(0, "^\\d+")),

                // eof
                param(eof(), "", Value.success(0, null)),
                param(eof(), "1", Value.failure(0, "EOF"))
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    // 
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_choice() {
        parameterized_test(
                param(choice(string("xm"), string("abc")), "xm", Value.success(2, "xm")),
                param(choice(string("xm"), string("abc")), "abc", Value.success(3, "abc")),
                param(choice(string("xm"), string("abc")), "xabc", Value.failure(1, "xm")),
                param(choice(string("xm"), string("abc")), "hhhh", Value.failure(0, "[xm, abc]")),
                param(choice(string("xm"), string("abc")), "abx", Value.failure(2, "abc")),
                param(choice(string("xm"), regex("ab."), string("abc")), "abx", Value.success(3, "abx"))
        );
    }

    @Test
    public void test_tryChoice() {
        parameterized_test(
                param(tryChoice(string("xm"), string("abc")), "xm", Value.success(2, "xm")),
                param(tryChoice(string("xm"), string("abc")), "abc", Value.success(3, "abc")),
                param(tryChoice(string("xm"), string("abc")), "xabc", Value.failure(0, "[xm, abc]")),
                param(tryChoice(string("xm"), string("abc")), "hhhh", Value.failure(0, "[xm, abc]")),
                param(tryChoice(string("xm"), string("abc")), "abx", Value.failure(0, "[xm, abc]")),
                param(tryChoice(string("xm"), string("abc"), regex("ab.")), "abx", Value.success(3, "abx"))
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    // 
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_compose() {

    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_separated() {

    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_optional() {

    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_joint() {
        parameterized_test(
                param(joint(string(">>"), regex("\\d+"), string("<<")),
                        ">>12345<<",
                        Value.success(9, asList(">>", "12345", "<<"))),
                param(joint(string(">>"), regex("\\d+"), string("<<")),
                        ">>>1234<<",
                        Value.failure(2, "^\\d+"))
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_times() {

        parameterized_test(
                // count
                param(count(string("xm"), 1), "xmxmxm", Value.success(2, singletonList("xm"))),
                param(count(string("xm"), 2), "xmxmxm", Value.success(4, asList("xm", "xm"))),
                param(count(string("xm"), 3), "xmxmxm", Value.success(6, asList("xm", "xm", "xm"))),
                param(count(string("xm"), 4), "xmxmxmx", Value.failure(7, "match xm between [4,4] times")),

                // many
                param(many(string("xm")), "xxmxmxm", Value.success(0, emptyList())),
                param(many(string("xm")), "xmxmxm", Value.success(6, asList("xm", "xm", "xm"))),
                param(many(string("xm")), "xmxmxmxm", Value.success(8, asList("xm", "xm", "xm", "xm"))),
                param(many(string("xm")), "xm", Value.success(2, singletonList("xm"))),
                param(many(string("xm")), "x", Value.success(0, emptyList())),

                // many1
                param(many1(string("xm")), "xm", Value.success(2, singletonList("xm"))),
                param(many1(string("xm")), "xmxm", Value.success(4, asList("xm", "xm"))),
                param(many1(string("xm")), "xmxmxmx", Value.success(6, asList("xm", "xm", "xm"))),
                param(many1(string("xm")), "xxm", Value.failure(1, "match xm between [1,2147483647] times")),

                // times
                param(times(string("xm"), 1, 3), "xm", Value.success(2, singletonList("xm"))),
                param(times(string("xm"), 1, 3), "xmxm", Value.success(4, asList("xm", "xm"))),
                param(times(string("xm"), 1, 3), "xmxmxm", Value.success(6, asList("xm", "xm", "xm"))),
                param(times(string("xm"), 1, 3), "xmxmxmxm", Value.success(6, asList("xm", "xm", "xm"))),
                param(times(string("xm"), 1, 3), "xmxmxmxmxm", Value.success(6, asList("xm", "xm", "xm"))),
                param(times(string("xm"), 1, 3), "x", Value.failure(1, "match xm between [1,3] times"))
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////


    @Test
    public void test_result() {
        parameterized_test(
                param(string("#f").result(false), "#ffff", Value.success(2, false)),
                param(string("#t").result(true), "#t", Value.success(2, true)),
                param(string("#t").result(true), "#f", Value.failure(1, "#t"))
        );
    }

    @Test
    public void test_map() {
        parameterized_test(
                param(regex("\\d+").map(Integer::parseInt), "12345", Value.success(5, 12345)),
                param(regex("\\d+\\.\\d+").map(Double::parseDouble), "123.45", Value.success(6, 123.45))
        );
    }
}