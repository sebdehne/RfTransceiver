package com.dehnes.rest.server.route;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dehnes.rest.server.utils.Tuple;


public interface RoutesFactory {

    List<Route> getRoutes(String acceptHeader);

    Tuple<HttpServletRequest,HttpServletResponse> preRouting(HttpServletRequest request, HttpServletResponse response);

    void postRouting(HttpServletRequest req, HttpServletResponse resp, Exception onError);
}
