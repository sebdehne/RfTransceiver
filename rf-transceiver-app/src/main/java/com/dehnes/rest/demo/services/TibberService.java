package com.dehnes.rest.demo.services;


import com.dehnes.rest.demo.clients.tibber.TibberPriceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TibberService {
    private static final Logger logger = LoggerFactory.getLogger(TibberService.class);

    private final Clock clock;
    private final TibberPriceClient tibberPriceClient;
    private final long tibberBackOffInMs = 60 * 60 * 1000;
    private Long lastReload = 0L;
    private List<TibberPriceClient.Price> priceCache = List.of();

    public TibberService(TibberPriceClient tibberPriceClient, Clock clock) {
        this.tibberPriceClient = tibberPriceClient;
        this.clock = clock;
    }

    public synchronized boolean isEnergyPriceOK(int numberOfHoursRequired) {
        if ((lastReload + tibberBackOffInMs) < System.currentTimeMillis()) {
            reloadCacheNow();
        }

        Instant now = Instant.now(clock);
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();

        List<TibberPriceClient.Price> todaysPrices = priceCache.stream()
                .filter(price -> price.isValidForDay(today))
                .sorted(Comparator.comparingDouble(o -> o.price))
                .collect(Collectors.toList());

        if (todaysPrices.isEmpty()) {
            return true;
        }

        return todaysPrices.subList(0, numberOfHoursRequired).stream().anyMatch(p -> p.isValidFor(now));
    }

    private void reloadCacheNow() {
        logger.info("Fetching tibber prices...");
        List<TibberPriceClient.Price> prices = tibberPriceClient.getPrices();
        lastReload = System.currentTimeMillis();
        if (prices != null) {
            priceCache = prices;
            logger.info("Fetching tibber prices...SUCCESS");
        } else {
            logger.info("Fetching tibber prices...FAILED");
        }
    }

}

