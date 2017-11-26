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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
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
    }

    public ResponseEntity<?> solveTask(Assignment assignment) {

        init();


        String task = assignment.getTask();
        String resource = assignment.getResource();
        String method = assignment.getMethod();
        String data = assignment.getData();



        String location = getLocation(task);

        String host = getHost(location);

        login(loginUrl); // login and save token

        String next = getNext(host+resource);

        List<String> steps = getSteps(host+next);

        List<String> stepsTokens = new ArrayList<>();
        for (String step : steps) {
            stepsTokens.add(getStepToken(step));
        }


        Callback callbackRequest = new Callback();
        callbackRequest.setId(assignment.getId());
        callbackRequest.setTask(assignment.getTask());
        callbackRequest.setResource(assignment.getResource());
        callbackRequest.setMethod(assignment.getMethod());
        callbackRequest.setData(stepsTokens);
        callbackRequest.setUser(userUrl);
        callbackRequest.setMessage(assignment.getMessage());

        return postOnCallBack(assignment.getCallback(), callbackRequest);
    }

    private ResponseEntity<?> postOnCallBack(String callbackUrl, Callback callbackRequest) {

        HttpEntity<Callback> entity = new HttpEntity<>(callbackRequest, headers);

        return restTemplate.exchange(callbackUrl, HttpMethod.POST, entity, Object.class);
    }

    private String getStepToken(String step) {
        //TODO auth with token header

        ObjectNode objectNode = restTemplate.getForObject(step, ObjectNode.class);

        return objectNode.get("token").asText();
    }

    private List<String> getSteps(String nextUrl) {

        //TODO auth with token header

        ObjectNode objectNode = restTemplate.getForObject(nextUrl, ObjectNode.class);


        JsonNode jsonNode = objectNode.get("step_todo");

        List<String> steps = new ArrayList<>();
        for (JsonNode node : jsonNode) {
            steps.add(node.asText());
        }

       return steps;
    }

    private String getNext(String resourceUrl) {

        //TODO auth with token header

        ObjectNode objectNode = restTemplate.getForObject(resourceUrl, ObjectNode.class);
        return objectNode.get("next").asText();
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
