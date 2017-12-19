package de.haw.heroservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.entities.*;
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

    @Autowired
    private Mutexstate mutextstate;

    @Autowired
    private List<Mutex> requests;

    @Autowired
    private List<Mutex> replies;

    private TavernaService tavernaService = new TavernaService();

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


        String location = getLocation(task);
        String host = "http://"+getHost(location);

        createAuthRestTemplate();

        // check if there is a next.
        String next = resource;
        String tempNext;
        boolean isCriticalSection = false;
        do {
            // Check if there is no critical section
            if (!isCriticalSection(host + next)) {
                tempNext = getNext(host + next);
                if (tempNext != null) {
                    next = tempNext;
                }
            } else {
                // We have a critical section
                mutextstate.setState(State.WANTING);
                List<String> heroes = tavernaService.getAllHeroes();
                postRequestMsg(heroes);
                List<String> heroes2 = new ArrayList<>();
                for (Mutex reply : replies) {
                    String user = reply.getUser();
                    for (String hero : heroes) {
                        String user2 = getUser(hero);
                        if (user.equals(user2)) {
                            heroes2.add(hero);
                            break;
                        }
                    }

                }
                if (heroes.size() > heroes2.size()) {
                    heroes.removeAll(heroes2);
                    getMutexState(heroes, 3);
                }

                isCriticalSection = true;
               break;
            }
        } while (tempNext!=null);

        Callback callbackRequest = new Callback();

        // Prepare tokens.
        List<String> tokens = new ArrayList<>();
        if (!isCriticalSection) {
            List<String> steps;
            steps = getSteps(host + next);
            try {
                for (String step : steps) {
                    tokens.add(mapper.writeValueAsString(getStepToken(host + step)));
                }
            } catch (JsonProcessingException e) {
                return null;
            }
            callbackRequest.setResource(assignment.getResource());
        } else {
            callbackRequest.setResource(next);
        }

        callbackRequest.setId(assignment.getId());
        callbackRequest.setTask(assignment.getTask());
        callbackRequest.setMethod(assignment.getMethod());
        JSONArray jArray = new JSONArray();//
        for (String token : tokens) {
            jArray.put(token);
        }
        callbackRequest.setData(jArray.toList());
        assignment.setData(jArray.toList());
        callbackRequest.setUser(userUri);
        callbackRequest.setMessage(assignment.getMessage());

        return sendResultsToCallback(assignment.getCallback(), callbackRequest);
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
        String questToken;
        //TODO make it safe
        String task = callbacks.get(0).getTask();
        String resource = callbacks.get(0).getResource();
        String quest = questUri + "/" + getQuestNumber(task);

        if (!callbacks.isEmpty() && callbacks.size() == 1 && callbacks.get(0).getData().isEmpty()) {
            Callback callback = callbacks.get(0);
            String location = getLocation(callback.getTask());
            String host = "http://" + getHost(location);
            mutextstate.setState(State.HELD);
            questToken = enterCriticalSection(host + callback.getResource());
            mutextstate.setState(State.RELEASED);
            postReplyMsg();
        } else {

            task = task.substring(1, task.length());
            logger.info(task);

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
        }

        if (!StringUtils.isEmpty(questToken)) {

            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ObjectNode tokensNode = rootNode.putObject("tokens");
            tokensNode
                    .put(task, questToken);
            HttpEntity<ObjectNode> entity = new HttpEntity<>(rootNode, headers);
            logger.info(restTemplate);
            //String json = "{\"tokens\":{\"" + task + "\":" + questToken + "}}";

            logger.info(entity.getBody());
            HttpStatus status = null;
            String deliverieUrl = blackboardUrl + quest + deliveriesUri;
            logger.info(deliverieUrl);
            ResponseEntity<ObjectNode> response;

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

    public String getUser(String heroUrl) {
            HttpEntity<String> entity = new HttpEntity<>(null,headers);

            ResponseEntity<ObjectNode> response = restTemplate.exchange(heroUrl, HttpMethod.GET, entity, ObjectNode.class);
            return response.getBody().get("user").asText();
    }

    private boolean getMutexState(List<String> heroes, int counter) {

        counter--;

        HttpEntity<Mutex> entity = new HttpEntity<>(null,headers);
        List<String> problems = new ArrayList<>();
        for (String hero : heroes) {
            String mutexstateUri = getMutexState(hero);
            ResponseEntity<ObjectNode> response = restTemplate.exchange(hero+mutexstateUri, HttpMethod.GET, entity, ObjectNode.class);
            String state = response.getBody().get("state").asText();
            if (state.equals(State.WANTING) || state.equals(State.HELD)) {
                problems.add(hero);
            }
        }
        if (!problems.isEmpty() && counter > 0) {
            try {
                wait(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getMutexState(problems, counter);
        } else {
            return true;
        }

    }

    private void postRequestMsg(List<String> heroes) {
        Mutex mutex = new Mutex();
        mutex.setMsg(Msg.REQUEST);
        mutex.setTime(mutextstate.incrementTime());
        mutex.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        mutex.setUser(userUri);
        HttpEntity<Mutex> entity = new HttpEntity<>(mutex,headers);
        for (String hero : heroes) {
            String mutexUri = getMutex(hero);
            restTemplate.exchange(hero+mutexUri, HttpMethod.POST, entity, ObjectNode.class);
        }
    }

    private void postReplyMsg() {
        Mutex reply = new Mutex();
        reply.setMsg(Msg.REPLYOK);
        reply.setTime(mutextstate.incrementTime());
        reply.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        reply.setUser(userUri);
        HttpEntity<Mutex> entity = new HttpEntity<>(reply,headers);
        for (Mutex mutex : requests) {
            restTemplate.exchange(mutex.getReply(), HttpMethod.POST, entity, ObjectNode.class);
        }
    }

    private String getMutex(String hero) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);
        ResponseEntity<ObjectNode> response = restTemplate.exchange(hero, HttpMethod.GET, entity, ObjectNode.class);
        return response.getBody().get("mutex").asText();
    }

    private String getMutexState(String hero) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);
        ResponseEntity<ObjectNode> response = restTemplate.exchange(hero, HttpMethod.GET, entity, ObjectNode.class);
        return response.getBody().get("mutexstate").asText();

    }

    private String enterCriticalSection(String resourceUrl) {
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        String criticalToken;
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange(resourceUrl, HttpMethod.POST, entity, ObjectNode.class);
            criticalToken = response.getBody().get("token").asText();
        } catch (HttpStatusCodeException e) {
            return null;
        }
        return criticalToken;
    }

    private boolean isCriticalSection(String resourceUrl) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);
        boolean criticalSection;
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange(resourceUrl, HttpMethod.GET, entity, ObjectNode.class);
            criticalSection = response.getBody().get("critical_section").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
        return criticalSection;
    }
}
