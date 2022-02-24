package webengine;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ServerProcess {

    private final static List<String> tpl; static {
        tpl = new ArrayList<>();
        var sc = new Scanner(new ByteArrayInputStream(readFile("template.html")));
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
    static byte[] readFile(String relativeUrl) {
        var baos = new ByteArrayOutputStream();
        var url = ServerProcess.class.getResourceAsStream(relativeUrl);
        var sc = new Scanner(Objects.requireNonNull(url));
        while(sc.hasNextLine()) {
            baos.writeBytes(sc.nextLine().getBytes(UTF_8));
            baos.write('\n');
        }
        sc.close();
        return baos.toByteArray();
    }

    public static void main(String[] args) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/tangle/", ServerProcess::handleTangle);
        server.createContext("/", ServerProcess::handleMain);

        server.start();
    }

    static void handleTangle(HttpExchange exchange) throws IOException {
        var uri = exchange.getRequestURI().toString();
        var response = "Running tangle %s".formatted(programName(uri)).getBytes(UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        answer(exchange, response);
    }

    static void handleMain(HttpExchange exchange) throws IOException {
        var uri = exchange.getRequestURI().toString();
        var response = templatize(readFile("src/%s".formatted(uri)));

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

}
