import java.util.Map;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
import static java.util.Arrays.stream;

public class FileSumExample extends Calang
{ {
  addType("FILEC", FileCursor::new);
  addOperator(FileCursor.class, "@"   , (v, args) -> v.openFile(asCalangBytes(args[0])));
  addOperator(FileCursor.class, "/"   , (v, __)   -> v.closeFile()                );
  addOperator(FileCursor.class, "<<<" , (v, __)   -> v.sc.nextLine().getBytes()   );
  addOperator(FileCursor.class, "...?", (v, __)   -> v.sc.hasNextLine()           );
}
public static class FileCursor extends Calang.TypedValue<FileCursor, Object>
{ public FileCursor(Calang runtime) { super(new Object(), runtime); }
  private Scanner sc;
  public FileCursor closeFile()            { sc.close(); return this; }
  public FileCursor openFile(byte[] value) { try { sc = new Scanner(java.util.Objects.requireNonNull(new FileInputStream(new File(new String(value)))));
                                             } catch(IOException e) { throw new UncheckedIOException(e); } return this; }
}

  public static void main(String... args)
  {
    new FileSumExample().run("file_sum", Map.of("$FILE_NAME", "file_sum_datasource.txt"));
  }
}
