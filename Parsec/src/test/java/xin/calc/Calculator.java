package xin.calc;

import xin.Parsec;

import static xin.Parsec.*;

public class Calculator {

    private static <T> Parsec<T> lexeme(Parsec<T> p) {
        return p.skip(spaces());
    }

    static final Parsec<Character> add = lexeme(char_('+'));
    static final Parsec<Character> minus = lexeme(char_('-'));
    static final Parsec<Character> multi = lexeme(char_('*'));
    static final Parsec<Character> divide = lexeme(char_('/'));
    static final Parsec<Character> reminder = lexeme(char_('%'));
    static final Parsec<Character> lparen = lexeme(char_('('));
    static final Parsec<Character> rparen = lexeme(char_(')'));

    static final Parsec<Double> num = lexeme(regex("[+-]?\\d+(\\.\\d+)?")).map(Double::parseDouble);
    
}
