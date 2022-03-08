package calang.instructions;

import java.util.Arrays;
import java.util.stream.Collectors;

import static calang.rejections.Rejections.MALFORMED_STORE_INSTRUCTION;

public interface StoreInstructionMk<T> extends InstructionMk<T> {

    T storeInstruction(String targetSymbol, String sourceSymbol);

    @Override
    default T makeInstruction(String[] tokens) {
        assert tokens[0].equals("STORE");

        if (tokens.length < 3 || !"IN".equals(tokens[1]))
            throw MALFORMED_STORE_INSTRUCTION.error(Arrays.toString(tokens));

        var targetSymbol = tokens[2];
        return storeInstruction(
                targetSymbol,
                Arrays.stream(tokens).skip(3).collect(Collectors.joining(" "))
        );
    }
}
