/**
 * Copyright (c) 2017 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.suse.manager.webui.websocket;

import com.redhat.rhn.domain.server.MinionServer;
import com.redhat.rhn.domain.server.MinionServerFactory;
import com.redhat.rhn.domain.session.WebSession;
import com.redhat.rhn.domain.session.WebSessionFactory;
import com.redhat.rhn.frontend.servlets.LocalizedEnvironmentFilter;
import com.suse.manager.webui.services.impl.SaltService;
import com.suse.manager.webui.websocket.json.AsyncJobStartEventDto;
import com.suse.manager.webui.websocket.json.ExecuteMinionActionDto;
import com.suse.manager.webui.websocket.json.MinionMatchResultEventDto;
import com.suse.manager.webui.websocket.json.ActionTimedOutEventDto;
import com.suse.manager.webui.websocket.json.MinionCommandResultEventDto;
import com.suse.manager.webui.websocket.json.ActionErrorEventDto;
import com.suse.manager.webui.websocket.json.AbstractSaltEventDto;
import com.suse.salt.netapi.datatypes.target.Glob;
import com.suse.salt.netapi.results.Result;
import com.suse.utils.Json;
import org.apache.log4j.Logger;

import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.EndpointConfig;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Websocket endpoint for executing remote commands on Salt minions.
 * NOTE: there's an endpoint instance for each websocket session
 */
@ServerEndpoint(value = "/websocket/minion/remote-commands")
public class RemoteMinionCommands {

    // Logger for this class
    private static final Logger LOG = Logger.getLogger(RemoteMinionCommands.class);

    private Long sessionId;

    private CompletableFuture failAfter;

    /**
     * Callback executed when the websocket is opened.
     * @param session the websocket session
     * @param config the endpoint config
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.sessionId = LocalizedEnvironmentFilter.getCurrentSessionId();
        if (this.sessionId == null) {
            try {
                LOG.debug("No web sessionId available. Closing the web socket.");
                session.close();
            }
            catch (IOException e) {
                LOG.debug("Error closing web socket session", e);
            }
        }
    }

    /**
     * Callback executed when the websocket is closed.
     */
    @OnClose
    public void onClose() {
        LOG.debug("Closing web socket session");
        if (this.failAfter != null) {
            this.failAfter.completeExceptionally(
                    new TimeoutException("Canceled waiting because of websocket close"));
        }

    }

    /**
     * Callback executed when a message is received.
     * @param session the websocket session
     * @param messageBody the message as string
     */
    @OnMessage
    public void onMessage(Session session, String messageBody) {
        ExecuteMinionActionDto msg = Json.GSON.fromJson(
                messageBody, ExecuteMinionActionDto.class);
        WebSession webSession = WebSessionFactory.lookupById(sessionId);
        if (webSession == null || webSession.getUser() == null) {
            LOG.debug("Invalid web sessionId. Closing the web socket.");
            sendMessage(session, new ActionErrorEventDto(null,
                    "INVALID_SESSION", "Invalid user session."));
            try {
                session.close();
                return;
            }
            catch (IOException e) {
                LOG.debug("Error closing web socket session", e);
            }
        }

        List<String> allVisibleMinions = MinionServerFactory
                .lookupVisibleToUser(webSession.getUser())
                .map(MinionServer::getMinionId)
                .collect(Collectors.toList());

        int timeOut = 600; // 10 minutes
        try {
            if (msg.isPreview()) {
                this.failAfter = SaltService.INSTANCE.failAfter(timeOut);
                Map<String, CompletionStage<Result<Boolean>>> res = SaltService.INSTANCE
                        .matchAsync(msg.getTarget(), failAfter);

                List<String> allMinions = res.keySet().stream()
                        .collect(Collectors.toList());
                allMinions.retainAll(allVisibleMinions);
                sendMessage(session, new AsyncJobStartEventDto("preview", allMinions));

                res.forEach((minionId, future) -> {
                    future.whenComplete((matchResult, err) -> {
                        if (matchResult != null) {
                            sendMessage(session, new MinionMatchResultEventDto(minionId));
                        }
                        if (err != null) {
                            if (err instanceof TimeoutException) {
                                sendMessage(session,
                                        new ActionTimedOutEventDto(minionId, "preview"));
                                LOG.debug("Timed out waiting for response from minion " +
                                        minionId);
                            }
                            else {
                                LOG.error("Error waiting for minion " + minionId, err);
                                sendMessage(session,
                                        new ActionErrorEventDto(minionId,
                                                "ERR_WAIT_MATCH",
                                                "Error waiting for matching: " +
                                                        err.getMessage()));
                            }
                        }
                    });
                });
            }
            else if (msg.isCancel()) {
                if (this.failAfter != null) {
                    this.failAfter.completeExceptionally(
                            new TimeoutException("Canceled waiting"));
                }
            }
            else {
                this.failAfter = SaltService.INSTANCE.failAfter(timeOut);
                Map<String, CompletionStage<Result<String>>> res = SaltService.INSTANCE
                        .runRemoteCommandAsync(new Glob(msg.getTarget()),
                                msg.getCommand(), failAfter);

                List<String> allMinions = res.keySet().stream()
                        .collect(Collectors.toList());
                sendMessage(session, new AsyncJobStartEventDto("run", allMinions));

                res.forEach((minionId, future) -> {
                    future.whenComplete((cmdResult, err) -> {
                        if (cmdResult != null) {
                            if (cmdResult.result().isPresent()) {
                                sendMessage(session, new MinionCommandResultEventDto(
                                        minionId, cmdResult.result().get()));
                            }
                            else {
                                sendMessage(session,
                                        new ActionErrorEventDto(minionId,
                                            "ERR_CMD_NO_RESULTS",
                                            "Command executed succesfully" +
                                                    " but no result was returned"));
                            }
                        }
                        if (err != null) {
                            if (err instanceof TimeoutException) {
                                sendMessage(session,
                                        new ActionTimedOutEventDto(minionId, "run"));
                                LOG.debug("Timed out waiting for response from minion " +
                                        minionId);
                            }
                            else {
                                LOG.error("Error waiting for minion " + minionId, err);
                                sendMessage(session,
                                        new ActionErrorEventDto(minionId,
                                                "ERR_WAIT_CMD",
                                                "Error waiting to execute command: " +
                                                        err.getMessage()));
                            }
                        }
                    });
                });
            }
        }
        catch (Exception e) {
            LOG.error("Error executing Salt async remote command", e);
            sendMessage(session, new ActionErrorEventDto(null,
                    "GENERIC_ERR", e.getMessage()));
        }

    }

    /**
     * Must be synchronized. Sending messages concurrently from separate threads
     * will result in IllegalStateException.
     */
    private synchronized void sendMessage(Session session, AbstractSaltEventDto dto) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(Json.GSON.toJson(dto));
            }
            else {
                LOG.debug("Could not send websocket message. Session is closed.");
            }
        }
        catch (IOException e) {
            LOG.error("Error sending websocket message", e);
        }
    }

    /**
     * Callback executed an error occurs.
     * @param session the websocket session
     * @param err the err that occurred
     */
    @OnError
    public void onError(Session session, Throwable err) {
        LOG.error("Websocket endpoint error", err);
    }

}