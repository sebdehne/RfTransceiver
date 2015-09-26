package com.dehnes.rest.server;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dehnes.rest.server.route.RoutesFactory;
import com.dehnes.rest.server.utils.Tuple;

public class EmbeddedJetty {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedJetty.class);

    public Server start(int port, RoutesFactory routesFactory) throws Exception {

        // setup the server
        Server server = new Server(new QueuedThreadPool(100, 10, 60000, new ArrayBlockingQueue<>(100)));
        ServerConnector serverConnector = new ServerConnector(server);
        serverConnector.setPort(port);
        server.setConnectors(new ServerConnector[]{serverConnector});
        server.setStopAtShutdown(true);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                Exception e = null;
                Tuple<HttpServletRequest, HttpServletResponse> t = routesFactory.preRouting(request, response);
                final HttpServletRequest req = t.x;
                final HttpServletResponse resp = t.y;

                try {
                    HttpMethod httpMethod = HttpMethod.fromString(baseRequest.getMethod());

                    routesFactory.getRoutes(baseRequest.getHeader("Accept")).stream()
                            .filter(r -> !resp.isCommitted())
                            .filter(r -> r.test(httpMethod, target))
                            .forEach(r -> r.execute(req, resp));

                    if (!resp.isCommitted()) {
                        RestResponseUtils.setJsonResponse(resp, 404, "No handler found for " + httpMethod + " " + target);
                    }
                } catch (Exception ex) {
                    e = ex;
                    RestResponseUtils.internalServer(resp, e);
                } finally {
                    routesFactory.postRouting(req, resp, e);
                }
            }
        });

        // start it
        try {
            server.start();
        } catch (Exception e) {
            logger.error("", e);
            try {
                server.stop();
            } catch (Exception e1) {
                logger.error("", e1);
            }
            server.destroy();
            throw new RuntimeException("Could not start server", e);
        }
        return server;
    }

}
