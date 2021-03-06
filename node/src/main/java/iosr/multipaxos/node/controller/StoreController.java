package iosr.multipaxos.node.controller;

import iosr.multipaxos.common.command.CommandResponse;
import iosr.multipaxos.common.command.GetCommand;
import iosr.multipaxos.common.command.PutCommand;
import iosr.multipaxos.common.command.RemoveCommand;
import iosr.multipaxos.node.paxos.MultiPaxosHandler;
import iosr.multipaxos.node.paxos.MultiPaxosInfoManager;
import iosr.multipaxos.node.store.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Leszek Placzkiewicz on 03.11.17.
 */
@RestController
@RequestMapping("/store")
public class StoreController {

    private final Logger LOG = LoggerFactory.getLogger(StoreController.class);

    @Autowired
    private MultiPaxosHandler multiPaxosHandler;

    @Autowired
    private MultiPaxosInfoManager multiPaxosInfoManager;

    @Autowired
    private KeyValueStore keyValueStore;


    @RequestMapping(method = RequestMethod.PUT)
    public Object put(@RequestBody PutCommand putCommand) {
        LOG.info("Received PUT command: " + putCommand.getKey() + ":" + putCommand.getValue());

        if (!multiPaxosInfoManager.isLeader()) {
            return prepareRedirectResponse();
        }

        Object result = multiPaxosHandler.executePutCommand(putCommand);
        return new ResponseEntity<>(new CommandResponse(result), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public Object remove(@RequestBody RemoveCommand removeCommand) {
        LOG.info("Received REMOVE command: " + removeCommand.getKey());

        if (!multiPaxosInfoManager.isLeader()) {
            return prepareRedirectResponse();
        }

        Object result = multiPaxosHandler.executeRemoveCommand(removeCommand);
        return new ResponseEntity<>(new CommandResponse(result), HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method = RequestMethod.POST)
    public Object get(@RequestBody GetCommand getCommand) {
        LOG.info("Received GET command: " + getCommand.getKey());

        if (!multiPaxosInfoManager.isLeader()) {
            return prepareRedirectResponse();
        }

        Object result = multiPaxosHandler.executeGetCommand(getCommand);
        return new ResponseEntity<>(new CommandResponse(result), HttpStatus.OK);
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public Object getAll() {
        return keyValueStore.getAll();
    }

    private ResponseEntity prepareRedirectResponse() {
        Map<Object, Object> body = new HashMap<>();
        body.put("leaderId", multiPaxosInfoManager.getLeaderId());
        return new ResponseEntity<>(body, HttpStatus.TEMPORARY_REDIRECT);
    }

}
