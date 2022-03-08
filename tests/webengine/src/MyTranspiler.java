package webengine.src;

import calang.Calang;
import calang.Tangle;
import calang.TranspileJs;
import calang.types.TypedValue;
import calang.types.builtin.BytesValue;
import calang.types.builtin.Operators;
import calang.types.builtin.ProgramValue;

import java.util.List;

import static java.util.Collections.emptyList;

public abstract class MyTranspiler extends TranspileJs implements Tangle, FileContent
{

    public static class ModalElementValue extends TypedValue<ModalElementValue, Object> {
        public ModalElementValue(Calang runtime) { super(new Object(), runtime); }
    }

    {
        addType("MODAL_ELEMENT", ModalElementValue::new);

        addOperator(ModalElementValue.class, "...", Operators.describes(ModalElementValue.class, ModalElementValue.class, ProgramValue.class));
        addOperator(ModalElementValue.class, "display!", Operators.describes(ModalElementValue.class, ModalElementValue.class, emptyList()));
        addOperator(ModalElementValue.class, "close!", Operators.describes(ModalElementValue.class, ModalElementValue.class, emptyList()));
        addOperator(ModalElementValue.class, "?", Operators.describes(ModalElementValue.class, BytesValue.class, emptyList()));
    }

    public List<String> transpile(String programName) {
        return transpile(programName, tangle(programName));
    }

    public List<String> tangle(String programName) {
        return tangle(fileContent(programName));
    }

}