import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

public class Calang {
  private final String basePath;
  private Map < String, Function < Calang, TypedValue < ? , ? >>> TOKENS;
  private Map < Class < ? extends TypedValue < ? , ? >> , Map < String, Operator < ? >>> OPERATORS;
  protected Calang() {
    this("");
  }
  protected Calang(String basePath) {
    assert basePath.isEmpty() || basePath.endsWith("/");
    this.basePath = basePath;
    TOKENS = new HashMap < > (Map.of(
      "INTEGER", IntegerValue::new, "BYTES", BytesValue::new, "BOOLEAN", BooleanValue::new
    ));
    OPERATORS = new HashMap < Class < ? extends TypedValue < ? /*T*/ , ? >> , Map < String, Operator < ? /*T*/ >>> (); {
      class Accessor {
        Stream < Integer > get(Object[] args) {
          return get(args, 0, args.length);
        }
        Stream < Integer > get(Object[] args, int start, int size) {
          return Arrays.stream(args).skip(start).limit(size).map(i -> new IntegerValue(i, Calang.this)).map(IntegerValue::get);
        }
      }
      addOperator(IntegerValue.class, "-", (v, args) -> new IntegerValue(v.get().intValue() - new Accessor().get(args).mapToInt(Integer::intValue).sum(), Calang.this));
      addOperator(IntegerValue.class, "+", (v, args) -> new IntegerValue(v.get().intValue() + new Accessor().get(args).mapToInt(Integer::intValue).sum(), Calang.this));
      addOperator(IntegerValue.class, "prec", (v, args) -> new IntegerValue(v.get().intValue() - 1, Calang.this));
      addOperator(IntegerValue.class, "succ", (v, args) -> new IntegerValue(v.get().intValue() + 1, Calang.this));
    } {
      addOperator(BytesValue.class, "|.|", (v, args) -> new IntegerValue(v.get().length, this));
    }
  }

  private enum Rejections {
    NO_PARAGRAPH_FOUND("There is no paragraph in the program.. That's unfortunate"),
    UNDEFINED_PARAGRAPH("Unresolved paragrah named %s"),
    UNMAPPABLE_INPUT("Provided input field named %s cannot be mapped on program inputs"),
    UNMAPPED_INPUT("Unable to run the program as not all inputs are given; missing at least %s"),
    UNKNOWN_VARIABLE("The requested scope does not contain any reference to %s symbol"),
    UNSUPPORTED_OPERATOR("Unsupported operator %s on %s"),
    UNRECOGNIZED_INSTRUCTION_TOKEN("Unrecognized instruction token %s"),
    UNRECOGNIZED_PERFORM_DECORATOR("Unrecognized <PERFORM> instruction decorator %s"),
    MALFORMED_PERFORM_INSTRUCTION("Malformed expression PERFORM |%s|"),
    UNSUPPORTED_FROM_BYTES_CONVERSION("Unsupported from-bytes conversion on %s"),
    UNSUPPORTED_FROM_OBJECT_CONVERSION("Unsupported from-object conversion on %s for source |%s|"),
    BOOLEAN_FLAG_IS_NOT_BOOLEAN("Boolean flag %s is not fed with boolean typed, got %s instead"),
    NON_TRANSPILED_INSTRUCTION("Unable to find a way to transpile instruction %s"),
    NON_TRANSPILED_PROGRAM("Unable to find a way to transpile program"),
    NON_TRANSPILED_TYPE("Unable to find a way to transpile type %s");
    private final String messageTemplate;
    Rejections(String tpl) {
      messageTemplate = tpl;
    }
    private AssertionError error(Object...args) {
      return new AssertionError(messageTemplate.formatted(args));
    }
  }

  public final Map < String, Object > run(String programName, Map < String, ? > arguments) {
    return run(getProgram(programName), arguments);
  }

  public final void addType(String typeIdentifier, Function < Calang, TypedValue < ? , ? >> typeFactory) {
    TOKENS.put(typeIdentifier, typeFactory);
  }

  @SuppressWarnings("unchecked")
  public final < T extends TypedValue < T, ? >> void addOperator(Class < T > clz, String operatorName, Operator < T > operator) {
    if (OPERATORS.containsKey(clz)) {
      var ops = (Map < String, Operator < T >> )(Map) OPERATORS.get(clz);
      ops.put(operatorName, operator);
    } else {
      OPERATORS.put(clz, new HashMap < > ());
      addOperator(clz, operatorName, operator);
    }
  }
  @SuppressWarnings("unchecked")
  public final < T extends TypedValue < T, ? >> Map < String, Operator < T >> getOperators(Class < T > clz) {
    var operators = (Map < String, Operator < T >> )(Map) OPERATORS.get(clz);
    return operators == null ? Collections.emptyMap() : operators;
  }

  /******************************************************************** */

  @FunctionalInterface public interface Operator < T extends TypedValue < T, ? >> {
    Object apply(T v, Object...args);
  }

  public static abstract class TypedValue < S extends TypedValue < S, V > /* Fluent API: S is Self type */ , V /* Value type */ > {
    @SuppressWarnings("unchecked") S self() {
      return (S) this;
    }
    @SuppressWarnings("unchecked") Class < S > selfType() {
      return (Class < S > ) self().getClass();
    }
    private final Map <String, Operator < S >> operators;
    private V value;
    protected TypedValue(V value, Calang runtime) {
      this.value = value;
      this.operators = runtime.getOperators(selfType());
    }

    public final V get() {
      return this.value;
    }
    @SuppressWarnings("unchecked") public final void set(Object v) {
      if (value.getClass().isInstance(v))
        value = (V) v;
      else if (this.getClass() == v.getClass())
        value = ((TypedValue < ? , V > )(TypedValue) v).get();
      else if (v instanceof TypedValue tv)
        value = convertFromObject(tv.get());
      else if (v instanceof byte[] data && data.length > 0)
        value = convertFromBytes(data);
      else if (v instanceof String data && data.length() > 0)
        value = convertFromBytes(data.getBytes());
      else
        value = convertFromObject(v);
    }
    protected V convertFromBytes(byte[] data) {
      throw Rejections.UNSUPPORTED_FROM_BYTES_CONVERSION.error(this);
    }
    protected V convertFromObject(Object v) {
      throw Rejections.UNSUPPORTED_FROM_OBJECT_CONVERSION.error(this, v);
    }
    public Object send(String operatorName, Object...args) {
      return sendBinding(operatorName).apply(args);
    }

    public Function <Object[],Object> sendBinding(String operatorName) {
      if (operators.containsKey(operatorName)) {
        var self = self();
        var op = operators.get(operatorName);
        return __ -> op.apply(self, __);
      }
      throw Rejections.UNSUPPORTED_OPERATOR.error(operatorName, this);
    }

    public final S with(Object v) {
      set(v);
      return self();
    }
    public String toString() {
      return new String(bytesValue());
    }
    protected byte[] bytesValue() {
      return this.get().toString().getBytes();
    }
  }

  public static class IntegerValue extends TypedValue <IntegerValue, Integer> {
    public IntegerValue(Calang runtime) {
      this(0, runtime);
    }
    public IntegerValue(int i, Calang runtime) {
      super(Integer.valueOf(i), runtime);
    }
    public IntegerValue(Object v, Calang runtime) {
      this(runtime);
      with(v);
    }

    protected Integer convertFromBytes(byte[] data) {
      return Integer.parseInt(new String(data));
    }
  }

  public static class BooleanValue extends TypedValue <BooleanValue, Boolean> {
    public BooleanValue(Calang runtime) {
      super(Boolean.FALSE, runtime);
    }
    protected Boolean convertFromBytes(byte[] data) {
      return data.length == 0 || (data.length == 1 && data[0] == 0) ? Boolean.FALSE : Boolean.TRUE;
    }
    protected Boolean convertFromObject(Object v) {
      if (v instanceof Integer i) return Integer.valueOf(0).equals(i) ? Boolean.FALSE : Boolean.TRUE;
      else return (Boolean) super.convertFromObject(v);
    }
  }

  public static class BytesValue extends TypedValue < BytesValue, byte[] > {
    public BytesValue(Calang runtime) {
      super(new byte[0], runtime);
    }
    protected byte[] convertFromBytes(byte[] data) {
      return data;
    }
    protected byte[] convertFromObject(Object v) {
      return v.toString().getBytes();
    }

    protected byte[] bytesValue() {
      return get();
    }
  }
  protected byte[] asCalangBytes(Object v) {
    return new BytesValue(this).with(v).get();
  }

  /******************************************************************** */

  public static interface PreInstruction {
    Instruction getInstruction(Scope scope);
    List<String> transpile(Scope scope);
  }

  private PreInstruction getInstruction(String line) {
    if (line.startsWith("  ")) return getInstruction(line.substring(2));
    assert line.indexOf(" ") > 0: "Malformed instruction line |%s|".formatted(line);
    var tokens = line.trim().split("\s+");
    return switch (tokens[0]) {
      case "PERFORM" -> prePerformInstruction(tokens);
      case "PRINT" -> prePrintInstruction(tokens);
      case "STORE" -> preStoreInstruction(tokens);
      case "COMPT" -> preComputeInstruction(tokens);
      case "CALL" -> preCallInstruction(tokens);
      default -> throw Rejections.UNRECOGNIZED_INSTRUCTION_TOKEN.error(tokens[0]);
    };
  }

  private PreInstruction prePerformInstruction(String[] tokens) {
    assert tokens[0].equals("PERFORM");
    return switch (tokens.length) {
      case 2 -> prePerformInstruction(tokens[1], null, false);
      case 4 -> prePerformInstruction(tokens[1], tokens[3],
        switch (tokens[2]) {
          case "IF" -> false;
          case "WHILE" -> true;
          default -> throw Rejections.UNRECOGNIZED_PERFORM_DECORATOR.error(tokens[2]);
        }
      );
      default -> throw Rejections.MALFORMED_PERFORM_INSTRUCTION.error(Arrays.toString(tokens));
    };
  }

  private PreInstruction prePerformInstruction(String paragraphName, String booleanValueSymbol, boolean isLoop) {
    return new PreInstruction() {
      private Supplier < Boolean > lazyValue(Scope scope) {
        if (booleanValueSymbol == null)
          return () -> Boolean.TRUE;
        var variable = scope.getOrDie(booleanValueSymbol);
        if (!(variable instanceof BooleanValue))
          throw Rejections.BOOLEAN_FLAG_IS_NOT_BOOLEAN.error(booleanValueSymbol, variable);
        return ((BooleanValue) variable)::get;
      }
      @Override public JumpInstruction getInstruction(Scope scope) {
        return new JumpInstruction(paragraphName, lazyValue(scope), isLoop);
      }
      @Override public List<String> transpile(Scope scope) {
        return transpilePerformInstruction(scope, paragraphName, booleanValueSymbol, isLoop);
      }
    };
  }

  private PreInstruction preStoreInstruction(String[] tokens) {
    assert tokens[0].equals("STORE");
    assert tokens[1].equals("IN");
    return preStoreInstruction(tokens[2], Arrays.stream(tokens).skip(3).collect(Collectors.joining(" ")));
  }

  private PreInstruction preStoreInstruction(String targetSymbol, String sourceSymbol) {
    return new PreInstruction() {
      @Override public StoreInstruction getInstruction(Scope scope) {
        var target = scope.getOrDie(targetSymbol);
        var lazyV = scope.symbol(sourceSymbol).<Supplier<Object>> map(v -> v::get)
                    .orElse(() -> sourceSymbol);
        return new StoreInstruction(target, lazyV);
      }

      @Override public List<String> transpile(Scope scope) {
        return transpileStoreInstruction(scope, sourceSymbol, targetSymbol);
      }
    };
  }

  private PreInstruction prePrintInstruction(String[] tokens) {
    assert tokens[0].equals("PRINT");
    assert tokens.length > 1;
    return prePrintInstruction(Arrays.stream(tokens).skip(1).toList());
  }

  private PreInstruction prePrintInstruction(List<String> message) {
    return new PreInstruction() {
      @Override public PrintInstruction getInstruction(Scope scope) {
        return new PrintInstruction(message);
      }

      @Override public List<String> transpile(Scope scope) {
        return transpilePrintInstruction(scope, message);
      }
    };
  }

  private PreInstruction preComputeInstruction(String[] tokens) {
    assert tokens[0].equals("COMPT");
    assert tokens[1].equals("IN");
    var target = tokens[2];
    var base = tokens[3];
    var operator = tokens[4];
    var parameters = Arrays.stream(tokens).skip(5).toList();
    return preComptInstruction(target, base, operator, parameters);
  }

  private PreInstruction preComptInstruction(String targetSymbol, String baseSymbol, String operator, List<String> parameterSymbols) {
    return new PreInstruction() {
      @Override public ComptInstruction getInstruction(Scope scope) {
        var target = scope.getOrDie(targetSymbol);
        var parameters = parameterSymbols.stream().map(scope::getOrDie).toList();
        var op = scope.getOrDie(baseSymbol).sendBinding(operator);
        return new ComptInstruction(target, op, parameters);
      }

      @Override public List<String> transpile(Scope scope) {
        return transpileComptInstruction(scope, baseSymbol, operator, parameterSymbols, targetSymbol);
      }
    };
  }

  private PreInstruction preCallInstruction(String[] tokens) {
    assert tokens[0].equals("CALL");
    Function < String, List < VariableBinding >> f = t -> IntStream
      .range(0, (tokens.length - 2) / 3).mapToObj(i -> IntStream.range(0, 3).map(j -> j + 2 + (i * 3)).mapToObj(j -> tokens[j]).toArray(String[]::new))
      .filter(arr -> t.equals(arr[1]))
      .map(arr -> new VariableBinding(arr[0], arr[2])).toList();
    return preCallInstruction(tokens[1], f.apply(">>"), f.apply("<<"));
  }

  private PreInstruction preCallInstruction(String childProgramName, List<VariableBinding> in , List<VariableBinding> out) {
    return new PreInstruction() {
      @Override public CallInstruction getInstruction(Scope scope) {
        return new CallInstruction(childProgramName, in , out);
      }

      @Override public List<String> transpile(Scope scope) {
        return transpileCallInstruction(scope, childProgramName, in, out);
      }
    };
  }
  public static record VariableBinding(String parentSymb, String childSymb) {}

  private static sealed interface Instruction permits JumpInstruction, PrintInstruction, StoreInstruction, CallInstruction, ComptInstruction {}
  private static record JumpInstruction(String paragraphName, Supplier < Boolean > shouldJump, boolean withRehook) implements Instruction {}
  private static record StoreInstruction(TypedValue < ? , ? > target, Supplier < Object > lazyValue) implements Instruction {}
  private static record PrintInstruction(List < String > message) implements Instruction {}
  private static record ComptInstruction(TypedValue < ? , ? > target, Function < Object[], Object > binding, List < TypedValue < ? , ? >> parameters) implements Instruction {}
  private static record CallInstruction(String childProgramName, List < VariableBinding > in , List < VariableBinding > out) implements Instruction {}

  public static interface Program {
    List < String > paragraphNames();
    Paragraph paragraph(String name);
    String headParagraphName();
    Scope scope();
    List < String > getDeclaredOutputs();
    List < String > getDeclaredInputs();

    default Paragraph headParagraph() {
      return paragraph(headParagraphName());
    }
  }
  public static interface Scope {
    Optional < TypedValue < ? , ? >> symbol(String token);
    List <String> symbolList();
    default TypedValue < ? , ? > getOrDie(String token) {
      return getOrDie(token, () -> Rejections.UNKNOWN_VARIABLE.error(token));
    }
    default TypedValue < ? , ? > getOrDie(String token, Supplier < AssertionError > errorLog) {
      return symbol(token).orElseThrow(errorLog);
    }
  }
  @FunctionalInterface public static interface Paragraph {
    List <PreInstruction> instructions();
  }

  /******************************************************************** */

  private Map < String, Program > PROGRAMS = new HashMap < > ();
  private Program getProgram(String programName) {
    if (!PROGRAMS.containsKey(programName)) {
      try {
        var lines = Files.readAllLines(Paths.get("./%s%s.calang".formatted(basePath, programName)));
        return parse(lines.stream().filter(l -> !l.isBlank()).toList());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    assert PROGRAMS.containsKey(programName);
    return PROGRAMS.get(programName);
  }

  private Program parse(List < String > lines) {
    assert lines.stream().noneMatch(String::isBlank);
    HashMap < String, TypedValue < ? , ? >> variables;
    ArrayList < String > inputs;
    ArrayList < String > outputs; {
      variables = new HashMap < > ();
      inputs = new ArrayList < > ();
      outputs = new ArrayList < > ();
      lines.stream().takeWhile(l -> l.startsWith("DECLARE")).forEach(line -> {
        var tokens = line.trim().split("\s+");assert tokens[0].equals("DECLARE");
        var varName = tokens[tokens.length - 2];
        var varType = tokens[tokens.length - 1];
        var variable = Objects.requireNonNull(TOKENS.get(varType), "Unable to resolve type %s".formatted(varType)).apply(this);
        variables.put(varName, variable);
        if (tokens.length == 4) {
          if ("INPUT".equals(tokens[1])) inputs.add(varName);
          else
          if ("OUTPUT".equals(tokens[1])) outputs.add(varName);
        }
      });
    }
    Scope scope = new Scope() {
      @Override public Optional < TypedValue < ? , ? >> symbol(String symbName) {
        return Optional.ofNullable(variables.get(symbName));
      }
      @Override public List < String > symbolList() {
        return new ArrayList < > (variables.keySet());
      }
    };

    record Par(int lineIndex, String name, List < PreInstruction > instructions) implements Paragraph {}

    List < Par > paragraphs;
    Par headParagraph; {
      class InstructionChecker {
        PreInstruction checkedPreInstruction(String line) {
          var preInstruction = getInstruction(line);
          assert preInstruction.getInstruction(scope) != null;
          return preInstruction;
        }
      }
      var checker = new InstructionChecker();
      paragraphs = IntStream.range(0, lines.size()).dropWhile(i -> lines.get(i).startsWith("DECLARE"))
        .filter(i -> !lines.get(i).startsWith("  "))
        .mapToObj(i -> new Par(i, lines.get(i).substring(0, lines.get(i).length() - 1),
          IntStream.range(i + 1, lines.size()).takeWhile(j -> lines.get(j).startsWith("  "))
          .mapToObj(lines::get).map(checker::checkedPreInstruction).toList()
        )).sorted(Comparator.comparing(Par::name)).toList();

      headParagraph = paragraphs.stream().min(Comparator.comparingInt(Par::lineIndex)).orElseThrow(() -> Rejections.NO_PARAGRAPH_FOUND.error());
    }

    return new Program() {
      public String headParagraphName() {
        return headParagraph.name();
      }
      public Paragraph headParagraph() {
        return headParagraph;
      }
      public Paragraph paragraph(String name) {
        return paragraphs.stream().filter(__ -> __.name().equals(name)).findFirst().orElseThrow(() -> Rejections.UNDEFINED_PARAGRAPH.error(name));
      }
      public List < String > paragraphNames() {
        return paragraphs.stream().map(Par::name).toList();
      }
      public Scope scope() {
        return scope;
      }
      public List < String > getDeclaredOutputs() {
        return outputs;
      }
      public List < String > getDeclaredInputs() {
        return inputs;
      }
    };
  }

  private Map < String, Object > run(Program masterProgram, Map < String, ? > arguments) {
    record ExecutionPlan(Program program, Paragraph paragraph, int instrIndex) {
      public String toString() {
        return "%s:%s (%d)".formatted(program.hashCode(), paragraph.hashCode(), instrIndex);
      }
    }
    var planning = new ArrayDeque < ExecutionPlan > () {
      {
        add(new ExecutionPlan(masterProgram, masterProgram.headParagraph(), 0));
      }
    };

    for (var key: arguments.keySet()) masterProgram.scope().getOrDie(key, () -> Rejections.UNMAPPABLE_INPUT.error(key)).set(arguments.get(key));
    for (var key: masterProgram.getDeclaredInputs())
      if (!arguments.containsKey(key)) throw Rejections.UNMAPPED_INPUT.error(key);

    while (!planning.isEmpty()) { // try { Thread.sleep(100); } catch(Exception ignored) {}

      var plan = planning.pollFirst();
      var program = plan.program();
      var paragraph = plan.paragraph();
      var instrIndex = plan.instrIndex();
      if (instrIndex >= paragraph.instructions().size()) continue;
      var scope = plan.program().scope();
      var instr = paragraph.instructions().get(instrIndex).getInstruction(scope);

      if (instr instanceof JumpInstruction jmpInstr) {
        if (jmpInstr.shouldJump.get()) {
          if (jmpInstr.withRehook()) {
            planning.push(new ExecutionPlan(program, paragraph, instrIndex));
          } else {
            planning.push(new ExecutionPlan(program, paragraph, instrIndex + 1));
          }
          planning.push(new ExecutionPlan(program, program.paragraph(jmpInstr.paragraphName()), 0));
        } else {
          planning.push(new ExecutionPlan(program, paragraph, instrIndex + 1));
        }
      } else {
        planning.push(new ExecutionPlan(program, paragraph, instrIndex + 1));

        if (instr instanceof StoreInstruction __) {
          __.target().set(__.lazyValue().get());
        } else
        if (instr instanceof PrintInstruction __) {
          System.out.print(__.message().stream().map(token -> scope.symbol(token).map(Object::toString).orElse(token))
            .map(token -> "\\n".equals(token) ? System.lineSeparator() : token).collect(Collectors.joining(" ")));
        } else
        if (instr instanceof CallInstruction __) {
          var childProgram = getProgram(__.childProgramName());
          var inputs = __.in().stream().collect(Collectors.toMap(VariableBinding::childSymb, binding -> scope.getOrDie(binding.parentSymb()).get()));
          var outputs = run(childProgram, inputs);
          for (var key: __.out()) program.scope().getOrDie(key.parentSymb()).set(outputs.get(key.childSymb()));
        } else
        if (instr instanceof ComptInstruction __) {
          __.target().set(__.binding().apply(__.parameters().toArray()));
        }
      }

    }

    return masterProgram.getDeclaredOutputs().stream().collect(Collectors.toMap(outToken -> outToken, outToken -> masterProgram.scope().getOrDie(outToken).get()));
  }

  /******************************************************************** */

  public List <String> transpile(String programName) {
    return transpile(programName, getProgram(programName));
  }

protected String transpileType(TypedValue<?,?> value) {
  throw Rejections.NON_TRANSPILED_TYPE.error(value.getClass());
}

protected List<String> transpile(String programName, Program program) {
  throw Rejections.NON_TRANSPILED_PROGRAM.error(programName);
}

protected List<String> transpilePerformInstruction(Scope scope, String paragraphName, String flagSymbol, boolean isLoop) {
  throw Rejections.NON_TRANSPILED_INSTRUCTION.error("PERFORM");
}

protected List<String> transpileStoreInstruction(Scope scope, String sourceSymbol, String targetSymbol) {
  throw Rejections.NON_TRANSPILED_INSTRUCTION.error("STORE");
}

protected List<String> transpileComptInstruction(Scope scope, String baseSymbol, String operator, List<String> arguments, String targetSymbol) {
  throw Rejections.NON_TRANSPILED_INSTRUCTION.error("COMPT");
}

protected List<String> transpilePrintInstruction(Scope scope, List<String> tokens) {
  throw Rejections.NON_TRANSPILED_INSTRUCTION.error("PRINT");
}

protected List<String> transpileCallInstruction(Scope scope, String programName, List<VariableBinding> in, List<VariableBinding> out) {
  throw Rejections.NON_TRANSPILED_INSTRUCTION.error("CALL");
}

}
