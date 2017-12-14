package de.haw.heroservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.haw.heroservice.component.entities.Assignment;
import de.haw.heroservice.component.entities.Callback;
import de.haw.heroservice.component.entities.Election;
import org.aspectj.lang.annotation.Before;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class BlackboardService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${user.username}")
    private String username;

    @Value("${user.password}")
    private String password;

    @Value("${url.blackboard}")
    private String blackboardUrl;

    @Value("${url.login}")
    private String loginUrl;

    @Value("${uri.user}")
    private String userUri;

    @Value("${uri.election}")
    private String electionUri;

    @Value("${uri.quest}")
    private String questUri;

    private String loginToken;

    private HttpHeaders headers = new HttpHeaders();

    public BlackboardService() {

    }

    public void init() {
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(username, password));

        login(loginUrl); // login and save token

    }

    private void createAuthRestTemplate() {
        restTemplate = new RestTemplate();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token "+loginToken);
    }

    public ResponseEntity<?> sendResultsToCallback(String callbackAddress, Callback callback) {

        if (!StringUtils.isEmpty(callbackAddress)) {

            HttpEntity<Callback> entity = new HttpEntity<>(callback, headers);

            ResponseEntity<?> response;
            try {
                response = restTemplate.exchange("http://"+callbackAddress, HttpMethod.POST, entity, Object.class);

            } catch (HttpStatusCodeException e) {
                return new ResponseEntity<>(e.getCause(), e.getStatusCode());
            }
            return response;
        }
        return new ResponseEntity<>("Callback address must be not empty!", HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<?> solveTask(Assignment assignment) {

        init();


        String task = assignment.getTask();
        String resource = assignment.getResource();
        String method = assignment.getMethod();
        String data = assignment.getData();


        //TODO als data?
        String location = getLocation(task);
        String host = "http://"+getHost(location);

        createAuthRestTemplate();

        // check if next is required.
        String next = getNext(host+resource);

        List<String> steps;
        if (next!=null) {
            steps = getSteps(host + next);
        } else {
            steps = getSteps(host+resource);
        }

        List<String> stepsTokens = new ArrayList<>();
        for (String step : steps) {
            stepsTokens.add(getStepToken(host+step));
        }

       // String deliveryToken = postTokensInNext(stepsTokens, host+next);




        Callback callbackRequest = new Callback();
        callbackRequest.setId(assignment.getId());
        callbackRequest.setTask(assignment.getTask());
        callbackRequest.setResource(assignment.getResource());
        callbackRequest.setMethod(assignment.getMethod());
        //JSONArray jArray = new JSONArray();//
        String tokens = "";
        for (String stepToken : stepsTokens) {
            tokens+=stepToken+",";
        }
        tokens = tokens.substring(0,tokens.length()-2);
        callbackRequest.setData(tokens);
        assignment.setData(tokens);
        callbackRequest.setUser(userUri);
        callbackRequest.setMessage(assignment.getMessage());

        return sendResultsToCallback(assignment.getCallback(), callbackRequest);
    }

    private String postTokensInNext(List<String> stepsTokens, String nextUrl) {

        JSONArray jArray = new JSONArray();

        for (String stepToken : stepsTokens) {
            jArray.put(stepToken);
        }

        HttpEntity<String> entity = new HttpEntity<>("{\"tokens\":"+jArray+"}",headers);

        ResponseEntity<ObjectNode> response = restTemplate.exchange(nextUrl, HttpMethod.POST, entity, ObjectNode.class);

        return response.getBody().get("token").asText();
    }

    private String getStepToken(String step) {

        HttpEntity<String> entity = new HttpEntity<>("{}",headers);

        ResponseEntity<ObjectNode> response = restTemplate.exchange(step, HttpMethod.POST, entity, ObjectNode.class);

        return response.getBody().get("token").asText();
    }

    private List<String> getSteps(String nextUrl) {


        HttpEntity<String> entity = new HttpEntity<>(null,headers);

        ResponseEntity<ObjectNode> response = restTemplate.exchange(nextUrl, HttpMethod.GET, entity, ObjectNode.class);

        JsonNode jsonNode = response.getBody().get("steps_todo");

        List<String> steps = new ArrayList<>();
        for (JsonNode node : jsonNode) {
            steps.add(node.asText());
        }

       return steps;
    }

    private String getNext(String resourceUrl) {

        HttpEntity<String> entity = new HttpEntity<>(null,headers);

       ResponseEntity<ObjectNode> response = restTemplate.exchange(resourceUrl, HttpMethod.GET, entity, ObjectNode.class);
       try {
           return response.getBody().get("next").asText();
       } catch (Exception e) {
           return null;
       }
    }

    private String getHost(String locationUri) {

        ObjectNode objectNode = restTemplate.getForObject(blackboardUrl+locationUri, ObjectNode.class);
        return objectNode.get("object").get("host").asText();
    }

    private String getLocation(String taskUri) {

        ObjectNode objectNode = restTemplate.getForObject(blackboardUrl+taskUri, ObjectNode.class);
        return objectNode.get("object").get("location").asText();
    }

    private void login(String urlLogin) {

        ObjectNode objectNode = restTemplate.getForObject(urlLogin, ObjectNode.class);
        loginToken = objectNode.get("token").asText();
    }

    public String getUsername(String userUri) {

        ObjectNode objectNode = restTemplate.getForObject(blackboardUrl+userUri, ObjectNode.class);
        return objectNode.get("name").asText();
    }

    public ResponseEntity<?> postElection(String heroUrl, Election election) {

        HttpEntity<Election> entity = new HttpEntity<>(election,headers);
        return restTemplate.exchange(blackboardUrl+heroUrl+electionUri, HttpMethod.POST, entity, ObjectNode.class);
    }

    public ResponseEntity<?> solveQuest(List<Callback> callbacks) {
        if (!callbacks.isEmpty()) {
            String resource = callbacks.get(0).getResource();
            String task = callbacks.get(0).getTask();
            String tokens = "";
            for (Callback c : callbacks) {
                tokens+=c.getData()+",";
            }
            tokens = tokens.substring(0,tokens.length()-2);

            String postResource = "{\"tokens\":[" + tokens + "}}";

            HttpEntity<String> entity = new HttpEntity<>(postResource, headers);

            ResponseEntity<ObjectNode> response;

            String token;
            try {
                String location = getLocation(task);


                String host = "http://" + getHost(location);
                response = restTemplate.exchange(host + resource, HttpMethod.POST, entity, ObjectNode.class);
                token = response.getBody().get("token").asText();
            } catch (HttpStatusCodeException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }


            if (!StringUtils.isEmpty(token)) {

                String json = "{\"tokens\":{\"" + task + "\":" + token + "}}";
                entity = new HttpEntity<>(json, headers);

                String quest = questUri + getQuest(task);

                try {
                    response = restTemplate.exchange(blackboardUrl + "/blackboard/" + quest, HttpMethod.POST, entity, ObjectNode.class);
                } catch (HttpStatusCodeException e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                }
                return response;
            }
        }
        return new ResponseEntity<>("Can't deliver token.", HttpStatus.BAD_REQUEST);
    }

    private String getQuest(String taskUri) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);

        ResponseEntity<ObjectNode> response = restTemplate.exchange(blackboardUrl + taskUri, HttpMethod.GET, entity, ObjectNode.class);
        return response.getBody().get("quest").asText();
    }

    public String getRequiredPlayers(String taskUri) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);

        ResponseEntity<ObjectNode> response = restTemplate.exchange(blackboardUrl + taskUri, HttpMethod.GET, entity, ObjectNode.class);
        JsonNode object =  response.getBody().get("object");
        return object.get("required_players").asText();
    }
}
