import java.util.Map;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
import static java.util.Arrays.stream;

public class FileSumExample
{ static {
  Calang.addType("FILEC", FileCursor::new);
  Calang.addOperator(FileCursor.class, "@"   , (v, args) -> v.openFile(new Calang.BytesValue().with(args[0]).get()));
  Calang.addOperator(FileCursor.class, "/"   , (v, __)   -> v.closeFile()                );
  Calang.addOperator(FileCursor.class, "<<<" , (v, __)   -> v.sc.nextLine().getBytes()   );
  Calang.addOperator(FileCursor.class, "...?", (v, __)   -> v.sc.hasNextLine()           );
}
public static class FileCursor extends Calang.TypedValue<FileCursor, Object>
{ public FileCursor() { super(new Object()); }
  private Scanner sc;
  public FileCursor closeFile()            { sc.close(); return this; }
  public FileCursor openFile(byte[] value) { try { sc = new Scanner(java.util.Objects.requireNonNull(new FileInputStream(new File(new String(value)))));
                                             } catch(IOException e) { throw new UncheckedIOException(e); } return this; }
}

  public static void main(String... args)
  {
    Calang.run("file_sum", Map.of("$FILE_NAME", "file_sum_datasource.txt"));
  }
}
