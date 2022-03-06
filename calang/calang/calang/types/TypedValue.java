package calang.types;

import calang.Calang;

import static calang.rejections.Rejections.*;

public abstract class TypedValue<S extends TypedValue<S, V> /* Fluent API: S is Self type */ , V /* Value type */> {
    @SuppressWarnings("unchecked")
    S self() {
        return (S) this;
    }

    private V value;
    protected TypedValue(V value, Calang runtime) {
        this.value = value;
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

    public final S with(Object v) {
        set(v);
        return self();
    }

    @Override
    public String toString() {
        return this.get().toString();
    }
}
