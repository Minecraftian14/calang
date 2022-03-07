package webengine;

import webengine.src.FileContent;

import java.util.Objects;

public interface SystemFileContent extends FileContent {
    default String basePath() {
        return Objects.requireNonNull(System.getenv("hcal-files"));
    }
}
