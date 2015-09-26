package com.dehnes.rest.demo.endpoints;

import com.dehnes.rest.demo.services.GarageDoorService;
import com.dehnes.rest.server.AbstractRestHandler;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ControlGarageDoorEndpoint extends AbstractRestHandler {

    private final GarageDoorService garageDoorService;

    public ControlGarageDoorEndpoint(GarageDoorService garageDoorService) {
        this.garageDoorService = garageDoorService;
    }

    @Override
    public void handle(String requestURI, List<String> fields, Map<String, String[]> params, JSONObject body, BiConsumer<Integer, Object> onDone) {
        if (!params.containsKey("command") || params.get("command").length != 1) {
            throw new RuntimeException("Parameter 'command' not correct");
        }
        String command = params.get("command")[0];
        boolean result;
        if ("open".equalsIgnoreCase(command)) {
            result = garageDoorService.sendOpenCommand();
        } else if ("close".equalsIgnoreCase(command)) {
            result = garageDoorService.sendCloseCommand();
        } else {
            throw new RuntimeException("Command can only be open/close");
        }

        if (result) {
            onDone.accept(200, "Done");
        } else {
            onDone.accept(500, "Could not send value right now");
        }
    }

}
