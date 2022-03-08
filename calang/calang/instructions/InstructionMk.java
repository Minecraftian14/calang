package calang.instructions;

@FunctionalInterface
public interface InstructionMk<T> {

    T makeInstruction(String[] tokens);

}
