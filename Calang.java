import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

public class Calang { protected Calang() { this(""); } protected Calang(String basePath) { assert basePath.isEmpty() || basePath.endsWith("/"); this.basePath = basePath;
  TOKENS = new HashMap<>(Map.of(
    "INTEGER", IntegerValue::new, "BYTES", BytesValue::new, "BOOLEAN", BooleanValue::new
  )); OPERATORS = new HashMap<Class<? extends TypedValue<?/*T*/,?>>, Map<String, Operator<?/*T*/>>>();
  { class Accessor { Stream<Integer> get(Object[] args) { return get(args, 0, args.length); } Stream<Integer> get(Object[] args, int start, int size) { return Arrays.stream(args).skip(start).limit(size).map(i -> new IntegerValue(i, Calang.this)).map(IntegerValue::get); } }
    addOperator(IntegerValue.class, "-"   , (v, args) -> new IntegerValue(v.get().intValue() - new Accessor().get(args).mapToInt(Integer::intValue).sum(), Calang.this) );
    addOperator(IntegerValue.class, "+"   , (v, args) -> new IntegerValue(v.get().intValue() + new Accessor().get(args).mapToInt(Integer::intValue).sum(), Calang.this) );
    addOperator(IntegerValue.class, "prec", (v, args) -> new IntegerValue(v.get().intValue()-1,                                                            Calang.this) );
    addOperator(IntegerValue.class, "succ", (v, args) -> new IntegerValue(v.get().intValue()+1,                                                            Calang.this) );
  }
  {
    addOperator(BytesValue.class, "|.|", (v, args) -> new IntegerValue(v.get().length, this));
  }
}
private final String basePath;
private Map<String, Function<Calang, TypedValue<?,?>>> TOKENS;
private Map<Class<? extends TypedValue<?,?>>, Map<String, Operator<?>>> OPERATORS;

private enum Rejections {
  NO_PARAGRAPH_FOUND                   ("There is no paragraph in the program.. That's unfortunate"                    ),
  UNDEFINED_PARAGRAPH                  ("Unresolved paragrah named %s"                                                 ),
  UNMAPPABLE_INPUT                     ("Provided input field named %s cannot be mapped on program inputs"             ),
  UNMAPPED_INPUT                       ("Unable to run the program as not all inputs are given; missing at least %s"   ),
  UNKNOWN_VARIABLE                     ("The requested scope does not contain any reference to %s symbol"              ),
  UNSUPPORTED_OPERATOR                 ("Unsupported operator %s on %s"                                                ),
  UNRECOGNIZED_INSTRUCTION_TOKEN       ("Unrecognized instruction token %s"                                            ),
  UNRECOGNIZED_PERFORM_DECORATOR       ("Unrecognized <PERFORM> instruction decorator %s"                              ),
  MALFORMED_PERFORM_INSTRUCTION        ("Malformed expression PERFORM |%s|"                                            ),
  UNSUPPORTED_FROM_BYTES_CONVERSION    ("Unsupported from-bytes conversion on %s"                                      ),
  UNSUPPORTED_FROM_OBJECT_CONVERSION   ("Unsupported from-object conversion on %s for source |%s|"                     )
  ;
  private String messageTemplate; Rejections(String tpl) { messageTemplate = tpl; }
  AssertionError error(Object... args) { return new AssertionError(messageTemplate.formatted(args)); }
}

public final Map<String, Object> run(String programName, Map<String, ?> arguments) { return run(getProgram(programName), arguments); }

public final void addType(String typeIdentifier, Function<Calang, TypedValue<?,?>> typeFactory) { TOKENS.put(typeIdentifier, typeFactory); }

@SuppressWarnings("unchecked")
public final <T extends TypedValue<T, ?>> void addOperator(Class<T> clz, String operatorName, Operator<T> operator) {
  if (OPERATORS.containsKey(clz)) {
    var ops = (Map<String, Operator<T>>)(Map) OPERATORS.get(clz);
    ops.put(operatorName, operator);
  } else {
    OPERATORS.put(clz, new HashMap<>());
    addOperator(clz, operatorName, operator);
  }
}
@SuppressWarnings("unchecked")
public final <T extends TypedValue<T, ?>> Map<String, Operator<T>> getOperators(Class<T> clz) {
  var operators = (Map<String, Operator<T>>)(Map) OPERATORS.get(clz);
  return operators == null ? Collections.emptyMap() : operators;
}

/******************************************************************** */

@FunctionalInterface public interface Operator<T extends TypedValue<T, ?>> { Object apply(T v, Object... args); }

public static abstract class TypedValue<S extends TypedValue<S,V> /* Fluent API: S is Self type */, V /* Value type */> { @SuppressWarnings("unchecked") S self() { return (S) this; }@SuppressWarnings("unchecked") Class<S> selfType() { return (Class<S>)self().getClass(); }
  private final Map<String, Operator<S>> operators; private V value; protected TypedValue(V value, Calang runtime) { this.value = value; this.operators = runtime.getOperators(selfType()); }

  public final V get() { return this.value; }
  @SuppressWarnings("unchecked") public final void set(Object v) {
    if (value.getClass().isInstance(v))              value = (V) v;                                    else
    if (this.getClass() == v.getClass())             value = ((TypedValue<?, V>)(TypedValue) v).get(); else
    if (v instanceof TypedValue tv)                  value = convertFromObject(tv.get());              else
    if (v instanceof byte[] data && data.length > 0) value = convertFromBytes(data);                   else
    if (v instanceof String data && data.length()>0) value = convertFromBytes(data.getBytes());        else
                                                     value = convertFromObject(v);
  }
  protected V convertFromBytes(byte[] data)               { throw Rejections.UNSUPPORTED_FROM_BYTES_CONVERSION.error(this); }
  protected V convertFromObject(Object v)                 { throw Rejections.UNSUPPORTED_FROM_OBJECT_CONVERSION.error(this, v); }
  public Object send(String operatorName, Object... args) { if(operators.containsKey(operatorName)) return operators.get(operatorName).apply(self(), args); throw Rejections.UNSUPPORTED_OPERATOR.error(operatorName, this); }
  
  public final S      with      (Object v) { set(v); return self(); }
  public       String toString  ()         { return new String(bytesValue()); }
  protected    byte[] bytesValue()         { return this.get().toString().getBytes(); }
}

public static class IntegerValue extends TypedValue<IntegerValue, Integer> {
  public IntegerValue(Calang runtime) { this(0, runtime); }
  public IntegerValue(int i, Calang runtime) { super(Integer.valueOf(i), runtime); }
  public IntegerValue(Object v, Calang runtime) { this(runtime); with(v); }

  protected Integer convertFromBytes(byte[] data)
  { return Integer.parseInt(new String(data)); }
}

public static class BooleanValue extends TypedValue<BooleanValue, Boolean> {
  public BooleanValue(Calang runtime) { super(Boolean.FALSE, runtime); }
  protected Boolean convertFromBytes(byte[] data) { return data.length == 0 || (data.length == 1 && data[0] == 0) ? Boolean.FALSE : Boolean.TRUE; }
  protected Boolean convertFromObject(Object v)   { if(v instanceof Integer i) return Integer.valueOf(0).equals(i) ? Boolean.FALSE : Boolean.TRUE; else return (Boolean) super.convertFromObject(v); }
}

public static class BytesValue extends TypedValue<BytesValue, byte[]> {
  public BytesValue(Calang runtime) { super(new byte[0], runtime); }
  protected byte[] convertFromBytes(byte[] data)      { return data; }
  protected byte[] convertFromObject(Object v)        { return v.toString().getBytes(); }

  protected byte[] bytesValue() { return get(); }
}
protected byte[] asCalangBytes(Object v) { return new BytesValue(this).with(v).get(); }

/******************************************************************** */

private static sealed interface Event permits JumpEvent, PrintEvent, CallEvent, ComputeEvent {}
private static record JumpEvent    (String paragraphName, boolean withRehook)                                                          implements Event { JumpEvent(String paragraphName) { this(paragraphName, false); } }
private static record PrintEvent   (List<String> message)                                                                              implements Event {}
private static record ComputeEvent (TypedValue<?,?> target, TypedValue<?,?> source, String operator, List<TypedValue<?,?>> parameters) implements Event {}
private static record CallEvent    (String childProgramName, List<VariableBinding> in, List<VariableBinding> out)                      implements Event {} static record VariableBinding(String parentSymb, String childSymb) {}  

private static interface Program {
  Paragraph    paragraph(String name);
  String       headParagraphName();
  Scope        scope();
  List<String> getDeclaredOutputs();
  List<String> getDeclaredInputs();

  default Paragraph headParagraph() { return paragraph(headParagraphName()); }
}
@FunctionalInterface private static interface Scope       {         Optional<TypedValue<?,?>> symbol   (String token);
                                                            default TypedValue<?,?>           getOrDie (String token)                                    { return getOrDie(token, () -> Rejections.UNKNOWN_VARIABLE.error(token)); }
                                                            default TypedValue<?,?>           getOrDie (String token, Supplier<AssertionError> errorLog) { return symbol(token).orElseThrow(errorLog); }
                                                                                               }
@FunctionalInterface private static interface Paragraph   { List<Instruction> instructions();          }
@FunctionalInterface private static interface Instruction { Event run(Scope scope);              }

private static Instruction getInstruction(String line, Map<String, TypedValue<?,?>> variables)
{
  if (line.startsWith("  ")) return getInstruction(line.substring(2), variables);
  assert line.indexOf(" ") > 0 : "Malformed instruction line |%s|".formatted(line);
  var tokens = line.trim().split("\s+");
  return switch(tokens[0]) {
    case "PERFORM" -> performInstruction(tokens);
    case "PRINT"   -> printInstruction(tokens);
    case "STORE"   -> storeInstruction(tokens);
    case "COMPT"   -> computeInstruction(tokens);
    case "CALL"    -> callInstruction(tokens);
    default        -> throw Rejections.UNRECOGNIZED_INSTRUCTION_TOKEN.error(tokens[0]);
  };
}

private static Instruction performInstruction(String[] tokens) { assert tokens[0].equals("PERFORM");
  return switch(tokens.length) {
    case 2 -> scope -> new JumpEvent(tokens[1]);
    case 4 -> {
      var testSymbol = tokens[3];
      yield switch(tokens[2]) {
        case "IF"    -> scope -> Boolean.FALSE.equals(scope.getOrDie(testSymbol).get()) ? null : new JumpEvent(tokens[1]);
        case "WHILE" -> scope -> Boolean.FALSE.equals(scope.getOrDie(testSymbol).get()) ? null : new JumpEvent(tokens[1], true);
        default      -> throw Rejections.UNRECOGNIZED_PERFORM_DECORATOR.error(tokens[2]);
      };
    }
    default -> throw Rejections.MALFORMED_PERFORM_INSTRUCTION.error(Arrays.toString(tokens));
  };
}

private static Instruction printInstruction(String[] tokens) { assert tokens[0].equals("PRINT"); assert tokens.length > 1;
  return scope -> new PrintEvent(Arrays.stream(tokens).skip(1)
                  .map(token -> scope.symbol(token).map(Object::toString)
                  .orElse("\\n".equals(token) ? System.lineSeparator() : token)).toList()
  );
}

private static Instruction storeInstruction(String[] tokens) { assert tokens[0].equals("STORE"); assert tokens[1].equals("IN");
  var target = tokens[2];
  var source = Arrays.stream(tokens).skip(3).collect(Collectors.joining(" "));
  return scope -> {
    scope.getOrDie(target).set(scope.symbol(source).map(Object.class::cast).orElse(source));
    return null;
  };
}

private static Instruction computeInstruction(String[] tokens) { assert tokens[0].equals("COMPT"); assert tokens[1].equals("IN");
  var target = tokens[2];
  var base = tokens[3];
  var operator = tokens[4];
  var parameters = Arrays.stream(tokens).skip(5).toList();
  return scope -> {
    var t = scope.getOrDie(target);
    var b = scope.getOrDie(base);
    return new ComputeEvent(t, b, operator, parameters.stream().map(scope::getOrDie).toList());
  };
}

private static Instruction callInstruction(String[] tokens) { assert tokens[0].equals("CALL");
  Function<String, List<VariableBinding>> f = t -> IntStream
                       .range(0, (tokens.length-2)/3).mapToObj(i -> IntStream.range(0, 3).map(j -> j+2+(i*3)).mapToObj(j -> tokens[j]).toArray(String[]::new))
                       .filter(arr -> t.equals(arr[1]))
                       .map(arr -> new VariableBinding(arr[0], arr[2])).toList();
  return __ -> {
    var childProgramName = tokens[1];
    return new CallEvent(childProgramName, f.apply(">>"), f.apply("<<"));
  };
}

/******************************************************************** */

private  Map<String, Program> PROGRAMS = new HashMap<>();
private Program getProgram(String programName) {
  if(! PROGRAMS.containsKey(programName)) {
    try {
      var lines = Files.readAllLines(Paths.get("./%s%s.calang".formatted(basePath, programName)));
      return parse(lines.stream().filter(l -> !l.isBlank()).toList());
    } catch(IOException e) { throw new UncheckedIOException(e); }
  } assert PROGRAMS.containsKey(programName);
  return PROGRAMS.get(programName);
}

private Program parse(List<String> lines) { assert lines.stream().noneMatch(String::isBlank);
  HashMap<String, TypedValue<?,?>> variables; ArrayList<String> inputs; ArrayList<String> outputs; {
    variables = new HashMap<>();
    inputs = new ArrayList<>();
    outputs = new ArrayList<>();
    lines.stream().takeWhile(l -> l.startsWith("DECLARE")).forEach(line -> {
      var tokens = line.trim().split("\s+"); assert tokens[0].equals("DECLARE");
      var varName = tokens[tokens.length - 2];
      var varType = tokens[tokens.length - 1];
      var variable = Objects.requireNonNull(TOKENS.get(varType), "Unable to resolve type %s".formatted(varType)).apply(this);
      variables.put(varName, variable);
      if (tokens.length == 4) {
        if("INPUT" .equals(tokens[1])) inputs.add(varName) ; else
        if("OUTPUT".equals(tokens[1])) outputs.add(varName);
      }
    });
  }

  record Par(int lineIndex, String name, List<Instruction> instructions) implements Paragraph {}
  List<Par> paragraphs; Par headParagraph; {
    class InstructionChecker {
      Instruction instructionOrDie(String line) {
        return getInstruction(line, variables);
      }
    } var checker = new InstructionChecker();
    paragraphs = IntStream.range(0, lines.size()).dropWhile(i -> lines.get(i).startsWith("DECLARE"))
                 .filter(i -> !lines.get(i).startsWith("  "))
                 .mapToObj(i -> new Par(i, lines.get(i).substring(0, lines.get(i).length()-1),
                                  IntStream.range(i+1, lines.size()).takeWhile(j -> lines.get(j).startsWith("  "))
                                           .mapToObj(lines::get).map(checker::instructionOrDie).toList()
                 )).sorted(Comparator.comparing(Par::name)).toList();

    headParagraph = paragraphs.stream().min(Comparator.comparingInt(Par::lineIndex)).orElseThrow(() -> Rejections.NO_PARAGRAPH_FOUND.error());
  }

  return new Program() {
    public String       headParagraphName  ()            { return headParagraph.name(); }
    public Paragraph    headParagraph      ()            { return headParagraph; }
    public Paragraph    paragraph          (String name) { return paragraphs.stream().filter(__ -> __.name().equals(name)).findFirst().orElseThrow(() -> Rejections.UNDEFINED_PARAGRAPH.error(name)); }
    public Scope        scope              ()            { return symbName -> Optional.ofNullable(variables.get(symbName)); }
    public List<String> getDeclaredOutputs ()            { return outputs; }
    public List<String> getDeclaredInputs  ()            { return inputs; }
  };
}

private Map<String, Object> run(Program masterProgram, Map<String, ?> arguments)
{
  record ExecutionPlan(Program program, Paragraph paragraph, int instrIndex) { public String toString() { return "%s:%s (%d)".formatted(program.hashCode(), paragraph.hashCode(), instrIndex); } }
  var planning = new ArrayDeque<ExecutionPlan>() {{ add(new ExecutionPlan(masterProgram, masterProgram.headParagraph(), 0)); }};

  for(var key: arguments.keySet()) masterProgram.scope().getOrDie(key, () -> Rejections.UNMAPPABLE_INPUT.error(key)).set(arguments.get(key));
  for(var key: masterProgram.getDeclaredInputs()) if(! arguments.containsKey(key)) throw Rejections.UNMAPPED_INPUT.error(key);

  while(! planning.isEmpty()) {// try { Thread.sleep(100); } catch(Exception ignored) {}

    var plan        = planning.pollFirst();
    var program     = plan.program();
    var paragraph   = plan.paragraph();
    var instrIndex  = plan.instrIndex();                                   if(instrIndex >= paragraph.instructions().size()) continue;
    var scope       = plan.program().scope();
    var maybeEvent  = paragraph.instructions().get(instrIndex).run(scope);

    if(maybeEvent instanceof JumpEvent jumpEvent && jumpEvent.withRehook())
      planning.push(new ExecutionPlan(program, paragraph, instrIndex));
    else
      planning.push(new ExecutionPlan(program, paragraph, instrIndex+1));

    if(maybeEvent != null) {
      if (maybeEvent instanceof JumpEvent    __) { planning.push(new ExecutionPlan(program, program.paragraph(__.paragraphName()), 0));}                                                                                 else
      if (maybeEvent instanceof PrintEvent   __) { System.out.print(__.message().stream().collect(Collectors.joining(" "))); }                                                                 else
      if (maybeEvent instanceof CallEvent    __) { var childProgram = getProgram(__.childProgramName());
                                                   var inputs = __.in().stream().collect(Collectors.toMap(VariableBinding::childSymb, binding -> scope.getOrDie(binding.parentSymb()).get()));
                                                   var outputs = run(childProgram, inputs);
                                                   for(var key: __.out()) program.scope().getOrDie(key.parentSymb()).set(outputs.get(key.childSymb())); }                                       else
      if (maybeEvent instanceof ComputeEvent __) { __.target().set(__.source().send(__.operator(), __.parameters().toArray())); }
    }
  }

  return masterProgram.getDeclaredOutputs().stream().collect(Collectors.toMap(outToken -> outToken, outToken -> masterProgram.scope().getOrDie(outToken).get()));
}

}
