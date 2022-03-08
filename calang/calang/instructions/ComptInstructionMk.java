package calang.instructions;

import java.util.Arrays;
import java.util.List;

import static calang.rejections.Rejections.MALFORMED_COMPT_INSTRUCTION;

public interface ComptInstructionMk<T> extends InstructionMk<T> {

    T computeInstruction(
            String targetSymbol,
            String baseSymbol,
            String operator,
            List<String> parameterSymbols
    );

    @Override
    default T makeInstruction(String[] tokens) {
        assert tokens[0].equals("COMPT");

        if (tokens.length < 5 || !"IN".equals(tokens[1]))
            throw MALFORMED_COMPT_INSTRUCTION.error(Arrays.toString(tokens));

        var targetSymbol = tokens[2];
        var baseSymbol = tokens[3];
        var operator = tokens[4];
        return computeInstruction(
                targetSymbol,
                baseSymbol,
                operator,
                Arrays.stream(tokens).skip(5).toList()
        );
    }
}
