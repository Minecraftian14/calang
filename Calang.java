import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

import static java.util.Collections.singletonList;
import static java.util.Collections.emptyList;

public class Calang {

public static Map<String, Object> run(String programName, Map<String, ?> arguments) { return run(getProgram(programName), arguments); }

/******************************************************************** */

static final Map<String, Supplier<TypedValue>> TOKENS = new HashMap<>(Map.of(
  "INTEGER", IntegerValue::new, "BYTES", BytesValue::new, "BOOLEAN", BooleanValue::new
));

public static abstract class TypedValue<S extends TypedValue /* Fluent API: S is Self type */, V /* Value type */> {
  private V value; protected TypedValue(V value) { this.value = value; } 

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
  public Object send(String operatorName, Object... args) { throw new AssertionError("Unsupported operator %s on %s".formatted(operatorName, this)); }
  
  @SuppressWarnings("unchecked")
  public final S with           (Object v) { set(v); return (S) this; }
  public       String toString  ()         { return new String(bytesValue()); }
  protected    byte[] bytesValue()         { return new BytesValue().convertFromObject(this.get()); }
}

static class IntegerValue extends TypedValue<IntegerValue, Integer> {
  IntegerValue() { this(0); }
  IntegerValue(int i) { super(Integer.valueOf(i)); }
  IntegerValue(Object v) { this(); with(v); }

  protected Integer convertFromBytes(byte[] data)
  { return Integer.parseInt(new String(data)); }

  public Object send(String operatorName, Object... args)
  { class Accessor { Stream<Integer> get() { return get(0, args.length); } Stream<Integer> get(int start, int size) { return Arrays.stream(args).skip(start - 1).limit(size).map(IntegerValue::new).map(IntegerValue::get); } }
    return switch(operatorName) {
      case "-"    -> new IntegerValue(get().intValue() - new Accessor().get().mapToInt(Integer::intValue).sum());
      case "+"    -> new IntegerValue(get().intValue() + new Accessor().get().mapToInt(Integer::intValue).sum());
      case "prec" -> new IntegerValue(get().intValue()-1);
      case "succ" -> new IntegerValue(get().intValue()+1);
      default     -> super.send(operatorName, args);
    };
  }
}

static class BooleanValue extends TypedValue<BooleanValue, Boolean> {
  BooleanValue() { super(Boolean.FALSE); }
  protected Boolean convertFromBytes(byte[] data) { return data.length == 0 || (data.length == 1 && data[0] == 0) ? Boolean.FALSE : Boolean.TRUE; }
  protected Boolean convertFromObject(Object v)   { if(v instanceof Integer i) return Boolean.valueOf(! Integer.valueOf(0).equals(i)); else return (Boolean) super.convertFromObject(v); }
}

static class BytesValue extends TypedValue<BytesValue, byte[]> {
  BytesValue() { super(new byte[0]); }
  protected byte[] convertFromBytes(byte[] data)      { return data; }
  protected byte[] convertFromObject(Object v)        { return v.toString().getBytes(); }
  public Object send(String operator, Object... args) { return switch(operator) {
    case "|.|" -> new IntegerValue().with(((byte[]) get()).length);
    default    -> super.send(operator, args);
  };}
  protected byte[] bytesValue() { return get(); }
}

/******************************************************************** */

static sealed interface Event permits JumpEvent, RehookEvent, PrintEvent, CallEvent, ComputeEvent {}
static record JumpEvent    (String paragraphName)                                                               implements Event {}
static record RehookEvent  ()                                                                                   implements Event {}
static record PrintEvent   (List<String> message)                                                               implements Event {}
static record ComputeEvent (TypedValue target, TypedValue source, String operator, List<TypedValue> parameters) implements Event {}
static record CallEvent    (String childProgramName, List<VariableBinding> in, List<VariableBinding> out)       implements Event {} static record VariableBinding(String parentSymb, String childSymb) {}  

static interface Program {
  Paragraph    paragraph(String name);
  String       headParagraphName();
  Scope        scope();
  List<String> getDeclaredOutputs();
  List<String> getDeclaredInputs();

  default Paragraph headParagraph() { return paragraph(headParagraphName()); }
}
@FunctionalInterface static interface Scope       { Optional<TypedValue> symbol   (String token);
                                                    default TypedValue   getOrDie (String token)                                    { return getOrDie(token, () -> new AssertionError("The requested scope does not contain any reference to %s symbol".formatted(token))); }
                                                    default TypedValue   getOrDie (String token, Supplier<AssertionError> errorLog) { return symbol(token).orElseThrow(errorLog); }
                                                                                               }
@FunctionalInterface static interface Paragraph   { List<Instruction> instructions();          }
@FunctionalInterface static interface Instruction { List<Event> run(Scope scope);              }

static Instruction getInstruction(String line)
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

static Instruction performInstruction(String[] tokens) { assert tokens[0].equals("PERFORM");
  var jumpEvent = new JumpEvent(tokens[1]);
  return switch(tokens.length) {
    case 2 -> scope -> singletonList(jumpEvent);
    case 4 -> {
      var testSymbol = tokens[3];
      yield switch(tokens[2]) {
        case "IF"    -> scope -> Boolean.TRUE.equals(scope.symbol(testSymbol).get().get()) ? singletonList(jumpEvent) : emptyList();
        case "WHILE" -> scope -> Boolean.TRUE.equals(scope.symbol(testSymbol).get().get()) ? List.of(new RehookEvent(), jumpEvent) : emptyList();
        default -> throw new AssertionError("Unrecognized pattern %s".formatted(tokens[2]));
      };
    }
    default -> throw new AssertionError("Malformed expression PERFORM: wrong number of tokens");
  };
}

static Instruction printInstruction(String[] tokens) { assert tokens[0].equals("PRINT"); assert tokens.length > 1;
  return scope -> singletonList(new PrintEvent(Arrays.stream(tokens).skip(1)
                                                     .map(token -> scope.symbol(token).map(Object::toString).orElse(token)).toList()
  ));
}

static Instruction storeInstruction(String[] tokens) { assert tokens[0].equals("STORE"); assert tokens[1].equals("IN");
  var target = tokens[2];
  var source = Arrays.stream(tokens).skip(3).collect(Collectors.joining(" "));
  return scope -> {
    scope.symbol(source).ifPresentOrElse(scope.getOrDie(target)::set, () -> scope.getOrDie(target).set(source));
    return emptyList();
  };
}

static Instruction computeInstruction(String[] tokens) { assert tokens[0].equals("COMPT"); assert tokens[1].equals("IN");
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

static Instruction callInstruction(String[] tokens) { assert tokens[0].equals("CALL");
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

static Map<String, Program> PROGRAMS = new HashMap<>();
static Program getProgram(String programName) {
  if(! PROGRAMS.containsKey(programName)) {
    try {
      var lines = Files.readAllLines(Paths.get("%s.calang".formatted(programName)));
      return parse(lines.stream().filter(l -> !l.isBlank()).toList());
    } catch(IOException e) { throw new UncheckedIOException(e); }
  } assert PROGRAMS.containsKey(programName);
  return PROGRAMS.get(programName);
}

static Program parse(List<String> lines) { assert lines.stream().noneMatch(String::isBlank);
  var variables = new HashMap<String, TypedValue>();
  var inputs = new ArrayList<String>();
  var outputs = new ArrayList<String>();
  lines.stream().takeWhile(l -> l.startsWith("DECLARE")).forEach(line -> {
    var tokens = line.trim().split("\s+"); assert tokens[0].equals("DECLARE");
    var varName = tokens[tokens.length - 2];
    var varType = tokens[tokens.length - 1];
    var variable = TOKENS.get(varType).get();
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

static Map<String, Object> run(Program masterProgram, Map<String, ?> arguments)
{
  record ExecutionPlan(Program program, Instruction instruction) {}
  var planning = new ArrayDeque<ExecutionPlan>(
    masterProgram.headParagraph().instructions().stream()
           .map(instruction -> new ExecutionPlan(masterProgram, instruction)).toList()
  );

  for(var key: arguments.keySet()) masterProgram.scope().getOrDie(key, () -> new AssertionError("Provided input field named %s cannot be mapped on program inputs".formatted(key))).set(arguments.get(key));
  for(var key: masterProgram.getDeclaredInputs()) if(! arguments.containsKey(key)) throw new AssertionError("Unable to run the program as not all inputs are given; missing at least %s".formatted(key));

  while(! planning.isEmpty()) {
    var plan        = planning.pollFirst();
    var program     = plan.program();
    var instruction = plan.instruction();
    var scope       = plan.program().scope();
    var events      = instruction.run(scope);
    for(var event: events) {
      if (event instanceof JumpEvent    jumpEvent   ) { var parInstructions = new ArrayList<ExecutionPlan>(
                                                          program.paragraph(jumpEvent.paragraphName()).instructions().stream().map(instr -> new ExecutionPlan(program, instr)).toList()
                                                        ); Collections.reverse(parInstructions); parInstructions.forEach(planning::push);
                                                      } else
      if (event instanceof RehookEvent              ) { planning.push(new ExecutionPlan(program, instruction)); } else
      if (event instanceof PrintEvent   printEvent  ) { System.out.println(printEvent.message().stream().collect(Collectors.joining(" "))); } else
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
