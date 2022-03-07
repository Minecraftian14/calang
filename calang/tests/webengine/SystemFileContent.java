package webengine;

import webengine.src.FileContent;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public interface SystemFileContent extends FileContent {
    default Path basePath() {
        return Paths.get(requireNonNull(System.getenv("hcal-files")));
    }
}
