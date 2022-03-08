package calang;

import calang.types.TypedValue;
import calang.types.builtin.BooleanValue;
import calang.types.builtin.BytesValue;
import calang.types.builtin.IntegerValue;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * Parsing test is done in the CalangParseE2ETest file.
 */
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

    @Test
    public void basicCalang_shouldSupport_addType() {
        var calang = new Calang();
        var magnet = "MY_TYPE";

        assertFalse(calang.TOKENS.containsKey(magnet));

        class MyType implements TypedValue<MyType, Object> {}

        calang.addType(magnet, MyType.class);

        assertTrue(calang.TOKENS.containsKey(magnet));
        assertSame(calang.TOKENS.get(magnet), MyType.class); // null yields false
    }

    @Test
    public void basicCalang_shouldSupport_addOperator() {
        var calang = new Calang();

        calang.addOperator(IntegerValue.class, "|.|", null);

        assertTrue(calang.OPERATORS.get(IntegerValue.class).containsKey("|.|"));
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
