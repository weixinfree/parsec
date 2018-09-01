package xin;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static xin.ParameterizedTest.param;
import static xin.ParameterizedTest.parameterized_test;
import static xin.Parsec.*;

public class ParsecTest {

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

                param(charMatcher(Character::isLetterOrDigit, ""), "1", Value.success(1, '1')),
                param(charMatcher(Character::isLetterOrDigit, ""), "x", Value.success(1, 'x')),

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
                param(regex(Pattern.compile("\\d+")), "123", Value.success(3, "123")),
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
        parameterized_test(
                param(char_('a').compose(string("bc")), "abc", Value.success(3, "bc")),
                param(char_('-').compose(regex("\\d+")).map(Integer::parseInt), "-1234", Value.success(5, 1234)),
                param(string("0x").compose(regex("[0-9a-fA-F]+")).map(s -> Integer.parseInt(s, 16)), "0xFFFF", Value.success(6, 0xFFFF)),
                param(char_('-').compose(regex("\\d+")).map(Integer::parseInt), "+1234", Value.failure(0, '-')),
                param(char_('-').compose(regex("\\d+")).map(Integer::parseInt), "-a1234", Value.failure(1, "^\\d+"))
        );
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_separated() {
        final Parsec<List> p = sepBy(regex("\\d+").map(Integer::parseInt), char_(','));
        final Parsec<List> p2 = sepBy1(regex("\\d+").map(Integer::parseInt), char_(','));
        final Parsec<List> p3 = separated(regex("\\d+").map(Integer::parseInt), char_(','), 2, 2);
        parameterized_test(
                // sep
                param(p, "1,2,23,456", Value.success(10, asList(1, 2, 23, 456))),
                param(p, "1", Value.success(1, singletonList(1))),
                param(p, "1,", Value.failure(2, "^\\d+")),
                param(p, "1, 2,23,456", Value.failure(2, "^\\d+")),
                param(p, "1,2 ,23,456", Value.success(3, asList(1, 2))),

                // sep1
                param(p2, "1,2 ,23,456", Value.success(3, asList(1, 2))),
                param(p2, "1,2,34,567", Value.success(10, asList(1, 2, 34, 567))),
                param(p2, "1,2,3 4,567", Value.success(5, asList(1, 2, 3))),
                param(p2, "1,2", Value.success(3, asList(1, 2))),
                param(p2, "1,2", Value.success(3, asList(1, 2))),
                param(p2, "9", Value.failure(1, ',')),
                param(p2, "1,", Value.failure(2, "^\\d+")),

                // separated
                param(p2, "1,", Value.failure(2, "^\\d+"))

        );
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Test
    public void test_optional() {
        parameterized_test(
                param(optional(regex("[+-]")), "+100", Value.success(1, "+")),
                param(optional(regex("[+-]")), "-100", Value.success(1, "-")),
                param(optional(regex("[+-]")), "100", Value.success(0, null)),
                param(joint(optional(regex("[+-]")), regex("\\d+").map(Integer::parseInt)), "+100", Value.success(4, asList("+", 100))),
                param(joint(optional(regex("[+-]")), regex("\\d+").map(Integer::parseInt)), "-100", Value.success(4, asList("-", 100)))
        );
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
                param(regex("\\d+\\.\\d+").map(Double::parseDouble), "123.45", Value.success(6, 123.45)),
                param(regex("abc").map(Double::parseDouble), "123.45", Value.failure(0, "^abc"))
        );
    }

    @Test
    public void test_skip() {
        parameterized_test(
                param(string("xm").skip(eof()), "xm", Value.success(2, "xm")),
                param(string("xm").skip(eof()), "xxm", Value.failure(1, "xm")),
                param(string("xm").skip(eof()), "xm and xh", Value.failure(2, "EOF"))
        );
    }

    @Test
    public void test_parse() {
        final String res = string("xm").parse("xm and xh");
        assertThat(res, equalTo("xm"));

    }

    @Test(expected = ParseException.class)
    public void test_parse_failed() {
        string("xm").parse("xh");
    }

    @Test(expected = ParseException.class)
    public void test_parseStrict() {
        string("xm").parseStrict("xm and xh");
    }

    @Test
    public void test_hashCode() {
        final HashMap<Value, Object> map = new HashMap<>();
        map.put(Value.success(1, "hello"), "hello");

        assertThat(Value.success(1, "xm"), equalTo(Value.success(1, "xm")));
    }

    @Test
    public void test_toString() {
        final String res = Value.failure(1, "hhh").toString();
        System.out.println("res = " + res);
    }
}