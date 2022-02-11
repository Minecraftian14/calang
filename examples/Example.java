import java.util.HashMap;
import static java.util.Arrays.stream;

public class Example extends Calang
{
  Example(String path) { super(path); }

  public static void main(String... args)
  {
    var basePath = args[0];
    var programName = args[1];

    var inputs = new HashMap<String, String>() {{
      for(int i = 2; i < args.length - 1; i++)
        put("$%s".formatted(args[i]), args[i+1]);
    }};

    var outputs = new Example(basePath).run(programName, inputs);
    for(var k : outputs.keySet()) System.out.println("Out %s = %s".formatted(k, outputs.get(k)));
  }
}
