package calang.instructions;

import java.util.Arrays;
import java.util.List;

import static calang.rejections.Rejections.MALFORMED_PRINT_INSTRUCTION;

public interface PrintInstructionMk<T> extends InstructionMk<T> {

    T printInstruction(List<String> tokens);

    @Override
    default T makeInstruction(String[] tokens) {
        assert tokens[0].equals("PRINT");

        if (tokens.length == 1)
            throw MALFORMED_PRINT_INSTRUCTION.error(Arrays.toString(tokens));

        return printInstruction(Arrays.stream(tokens).skip(1).toList());
    }
}
