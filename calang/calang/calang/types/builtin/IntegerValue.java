package calang.types.builtin;

import calang.Calang;
import calang.types.TypedValue;

public class IntegerValue extends TypedValue<IntegerValue, Integer> {
    public IntegerValue(Calang runtime) {
        this(0, runtime);
    }

    public IntegerValue(int i, Calang runtime) {
        super(i, runtime);
    }

    public IntegerValue(Object v, Calang runtime) {
        this(runtime);
        with(v);
    }

    protected Integer convertFromBytes(byte[] data) {
        return Integer.parseInt(new String(data));
    }
}
