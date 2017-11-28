package de.haw.heroservice.component;

import de.haw.heroservice.HeroServiceApplication;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

    @Value("${url.tavernaAdventurers}")
    private String tavernaAdventurersUrl;

    private static Logger logger = Logger.getLogger(TavernaService.class);

    /**
     *
     * @return Message as text.
     */
    public ResponseEntity<?> joinGroup(String tavernaGroupUrl) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(username, password));

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        try {

            ResponseEntity<Object> response = restTemplate.exchange(tavernaGroupUrl+tavernaMembersUri, HttpMethod.POST, entity, Object.class);
            HttpStatus statusCode = response.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
                return new ResponseEntity<>("Hero joined the group.", statusCode);
            }
        } catch (HttpStatusCodeException e) {

            System.out.println("Youre cridentials are wrong, please verify.");
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
        return new ResponseEntity<>("Can't join the group", HttpStatus.CONFLICT);
    }

    public void updateUs() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(username, password));


        // Add capability group
        String request = "{\"heroclass\":\"Pirat\",\"capabilities\":\"group\",\"url\":\"172.19.0.30:5000/hero\"}";
        HttpEntity<String> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Object> response = restTemplate.exchange(tavernaAdventurersUrl, HttpMethod.PUT, entity, Object.class);

        logger.info("Update taverna adventurers: "+response.getStatusCode());
    }

    //TODO
    public List<String> getMembersUsernames() {

        return null;
    }
}
