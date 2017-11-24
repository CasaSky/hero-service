package de.haw.heroservice.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class TavernaService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${uri.taverna.groups.members}")
    private String tavernaMembersUri;

    @Value("${user.username}")
    private String username;

    @Value("${user.password}")
    private String password;

    /**
     *
     * @return Message as text.
     */
    public ResponseEntity<?> joinGroup(String tavernaGroupUrl) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(username, password));

        try {

            ResponseEntity<Object> response = restTemplate.exchange(tavernaGroupUrl+tavernaMembersUri, HttpMethod.POST, entity, Object.class);
            HttpStatus statusCode = response.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
            }
        } catch (HttpStatusCodeException e) {

            System.out.println("Youre cridentials are wrong, please verify.");
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
        return new ResponseEntity<>("Can't join the group.",HttpStatus.CONFLICT);
    }
}
