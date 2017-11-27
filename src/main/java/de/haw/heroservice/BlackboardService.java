package de.haw.heroservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.haw.heroservice.component.entities.Assignment;
import de.haw.heroservice.component.entities.Callback;
import org.aspectj.lang.annotation.Before;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
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

    @Value("${url.user}")
    private String userUrl;

    private ApacheClient apacheClient = new ApacheClient();

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

    private void createNewRestTemplate() {
        restTemplate = new RestTemplate();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    public ResponseEntity<?> solveTask(Assignment assignment) {

        init();


        String task = assignment.getTask();
        String resource = assignment.getResource();
        String method = assignment.getMethod();
        String data = assignment.getData();



        String location = getLocation(task);

        String host = "http://"+getHost(location);

        createNewRestTemplate();

        headers.set("Authorization", "Token "+loginToken);

        String next = getNext(host+resource);

        List<String> steps = getSteps(host+next);

        List<String> stepsTokens = new ArrayList<>();
        for (String step : steps) {
            stepsTokens.add(getStepToken(host+step));
        }

        String deliveryToken = postTokensInNext(stepsTokens, host+next);




        Callback callbackRequest = new Callback();
        callbackRequest.setId(assignment.getId());
        callbackRequest.setTask(assignment.getTask());
        callbackRequest.setResource(assignment.getResource());
        callbackRequest.setMethod(assignment.getMethod());
        callbackRequest.setData(deliveryToken);
        callbackRequest.setUser(userUrl);
        callbackRequest.setMessage(assignment.getMessage());

        return postOnCallBack(assignment.getCallback(), callbackRequest);
    }

    private ResponseEntity<?> postOnCallBack(String callbackUrl, Callback callbackRequest) {

        HttpEntity<Callback> entity = new HttpEntity<>(callbackRequest, headers);

        return restTemplate.exchange(callbackUrl, HttpMethod.POST, entity, Object.class);
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
       return response.getBody().get("next").asText();
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
}
