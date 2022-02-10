import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

import static java.util.Collections.singletonList;
import static java.util.Collections.emptyList;

public class Calang { protected Calang() {
  TOKENS = new HashMap<>(Map.of(
    "INTEGER", IntegerValue::new, "BYTES", BytesValue::new, "BOOLEAN", BooleanValue::new
  )); OPERATORS = new HashMap<Class, Map>();
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
private Map<String, Function<Calang, TypedValue<?,?>>> TOKENS;
private Map<Class, Map/*String , Operator*/> OPERATORS;

public final Map<String, Object> run(String programName, Map<String, ?> arguments) { return run(getProgram(programName), arguments); }

public final void addType(String typeIdentifier, Function<Calang, TypedValue<?,?>> typeFactory) { TOKENS.put(typeIdentifier, typeFactory); }

@SuppressWarnings("unchecked")
public final <T extends TypedValue<T, ?>> void addOperator(Class<T> clz, String operatorName, Operator<T> operator) {
  if (OPERATORS.containsKey(clz)) {
    var ops = (Map<String, Operator>) OPERATORS.get(clz);
    ops.put(operatorName, operator);
  } else {
    OPERATORS.put(clz, new HashMap<>());
    addOperator(clz, operatorName, operator);
  }
}
@SuppressWarnings("unchecked")
public final <T extends TypedValue<T, ?>> Map<String, Operator<T>> getOperators(Class<T> clz) {
  var operators = (Map<String, Operator<T>>) OPERATORS.get(clz);
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
  protected V convertFromBytes(byte[] data)               { throw new AssertionError("Unsupported from-bytes conversion on %s".formatted(this)); }
  protected V convertFromObject(Object v)                 { throw new AssertionError("Unsupported from-object conversion on %s for source |%s|".formatted(this, v)); }
  public Object send(String operatorName, Object... args) { if(operators.containsKey(operatorName)) return operators.get(operatorName).apply(self(), args); throw new AssertionError("Unsupported operator %s on %s".formatted(operatorName, this)); }
  
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

private static sealed interface Event permits JumpEvent, RehookEvent, PrintEvent, CallEvent, ComputeEvent {}
private static record JumpEvent    (String paragraphName)                                                                              implements Event {}
private static record RehookEvent  ()                                                                                                  implements Event {}
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
                                                            default TypedValue<?,?>           getOrDie (String token)                                    { return getOrDie(token, () -> new AssertionError("The requested scope does not contain any reference to %s symbol".formatted(token))); }
                                                            default TypedValue<?,?>           getOrDie (String token, Supplier<AssertionError> errorLog) { return symbol(token).orElseThrow(errorLog); }
                                                                                               }
@FunctionalInterface private static interface Paragraph   { List<Instruction> instructions();          }
@FunctionalInterface private static interface Instruction { List<Event> run(Scope scope);              }

private static Instruction getInstruction(String line)
{
  if (line.startsWith("  ")) return getInstruction(line.substring(2));
  assert line.indexOf(" ") > 0 : "Malformed instruction line |%s|".formatted(line);
  var tokens = line.trim().split("\s+");
  return switch(tokens[0]) {
    case "PERFORM" -> performInstruction(tokens);
    case "PRINT"   -> printInstruction(tokens);
    case "STORE"   -> storeInstruction(tokens);
    case "COMPT"   -> computeInstruction(tokens);
    case "CALL"    -> callInstruction(tokens);
    default        -> throw new AssertionError("Unrecognized token %s".formatted(tokens[0]));
  };
}

private static Instruction performInstruction(String[] tokens) { assert tokens[0].equals("PERFORM");
  var jumpEvent = new JumpEvent(tokens[1]);
  return switch(tokens.length) {
    case 2 -> scope -> singletonList(jumpEvent);
    case 4 -> {
      var testSymbol = tokens[3];
      yield switch(tokens[2]) {
        case "IF"    -> scope -> Boolean.FALSE.equals(scope.getOrDie(testSymbol).get()) ? emptyList() : singletonList(jumpEvent);
        case "WHILE" -> scope -> Boolean.FALSE.equals(scope.getOrDie(testSymbol).get()) ? emptyList() : List.of(new RehookEvent(), jumpEvent);
        default -> throw new AssertionError("Unrecognized pattern %s".formatted(tokens[2]));
      };
    }
    default -> throw new AssertionError("Malformed expression PERFORM: wrong number of tokens");
  };
}

private static Instruction printInstruction(String[] tokens) { assert tokens[0].equals("PRINT"); assert tokens.length > 1;
  return scope -> singletonList(new PrintEvent(Arrays.stream(tokens).skip(1)
                                                     .map(token -> scope.symbol(token).map(Object::toString).orElse("\\n".equals(token) ? System.lineSeparator() : token)).toList()
  ));
}

private static Instruction storeInstruction(String[] tokens) { assert tokens[0].equals("STORE"); assert tokens[1].equals("IN");
  var target = tokens[2];
  var source = Arrays.stream(tokens).skip(3).collect(Collectors.joining(" "));
  return scope -> {
    scope.getOrDie(target).set(scope.symbol(source).map(Object.class::cast).orElse(source));
    return emptyList();
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
    return singletonList(new ComputeEvent(t, b, operator, parameters.stream().map(scope::getOrDie).toList()));
  };
}

private static Instruction callInstruction(String[] tokens) { assert tokens[0].equals("CALL");
  Function<String, List<VariableBinding>> f = t -> IntStream
                       .range(0, (tokens.length-2)/3).mapToObj(i -> IntStream.range(0, 3).map(j -> j+2+(i*3)).mapToObj(j -> tokens[j]).toArray(String[]::new))
                       .filter(arr -> t.equals(arr[1]))
                       .map(arr -> new VariableBinding(arr[0], arr[2])).toList();
  return __ -> {
    var childProgramName = tokens[1];
    return singletonList(new CallEvent(childProgramName,
      f.apply(">>"), f.apply("<<")
    ));
  };
}

/******************************************************************** */

private  Map<String, Program> PROGRAMS = new HashMap<>();
private Program getProgram(String programName) {
  if(! PROGRAMS.containsKey(programName)) {
    try {
      var lines = Files.readAllLines(Paths.get("%s.calang".formatted(programName)));
      return parse(lines.stream().filter(l -> !l.isBlank()).toList());
    } catch(IOException e) { throw new UncheckedIOException(e); }
  } assert PROGRAMS.containsKey(programName);
  return PROGRAMS.get(programName);
}

private Program parse(List<String> lines) { assert lines.stream().noneMatch(String::isBlank);
  var variables = new HashMap<String, TypedValue<?,?>>();
  var inputs = new ArrayList<String>();
  var outputs = new ArrayList<String>();
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
  record Par(int lineIndex, String name, List<Instruction> instructions) implements Paragraph {}
  var paragraphs = IntStream.range(0, lines.size()).dropWhile(i -> lines.get(i).startsWith("DECLARE"))
           .filter(i -> !lines.get(i).startsWith("  "))
           .mapToObj(i -> new Par(i, lines.get(i).substring(0, lines.get(i).length()-1),
                                  IntStream.range(i+1, lines.size()).takeWhile(j -> lines.get(j).startsWith("  "))
                                           .mapToObj(lines::get).map(Calang::getInstruction).toList()
           )).sorted(Comparator.comparing(Par::name)).toList();
  var headParagraph = paragraphs.stream().min(Comparator.comparingInt(Par::lineIndex)).orElseThrow(() -> new AssertionError("There is no paragraph in the program.. That's unfortunate"));

  return new Program() {
    public String       headParagraphName  ()            { return headParagraph().name(); }
    public Par          headParagraph      ()            { return headParagraph; }
    public Paragraph    paragraph          (String name) { return paragraphs.stream().filter(__ -> __.name().equals(name)).findFirst().orElseThrow(() -> new AssertionError("Unresolved paragrah named %s".formatted(name))); }
    public Scope        scope              ()            { return symbName -> Optional.ofNullable(variables.get(symbName)); }
    public List<String> getDeclaredOutputs ()            { return outputs; }
    public List<String> getDeclaredInputs  ()            { return inputs; }
  };
}

private Map<String, Object> run(Program masterProgram, Map<String, ?> arguments)
{
  record ExecutionPlan(Program program, Paragraph paragraph, int instrIndex) { public String toString() { return "%s:%s (%d)".formatted(program.hashCode(), paragraph.hashCode(), instrIndex); } }
  var planning = new ArrayDeque<ExecutionPlan>() {{ add(new ExecutionPlan(masterProgram, masterProgram.headParagraph(), 0)); }};

  for(var key: arguments.keySet()) masterProgram.scope().getOrDie(key, () -> new AssertionError("Provided input field named %s cannot be mapped on program inputs".formatted(key))).set(arguments.get(key));
  for(var key: masterProgram.getDeclaredInputs()) if(! arguments.containsKey(key)) throw new AssertionError("Unable to run the program as not all inputs are given; missing at least %s".formatted(key));

  while(! planning.isEmpty()) {// try { Thread.sleep(100); } catch(Exception ignored) {}

    var plan        = planning.pollFirst();
    var program     = plan.program();
    var paragraph   = plan.paragraph();
    var instrIndex  = plan.instrIndex();                                   if(instrIndex >= paragraph.instructions().size()) continue;
    var scope       = plan.program().scope();
    var events      = paragraph.instructions().get(instrIndex).run(scope); assert events.stream().noneMatch(RehookEvent.class::isInstance) || events.stream().skip(1).noneMatch(RehookEvent.class::isInstance);

    if(events.isEmpty() || ! (events.get(0) instanceof RehookEvent))
      planning.push(new ExecutionPlan(program, paragraph, instrIndex+1));    

    for(var event: events) {
      if (event instanceof JumpEvent    jumpEvent   ) { planning.push(new ExecutionPlan(program, program.paragraph(jumpEvent.paragraphName()), 0)); } else
      if (event instanceof RehookEvent              ) { planning.push(new ExecutionPlan(program, paragraph, instrIndex)); } else
      if (event instanceof PrintEvent   printEvent  ) { System.out.print(printEvent.message().stream().collect(Collectors.joining(" "))); } else
      if (event instanceof CallEvent    callEvent   ) { var childProgram = getProgram(callEvent.childProgramName());
                                                        var inputs = callEvent.in().stream().collect(Collectors.toMap(VariableBinding::childSymb, binding -> scope.getOrDie(binding.parentSymb()).get()));
                                                        var outputs = run(childProgram, inputs);
                                                        for(var key: callEvent.out()) program.scope().getOrDie(key.parentSymb()).set(outputs.get(key.childSymb())); } else
      if (event instanceof ComputeEvent computeEvent) { computeEvent.target().set(computeEvent.source().send(computeEvent.operator(), computeEvent.parameters().toArray())); }
    }
  }

  return masterProgram.getDeclaredOutputs().stream().collect(Collectors.toMap(outToken -> outToken, outToken -> masterProgram.scope().getOrDie(outToken).get()));
}

}
