package webengine.src;

import calang.Calang;
import calang.Tangle;
import calang.TranspileJs;
import calang.types.TypedValue;
import calang.types.builtin.ProgramValue;

import java.util.List;

public abstract class MyTranspiler extends TranspileJs implements Tangle, FileContent
{

    public static class ModalElementValue extends TypedValue<ModalElementValue, Object> {
        public ModalElementValue(Calang runtime) { super(new Object(), runtime); }
    }

    {
        addType("MODAL_ELEMENT", ModalElementValue::new);

        addOperator(ModalElementValue.class, "...", (v, args) -> new ProgramValue(this));
        addOperator(ModalElementValue.class, "display!", (v, args) -> v);
        addOperator(ModalElementValue.class, "close!", (v, args) -> v);
        addOperator(ModalElementValue.class, "?", (v, args) -> "Some text");
    }

    public List<String> transpile(String programName) {
        return transpile(programName, tangle(programName));
    }

    public List<String> tangle(String programName) {
        return tangle(fileContent(programName));
    }

}