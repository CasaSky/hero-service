package de.haw.heroservice.component.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.entities.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class MutexAlgorithm {

    @Value("${user.username}")
    private String username;

    @Value("${uri.user}")
    private String userUri;

    @Autowired
    private BlackboardService blackboardService;

    @Autowired
    private TavernaService tavernaService;

    @Autowired
    private Mutexstate mutexstate;

    @Autowired
    private List<MutexMessage> requests;

    @Autowired
    private List<MutexMessage> replies;

    @Autowired
    private RestTemplate restTemplate;

    private HttpHeaders headers = new HttpHeaders();

    @Value("${user.password}")
    private String password;

    @Value("${url.login}")
    private String loginUrl;

    private String loginToken;


//    TODO: Prüfung auf Critical Section (wo auch auf weitere "next" geprüft wird)



//    Compute bigger time of two given times and increment bigger time by 1
    public int incrementBiggerTime(int ownTime, int givenTime) {
        return givenTime > ownTime ? givenTime + 1 : ownTime + 1;
    }

    public void startMutex(MutexMessage mutexMessage){
        mutexstate.setTime(incrementBiggerTime(mutexstate.getTime(), mutexMessage.getTime()));

        if (mutexMessage.getMsg().equals(Msg.REQUEST)) {
            if (mutexstate.getState().equals(State.RELEASED)
                    || (mutexstate.getState().equals(State.WANTING) &&
                    (mutexMessage.getTime() < mutexstate.getTime() || (mutexMessage.getTime() == mutexstate.getTime() && mutexMessage.getUser().compareTo(userUri) == -1)))){
                postReplyMsg(mutexMessage.getReply());
            } else {
                requests.add(mutexMessage);
            }
        } else {
            replies.add(mutexMessage);
        }
    }

    public void held() {
        this.mutexstate.setState(State.HELD);
    }

    public void released() {
        this.mutexstate.setState(State.RELEASED);
    }

    public void wanting() {
        this.mutexstate.setState(State.WANTING);
    }


    public List<String> prepareCriticalSection() {
        wanting();
        List<String> heroes = tavernaService.getAllHeroes(); //TODO only those with capability mutex
        postRequestMsg(heroes);
        return heroes;
    }

    public String enterCriticalSection(String resourceUrl) {
        held();
        createAuthRestTemplate();
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        String criticalToken;
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange(resourceUrl, HttpMethod.POST, entity, ObjectNode.class);
            criticalToken = response.getBody().get("token").asText();
        } catch (HttpStatusCodeException e) {
            return null;
        }
        released();
        postReplyMsg();
        return criticalToken;
    }

    private void postReplyMsg(String reply) {
        MutexMessage replyMessage = new MutexMessage();
        replyMessage.setMsg(Msg.REPLYOK);
        replyMessage.setTime(mutexstate.incrementTime());
        replyMessage.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        replyMessage.setUser(userUri);
        blackboardService.postReplyMsg(replyMessage, reply);
    }


    public void postRequestMsg(List<String> heroes) {
        MutexMessage mutexMessage = new MutexMessage();
        mutexMessage.setMsg(Msg.REQUEST);
        mutexMessage.setTime(mutexstate.incrementTime());
        mutexMessage.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        mutexMessage.setUser(userUri);
        HttpEntity<MutexMessage> entity = new HttpEntity<>(mutexMessage,headers);
        for (String hero : heroes) {
            String mutexUri = getMutex(hero);
            try {
                restTemplate.exchange("http://" + hero + mutexUri, HttpMethod.POST, entity, ObjectNode.class);
            } catch (Exception e) {

            }
        }
    }

    public void postReplyMsg() {
        MutexMessage reply = new MutexMessage();
        reply.setMsg(Msg.REPLYOK);
        reply.setTime(mutexstate.incrementTime());
        reply.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        reply.setUser(userUri);
        HttpEntity<MutexMessage> entity = new HttpEntity<>(reply,headers);
        for (MutexMessage mutexMessage : requests) {
            try {
                restTemplate.exchange(mutexMessage.getReply(), HttpMethod.POST, entity, ObjectNode.class);
            } catch (Exception e) {

            }
        }
    }


    private String getMutex(String hero) {
        HttpEntity<String> entity = new HttpEntity<>(null,headers);
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange("http://" + hero, HttpMethod.GET, entity, ObjectNode.class);
            return response.getBody().get("mutex").asText();
        } catch (HttpStatusCodeException e) {
            return null;
        } catch (Exception e2) {
            return null;
        }
    }

    private void login(String urlLogin) {

        ObjectNode objectNode = restTemplate.getForObject(urlLogin, ObjectNode.class);
        loginToken = objectNode.get("token").asText();
    }


    public void init() {
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(username, password));

        login(loginUrl); // login and save token
        createAuthRestTemplate();

    }
    private void createAuthRestTemplate() {
        restTemplate = new RestTemplate(getClientHttpRequestFactory());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token "+loginToken);
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


}
