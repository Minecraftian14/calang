package calang.types.builtin;

import calang.Calang;
import calang.types.TypedValue;

public class BytesValue extends TypedValue<BytesValue, byte[]> {
    public BytesValue(Calang runtime) {
        super(new byte[0], runtime);
    }

    protected byte[] convertFromBytes(byte[] data) {
        return data;
    }

    protected byte[] convertFromObject(Object v) {
        return v.toString().getBytes();
    }
}
