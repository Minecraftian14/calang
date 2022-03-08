package calang.instructions;

import java.util.Arrays;

import static calang.rejections.Rejections.MALFORMED_PERFORM_INSTRUCTION;
import static calang.rejections.Rejections.UNRECOGNIZED_PERFORM_DECORATOR;

public interface PerformInstructionMk<T> extends InstructionMk<T> {

    T performInstruction(
            String paragraphName,
            String alternativeParagraphName,
            String booleanValueSymbol,
            boolean isLoop,
            boolean isContraCondition
    );

    @Override
    default T makeInstruction(String[] tokens) {
        assert tokens[0].equals("PERFORM");

        switch (tokens.length) {
            case 2:
                return simplePerformInstruction(tokens);
            case 4:
                return switch (tokens[2]) {
                    case "IF" -> simplePerformIfInstruction(tokens);
                    case "WHILE" -> simplePerformWhileInstruction(tokens);
                    default -> throw UNRECOGNIZED_PERFORM_DECORATOR.error(tokens[2]);
                };
            case 5:
                if (!"IF".equals(tokens[2]) || !"NOT".equals(tokens[3])) break;
                return simplePerformIfNotInstruction(tokens);
            case 6:
                if (!"IF".equals(tokens[2]) || !"ELSE".equals(tokens[4])) break;
                return simplePerformIfElseInstruction(tokens);
        }
        throw MALFORMED_PERFORM_INSTRUCTION.error(Arrays.toString(tokens));
    }

    default T simplePerformIfElseInstruction(String[] tokens) {
        assert tokens.length == 6 && tokens[2].equals("IF") && tokens[4].equals("ELSE");
        return performInstruction(
                paragraphNameOf(tokens),
                tokens[5], tokens[3],
                false, false
        );
    }

    default T simplePerformIfNotInstruction(String[] tokens) {
        assert tokens.length == 5 && tokens[2].equals("IF") && tokens[3].equals("NOT");
        return performInstruction(
                paragraphNameOf(tokens),
                null, tokens[4],
                false, true
        );
    }

    default T simplePerformWhileInstruction(String[] tokens) {
        assert tokens.length == 4 && tokens[2].equals("WHILE");
        return performInstruction(
                paragraphNameOf(tokens),
                null, tokens[3],
                true, false
        );
    }

    default T simplePerformIfInstruction(String[] tokens) {
        assert tokens.length == 4 && tokens[2].equals("IF");
        return performInstruction(
                paragraphNameOf(tokens),
                null, tokens[3],
                false, false
        );
    }

    default T simplePerformInstruction(String[] tokens) {
        assert tokens.length == 2;
        return performInstruction(
                paragraphNameOf(tokens),
                null, null,
                false, false
        );
    }

    default String paragraphNameOf(String[] tokens) {
        assert tokens[0].equals("PERFORM"); assert tokens.length >= 2;
        return tokens[1];
    }

}
