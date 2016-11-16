package com.dehnes.rest.demo.endpoints;

import com.dehnes.rest.demo.services.HeatingControllerService;
import com.dehnes.rest.server.AbstractRestHandler;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class HeaterControllerEndpoint extends AbstractRestHandler {

    private final HeatingControllerService heatingControllerService;

    public HeaterControllerEndpoint(HeatingControllerService heatingControllerService) {
        this.heatingControllerService = heatingControllerService;
    }

    @Override
    public void handle(
            String requestURI,
            List<String> fields,
            Map<String, String[]> params,
            JSONObject body,
            BiConsumer<Integer, Object> onDone) {

        Action a = Action.valueOf(body.getString("action"));
        switch (a) {
            case set_target_temperature:
                heatingControllerService.setTargetTemperature(body.getInt("value"));
                break;
            case switch_automatic:
                heatingControllerService.setAutomaticMode(body.getBoolean("value"));
                break;
            case switch_heater:
                if (body.getBoolean("value")) {
                    heatingControllerService.switchOn();
                } else {
                    heatingControllerService.switchOff();
                }
                break;
            default:
                throw new RuntimeException("Do not know action " + a);
        }

        onDone.accept(200, null);

    }

    private enum Action {
        switch_automatic,
        switch_heater,
        set_target_temperature
    }
}
