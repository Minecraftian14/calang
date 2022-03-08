package calang;

import calang.types.TypedValue;

import java.util.List;
import java.util.Optional;

import static calang.rejections.Rejections.UNKNOWN_VARIABLE;

public interface Scope {
    Optional<TypedValue<?, ?>> symbol(String token);

    List<String> symbolList();

    default TypedValue<?, ?> getOrDie(String token) {
        return symbol(token).orElseThrow(() -> UNKNOWN_VARIABLE.error(token));
    }
}
