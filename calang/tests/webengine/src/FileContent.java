package webengine.src;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public interface FileContent {
    Path basePath();

    default Stream<String> hcalFileList() throws IOException {
        return Files.list(basePath())
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(f -> f.endsWith(".hcal"))
                .map(f -> f.substring(0, f.lastIndexOf(".hcal")));
    }

    default List<String> fileContent(String programName) {
        programName = programName + ".hcal";
        var arr = new ArrayList<String>(); {
            var base = basePath();
            try(var bis = new BufferedInputStream(new FileInputStream(base.resolve(programName).toFile()))) {
                var sc = new Scanner(bis);
                while(sc.hasNextLine())
                    arr.add(sc.nextLine());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Collections.unmodifiableList(arr);
    }
}
