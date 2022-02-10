import java.util.HashMap;
import static java.util.Arrays.stream;

public class Example
{
  public static void main(String... args)
  {
    var programName = args[0];
    var inputs = new HashMap<String, String>() {{
      for(int i = 1; i < args.length - 1; i++)
        put("$%s".formatted(args[i]), args[i+1]);
    }};

    var outputs = Calang.run(programName, inputs);
    for(var k : outputs.keySet()) System.out.println("Out %s = %s".formatted(k, outputs.get(k)));
  }
}
