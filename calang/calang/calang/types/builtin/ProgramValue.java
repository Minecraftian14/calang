package calang.types.builtin;

import calang.Calang;
import calang.types.TypedValue;

public class ProgramValue extends TypedValue<ProgramValue, ProgramValue.ProgramObject> {

    public static class ProgramObject {}

    public ProgramValue(Calang runtime) {super(new ProgramObject(), runtime);}
}
