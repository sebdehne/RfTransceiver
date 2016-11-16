package com.dehnes.rest.demo;

import com.dehnes.rest.demo.endpoints.*;
import com.dehnes.rest.server.route.Route;
import com.dehnes.rest.server.route.RouteBuilder;
import com.dehnes.rest.server.route.RoutesFactory;
import com.dehnes.rest.server.utils.Tuple;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class Routes implements RoutesFactory {

    private final List<Route> routes;

    public Routes(
            GetGarageDoorEndpoint getGarageDoorEndpoint,
            ControlGarageDoorEndpoint controlGarageDoorEndpoint,
            RedirectionEndpoint redirectionEndpoint,
            StaticFileFetcher staticFileFetcher,
            GetHeaterStatusEndpoint getHeaterStatusEndpoint,
            HeaterControllerEndpoint heaterControllerEndpoint) {

        routes = new RouteBuilder()

                .when("^/api")

                  .when("/garage_door")
                    .get(getGarageDoorEndpoint)
                    .when("/action")
                      .post(controlGarageDoorEndpoint)
                    .done()
                  .done()

                  .when("/heater")
                    .get(getHeaterStatusEndpoint)
                    .when("/action")
                      .post(heaterControllerEndpoint)
                    .done()
                  .done()

                .done()

                .when(".*").get(staticFileFetcher).done()

                .when(".*").any(redirectionEndpoint).done()

                .build();
    }

    @Override
    public List<Route> getRoutes(String acceptHeader) {
        return routes;
    }

    @Override
    public Tuple<HttpServletRequest, HttpServletResponse> preRouting(HttpServletRequest request, HttpServletResponse response) {
        return new Tuple<>(request, response);
    }

    @Override
    public void postRouting(HttpServletRequest req, HttpServletResponse resp, Exception onError) {

    }
}
