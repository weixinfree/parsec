package xin.calc;

import org.junit.Test;
import xin.Parsec.Value;

import static xin.ParameterizedTest.param;
import static xin.ParameterizedTest.parameterized_test;
import static xin.calc.Calculator.term;

public class CalculatorTest {

    @Test
    public void test_term() {
        parameterized_test(
                param(term, "123", Value.success(3, "123"))
        );
    }
}
