package calang.types.builtin;

import calang.Calang;
import calang.types.TypedValue;

public class BooleanValue extends TypedValue<BooleanValue, Boolean> {
    public BooleanValue(Calang runtime) {
        super(Boolean.FALSE, runtime);
    }

    protected Boolean convertFromBytes(byte[] data) {
        return data.length == 0 || (data.length == 1 && data[0] == 0) ? Boolean.FALSE : Boolean.TRUE;
    }

    protected Boolean convertFromObject(Object v) {
        if (v instanceof Integer i) return Integer.valueOf(0).equals(i) ? Boolean.FALSE : Boolean.TRUE;
        else return super.convertFromObject(v);
    }
}
