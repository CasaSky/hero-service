package de.haw.heroservice.component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.haw.heroservice.BlackboardService;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class TavernaService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${uri.taverna.groups.members}")
    private String tavernaMembersUri;

    @Value("${uri.user}")
    private String userUri;

    @Value("${user.username}")
    private String username;

    @Value("${user.password}")
    private String password;

    @Value("${url.tavernaAdventurers}")
    private String tavernaAdventurersUrl;

    @Autowired
    private BlackboardService blackboardService;

    private List<String> membersUsernames = new ArrayList<>();

    private List<String> heroUrls = new ArrayList<>();

    private static Logger logger = Logger.getLogger(TavernaService.class);

    private HttpHeaders headers = new HttpHeaders();
    private HttpEntity<String> entity;

    public TavernaService() {
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(username, password));

        entity = new HttpEntity<>(null, headers);
    }

    /**
     *
     * @return Message as text.
     */
    public ResponseEntity<?> joinGroup(String tavernaGroupUrl) {
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

        // Add capability group
        String request = "{\"heroclass\":\"Pirat\",\"capabilities\":\"group\",\"url\":\"172.19.0.30:5000/hero\"}";
        HttpEntity<String> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Object> response = restTemplate.exchange(tavernaAdventurersUrl, HttpMethod.PUT, entity, Object.class);

        logger.info("Update taverna adventurers: "+response.getStatusCode());
    }

    // Returns a list of usernames of members in my group.
    public List<String> getMembersUsernames() {

        ResponseEntity<ArrayList> response = restTemplate.exchange(tavernaAdventurersUrl, HttpMethod.GET, entity, ArrayList.class);
        for (Object o : response.getBody()) {
            JSONObject member = (JSONObject) o;
            String userUri = member.getString("user");

            membersUsernames.add(blackboardService.getUsername(userUri));
        }

        membersUsernames.remove(userUri); // without own username.

        return membersUsernames;
    }

    // Returns a list of hero urls
    public List<String> getHeroUrls() {

        for (String name : membersUsernames) {
            ResponseEntity<ObjectNode> response = restTemplate.exchange(tavernaAdventurersUrl + "/" + name, HttpMethod.GET, entity, ObjectNode.class);

           heroUrls.add(response.getBody().get("url").asText());
        }

        return heroUrls;
    }
}
