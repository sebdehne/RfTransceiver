package com.dehnes.rest.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.http.MimeTypes;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;


public class RestResponseUtils {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void jsonResponse(HttpServletResponse r, Object body) {
        setJsonResponse(r, 200, body);
    }

    public static void setJsonResponse(HttpServletResponse r, int statusCode, Object body) {
        try {
            r.reset();
            r.setStatus(statusCode);
            r.setContentType(MimeTypes.Type.APPLICATION_JSON.asString());
            String bodyStr;
            if (body instanceof String) {
                bodyStr = new JSONObject().put("response", body.toString()).toString();
            } else if (body instanceof JSONObject || body instanceof JSONArray) {
                bodyStr = body.toString();
            } else {
                bodyStr = gson.toJson(body);
            }
            r.getOutputStream().write(bodyStr.getBytes("UTF-8"));
            r.getOutputStream().flush();
            r.getOutputStream().close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void internalServer(HttpServletResponse r, Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            message = t.getClass().getSimpleName();
        }
        setJsonResponse(r, 500, message);
    }

}
