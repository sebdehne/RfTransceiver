package com.dehnes.rest.demo.clients.tibber;

import com.dehnes.rest.demo.clients.serial.SerialConnection;
import com.dehnes.rest.demo.services.PersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class TibberPriceClient {
    private static final Logger logger = LoggerFactory.getLogger(SerialConnection.class);
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PersistenceService persistenceService;

    public TibberPriceClient(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public List<Price> getPrices() {

        String query = "{\"query\":\"{\\n  viewer {\\n    homes {\\n      currentSubscription {\\n        priceInfo {\\n          today {\\n            total\\n            energy\\n            tax\\n            startsAt\\n          }\\n          tomorrow {\\n            total\\n            energy\\n            tax\\n            startsAt\\n          }\\n        }\\n      }\\n    }\\n  }\\n}\\n\",\"variables\":null,\"operationName\":null}";

        RequestBody body = RequestBody.create(query, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .header("Authorization",
                        "Bearer " + persistenceService.get("tibberAuthBearer", "authkeyMangler"))
                .url("https://api.tibber.com/v1-beta/gql")
                .post(body)
                .build();

        String responseBody = "";
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                responseBody = response.body().string();
                Map jsonRaw = objectMapper.readValue(responseBody, Map.class);
                Map data = (Map) jsonRaw.get("data");
                Map viewer = (Map) data.get("viewer");
                List homes = (List) viewer.get("homes");
                Map home = (Map) homes.get(0);
                Map currentSubscription = (Map) home.get("currentSubscription");
                Map priceInfo = (Map) currentSubscription.get("priceInfo");
                List<Map> prices = new ArrayList();
                prices.addAll((Collection) priceInfo.get("today"));
                prices.addAll((Collection) priceInfo.get("tomorrow"));

                AtomicReference<ZonedDateTime> lastStartsAt = new AtomicReference<>(ZonedDateTime.now().minusMonths(1));
                List<Price> result = new ArrayList<>();

                for (int i = 0; i < prices.size(); i++) {
                    Map current = prices.get(i);
                    Instant startAt = Instant.parse((CharSequence) current.get("startsAt"));
                    Instant endsAt = startAt.plus(30, ChronoUnit.DAYS);
                    if (i + 1 < prices.size()) {
                        endsAt = Instant.parse((CharSequence) prices.get(i + 1).get("startsAt"));
                    }
                    result.add(new Price(
                            startAt,
                            endsAt,
                            (Double) current.get("total")
                    ));
                }

                return result;
            }
        } catch (Exception e) {
            logger.error("responseBody=" + responseBody, e);
        }
        return null;
    }

    public static class Price {
        public final Instant from;
        public final Instant to;
        public final double price;

        public Price(Instant from, Instant to, double price) {
            this.price = price;
            this.from = from;
            this.to = to;
        }

        public boolean isValidFor(Instant input) {
            return (input.isAfter(from) || input.equals(from)) && input.isBefore(to);
        }

        public boolean isValidForDay(LocalDate input) {
            LocalDate fromDay = from.atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate toDay = to.atZone(ZoneId.systemDefault()).toLocalDate();
            return input.equals(fromDay) || input.equals(toDay);
        }
    }

}

