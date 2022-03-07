package webengine;

import webengine.src.FileContent;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public interface SystemFileContent extends FileContent {
    default Path basePath() {
        var hcalFiles = requireNonNull(System.getenv("hcal-files"),
                "Unset environment variable hcal-files: please fill with an absolute path to the folder of hcal files"
        );
        try {
            return Paths.get(hcalFiles);
        } catch(InvalidPathException e) {
            throw new IllegalArgumentException(
                    "The computed path (from environment variable hcal-files=<%s>) cannot be converted to Path object".formatted(hcalFiles),
                    e
            );
        }
    }
}
