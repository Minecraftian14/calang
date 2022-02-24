package calang;

import calang.types.TypedValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static calang.rejections.Rejections.UNKNOWN_VARIABLE;

public interface Scope {
    Optional<TypedValue<?, ?>> symbol(String token);

    List<String> symbolList();

    default TypedValue<?, ?> getOrDie(String token) {
        return getOrDie(token, () -> UNKNOWN_VARIABLE.error(token));
    }

    default TypedValue<?, ?> getOrDie(String token, Supplier<AssertionError> errorLog) {
        return symbol(token).orElseThrow(errorLog);
    }
}
