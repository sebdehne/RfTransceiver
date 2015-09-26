package com.dehnes.rest.demo.endpoints;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.json.JSONObject;

import com.dehnes.rest.demo.services.StorageFanSpeedController;
import com.dehnes.rest.server.AbstractRestHandler;

public class SetStorageFanSpeedEndpoint extends AbstractRestHandler {

    private final StorageFanSpeedController storageFanSpeedController;

    public SetStorageFanSpeedEndpoint(StorageFanSpeedController storageFanSpeedController) {
        this.storageFanSpeedController = storageFanSpeedController;
    }


    @Override
    public void handle(String requestURI, List<String> fields, Map<String, String[]> params, JSONObject body, BiConsumer<Integer, Object> onDone) {
        if (!params.containsKey("set_level") || params.get("set_level").length != 1) {
            throw new RuntimeException("Parameter 'set_level' not correct");
        }
        int targetLevel = Integer.parseInt(params.get("set_level")[0]);
        if (storageFanSpeedController.setNewValue(targetLevel)) {
            onDone.accept(200, "OK");
        } else {
            onDone.accept(500, "Could not send requested value");
        }
    }
}
