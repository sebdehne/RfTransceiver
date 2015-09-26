package com.dehnes.rest.server.route;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;

import com.dehnes.rest.server.config.TriConsumer;

public class RouteBuilder {

    private final RouteBuilderState builderState;

    public RouteBuilder() {
        this.builderState = new RouteBuilderState(new LinkedList<>(), new LinkedList<>());
    }

    protected RouteBuilder(RouteBuilderState routeBuilderState) {
        this.builderState = routeBuilderState;
    }

    public RouteBuilder get(TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h) {
        return new RouteBuilder(builderState.append(HttpMethod.GET, h));
    }

    public RouteBuilder post(TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h) {
        return new RouteBuilder(builderState.append(HttpMethod.POST, h));
    }

    public RouteBuilder put(TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h) {
        return new RouteBuilder(builderState.append(HttpMethod.PUT, h));
    }

    public RouteBuilder delete(TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h) {
        return new RouteBuilder(builderState.append(HttpMethod.DELETE, h));
    }

    public RouteBuilder any(TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h) {
        return new RouteBuilder(builderState.append(null, h));
    }

    public RouteBuilder when(String pattern) {
        return new RouteBuilder(builderState.startSubRoute(pattern));
    }

    public RouteBuilder done() {
        return new RouteBuilder(builderState.endSubRoute());
    }

    public List<Route> build() {
        return builderState.getRoutes();
    }

    public static class RouteBuilderState {

        private final LinkedList<String> patternStack;
        private final LinkedList<Route> routes;

        protected RouteBuilderState(LinkedList<String> patternStack, LinkedList<Route> routes) {
            this.patternStack = patternStack;
            this.routes = routes;
        }

        protected RouteBuilderState startSubRoute(String pattern) {
            LinkedList<String> stack = new LinkedList<>(patternStack);
            stack.add(pattern);
            return new RouteBuilderState(stack, routes);
        }

        protected RouteBuilderState endSubRoute() {
            LinkedList<String> stack = new LinkedList<>(patternStack);
            stack.removeLast();
            return new RouteBuilderState(stack, routes);
        }

        protected RouteBuilderState append(HttpMethod method, TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> h) {
            LinkedList<Route> routes = (LinkedList<Route>) this.routes.clone();
            routes.add(new Route(patternStack.stream().collect(Collectors.joining()), method, h));
            return new RouteBuilderState(patternStack, routes);
        }

        protected List<Route> getRoutes() {
            return Collections.unmodifiableList(routes);
        }
    }
}
