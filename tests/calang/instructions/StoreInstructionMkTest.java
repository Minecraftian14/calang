package calang.instructions;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class StoreInstructionMkTest extends InstructionMkTestTemplate {

    @Test
    public void makeInstruction_shouldReturnsOk_givenSomeRightHand() {
        class Spy implements StoreInstructionMk<Object[]> {
            @Override
            public Object[] storeInstruction(String targetSymbol, String sourceSymbol) {
                return new Object[] { targetSymbol, sourceSymbol };
            }
        }
        assertArrayEquals(new Object[] { "$X", "Something funny" },
                new Spy().makeInstruction(tokensOf("STORE IN $X Something  funny")));
    }

}
