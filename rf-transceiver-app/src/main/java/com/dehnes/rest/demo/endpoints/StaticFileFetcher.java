package com.dehnes.rest.demo.endpoints;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.dehnes.rest.server.config.TriConsumer;

public class StaticFileFetcher implements TriConsumer<HttpServletRequest, HttpServletResponse, List<String>> {

    public static final String ROOT_PATH = "static";

    @Override
    public void accept(HttpServletRequest request, HttpServletResponse httpServletResponse, List<String> strings) {
        String file = ROOT_PATH + request.getRequestURI();
        String[] split = file.split("/");
        String fileName = split[split.length - 1];

        if (file.endsWith("/")) {
            return; // no directory listings
        }

        try {

            InputStream stream;
            if (new File(file).exists()) {
                stream = new FileInputStream(file);
            } else {
                stream = StaticFileFetcher.class.getClassLoader().getResourceAsStream(file);
            }

            if (stream != null) {
                File f = File.createTempFile("testProxy", fileName);
                FileOutputStream out = new FileOutputStream(f);
                IOUtils.copy(stream, out);
                out.flush();
                out.close();

                String s = filenameToMimeType(fileName);
                if (s == null) {
                    s = URLConnection.guessContentTypeFromName(f.getAbsolutePath());
                }
                if (s == null) {
                    FileInputStream fileInputStream = new FileInputStream(f);
                    s = URLConnection.guessContentTypeFromStream(fileInputStream);
                    fileInputStream.close();
                }

                httpServletResponse.setStatus(200);
                httpServletResponse.setContentType(s);
                ServletOutputStream outputStream = httpServletResponse.getOutputStream();
                FileInputStream input = new FileInputStream(f);
                IOUtils.copy(input, outputStream);
                input.close();
                outputStream.flush();
                outputStream.close();

                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String filenameToMimeType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        }

        return null;
    }

}
