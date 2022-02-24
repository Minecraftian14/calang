package calang.types;

import calang.Calang;

import java.util.Map;
import java.util.function.Function;

import static calang.rejections.Rejections.*;

public abstract class TypedValue<S extends TypedValue<S, V> /* Fluent API: S is Self type */ , V /* Value type */> {
    @SuppressWarnings("unchecked")
    S self() {
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    Class<S> selfType() {
        return (Class<S>) self().getClass();
    }

    private final Map<String, Operator<S>> operators;
    private V value;

    protected TypedValue(V value, Calang runtime) {
        this.value = value;
        this.operators = runtime.getOperators(selfType());
    }

    public final V get() {
        return this.value;
    }

    @SuppressWarnings("unchecked")
    public final void set(Object v) {
        if (value.getClass().isInstance(v))
            value = (V) v;
        else if (this.getClass() == v.getClass())
            value = ((TypedValue<?, V>) v).get();
        else if (v instanceof TypedValue<?, ?> tv)
            value = convertFromObject(tv.get());
        else if (v instanceof byte[] data && data.length > 0)
            value = convertFromBytes(data);
        else if (v instanceof String data && data.length() > 0)
            value = convertFromBytes(data.getBytes());
        else
            value = convertFromObject(v);
    }

    protected V convertFromBytes(byte[] data) {
        throw UNSUPPORTED_FROM_BYTES_CONVERSION.error(this);
    }

    protected V convertFromObject(Object v) {
        throw UNSUPPORTED_FROM_OBJECT_CONVERSION.error(this, v);
    }

    public Function<Object[], Object> sendBinding(String operatorName) {
        if (operators.containsKey(operatorName)) {
            var self = self();
            var op = operators.get(operatorName);
            return __ -> op.apply(self, __);
        }
        throw UNSUPPORTED_OPERATOR.error(operatorName, this);
    }

    public final S with(Object v) {
        set(v);
        return self();
    }

    public String toString() {
        return new String(bytesValue());
    }

    protected byte[] bytesValue() {
        return this.get().toString().getBytes();
    }
}
