package calang;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

public class Tangle {

    public static void main(String... args) {

        var content = data();

        var declarations = new ArrayList<String[]>();

        for (var line : content) {
            var segment = line;
            while (segment.indexOf("<dfn><code>") > 0) {
                var def = segment.substring(segment.indexOf("<dfn><code>") + "<dfn><code>".length(), segment.indexOf("</code></dfn>"));
                var identifier = def.substring(0, def.indexOf(":")).trim();
                var type = def.substring(def.indexOf(":") + 1).trim();
                declarations.add(new String[]{identifier, type});
                segment = segment.substring(segment.indexOf("</dfn>"));
            }
        }

        var paragraphs = new HashMap<String, List<String>>();
        var mainParagraph = (String) null;
        {
            var references = new HashSet<String>();
            var currentParagraph = (String) null;
            var collect = false;

            for (var line : content) {
                var segment = line;
                if (segment.contains("<a name")) {
                    segment = segment.substring(segment.indexOf("<a name"));
                    segment = segment.substring(segment.indexOf("=\"") + 2);
                    currentParagraph = segment.substring(0, segment.indexOf("\""));
                    paragraphs.put(currentParagraph, new ArrayList<>());
                }

                if (segment.contains("<pre><code>")) {
                    collect = true;
                    segment = segment.substring(segment.indexOf("<pre><code>") + "<pre><code>".length());
                }
                if (collect) {
                    if (segment.contains("</code></pre>")) {
                        collect = false;
                        segment = segment.substring(0, segment.indexOf("</code>"));
                    }
                    {
                        if (segment.startsWith("PERFORM")) {
                            var target = segment.substring(segment.indexOf("<a href=\"#") + "<a href=\"#".length(), segment.indexOf("\">"));
                            references.add(target);
                            segment = "PERFORM " + target + segment.substring(segment.indexOf("</a>") + "</a>".length());
                        }
                    }
                    paragraphs.get(currentParagraph).add(segment);
                }
            }

            mainParagraph = paragraphs.keySet().stream().filter(p -> !references.contains(p)).findAny().orElseThrow(() -> new AssertionError("Unable to identify a main paragraph"));
        }

        {
            for (var d : declarations)
                System.out.printf("DECLARE %s %s%n", d[0], d[1]);

            var it = (Iterable<String>) Stream.concat(Stream.of(mainParagraph), paragraphs.keySet().stream()).distinct()::iterator;
            for (var p : it) {
                System.out.printf("%n%s.%n", p);
                for (var inst : paragraphs.get(p))
                    System.out.printf("  %s%n", inst);
            }
        }

    }

    private static List<String> data() {
        try {
            return Files.readAllLines(Paths.get("./password_input.html"));
        }
        catch (IOException e) {throw new UncheckedIOException(e);}
    }

}

