package xin.json;

import xin.Parsec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static xin.Parsec.*;

public class JsonParser {

    private static final Parsec<String> ignore = spaces();

    private static <T> Parsec<T> lexeme(Parsec<T> parsec) {
        return parsec.skip(ignore);
    }

    private static final Parsec<String> lbrace = lexeme(string("{"));
    private static final Parsec<String> rbrace = lexeme(string("}"));
    private static final Parsec<String> lbrack = lexeme(string("["));
    private static final Parsec<String> rbrack = lexeme(string("]"));

    private static final Parsec<String> colon = lexeme(string(":"));
    private static final Parsec<String> comma = lexeme(string(","));
    private static final Parsec<Boolean> true_ = lexeme(string("true")).result(true);
    private static final Parsec<Boolean> false_ = lexeme(string("false")).result(false);
    private static final Parsec<Object> null_ = lexeme(string("null")).result(null);

    private static final Parsec<Double> number = lexeme(regex("-?(0|[1-9][0-9]*)([.][0-9]+)?([eE][+-]?[0-9]+)?")).map(Double::parseDouble);

    private static Parsec<String> charSeq() {
        final Parsec<String> string_part = regex("[^\"\\\\]");
        //noinspection unchecked
        final Parsec<String> string_esc = string("\\").compose(
                choice(string("\\"),
                        string("/"),
                        string("\""),
                        string("b").result("\b"),
                        string("f").result("\f"),
                        string("n").result("\n"),
                        string("r").result("\r"),
                        string("t").result("\t"),
                        regex("u[0-9a-fA-F]{4}").map(s -> String.valueOf("\\" + s))));

        //noinspection unchecked
        return choice(string_part, string_esc);
    }

    private static Parsec<String> quoted() {
        return joint(string("\""), many(charSeq()), string("\""))
                .map(list -> {
                    assert list.size() >= 2;
                    final List<String> strings = (List<String>) list.get(1);
                    return strings.stream()
                            .collect(Collectors.joining());
                });
    }

    private static Parsec value() {
        return choice(quoted(), number, jsonObject(), array(), true_, false_, null_);
    }

    private static final Parsec value = value();

    private static Parsec<Pair<String, Object>> object_pair() {
        return joint(quoted(), colon, value)
                .map(list -> {
                    final String key = ((String) list.get(0));
                    final Object value = list.get(2);
                    return new Pair<>(key, value);
                });
    }

    private static Parsec<List> array() {
        return joint(lbrack, sepBy(value, comma), rbrack)
                .map(list -> ((List) list.get(1)));
    }

    private static Parsec<Map<String, Object>> jsonObject() {
        return joint(lbrace, sepBy(object_pair(), comma), rbrace)
                .map(list -> {
                    final List<Pair<String, Object>> pairs = (List<Pair<String, Object>>) list.get(0);
                    final Map<String, Object> result = new LinkedHashMap<>();
                    for (Pair<String, Object> pair : pairs) {
                        result.put(pair.first, pair.second);
                    }

                    return result;
                });
    }

    public static Parsec<Map<String, Object>> jsonc() {
        return compose(spaces(), jsonObject());
    }

    static class Pair<A, B> {
        final A first;
        final B second;

        Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

    public static void main(String[] args) {

//        final Value<String> _parse = regex("\\d+")._parse("1234abc", 0);
//        System.out.println("_parse = " + _parse);
//
//        final Value<Double> parse1 = number._parse("1231", 0);
//        System.out.println("parse1 = " + parse1);
//
//        final Value<List> parse2 = sepBy(number, comma)._parse("12, 23, 34", 0);
//        System.out.println("parse2 = " + parse2);
//
//        final Value<List> parse3 = array()._parse("[21, 12, 23]", 0);
//        System.out.println("parse3 = " + parse3);

        final Value<Map<String, Object>> result = jsonObject()._parse("{\"a\" : [1,2,3,4] }", 0);
        System.out.println("result = " + result);
    }
}
