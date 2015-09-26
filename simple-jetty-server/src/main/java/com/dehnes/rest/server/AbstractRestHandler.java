package com.dehnes.rest.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.json.JSONObject;

import com.dehnes.rest.server.config.TriConsumer;

public abstract class AbstractRestHandler implements TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> {

    private final List<MimeTypes.Type> allowedTypes = Arrays.asList(
            MimeTypes.Type.APPLICATION_JSON,
            MimeTypes.Type.TEXT_JSON
    );

    private final ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

    @Override
    public void accept(HttpServletRequest request, HttpServletResponse response, List<String> fields) {
        currentRequest.set(request);
        try {

            JSONObject body;
            try {
                if (request.getContentLength() > 0) {
                    assertIsJson(request.getContentType());
                    body = new JSONObject(extractBody(request));
                } else {
                    body = null;
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not read body from request", e);
            }

            handle(request.getRequestURI(),
                    fields,
                    request.getParameterMap(),
                    body,
                    (code, o) -> RestResponseUtils.setJsonResponse(response, code, o));

        } finally {
            currentRequest.remove();
        }
    }

    protected HttpServletRequest getRequest() {
        return currentRequest.get();
    }

    private void assertIsJson(String contentType) {
        MimeTypes.Type typeFound = null;
        for (MimeTypes.Type type : allowedTypes) {
            if (contentType.startsWith(type.getBaseType().toString())) {
                typeFound = type;
            }
        }

        if (typeFound == null) {
            throw new RuntimeException("contentype '" + contentType + "' not supported");
        }
    }

    public static String extractBody(HttpServletRequest request) {
        try {
            String charSet = MimeTypes.getCharsetFromContentType(request.getContentType());
            if (charSet == null) {
                charSet = Charset.defaultCharset().toString();
            }
            return new String(extractRawBody(request), charSet);
        } catch (IOException e) {
            throw new RuntimeException("Could not read request body", e);
        }
    }

    public static byte[] extractRawBody(HttpServletRequest request) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[request.getContentLength()];

            while ((nRead = request.getInputStream().read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();

            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not read request body", e);
        }
    }

    public abstract void handle(
            String requestURI,
            List<String> fields,
            Map<String, String[]> params,
            JSONObject body,
            BiConsumer<Integer, Object> onDone);
}
