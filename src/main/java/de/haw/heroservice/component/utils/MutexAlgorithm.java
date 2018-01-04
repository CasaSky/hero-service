package de.haw.heroservice.component.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
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
    private List<MutexMessage> requestedHeroesList;

    @Autowired
    private List<MutexMessage> repliedHeroesList;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Auth auth;


    // Compute bigger time of two given times and increment bigger time by 1
    public int incrementBiggerTime(int ownTime, int givenTime) {
        return givenTime > ownTime ? givenTime + 1 : ownTime + 1;
    }

    @PostConstruct
    public void init() {
       restTemplate = auth.init();
    }

    public void startMutex(MutexMessage mutexMessage){

        mutexstate.setTime(incrementBiggerTime(mutexstate.getTime(), mutexMessage.getTime()));

        if (mutexMessage.getMsg().equals(Msg.REQUEST)) {
            if (mutexstate.getState().equals(State.RELEASED)
                    || (mutexstate.getState().equals(State.WANTING) &&
                    (mutexMessage.getTime() < mutexstate.getTime() || (mutexMessage.getTime() == mutexstate.getTime() && mutexMessage.getUser().compareTo(userUri) == -1)))){
                postReplyMsg(mutexMessage.getReply());
            } else {
                requestedHeroesList.add(mutexMessage);
            }
        } else {
            repliedHeroesList.add(mutexMessage);
        }
    }

    /**
     * Setzt mutex status auf wanting und schickt eine Request-Message an allen Heroes
     * @return heroes list.
     */
    public List<String> prepareCriticalSection() {
        wanting();
        List<String> heroes = tavernaService.getAllMutexHeroes();
        postRequestMsg(heroes);
        return heroes;
    }

    /**
     * Setzt mutex status auf held, enter critical section and save result token, setzt mutex status auf released und schickt reply message an alle.
     * @param resourceUrl
     * @return result token
     */
    public String enterCriticalSection(String resourceUrl) {
        held();
        HttpEntity<String> entity = new HttpEntity<>("{}", auth.getHeaders());
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

    /**
     * Erhoeht die Uhr um 1 und schickt eine Reply-Message zum Sender der Request-Message
     * @param reply die Reply-Addresse des Senders
     */
    private void postReplyMsg(String reply) {
        MutexMessage replyMessage = new MutexMessage();
        replyMessage.setMsg(Msg.REPLYOK);
        replyMessage.setTime(mutexstate.incrementTime());
        replyMessage.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        replyMessage.setUser(userUri);
        blackboardService.postReplyMsg(replyMessage, reply);
    }


    /**
     * Schickt eine Request-Message an alle Heroes
     * @param heroes Liste der Heroes
     */
    public void postRequestMsg(List<String> heroes) {
        MutexMessage mutexMessage = new MutexMessage();
        mutexMessage.setMsg(Msg.REQUEST);
        mutexMessage.setTime(mutexstate.incrementTime());
        mutexMessage.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        mutexMessage.setUser(userUri);
        HttpEntity<MutexMessage> entity = new HttpEntity<>(mutexMessage,auth.getHeaders());
        for (String hero : heroes) {
            String mutexUri = getMutexUri(hero);
            try {
                restTemplate.exchange("http://" + hero + mutexUri, HttpMethod.POST, entity, ObjectNode.class);
            } catch (Exception e) {

            }
        }
    }

    /**
     * Schickt eine Reply-Message an alle Heroes, die uns eine Request-Message geschickt haben.
     */
    public void postReplyMsg() {
        MutexMessage reply = new MutexMessage();
        reply.setMsg(Msg.REPLYOK);
        reply.setTime(mutexstate.incrementTime());
        reply.setReply(tavernaService.getHeroUrl(username)+"/mutex");
        reply.setUser(userUri);
        HttpEntity<MutexMessage> entity = new HttpEntity<>(reply,auth.getHeaders());
        for (MutexMessage mutexMessage : requestedHeroesList) {
            try {
                restTemplate.exchange(mutexMessage.getReply(), HttpMethod.POST, entity, ObjectNode.class);
            } catch (Exception e) {

            }
        }
    }


    /**
     * Liefert die Mutex Uri vom Hero
     * @param heroUrl
     * @return
     */
    private String getMutexUri(String heroUrl) {
        HttpEntity<String> entity = new HttpEntity<>(null,auth.getHeaders());
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange("http://" + heroUrl, HttpMethod.GET, entity, ObjectNode.class);
            return response.getBody().get("mutex").asText();
        } catch (HttpStatusCodeException e) {
            return null;
        } catch (Exception e2) {
            return null;
        }
    }

    /**
     * Liefert die Mutextstate Uri vom Hero
     * @param heroUrl
     * @return
     */
    private String getMutexStateUri(String heroUrl) {
        HttpEntity<String> entity = new HttpEntity<>(null,auth.getHeaders());
        try {
            ResponseEntity<ObjectNode> response = restTemplate.exchange(heroUrl, HttpMethod.GET, entity, ObjectNode.class);
            return response.getBody().get("mutexstate").asText();
        } catch (HttpStatusCodeException e) {
            return null;
        } catch (Exception e2) {
            return null;
        }
    }

    /**
     * Prueft den Mutex-Status von den Heroes, die noch nicht geantwortet haben.
     * Wird maximal 100 Mal wiederholt solange nicht alle mit Reply-Message geantwortet haben oder wenn sich der Status von keinem verändert hat.
     * @param notReleasedHeroesList Liste der Heroes, die noch nicht released sind
     */
    public synchronized void checkMutexState(List<String> notReleasedHeroesList) {

        HttpEntity<MutexMessage> entity = new HttpEntity<>(null,auth.getHeaders());
        int statusCheckNumber = 100;


        for (int i=0; i<statusCheckNumber; i++) {
            List<String> newNotReleasedList = new ArrayList<>();

            // Check mutex state
            for (String hero : notReleasedHeroesList) {
                String mutexstateUri = getMutexStateUri(hero);
                try {
                    ResponseEntity<ObjectNode> response = restTemplate.exchange(hero + mutexstateUri, HttpMethod.GET, entity, ObjectNode.class);
                    String state = response.getBody().get("state").asText();
                    if (state.equals(State.WANTING) || state.equals(State.HELD)) {
                        newNotReleasedList.add(hero);
                    }
                } catch (Exception e) {
                    newNotReleasedList.add(hero);
                }
            }

            // Abbruch, wenn ab der zweiten Ueberpruefung sich die Liste der unreleased Heroes nicht mehr verkleinert hat.
            if (i > 0 && !newNotReleasedList.isEmpty() && !(newNotReleasedList.size() < notReleasedHeroesList.size())) {
                return;
            } else {
                try {
                    wait(120000); // 2min warten.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                notReleasedHeroesList = newNotReleasedList;
            }
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

    public List<MutexMessage> getRepliedHeroesList() {
        return repliedHeroesList;
    }
}
