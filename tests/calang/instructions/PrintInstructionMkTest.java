package calang.instructions;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PrintInstructionMkTest extends InstructionMkTestTemplate {

    @Test
    public void makeInstruction_shouldReturnsOk_givenTokenList() {
        class Spy implements PrintInstructionMk<List<String>> {
            @Override
            public List<String> printInstruction(List<String> tokens) {
                return tokens;
            }
        }
        assertEquals(List.of("HELLO", "WORLD"),
                new Spy().makeInstruction(tokensOf("PRINT HELLO WORLD")));
    }

}
