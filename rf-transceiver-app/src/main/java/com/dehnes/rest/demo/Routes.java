package com.dehnes.rest.demo;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dehnes.rest.demo.endpoints.ControlGarageDoorEndpoint;
import com.dehnes.rest.demo.endpoints.GetGarageDoorEndpoint;
import com.dehnes.rest.demo.endpoints.GetStorageFanSpeedEndpoint;
import com.dehnes.rest.demo.endpoints.RedirectionEndpoint;
import com.dehnes.rest.demo.endpoints.SetStorageFanSpeedEndpoint;
import com.dehnes.rest.demo.endpoints.StaticFileFetcher;
import com.dehnes.rest.server.route.Route;
import com.dehnes.rest.server.route.RouteBuilder;
import com.dehnes.rest.server.route.RoutesFactory;
import com.dehnes.rest.server.utils.Tuple;

public class Routes implements RoutesFactory {

    private final List<Route> routes;

    public Routes(
            GetGarageDoorEndpoint getGarageDoorEndpoint,
            ControlGarageDoorEndpoint controlGarageDoorEndpoint,
            GetStorageFanSpeedEndpoint getStorageFanSpeedEndpoint,
            SetStorageFanSpeedEndpoint setStorageFanSpeedEndpoint,
            RedirectionEndpoint redirectionEndpoint,
            StaticFileFetcher staticFileFetcher) {

        routes = new RouteBuilder()

                .when("^/garage_door")
                  .get(getGarageDoorEndpoint)
                  .when("/action")
                    .post(controlGarageDoorEndpoint)
                  .done()
                .done()

                .when("^/storage_fan")
                  .get(getStorageFanSpeedEndpoint)
                  .when("/action")
                    .post(setStorageFanSpeedEndpoint)
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
