package webengine;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import webengine.src.MyTranspiler;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

public class ServerProcess {

    public static class ServerTranspiler extends MyTranspiler implements SystemFileContent {}

    private final static List<String> tpl; static {
        tpl = new ArrayList<>();
        var baos = new ByteArrayOutputStream(); {
            var url = ServerProcess.class.getResourceAsStream("template.html");
            try(var sc = new Scanner(Objects.requireNonNull(url))) {
                while (sc.hasNextLine())
                    baos.writeBytes("%s%n".formatted(sc.nextLine()).getBytes(UTF_8));
            }
        }
        var sc = new Scanner(new ByteArrayInputStream(baos.toByteArray()));
        while(sc.hasNextLine())
            tpl.add(sc.nextLine());
    }
    static byte[] templatize(byte[] content) {
        var baos = new ByteArrayOutputStream();
        for (String s : tpl) {
            if("##__CONTENT_HERE__##".equals(s)) {
                baos.writeBytes(content);
            } else {
                baos.writeBytes(s.getBytes(UTF_8));
            }
        }
        return baos.toByteArray();
    }

    public static void main(String[] args) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/toc/create", orDie(ServerProcess::handleToc));
        server.createContext("/toc/transpile", orDie(ServerProcess::handleTranspileEntirely));
        server.createContext("/js/", orDie(ServerProcess::handleTranspile));
        server.createContext("/tangle/", orDie(ServerProcess::handleTangle));
        server.createContext("/", orDie(ServerProcess::handleMain));

        server.start();
    }

    static HttpHandler orDie(HttpHandler h) {
        return e -> {
            try {
                h.handle(e);
            } catch (Throwable err) {
                var data = (byte[]) null; {
                    try(var baos = new ByteArrayOutputStream();
                        var pw = new PrintWriter(baos)
                    ) {
                        err.printStackTrace(pw);
                        pw.flush();
                        data = baos.toByteArray();
                    }
                }
                e.sendResponseHeaders(500, data.length);
                try(var os = new BufferedOutputStream(e.getResponseBody())) {
                    os.write(data);
                    os.flush();
                }
            }
        };
    }

    static void handleToc(HttpExchange exchange) throws IOException {
        class FilesExplorer implements SystemFileContent {
            byte[] jsonFileList() throws IOException {
                return hcalFileList()
                        .map("\"%s\""::formatted)
                        .collect(joining(", ", "[", "]")).getBytes(UTF_8);
            }
        }

        var list = new FilesExplorer().jsonFileList();
        answer(exchange, list);
    }

    static void handleTranspileEntirely(HttpExchange exchange) throws IOException {
        var response = (byte[]) null; {
            var lines = new SystemFileContent() {}.hcalFileList()
                    .map(new ServerTranspiler()::transpile)
                    .flatMap(List::stream)
                    .toList();
            response = toBytes(lines, '\n');
        }
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        answer(exchange, response);
    }

    static void handleTangle(HttpExchange exchange) throws IOException {
        var response = (byte[]) null; {
            var uri = exchange.getRequestURI().toString();
            var lines = new ServerTranspiler().tangle(programName(uri));
            response = toBytes(lines, -1);
        }

        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        answer(exchange, response);
    }

    static void handleTranspile(HttpExchange exchange) throws IOException {
        var response = (byte[]) null; {
            var uri = exchange.getRequestURI().toString();
            var lines = new ServerTranspiler().transpile(programName(uri));
            response = toBytes(lines, -1);
        }

        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        answer(exchange, response);
    }

    static void handleMain(HttpExchange exchange) throws IOException {
        var content = (byte[]) null; {
            class FileReader implements SystemFileContent {
                List<String> lines() {
                    var uri = exchange.getRequestURI().toString();
                    return fileContent(programName(uri));
                }
            }
            var lines = new FileReader().lines();
            content = toBytes(lines, '\n');
        }
        var response = templatize(content);

        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        answer(exchange, response);
    }

    static String programName(String rawUri) {
        int index = rawUri.lastIndexOf("/");
        if (index > -1)
            rawUri = rawUri.substring(index + 1);
        return rawUri;
    }

    static void answer(HttpExchange exchange, byte[] data) throws IOException {
        exchange.sendResponseHeaders(200, data.length);
        try(var os = new BufferedOutputStream(exchange.getResponseBody())) {
            os.write(data);
            os.flush();
        }
    }

    static byte[] toBytes(List<String> lines, int separator) {
        var baos = new ByteArrayOutputStream();
        for (String line : lines) {
            baos.writeBytes(line.getBytes(UTF_8));
            if(separator > 0) baos.write(separator);
        }
        return baos.toByteArray();
    }

}
