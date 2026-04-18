package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String serverUrl;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(RestTemplateBuilder builder, @Value("${stats-server.url:http://localhost:9090}") String serverUrl) {
        this.restTemplate = builder.build();
        this.serverUrl = serverUrl;
    }

    public void saveHit(EndpointHit hit) {
        String url = serverUrl + "/hit";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(hit, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
            log.debug("Хит сохранён: {}", hit);
        } catch (Exception e) {
            log.error("Ошибка при сохранении хита: {}", e.getMessage());
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER));

        if (uris != null && !uris.isEmpty()) {
            uris.forEach(uri -> builder.queryParam("uris", uri));
        }
        if (unique != null && unique) {
            builder.queryParam("unique", true);
        }

        String url = builder.build().encode().toUriString();

        try {
            ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url, ViewStats[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
        }
        return List.of();
    }
}
