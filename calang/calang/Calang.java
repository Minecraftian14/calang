package calang;

import calang.instructions.*;
import calang.types.Operator;
import calang.types.TypedValue;
import calang.types.builtin.BooleanValue;
import calang.types.builtin.BytesValue;
import calang.types.builtin.IntegerValue;
import calang.types.builtin.ProgramValue;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static calang.rejections.Rejections.*;
import static java.util.function.Predicate.not;

public class Calang {
    final Map<String, Function<Calang, TypedValue<?, ?>>> TOKENS;
    final Map<Class<? extends TypedValue<?, ?>>, Map<String, Operator<?>>> OPERATORS;

    protected Calang() {
        TOKENS = new HashMap<>(Map.of(
                "INTEGER", IntegerValue::new,
                "BYTES", BytesValue::new,
                "BOOLEAN", BooleanValue::new,
                "PROGRAM", ProgramValue::new
        ));
        OPERATORS = new HashMap<>();
        {
            class Accessor {
                Stream<Integer> get(Object[] args) {
                    return Arrays.stream(args).map(i -> new IntegerValue(i, Calang.this)).map(IntegerValue::get);
                }

                int sum(Object[] args) {
                    return get(args).mapToInt(Integer::intValue).sum();
                }
            }
            addOperator(IntegerValue.class, "-", (v, args) -> new IntegerValue(v.get() - new Accessor().sum(args), Calang.this));
            addOperator(IntegerValue.class, "+", (v, args) -> new IntegerValue(v.get() + new Accessor().sum(args), Calang.this));
            addOperator(IntegerValue.class, "prec", (v, args) -> new IntegerValue(v.get() - 1, Calang.this));
            addOperator(IntegerValue.class, "succ", (v, args) -> new IntegerValue(v.get() + 1, Calang.this));
        }
        {
            addOperator(BytesValue.class, "|.|", (v, args) -> new IntegerValue(v.get().length, this));
        }
        {
            addOperator(BooleanValue.class, "NEGATE", (v, args) -> new BooleanValue(this).with(!v.get()));
        }
    }

    public final void addType(String typeIdentifier, Function<Calang, TypedValue<?, ?>> typeFactory) {
        TOKENS.put(typeIdentifier, typeFactory);
    }

    @SuppressWarnings("unchecked")
    public final <T extends TypedValue<T, ?>> void addOperator(Class<T> clz, String operatorName, Operator<T> operator) {
        if (OPERATORS.containsKey(clz)) {
            var ops = (Map<String, Operator<T>>) (Map<?,?>) OPERATORS.get(clz);
            ops.put(operatorName, operator);
        }
        else {
            OPERATORS.put(clz, new HashMap<>());
            addOperator(clz, operatorName, operator);
        }
    }

    /******************************************************************** */

    public interface PreInstruction {
        List<String> transpile(Scope scope);
    }

    private PreInstruction getInstruction(String line) {
        if (line.startsWith("  ")) return getInstruction(line.substring(2));
        assert line.indexOf(" ") > 0 : "Malformed instruction line |%s|".formatted(line);
        var tokens = line.trim().split("\s+");
        return (switch (tokens[0]) {
            case "PERFORM" -> (PerformInstructionMk<PreInstruction>) (_1, _2, _3, _4, _5) -> __
                    -> transpilePerformInstruction(__, _1, _2, _3, _4, _5);
            case "PRINT" -> (PrintInstructionMk<PreInstruction>) _1 -> __
                    -> transpilePrintInstruction(__, _1);
            case "STORE" -> (StoreInstructionMk<PreInstruction>) (_1, _2) -> __
                    -> transpileStoreInstruction(__, _1, _2);
            case "COMPT" -> (ComptInstructionMk<PreInstruction>) (_1, _2, _3, _4) -> __
                    -> transpileComptInstruction(__, _1, _2, _3, _4);
            case "CALL" -> (CallInstructionMk<PreInstruction>) (_1, _2, _3) -> __
                    -> transpileCallInstruction(__, _1, _2, _3);
            default -> throw UNRECOGNIZED_INSTRUCTION_TOKEN.error(tokens[0]);
        }).makeInstruction(tokens);
    }

    public interface Program {
        List<String> paragraphNames();

        Paragraph paragraph(String name);

        String headParagraphName();

        Scope scope();

        List<String> getDeclaredOutputs();

        List<String> getDeclaredInputs();
    }

    @FunctionalInterface
    public interface Paragraph {
        List<PreInstruction> instructions();
    }

    protected Program parse(List<String> lines) {
        if (lines.stream().anyMatch(String::isBlank)) {
            return parse(lines.stream().filter(not(String::isBlank)).toList());
        }
        HashMap<String, TypedValue<?, ?>> variables;
        ArrayList<String> inputs;
        ArrayList<String> outputs;
        {
            variables = new HashMap<>();
            inputs = new ArrayList<>();
            outputs = new ArrayList<>();
            lines.stream().takeWhile(l -> l.startsWith("DECLARE")).forEach(line -> {
                var tokens = line.trim().split("\s+");
                assert tokens[0].equals("DECLARE");
                var varName = tokens[tokens.length - 2];
                var varType = tokens[tokens.length - 1];
                var variable = Objects.requireNonNull(TOKENS.get(varType), "Unable to resolve type %s".formatted(varType)).apply(this);
                variables.put(varName, variable);
                if (tokens.length == 4) {
                    if ("INPUT".equals(tokens[1])) inputs.add(varName);
                    else if ("OUTPUT".equals(tokens[1])) outputs.add(varName);
                }
            });
        }
        Scope scope = new Scope() {
            @Override
            public Optional<TypedValue<?, ?>> symbol(String symbName) {
                return Optional.ofNullable(variables.get(symbName));
            }

            @Override
            public List<String> symbolList() {
                return new ArrayList<>(variables.keySet());
            }
        };

        record Par(int lineIndex, String name, List<PreInstruction> instructions) implements Paragraph {}

        List<Par> paragraphs;
        Par headParagraph;
        {
            class InstructionChecker {
                PreInstruction checkedPreInstruction(String line) {
                    return getInstruction(line);
                }
            }
            var checker = new InstructionChecker();
            paragraphs = IntStream.range(0, lines.size())
                    .dropWhile(i -> lines.get(i).startsWith("DECLARE"))
                    .filter(i -> !lines.get(i).startsWith("  "))
                    .mapToObj(i -> new Par(i, lines.get(i).substring(0, lines.get(i).length() - 1), IntStream.range(i + 1, lines.size()).takeWhile(j -> lines.get(j).startsWith("  ")).mapToObj(lines::get).map(checker::checkedPreInstruction).toList())).sorted(Comparator.comparing(Par::name)).toList();

            headParagraph = paragraphs.stream().min(Comparator.comparingInt(Par::lineIndex)).orElseThrow(NO_PARAGRAPH_FOUND::error);
        }

        return new Program() {
            @Override
            public String headParagraphName() {
                return headParagraph.name();
            }

            @Override
            public Paragraph paragraph(String name) {
                return paragraphs.stream().filter(__ -> __.name().equals(name)).findFirst().orElseThrow(() -> UNDEFINED_PARAGRAPH.error(name));
            }

            @Override
            public List<String> paragraphNames() {
                return paragraphs.stream().map(Par::name).toList();
            }

            @Override
            public Scope scope() {
                return scope;
            }

            @Override
            public List<String> getDeclaredOutputs() {
                return outputs;
            }

            @Override
            public List<String> getDeclaredInputs() {
                return inputs;
            }
        };
    }

    /******************************************************************** */

    protected String transpileType(TypedValue<?, ?> value) {
        throw NON_TRANSPILED_TYPE.error(value.getClass());
    }

    protected List<String> transpile(String programName, Program program) {
        throw NON_TRANSPILED_PROGRAM.error(programName);
    }

    protected List<String> transpilePerformInstruction(Scope scope, String paragraphName, String altParagraphName, String booleanValueSymbol, boolean isLoop, boolean contraCondition) {
        throw NON_TRANSPILED_INSTRUCTION.error("PERFORM");
    }

    protected List<String> transpileStoreInstruction(Scope scope, String sourceSymbol, String targetSymbol) {
        throw NON_TRANSPILED_INSTRUCTION.error("STORE");
    }

    protected List<String> transpileComptInstruction(Scope scope, String targetSymbol, String baseSymbol, String operator, List<String> arguments) {
        throw NON_TRANSPILED_INSTRUCTION.error("COMPT");
    }

    protected List<String> transpilePrintInstruction(Scope scope, List<String> tokens) {
        throw NON_TRANSPILED_INSTRUCTION.error("PRINT");
    }

    protected List<String> transpileCallInstruction(Scope scope, String programName, List<VariableBinding> in, List<VariableBinding> out) {
        throw NON_TRANSPILED_INSTRUCTION.error("CALL");
    }

}
