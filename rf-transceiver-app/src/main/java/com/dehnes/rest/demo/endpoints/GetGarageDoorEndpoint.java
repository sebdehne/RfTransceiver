package com.dehnes.rest.demo.endpoints;

import com.dehnes.rest.demo.services.GarageDoorService;
import com.dehnes.rest.server.AbstractRestHandler;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class GetGarageDoorEndpoint extends AbstractRestHandler {

    private final GarageDoorService garageDoorService;

    public GetGarageDoorEndpoint(GarageDoorService garageDoorService) {
        this.garageDoorService = garageDoorService;
    }

    @Override
    public void handle(String requestURI, List<String> fields, Map<String, String[]> params, JSONObject body, BiConsumer<Integer, Object> onDone) {
        Optional<GarageDoorService.StatusRecord> currentState = garageDoorService.getCurrentState();

        if (currentState.isPresent()) {
            onDone.accept(200, new JSONObject()
                    .put("door_state", currentState.get().isDoorIsOpen() ? "open" : "closed")
                    .put("light_state", currentState.get().isLightIsOn() ? "on" : "off"));
        } else {
            onDone.accept(200, new JSONObject()
                    .put("door_state", "unknown")
                    .put("light_state", "unknown"));
        }

    }
}
