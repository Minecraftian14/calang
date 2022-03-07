package calang.types;

@FunctionalInterface
public interface Operator<T extends TypedValue<T, ?>> {
    Object apply(T v, Object... args);
}
