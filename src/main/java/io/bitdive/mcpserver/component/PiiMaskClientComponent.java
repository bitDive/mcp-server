package io.bitdive.mcpserver.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class PiiMaskClientComponent {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${pii.service.url:http://localhost:8888/mask}")
    private String maskEndpoint;

    public String maskText(String text) {
        // Заголовки
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Тело запроса
        MaskRequest request = new MaskRequest(text);
        HttpEntity<MaskRequest> entity = new HttpEntity<>(request, headers);

        var maskResponse= restTemplate.postForObject(maskEndpoint, entity, MaskResponse.class);
        // Отправка POST и получение MaskResponse
        return maskResponse.maskedText;
    }

    record MaskRequest (String text) {}

    static class MaskResponse {
        @JsonProperty("masked_text")
        private String maskedText;

        private Map<String, List<String>> entities;
    }
}


