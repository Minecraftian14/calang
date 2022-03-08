package calang;

import java.util.*;
import java.util.stream.*;

import static java.util.function.Predicate.not;

public interface Tangle {

    default List<String> tangle(List<String> content) {
        var declarations = new ArrayList<String[]>();

        for (var line : content) {
            var segment = line;
            while (segment.indexOf("<dfn") > 0) {
                var def = segment.substring(segment.indexOf("<dfn") + "<dfn".length(), segment.indexOf("</code></dfn>"));
                var marker = def.substring(0, def.indexOf("<code>"));
                boolean isInput = marker.contains("input"), isOutput = marker.contains("output");
                def = def.substring(def.indexOf("<code>") + "<code>".length());
                var identifier = def.substring(0, def.indexOf(":")).trim();
                var type = def.substring(def.indexOf(":") + 1).trim();
                declarations.add(new String[]{isInput ? "INPUT" : isOutput ? "OUTPUT" : "", identifier, type});
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
                        segment = segment.substring(0, segment.indexOf("</code></pre>"));
                    }
                    {
                        if (segment.startsWith("PERFORM")) {
                            var joiner = new StringBuilder();
                            while(segment.contains("<a")) {
                                joiner.append(segment, 0, segment.indexOf("<a"));
                                segment = segment.substring(segment.indexOf("<a"));
                                var target = segment.substring(segment.indexOf("#")+1, segment.indexOf(">")).trim().replaceAll("['\"]", "");
                                references.add(target);
                                joiner.append(target);
                                segment = segment.substring(segment.indexOf("</a>")+"</a>".length());
                            }
                            joiner.append(segment);
                            segment = joiner.toString();
                        } else if (segment.startsWith("CALL") && segment.contains("<a href=")) {
                            var target = segment.substring(segment.indexOf("<a href=\"/") + "<a href=\"/".length(), segment.indexOf("\">"));
                            references.add(target);
                            segment = "CALL " + target + segment.substring(segment.indexOf("</a>") + "</a>".length());
                        }
                    }
                    paragraphs.get(currentParagraph).add(segment);
                }
            }

            mainParagraph = paragraphs.keySet().stream()
                    .filter(not(references::contains))
                    .findAny().orElseThrow(() -> new AssertionError("Unable to identify a main paragraph"));
        }

        var arr = new ArrayList<String>();
        {
            for (var d : declarations)
                arr.add("DECLARE %s %s %s%n".formatted(d[0], d[1], d[2]));

            var it = (Iterable<String>) Stream.concat(Stream.of(mainParagraph), paragraphs.keySet().stream()).distinct()::iterator;
            for (var p : it) {
                arr.add("%n%s.%n".formatted(p));
                for (var inst : paragraphs.get(p))
                    arr.add("  %s%n".formatted(inst));
            }
        }
        return arr;
    }

}

