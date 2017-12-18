package de.haw.heroservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.haw.heroservice.component.entities.Assignment;
import de.haw.heroservice.component.entities.Callback;
import de.haw.heroservice.component.entities.Election;
import de.haw.heroservice.component.entities.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Component
public class BlackboardService {

    @Autowired
    private RestTemplate restTemplate;

    private Logger logger = Logger.getLogger(BlackboardService.class);


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

    @Value("${uri.deliveries}")
    private String deliveriesUri;

    private String loginToken;

    private HttpHeaders headers = new HttpHeaders();

    ObjectMapper mapper = new ObjectMapper();

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
        List<Object> data = Arrays.asList(assignment.getData());


        //TODO als data?
        String location = getLocation(task);
        String host = "http://"+getHost(location);

        createAuthRestTemplate();

        // check if there is a next.
        String next = resource;
        String tempNext = null;
        do {
            if (!isCriticalSection(host + next)) {
                tempNext = getNext(host + next);
                if (tempNext != null) {
                    next = tempNext;
                }
            }
            //TODO ....
        } while (tempNext!=null);

        //TODO.. be aware of critical section, what to do then????
        List<String> steps = new ArrayList<>();
            steps = getSteps(host + next);

        List<String> stepsTokens = new ArrayList<>();

        try {
            for (String step : steps) {
                stepsTokens.add(mapper.writeValueAsString(getStepToken(host+step)));
            }
        } catch (JsonProcessingException e) {
            return null;
        }




       // String deliveryToken = postTokensInNext(stepsTokens, host+next);


        Callback callbackRequest = new Callback();
        callbackRequest.setId(assignment.getId());
        callbackRequest.setTask(assignment.getTask());
        callbackRequest.setResource(assignment.getResource());
        callbackRequest.setMethod(assignment.getMethod());
        JSONArray jArray = new JSONArray();//
        for (String stepToken : stepsTokens) {
            jArray.put(stepToken);
        }
        callbackRequest.setData(jArray.toList());
        assignment.setData(jArray.toList());
        callbackRequest.setUser(userUri);
        callbackRequest.setMessage(assignment.getMessage());

        return sendResultsToCallback(assignment.getCallback(), callbackRequest);
    }

    private boolean isCriticalSection(String resourceUrl) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);
        boolean criticalSection;
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange(resourceUrl, HttpMethod.POST, entity, ObjectNode.class);
            criticalSection = response.getBody().get("critical_section").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
        return criticalSection;
    }

    private String postTokens(String stepsTokens, String url) {
        String tokens = "tokens";
        String tokensValues;
        try {
            tokens = mapper.writeValueAsString(tokens);
            tokensValues = stepsTokens;
        } catch (JsonProcessingException e) {
           return null;
        }
        //HttpEntity<String> entity = new HttpEntity<>("{\"tokens\" : \"" +stepsTokens+"}",headers);
        HttpEntity<String> entity = new HttpEntity<>("{" + tokens + ":[" + tokensValues + "]}",headers);

        logger.info(entity.getBody());
        ResponseEntity<ObjectNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, ObjectNode.class);

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

        String next;
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange(resourceUrl, HttpMethod.GET, entity, ObjectNode.class);
            next = response.getBody().get("next").asText();
       } catch (Exception e) {
           return null;
       }
       return next;
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
        JsonNode object = objectNode.get("object");
        return object.get("name").asText();
    }

    public ResponseEntity<?> postElection(String heroUrl, Election election) {

        HttpEntity<Election> entity = new HttpEntity<>(election,headers);
        HttpStatus status;
        ResponseEntity<?> response;
        try {
            response = restTemplate.exchange("http://"+heroUrl + electionUri, HttpMethod.POST, entity, ObjectNode.class);
            status = response.getStatusCode();
        } catch (HttpStatusCodeException e) {
            status = e.getStatusCode();
        }
        return new ResponseEntity<>(new Message("election posted!", status.value()), status);
    }

    public ResponseEntity<Message> solveQuest(List<Callback> callbacks) {
        if (!callbacks.isEmpty()) {
            String resource = callbacks.get(0).getResource();
            String task = callbacks.get(0).getTask();
            task = task.substring(1, task.length());
            logger.info(task);
            String quest = questUri + "/" + getQuestNumber(task);

            String questToken;
            ResponseEntity<ObjectNode> response = null;
            String tokens = "";
            String komma = ",";
            List<String> newList = new ArrayList<>();

            for (Callback callback : callbacks) { // make a new list of all tokens
                for (Object obj : callback.getData()) {
                    newList.add(obj.toString());
                }
            }
            // komma separated tokens
            tokens+=StringUtils.join(newList, komma);
            try {
                String location = getLocation(task);
                String host = "http://" + getHost(location);

                questToken = postTokens(tokens, host+resource);
            } catch (HttpStatusCodeException e) {
                logger.error("could not deliver resource token!");
                logger.error(e.getMessage());
                return new ResponseEntity<>(new Message("could not deliver resource token!", 404), HttpStatus.NOT_FOUND);
            }

            if (!StringUtils.isEmpty(questToken)) {

                ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
                ObjectNode tokensNode = rootNode.putObject("tokens");
                tokensNode
                        .put(task, questToken);
                HttpEntity<ObjectNode> entity = new HttpEntity<>(rootNode, headers);
                //createAuthRestTemplate();
                logger.info(restTemplate);
                //String json = "{\"tokens\":{\"" + task + "\":" + questToken + "}}";

                logger.info(entity.getBody());
                HttpStatus status = null;
                String deliverieUrl = blackboardUrl + quest + deliveriesUri;
                logger.info(deliverieUrl);
                try {
                    response = restTemplate.exchange(deliverieUrl, HttpMethod.POST, entity, ObjectNode.class);
                    status = response.getStatusCode();
                    return new ResponseEntity<>(new Message("Maybe delivered?", status.value()), status);
                } catch (HttpStatusCodeException e) {
                    logger.error("could not deliver quest token!");
                    logger.error(e.getMessage());
                    status = e.getStatusCode();
                    return new ResponseEntity<>(new Message("could not deliver quest token", status.value()), e.getStatusCode());
                }
                //return new ResponseEntity<>(new Message("quest token delivered!"), status);
            }
        }
        logger.error("Missing callback address!");
        return new ResponseEntity<>(new Message("Missing callback address!", 400), HttpStatus.BAD_REQUEST);
    }

    private String getQuestNumber(String taskUri) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);

        ResponseEntity<ObjectNode> response = restTemplate.exchange(blackboardUrl + taskUri, HttpMethod.GET, entity, ObjectNode.class);
        return response.getBody().get("object").get("quest").asText();
    }

    public String getRequiredPlayers(String taskUri) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);

        ResponseEntity<ObjectNode> response = restTemplate.exchange(blackboardUrl + taskUri, HttpMethod.GET, entity, ObjectNode.class);
        JsonNode object =  response.getBody().get("object");
        return object.get("required_players").asText();
    }
}
