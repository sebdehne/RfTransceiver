package com.dehnes.rest.server.route;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;

import com.dehnes.rest.server.config.TriConsumer;

public class Route {

    protected final Pattern pattern;
    protected final HttpMethod method;
    protected final TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h;

    public Route(String pattern, HttpMethod method, TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h) {
        this.pattern = Pattern.compile(pattern);
        this.method = method;
        this.h = h;
    }

    public boolean test(HttpMethod httpMethod, String s) {
        return (this.method == null || this.method == httpMethod) && pattern.matcher(s).matches();
    }

    public void execute(HttpServletRequest req, HttpServletResponse resp) {
        Matcher matcher = pattern.matcher(req.getRequestURI());
        List<String> fields = new LinkedList<>();
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                fields.add(matcher.group(i));
            }
        }

        h.accept(req, resp, fields);
    }
}
