package xin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isWhitespace;
import static java.lang.Math.min;

@SuppressWarnings("unused")
public interface Parsec<T> {

    class ParseException extends RuntimeException {
        ParseException(String message) {
            super(message);
        }
    }

    default T parse(String input) {
        final Value<T> res = _parse(input, 0);
        if (!res.status) {
            throw new ParseException(
                    "expect: " + res.expected + " on input index: " + res.index
                            + ", but got: " + input.substring(res.index, min(input.length(), res.index + 5))
            );
        }

        return res.value;
    }

    default T parseStrict(String input) {
        return this.skip(eof()).parse(input);
    }

    Value<T> _parse(String input, int index);

    default <R> Parsec<R> map(Function<T, R> transform) {
        final Parsec<T> self = this;
        return (input, index) -> {
            final Value<T> v = self._parse(input, index);
            if (v.status) {
                return Value.success(v.index, transform.apply((v.value)));
            } else {
                return Value.failure(v.index, v.expected);
            }
        };
    }

    default <R> Parsec<R> result(R result) {
        return map(t -> result);
    }

    default Parsec<T> skip(Parsec parsec) {
        final Parsec<T> self = this;
        return (input, index) -> {
            final Value<T> res = self._parse(input, index);
            if (!res.status) {
                return res;
            }

            final Value end = parsec._parse(input, res.index);
            if (end.status) {
                return Value.success(end.index, res.value);
            } else {
                return Value.failure(end.index, end.expected);
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess")
    class Value<T> {

        public final boolean status;
        public final int index;
        public final T value;
        public final Object expected;

        private Value(boolean status, int index, T value, Object expected) {
            this.status = status;
            this.index = index;
            this.value = value;
            this.expected = expected;
        }

        public static <T> Value<T> success(int index, T value) {
            return new Value<>(true, index, value, value);
        }

        public static <T> Value<T> failure(int index, Object expected) {
            return new Value<>(false, index, null, expected);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Value<?> value1 = (Value<?>) o;
            return status == value1.status &&
                    index == value1.index &&
                    Objects.equals(value, value1.value) &&
                    Objects.equals(expected, value1.expected);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, index, value, expected);
        }

        @Override
        public String toString() {
            return "Value{" +
                    "status=" + status +
                    ", index=" + index +
                    ", value=" + value +
                    ", expected=" + expected +
                    '}';
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    static Parsec<Character> charMatcher(Predicate<Character> predicate, String expect) {
        return (input, index) -> {
            if (!input.isEmpty() && predicate.test(input.charAt(index))) {
                return Value.success(index + 1, input.charAt(index));
            } else {
                return Value.failure(index, expect);
            }
        };
    }

    static Parsec<Character> char_(char c) {
        return (input, index) -> {
            if (index < input.length() && input.charAt(index) == c) {
                return Value.success(index + 1, c);
            } else {
                return Value.failure(index, c);
            }
        };
    }

    static Parsec<String> string(String str) {
        return (input, index) -> {
            final int len = str.length();
            if (index + len <= input.length() && str.equals(input.substring(index, index + len))) {
                return Value.success(index + len, str);
            } else {
                int matched = 0;
                while (index + matched < input.length() && input.charAt(index + matched) == str.charAt(matched)) {
                    matched += 1;
                }
                return Value.failure(index + matched, str);
            }
        };
    }

    static Parsec<Character> digit() {
        return charMatcher(Character::isDigit, "a digit");
    }

    static Parsec<Character> space() {
        return charMatcher(Character::isWhitespace, "a space");
    }

    static Parsec<Character> letter() {
        return charMatcher(Character::isLetter, "a letter");
    }

    static Parsec<String> spaces() {
        return (input, index) -> {
            int step = 0;
            while (index + step < input.length() && isWhitespace(input.charAt(index + step))) {
                step++;
            }

            final int endIndex = index + step;
            return Value.success(endIndex, input.substring(index, endIndex));
        };
    }

    static Parsec<Character> oneOf(String chars) {
        return charMatcher(character -> chars.contains(character.toString()), "one of " + chars);
    }

    static Parsec<Character> noneOf(String chars) {
        return charMatcher(character -> !chars.contains(character.toString()), "none of " + chars);
    }

    static Parsec<Character> eof() {
        return (input, index) -> {
            if (index >= input.length()) {
                return Value.success(index, null);
            } else {
                return Value.failure(index, "EOF");
            }
        };
    }

    static Parsec<String> regex(String regex) {
        if (!regex.startsWith("^")) {
            regex = "^" + regex;
        }
        return regex(Pattern.compile(regex));
    }

    static Parsec<String> regex(Pattern pat) {
        return (input, index) -> {
            final Matcher matcher = pat.matcher(input.substring(index));
            if (matcher.find(0)) {
                return Value.success(index + matcher.end(), matcher.group(0));
            } else {
                return Value.failure(index, pat.pattern());
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    static Parsec choice(Parsec... parsecs) {
        _check(parsecs.length >= 2, "choice need at least 2 Parsec");

        return (input, index) -> {

            final List<Object> expects = new ArrayList<>();

            for (Parsec p : parsecs) {
                final Value v = p._parse(input, index);

                if (!v.status) {
                    expects.add(v.expected);
                }

                if (v.status || v.index != index) {
                    return v;
                }
            }

            return Value.failure(index, expects.toString());
        };
    }

    static Parsec tryChoice(Parsec... parsecs) {
        return (input, index) -> {

            final List<Object> expects = new ArrayList<>();

            for (Parsec p : parsecs) {
                final Value v = p._parse(input, index);
                if (v.status) {
                    return v;
                }

                expects.add(v.expected);
            }

            return Value.failure(index, expects.toString());
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    default <R> Parsec<R> compose(Parsec<R> parsec) {
        final Parsec<T> _this = this;
        return (input, index) -> {
            final Value<T> res = _this._parse(input, index);
            if (!res.status) {
                return Value.failure(res.index, res.expected);
            } else {
                return parsec._parse(input, res.index);
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    static Parsec<List> joint(Parsec... parsecs) {
        return (input, index) -> {

            final List<Object> values = new ArrayList<>(parsecs.length);
            int increasing_index = index;
            for (Parsec p : parsecs) {
                final Value res = p._parse(input, increasing_index);
                if (!res.status) {
                    return Value.failure(res.index, res.expected);
                }

                increasing_index = res.index;
                values.add(res.value);
            }

            return Value.success(increasing_index, values);
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    static <T> Parsec<T> optional(Parsec<T> parsec) {
        return (input, index) -> {
            final Value<T> res = parsec._parse(input, index);
            if (res.status) {
                return res;
            } else {
                return Value.success(res.index, null);
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    static <T> Parsec<List<T>> many(Parsec<T> parsec) {
        return times(parsec, 0, Integer.MAX_VALUE);
    }

    static <T> Parsec<List<T>> many1(Parsec<T> parsec) {
        return times(parsec, 1, Integer.MAX_VALUE);
    }

    static <T> Parsec<List<T>> count(Parsec<T> parsec, int n) {
        _check(n > 0, "count must be greater than zero");
        return times(parsec, n, n);
    }

    static <T> Parsec<List<T>> times(Parsec<T> parsec, int min, int max) {
        _check(min <= max, "min > max, very wrong!!!");
        _check(max >= 0, "max should be positive");
        _check(min >= 0, "min should be positive");

        return (input, index) -> {

            final List<T> values = new ArrayList<>(min);

            int increasing_index = index;
            int count = 0;
            while (count < max) {

                final Value<T> v = parsec._parse(input, increasing_index);
                if (v.status) {
                    values.add(v.value);
                    increasing_index = v.index;
                    count += 1;
                } else {

                    if (count >= min) {
                        break;
                    } else {
                        return Value.failure(v.index,
                                "match " + v.expected + " between [" + min + "," + max + "] times");
                    }
                }

                if (count >= max) {
                    break;
                }
            }

            return Value.success(increasing_index, values);

        };
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    static Parsec<List> separated(Parsec p, Parsec seq, int min, int max) {

        _check(max >= min, "max < min, very wrong!!!");
        _check(max >= 0, "max must positive");
        _check(min >= 0, "min must positive");

        return (input, index) -> {

            final List<Object> values = new ArrayList<>(min);

            Value res = p._parse(input, index);
            if (!res.status) {
                return Value.failure(res.index, res.expected);
            }

            values.add(res.value);

            int count = 0;

            while (true) {

                res = seq._parse(input, res.index);
                if (!res.status) {
                    if (count >= min) {
                        break;
                    }
                    return Value.failure(res.index, res.expected);
                }

                res = p._parse(input, res.index);

                if (!res.status) {
                    return Value.failure(res.index, res.expected);
                }

                values.add(res.value);
                count += 1;

                if (count >= max) {
                    break;
                }
            }


            if (count < min) {
                return Value.failure(res.index, "");
            }

            return Value.success(res.index, values);
        };
    }

    static Parsec<List> sepBy(Parsec p, Parsec seq) {
        return separated(p, seq, 0, Integer.MAX_VALUE);
    }

    static Parsec<List> sepBy1(Parsec p, Parsec seq) {
        return separated(p, seq, 1, Integer.MAX_VALUE);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    static void _check(boolean expression, String msg) {
        if (!expression) {
            throw new IllegalArgumentException(msg);
        }
    }
}
