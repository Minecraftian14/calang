package calang.instructions;

import calang.VariableBinding;

import java.util.ArrayList;
import java.util.List;

import static calang.rejections.Rejections.MALFORMED_CALL_INSTRUCTION;

public interface CallInstructionMk<T> extends InstructionMk<T> {

    T callInstruction(
            String programSymbol,
            List<VariableBinding> inputs,
            List<VariableBinding> outputs
    );

    @Override
    default T makeInstruction(String[] tokens) {
        assert tokens[0].equals("CALL");

        if (tokens.length < 2)
            throw MALFORMED_CALL_INSTRUCTION.error("Unspecified program name");
        else if ((tokens.length - 2) % 3 != 0)
            throw MALFORMED_CALL_INSTRUCTION.error("Impossible to create IO-pairs");

        List<VariableBinding>
                inputs = new ArrayList<>(),
                outputs = new ArrayList<>();
        for(int i = 2; i < tokens.length; i += 3) {
            (switch(tokens[i+1]) {
                case ">>" -> inputs;
                case "<<" -> outputs;
                default -> throw MALFORMED_CALL_INSTRUCTION.error(
                        "Unrecognized separator token %s".formatted(tokens[i+1])
                );
            }).add(new VariableBinding(tokens[i], tokens[i+2]));
        }

        return callInstruction(
                tokens[1],
                inputs, outputs
        );
    }
}
