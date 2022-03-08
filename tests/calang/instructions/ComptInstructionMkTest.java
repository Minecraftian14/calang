package calang.instructions;

import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertArrayEquals;

public class ComptInstructionMkTest extends InstructionMkTestTemplate {

    @Test
    public void makeInstruction_shouldReturnsOk_givenSomeRightHand() {
        class Spy implements ComptInstructionMk<Object[]> {
            @Override
            public Object[] computeInstruction(String targetSymbol, String baseSymbol, String operator, List<String> parameterSymbols) {
                return new Object[] {
                        targetSymbol, baseSymbol, operator,
                        parameterSymbols
                };
            }
        }
        assertArrayEquals(new Object[] { "$Y", "$X", "+", List.of("$A", "$B", "$C") },
                new Spy().makeInstruction(tokensOf("COMPT IN $Y $X + $A $B $C")));
        assertArrayEquals(new Object[] { "$Y", "$X", "++", emptyList() },
                new Spy().makeInstruction(tokensOf("COMPT IN $Y $X ++")));
    }

}
