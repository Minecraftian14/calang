import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

import static java.util.Arrays.stream;

public class TranspileJs extends Calang
{ TranspileJs(String basePath) { super(basePath); }

  public static void main(String... args)
  {
    var basePath = args[0];

    Stream.of(args).skip(1)
    .map(new TranspileJs(basePath)::transpile)
    .flatMap(List::stream)
    .forEach(System.out::println);
  }

/************************************************************************ */

protected String transpileType(TypedValue<?,?> value) {
  return "CALANG['%s']".formatted(value.getClass().getSimpleName());
}

private String fPar(String paragraphName) {
  return "__%s".formatted(paragraphName);
}
private String fVar(String varName) {
  return "this.%s".formatted(varName);
}

protected List<String> transpile(String programName, Program program) {
  var scope = program.scope();
  var inputs = new HashSet < > (program.getDeclaredInputs());

  var linesToWrite = new ArrayList<String> ();
  linesToWrite.add("var %s = (function() {%nvar def = function({ %s }) { this.printer = new Print();".formatted(programName, inputs.stream().collect(Collectors.joining(", "))));
  for (var s: scope.symbolList()) {
    linesToWrite.add("  %s = %s.newInstance();".formatted(fVar(s), transpileType(scope.getOrDie(s))));
    if (inputs.contains(s))
     linesToWrite.add("    %s.setValue(%s);".formatted(fVar(s), s));
  }
  linesToWrite.add("};%ndef.prototype = {".formatted(programName));
  for (var name: program.paragraphNames()) {
    linesToWrite.add("  %s: async function() {".formatted(fPar(name)));
    var paragraph = program.paragraph(name);
    assert paragraph != null;
    for (var instr: paragraph.instructions())
      linesToWrite.addAll(instr.transpile(scope));
    linesToWrite.add("  },");
  }
  {
    linesToWrite.add("  run: async function() { await %s(); this.printer.flush(); return { %s }; }\n};".formatted(
      fVar(fPar(program.headParagraphName())),
      program.getDeclaredOutputs().stream().map(t -> "%s:%s".formatted(t, fVar(t))).collect(Collectors.joining(", "))
    ));
  }
  linesToWrite.add("return def; })();");
  return linesToWrite;
}

protected List<String> transpilePerformInstruction(Scope scope,
  String paragraphName, String flagSymbol, boolean isLoop
) {
  var parName = fVar(fPar(paragraphName));
  var flagName = flagSymbol == null ? null : fVar(flagSymbol);

  String line; {
    if(isLoop)
      line = "while(%s.getValue()) %s();".formatted(flagName, parName);
    else if(flagName != null)
      line = "if(%s.getValue()) %s();".formatted(flagName, parName);
    else
      line = "%s();".formatted(parName);
  }
  return Collections.singletonList(line);
}

protected List<String> transpileStoreInstruction(Scope scope,
  String sourceSymbol, String targetSymbol
) {
  var target = fVar(targetSymbol);
  String value; {
    if (scope.symbol(sourceSymbol).isPresent())
      value = fVar(sourceSymbol);
    else
      value = "\"%s\"".formatted(sourceSymbol);
  }
  var line = "%s.setValue(%s);".formatted(target, value);
  return Collections.singletonList(line);
}

protected List<String> transpileComptInstruction(Scope scope,
  String baseSymbol, String operator, List<String> arguments, String targetSymbol
) {
  var target = fVar(targetSymbol);
  var base = fVar(baseSymbol);
  var args = arguments.stream().map(this::fVar).collect(Collectors.joining(", "));

  var line = "%s.setValue(%s.sendMessage(\"%s\", [%s]));".formatted(target, base, operator, args);
  return Collections.singletonList(line);
}

protected List<String> transpilePrintInstruction(Scope scope, List<String> tokens) {
  var words = tokens.stream().map(t -> scope.symbol(t).isPresent() ? "{%s}".formatted(fVar(t)) : t)
              .collect(Collectors.joining(" "));
  var line = "this.printer.append(`%s`);".formatted(words);
  return Collections.singletonList(line);
}

protected List<String> transpileCallInstruction(Scope scope,
  String programName, List<VariableBinding> in, List<VariableBinding> out
) {
  var lines = new ArrayList<String>();
  String programIdentifier; {
    programIdentifier = scope.symbol(programName).isPresent() ? programName : "new %s".formatted(programName);
  }
  lines.add("await %s({ %s }).run()".formatted(
    programIdentifier,
    in.stream().map(b -> "%s:%s".formatted(b.childSymb(), fVar(b.parentSymb())))
      .collect(Collectors.joining(","))
  ));
  {
    lines.add(".then(__ => {");
    for (var ob: out)
      lines.add("  %s = __.%s;".formatted(fVar(ob.parentSymb()), ob.childSymb()));
    lines.add("})");
    lines.add(";");
  }
  return lines;
}

}
