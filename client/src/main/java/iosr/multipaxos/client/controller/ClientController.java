package iosr.multipaxos.client.controller;

import static java.util.Collections.emptyMap;

import iosr.multipaxos.client.model.TargetAddresses;
import iosr.multipaxos.common.command.Command;
import iosr.multipaxos.common.command.GetCommand;
import iosr.multipaxos.common.command.PutCommand;
import iosr.multipaxos.common.command.RemoveCommand;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;

@RestController("/client")
public class ClientController {
    private static final String DEFAULT_LEADER_ID = "0";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private TargetAddresses targetAddresses;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping(method = RequestMethod.PUT)
    public Object put(@RequestParam("key") final String key, @RequestParam("value") final Integer value) {
        if(Strings.isNullOrEmpty(key) || value == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final HttpEntity<Command> entity = new HttpEntity<>(new PutCommand(key, value));
        final ResponseEntity response = executeRequest(HttpMethod.PUT, entity, DEFAULT_LEADER_ID);
        return response;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Object get(@RequestParam("key") final String key) {
        if(Strings.isNullOrEmpty(key)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final HttpEntity<Command> entity = new HttpEntity<>(new GetCommand(key));
        final ResponseEntity response = executeRequest(HttpMethod.GET, entity, DEFAULT_LEADER_ID);
        return response;
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Object delete(@RequestParam("key") final String key) {
        if(Strings.isNullOrEmpty(key)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final HttpEntity<Command> entity = new HttpEntity<>(new RemoveCommand(key));
        final ResponseEntity response = executeRequest(HttpMethod.DELETE, entity, DEFAULT_LEADER_ID);
        return response;
    }

    private static boolean isRedirectResponse(final ResponseEntity response) {
        return HttpStatus.TEMPORARY_REDIRECT.equals(response.getStatusCode());
    }

    private ResponseEntity processRedirectResponse(final HttpMethod method,
                                           final HttpEntity<Command> entity,
                                           final ResponseEntity response) {
        LOGGER.info("Redirecting ... ");
        final Map<String, Integer> responseBody = (Map<String, Integer>) response.getBody();
        final Integer leaderId = responseBody.get("leaderId");
        return executeRequest(method, entity, leaderId.toString());
    }

    private ResponseEntity executeRequest(final HttpMethod method,
                                          final HttpEntity<Command> entity,
                                          final String leaderId) {
        final String targetUrl = this.targetAddresses.getTargetAddressById(leaderId);
        LOGGER.info("Execute " + method.name() + " request to: " + targetUrl);
        ResponseEntity response = this.restTemplate.exchange(targetUrl, method, entity, Object.class, emptyMap());
        if(isRedirectResponse(response)) {
             response = processRedirectResponse(method, entity, response);
        }
        return response;
    }
}
