package calang;

import calang.types.builtin.BooleanValue;
import calang.types.builtin.BytesValue;
import calang.types.builtin.IntegerValue;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.assertTrue;

public class CalangTest {

    @Test
    public void basicCalang_shouldSupport_basicTypes() {
        var calang = new Calang();

        checkMany("Basic Calang should support %s type",
                calang.TOKENS::containsKey,
                "INTEGER", "PROGRAM", "BYTES", "BOOLEAN"
        );
    }

    @Test
    public void basicCalang_shouldSupport_basicOperators() {
        var calang = new Calang();

        checkMany("Basic Calang should support %s operator on INTEGER",
                calang.OPERATORS.get(IntegerValue.class)::containsKey,
                "+", "-", "prec", "succ"
        );

        checkMany("Basic Calang should support %s operator on BOOLEAN",
                calang.OPERATORS.get(BooleanValue.class)::containsKey,
                "NEGATE"
        );

        checkMany("Basic Calang should support %s operator on BYTES",
                calang.OPERATORS.get(BytesValue.class)::containsKey,
                "|.|"
        );
    }

    @SafeVarargs
    static <T> void checkMany(String messageTplOnError, Predicate<T> p, T... tokens) {
        for(var token : tokens)
            check(messageTplOnError, p, token);
    }

    static <T> void check(String messageTplOnError, Predicate<T> p, T token) {
        assertTrue(messageTplOnError.formatted(token), p.test(token));
    }

}
