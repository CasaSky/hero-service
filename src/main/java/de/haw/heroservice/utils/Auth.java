package de.haw.heroservice.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Auth {

    private RestTemplate restTemplate;
    private String loginToken;
    private HttpHeaders headers = new HttpHeaders();

    @Value("${user.password}")
    private String password;

    @Value("${user.username}")
    private String username;

    @Value("${url.login}")
    private String loginUrl;


    public Auth() {}


    public RestTemplate init() {
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(username, password));

        login(loginUrl); // login and save token
        createAuthRestTemplate();
        return restTemplate;
    }


    private RestTemplate createAuthRestTemplate() {
        restTemplate = new RestTemplate(getClientHttpRequestFactory());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token "+loginToken);
        return restTemplate;
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        int timeout = 1000;
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        CloseableHttpClient client = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(config)
                .build();
        return new HttpComponentsClientHttpRequestFactory(client);
    }

    private String login(String urlLogin) {

        ObjectNode objectNode = restTemplate.getForObject(urlLogin, ObjectNode.class);
        loginToken = objectNode.get("token").asText();
        return loginToken;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

}
