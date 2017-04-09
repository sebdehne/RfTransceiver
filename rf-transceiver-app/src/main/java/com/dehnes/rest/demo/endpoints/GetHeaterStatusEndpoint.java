package com.dehnes.rest.demo.endpoints;

import com.dehnes.rest.demo.services.HeatingControllerService;
import com.dehnes.rest.server.AbstractRestHandler;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class GetHeaterStatusEndpoint extends AbstractRestHandler {

    private final HeatingControllerService heatingControllerService;

    public GetHeaterStatusEndpoint(HeatingControllerService heatingControllerService) {
        this.heatingControllerService = heatingControllerService;
    }

    @Override
    public void handle(
            String requestURI,
            List<String> fields,
            Map<String, String[]> params,
            JSONObject body,
            BiConsumer<Integer, Object> onDone) {

        JSONObject result = new JSONObject();
        result.put("mode", heatingControllerService.getCurrentMode());
        result.put("target_temperature", heatingControllerService.getTargetTemperature());
        result.put("target_heater_status", heatingControllerService.getConfiguredHeaterTarget());

        onDone.accept(200, result);

    }
}
