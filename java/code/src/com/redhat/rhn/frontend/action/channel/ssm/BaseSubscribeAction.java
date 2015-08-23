/**
 * Copyright (c) 2009--2014 Red Hat, Inc.
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
package com.redhat.rhn.frontend.action.channel.ssm;

import com.redhat.rhn.common.db.datasource.DataResult;
import com.redhat.rhn.common.localization.LocalizationService;
import com.redhat.rhn.common.messaging.MessageQueue;
import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.channel.ChannelFactory;
import com.redhat.rhn.domain.channel.DistChannelMap;
import com.redhat.rhn.domain.server.Server;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.frontend.dto.ChildChannelPreservationDto;
import com.redhat.rhn.frontend.dto.EssentialChannelDto;
import com.redhat.rhn.frontend.dto.EssentialServerDto;
import com.redhat.rhn.frontend.dto.SystemsPerChannelDto;
import com.redhat.rhn.frontend.events.SsmChangeBaseChannelSubscriptionsEvent;
import com.redhat.rhn.frontend.struts.RequestContext;
import com.redhat.rhn.frontend.struts.RhnHelper;
import com.redhat.rhn.frontend.struts.RhnLookupDispatchAction;
import com.redhat.rhn.frontend.struts.StrutsDelegate;
import com.redhat.rhn.frontend.taglibs.list.ListTagHelper;
import com.redhat.rhn.manager.channel.ChannelManager;
import com.redhat.rhn.manager.rhnset.RhnSetDecl;
import com.redhat.rhn.manager.ssm.SsmOperationManager;
import com.redhat.rhn.manager.system.SystemManager;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * BaseSubscribeAction
 * @version $Rev$
 */
public class BaseSubscribeAction extends RhnLookupDispatchAction {

    private static Logger log = Logger.getLogger(BaseSubscribeAction.class);

    static final String PREFIX = "base-for-";
    static final String NO_CHG = "__no_change__";
    static final String DFLT   = "__default__";

    static final String BASE_CHANNEL_IDS = "base_channel_ids";
    static final String NEW_BASE_CHANNEL_IDS = "new_base_channel_ids";
    static final String MATCHED_CHILD_CHANNELS = "matched_child_channels";
    static final String UNMATCHED_CHILD_CHANNELS = "unmatched_child_channels";
    static final String FOUND_UNMATCHED_CHANNELS = "foundUnmatchedChannels";

    /*
    Map<Long, List<Long>> successes = new HashMap<Long, List<Long>>();
    Map<Long, List<Long>> failures = new HashMap<Long, List<Long>>();
    Map<Long, List<Long>> skipped = new HashMap<Long, List<Long>>();
*/


    @Override
    protected Map<String, String> getKeyMethodMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("basesub.jsp.confirmSubscriptions", "confirmUpdateBaseChannels");
        map.put("basesub.jsp.confirm.alter", "changeChannels");
        map.put("basesub.jsp.confirm.cancel", "unspecified");
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward unspecified(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("unspecified()");

        RequestContext rctx = new RequestContext(request);
        User user = rctx.getCurrentUser();
        request.setAttribute(ListTagHelper.PARENT_URL, request.getRequestURI());

        ActionForward af = mapping.findForward(RhnHelper.DEFAULT_FORWARD);

        // Provide the list of all base channels for all systems in the SSM
        List<SystemsPerChannelDto> ldr = setupList(user, request);
        request.setAttribute("baselist", ldr);
        return af;
    }

    private ActionForward handleNoChanges(ActionMapping mapping,
            HttpServletRequest request) {
        log.debug("No channels being changed.");

        StrutsDelegate strutsDelegate = getStrutsDelegate();
        ActionMessages msgs = new ActionMessages();
        msgs.add(ActionMessages.GLOBAL_MESSAGE,
                new ActionMessage("basesub.jsp.noChangesMade"));
        strutsDelegate.saveMessages(request, msgs);

        return strutsDelegate.forwardParams(mapping.findForward("success"),
                new HashMap());
    }

    /**
     * Confirm the base channel changes.
     *
     * @param mapping ActionMapping
     * @param formIn ActionForm
     * @param request ServletRequest
     * @param response ServletResponse
     * @return The ActionForward to go to next.
     */
    public ActionForward confirmUpdateBaseChannels(ActionMapping mapping,
            ActionForm formIn, HttpServletRequest request, HttpServletResponse response) {

        log.debug("confirmUpdateBaseChannels()");
        RequestContext rctx = new RequestContext(request);
        User user = rctx.getCurrentUser();

        List<ChildChannelPreservationDto> unmatched =
            new LinkedList<ChildChannelPreservationDto>();
        List<ChildChannelPreservationDto> matched =
            new LinkedList<ChildChannelPreservationDto>();

        Map<Long, Long> changedChannels = copyChangedChannels(request);

        // Technically speaking an inattentive user could submit this screen with all
        // channels set to "No Change":
        if (changedChannels.entrySet().size() == 0) {
            return handleNoChanges(mapping, request);
        }

        for (Long oldBaseChannelId : changedChannels.keySet()) {
            Channel oldBase = null;
            if (oldBaseChannelId.intValue() != -1) {
                oldBase = ChannelFactory.lookupByIdAndUser(oldBaseChannelId, user);
            }
            Channel newBase = null;

            Long newBaseChannelId = changedChannels.get(oldBaseChannelId);
            log.debug("newBaseChannelId = " + newBaseChannelId);

            // First add an entry for the default base channel:
            if (newBaseChannelId.intValue() == -1) {
                log.debug("Default system base channel was selected.");
                List<DistChannelMap> dcms = ChannelFactory.listDistChannelMaps(oldBase);
                if (!dcms.isEmpty() && oldBase != null) {
                    for (DistChannelMap dcm : dcms) {
                        String version = dcm.getRelease();
                        DistChannelMap defaultDcm =
                            ChannelManager.lookupDistChannelMapByPnReleaseArch(
                                user.getOrg(),
                                ChannelManager.RHEL_PRODUCT_NAME, version,
                                oldBase.getChannelArch());
                        if (defaultDcm != null) {
                            newBase = defaultDcm.getChannel();
                            log.debug("Determined default base channel will be: " +
                                    newBase.getLabel());
                                break;
                        }
                    }
                }
                if (newBase == null) {
                    // Looks like an EUS or custom channel, need to get a little crazy :(
                    // Should be safe to assume there's at least one result returned here,
                    // we need a server object to call the stored procedure and guess a
                    // default base channel:
                    List<Long> servers = serversInSSMWithBase(user, oldBaseChannelId);
                    Server s = SystemManager.lookupByIdAndUser(servers.get(0), user);
                    newBase = ChannelManager.guessServerBase(user, s);

                    if (newBase == null) {
                        // lets search for suse systems
                        List<EssentialChannelDto> dr = ChannelManager.
                                listPossibleSuseBaseChannelsForServer(s);
                        if (dr != null && dr.get(0) != null) {
                            newBase = ChannelFactory.lookupByIdAndUser(
                                    dr.get(0).getId(), user);
                        }
                    }
                }
            }
            if (newBase == null) {

                if (newBaseChannelId.intValue() == -1) {
                    // System default base channel selected but we couldn't guess a
                    // channel. (can happen in the case of solaris systems)
                    // Display a warning to the user and return empty handed.
                    StrutsDelegate strutsDelegate = getStrutsDelegate();
                    ActionMessages msgs = new ActionMessages();
                    msgs.add(ActionMessages.GLOBAL_MESSAGE,
                            new ActionMessage(
                                    "basesub.jsp.unableToLookupSystemDefaultChannel"));
                    strutsDelegate.saveMessages(request, msgs);

                    return strutsDelegate.forwardParams(mapping.findForward("success"),
                            new HashMap());
                }
                newBase = ChannelManager.lookupByIdAndUser(newBaseChannelId, user);
            }

            if (oldBase != null) {
                log.debug(oldBase.getName() + " -> " + newBase.getName());
                Map<Channel, Channel> preservations = ChannelManager.findCompatibleChildren(
                        oldBase, newBase, user);

                for (Channel c : preservations.keySet()) {
                    Channel match = preservations.get(c);
                    List<Map<String, Object>> serversAffected =
                            SystemManager.
                        getSsmSystemsSubscribedToChannel(user, c.getId());
                    log.debug("found " + serversAffected.size() +
                            " servers in set with channel: " + c.getId());
                    if (serversAffected.size() > 0) {
                        matched.add(new ChildChannelPreservationDto(c.getId(), c.getName(),
                                match.getId(), match.getName(), serversAffected));
                    }
                }

                for (Channel c : oldBase.getAccessibleChildrenFor(user)) {
                    if (!preservations.containsKey(c)) {
                        List<Map<String, Object>> serversAffected =
                            SystemManager.getSsmSystemsSubscribedToChannel(user, c.getId());
                        log.debug("found " + serversAffected.size() +
                                " servers in set with channel: " + c.getId());
                        if (serversAffected.size() > 0) {
                            unmatched.add(new ChildChannelPreservationDto(c.getId(),
                                    c.getName(), c.getParentChannel().getId(),
                                    c.getParentChannel().getName(), serversAffected));
                        }
                    }
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Matches:");
            for (ChildChannelPreservationDto dto : matched) {
                log.debug("   " + dto.getOldChannelName() + " " +
                        dto.getOtherChannelName() + " " + dto.getSystemsAffected());
            }

            log.debug("Unmatches:");
            for (ChildChannelPreservationDto dto : unmatched) {
                log.debug("   " + dto.getOldChannelName() + " " +
                        dto.getOtherChannelName() + " " + dto.getSystemsAffected());
            }
        }

        request.setAttribute(ListTagHelper.PARENT_URL, request.getRequestURI());
        request.setAttribute(MATCHED_CHILD_CHANNELS, matched);
        request.setAttribute(FOUND_UNMATCHED_CHANNELS, unmatched.size() > 0);
        request.setAttribute(UNMATCHED_CHILD_CHANNELS, unmatched);

        log.debug("end confirmUpdateBaseChannels()");
        return mapping.findForward(RhnHelper.CONFIRM_FORWARD);
    }

    /**
     * Change channels for the selected SSM systems.
     *
     * For each key:
     * - Find the associated channel
     * - Find the channel associated with the value for that key
     * - Find the list of all systems in the system-set that
     *   are currently subscribed to the channel of the KEY
     * - Change the subscription for those systems to the
     *   channel associated with the VALUE for that key IFF:
     *   - Value maps to a real channel
     *   - User has access to value-channel
     *   - Value-channel has available subscriptions
     *   - Value-channel is compatible with a system
     * - If keyId == -1, subscribe all systems in the system-set which do not
     *   currently have base channels
     * - DO NOT change base-channels for systems that are satellite or proxy here!
     *
     * Possible errors:
     * - Systems A,B,C,D not compatible with Channel X
     * - Systems A,B,C,D not subscribed to Channel X because there are no more
     *   subscriptions available
     * - User does not have access to Channel X
     * - System A,B,C,D is a Spacewalk or a Proxy - base channel unchanged
     *
     * @param mapping ActionMapping
     * @param formIn ActionForm
     * @param request ServletRequest
     * @param response ServletResponse
     * @return The ActionForward to go to next.
     */
    public ActionForward changeChannels(ActionMapping mapping, ActionForm formIn,
            HttpServletRequest request, HttpServletResponse response) {
        log.debug("changeChannels()");

        Map<Long, List<Long>> successes = new HashMap<Long, List<Long>>();
        Map<Long, List<Long>> skipped = new HashMap<Long, List<Long>>();


        RequestContext rctx = new RequestContext(request);
        User user = rctx.getCurrentUser();
        request.setAttribute(ListTagHelper.PARENT_URL, request.getRequestURI());

        String barSeparatedChannelIds = request.getParameter(BASE_CHANNEL_IDS);
        String barSeparatedNewChannelIds = request.getParameter(NEW_BASE_CHANNEL_IDS);
        log.debug("base channel ids = " + barSeparatedChannelIds);
        log.debug("new base channel ids = " + barSeparatedNewChannelIds);
        String [] oldChannelIds = barSeparatedChannelIds.split("\\|");
        String [] newChannelIds = barSeparatedNewChannelIds.split("\\|");
        log.debug("ids size = " + oldChannelIds.length);
        log.debug("new ids size = " + newChannelIds.length);
        assert oldChannelIds.length == newChannelIds.length;

        // Map<Channel-Id, List<Server-Id>> - cid == -1 => system-best-guess-default
        Map<Long, List<Long>> requestedChanges = new HashMap<Long, List<Long>>();

        for (int i = 0; i < oldChannelIds.length; i++) {
            Long oldChanId = Long.parseLong(oldChannelIds[i]);
            Long newChanId = Long.parseLong(newChannelIds[i]);

            List<Long> servers = serversInSSMWithBase(user, oldChanId);

            if (requestedChanges.get(newChanId) != null) {
                requestedChanges.get(newChanId).addAll(servers);
            }
            else {
                requestedChanges.put(newChanId, servers);
            }
        }

        alterSubscriptions(user, requestedChanges, successes, skipped);
        addMessages(request, buildMessages(user, successes, skipped));

        // Provide the list of all base channels for all systems in the SSM
        List<SystemsPerChannelDto> ldr = setupList(user, request);
        request.setAttribute("baselist", ldr);

        log.debug("end changeChannels()");
        return mapping.findForward(RhnHelper.DEFAULT_FORWARD);
    }

    /**
     * Get the list of base-channels available to the System Set
     * @param user User requesting.
     * @param request Request object
     * @return list
     */
    protected List<SystemsPerChannelDto> setupList(User user, HttpServletRequest request) {
        log.debug("setupList");
        List<SystemsPerChannelDto> ldr = ChannelManager.baseChannelsInSet(user);

        for (SystemsPerChannelDto spc : ldr) {
            //We dont' need to do user auth, here because if the user doesn't have
            // subscribe access to the subscribed channel we still want to let them
            //  change the systems base channel
            Channel c = ChannelFactory.lookupById(spc.getId().longValue());

            List<EssentialChannelDto> compatibles = ChannelManager
                    .listCompatibleBaseChannelsForChannel(user, c);
            log.debug("Sorting channels: " + compatibles.size());
            List<EssentialChannelDto> rhn = new ArrayList<EssentialChannelDto>();
            List<EssentialChannelDto> custom = new ArrayList<EssentialChannelDto>();
            for (EssentialChannelDto ecd : compatibles) {
                log.debug("   " + ecd.getName());
                if (ecd.isCustom()) {
                    custom.add(ecd);
                }
                else {
                    rhn.add(ecd);
                }
            }
            spc.setAllowedBaseChannels(rhn);
            spc.setAllowedCustomChannels(custom);
        }

        SystemsPerChannelDto nobase = createSPCForUnbasedSystems(user);
        if (nobase != null) {
            ldr.add(0, nobase);
        }
        return ldr;
    }

    // Create the container for the "No Base Channel Currently" 'row' in our UI
    protected SystemsPerChannelDto createNoneRow(DataResult noBase) {
        SystemsPerChannelDto rslt;
        String none = LocalizationService.getInstance().getMessage("none");
        rslt = new SystemsPerChannelDto();
        rslt.setId(new Long(-1L));
        rslt.setSystemCount(noBase.size());
        rslt.setName(none);
        return rslt;
    }

    // Create the data-structures needed for systems that aren't currently subscribed to
    // any base channels
    protected SystemsPerChannelDto createSPCForUnbasedSystems(User user) {
        SystemsPerChannelDto rslt = null;

        // How many systems don't currently have a base channeL?
        DataResult<EssentialServerDto> noBase =
                SystemManager.systemsWithoutBaseChannelsInSet(user);

        // If there are any...
        if (noBase != null && noBase.size() > 0) {
            // ...create the "(None)" row
            rslt = createNoneRow(noBase);

            List<EssentialChannelDto> customChs = new ArrayList<EssentialChannelDto>();
            for (Channel c : ChannelFactory.listCustomBaseChannelsForSSMNoBase(user)) {
                customChs.add(new EssentialChannelDto(c));
            }
            rslt.setAllowedCustomChannels(customChs);

            List<EssentialChannelDto> nullOrgChs = new ArrayList<EssentialChannelDto>();
            for (Channel c :
                        ChannelFactory.listCompatibleBasesForSSMNoBaseInNullOrg(user)) {
                nullOrgChs.add(new EssentialChannelDto(c));
            }
            rslt.setAllowedBaseChannels(nullOrgChs);
        }
        return rslt;
    }

    // List all the servers in the current System Set with the specified Base Channel
    protected List<Long> serversInSSMWithBase(User u, Long cid) {
        List<Long> servers = new ArrayList<Long>();
        DataResult<EssentialServerDto> dr = null;
        if (cid == -1L) {
            dr = SystemManager.systemsWithoutBaseChannelsInSet(u);
        }
        else {
            dr = SystemManager.systemsSubscribedToChannelInSet(cid, u,
                    RhnSetDecl.SYSTEMS.getLabel());
        }

        Iterator<EssentialServerDto> itr = dr.iterator();
        while (itr.hasNext()) {
            EssentialServerDto esd = itr.next();
            servers.add(esd.getId().longValue());
        }

        return servers;
    }

    /**
     * Copy keys back into the request for forwarding beyond the confirmation screen.
     */
    private Map<Long, Long> copyChangedChannels(HttpServletRequest request) {
        Enumeration<String> names = request.getParameterNames();
        Map<Long, Long> oldToNewMap = new HashMap<Long, Long>();
        StringBuilder idsBuf = new StringBuilder();
        StringBuilder valuesBuf = new StringBuilder();

        while (names.hasMoreElements()) {
            String aName = names.nextElement();
            String aValue = request.getParameter(aName);

            if (aName.startsWith(PREFIX) && !NO_CHG.equals(aValue)) {
                Long keyId = Long.parseLong(aName.substring(PREFIX.length()));
                oldToNewMap.put(keyId, Long.parseLong(aValue));
                if (idsBuf.length() > 0) {
                    idsBuf.append("|");
                    valuesBuf.append("|");
                }

                idsBuf.append(keyId);
                valuesBuf.append(aValue);
            }
        }
        request.setAttribute(BASE_CHANNEL_IDS, idsBuf.toString());
        request.setAttribute(NEW_BASE_CHANNEL_IDS, valuesBuf.toString());
        return oldToNewMap;
    }

    protected void alterSubscriptions(User u, Map<Long, List<Long>> chgs,
            Map<Long, List<Long>> successes, Map<Long, List<Long>> skipped) {

        successes.clear();
        skipped.clear();
        List<ChannelActionDAO> actions = new ArrayList<ChannelActionDAO>();
        Map<Long, Channel> channelMap = new HashMap<Long, Channel>();

        for (Long toId : chgs.keySet()) {
            successes.put(toId, new ArrayList<Long>());
            skipped.put(toId, new ArrayList<Long>());

            for (Long srvId : chgs.get(toId)) {
                Server s = SystemManager.lookupByIdAndUser(srvId, u);
                if (s.isSatellite()) {
                    skip(toId, srvId, skipped);
                    continue;
                }

                Long cid = null;
                if (toId == -1L) {
                    cid = ChannelManager.guessServerBase(u, s.getId());
                }
                else {
                    cid = toId;
                }

                // let's only hydrate the channel objects once
                if (!channelMap.containsKey(cid)) {
                    channelMap.put(cid, ChannelManager.lookupByIdAndUser(cid, u));
                }

                Channel c = channelMap.get(cid);

                if (c == null) {
                    skip(toId, srvId, skipped);
                    continue;
                }
                else if (c.equals(s.getBaseChannel())) {
                    skip(toId, srvId, skipped);
                    continue;
                }

                ChannelActionDAO action = new ChannelActionDAO();
                action.setId(srvId);
                action.addSubscribeChannelId(c.getId());
                actions.add(action);
                success(toId, srvId, successes);
            }
        }

        Long operationId = SsmOperationManager.createOperation(u,
                "ssm.base.subscription.operation.label", null);
        List<Long> sids = new ArrayList<Long>();
        for (Long cid : successes.keySet()) {
            sids.addAll(successes.get(cid));
        }

        SsmOperationManager.associateServersWithOperation(operationId, u.getId(), sids);

        // Fire the request off asynchronously
        SsmChangeBaseChannelSubscriptionsEvent event = new
                SsmChangeBaseChannelSubscriptionsEvent(u, actions, operationId);
        MessageQueue.publish(event);
    }

    protected void success(Long toId, Long srvId, Map<Long, List<Long>> successes) {
        List<Long> l = successes.get(toId);
        l.add(srvId);
        successes.put(toId, l);
    }

    protected void skip(Long toId, Long srvId, Map<Long, List<Long>> skipped) {
        List<Long> l = skipped.get(toId);
        l.add(srvId);
        skipped.put(toId, l);
    }

    // Foreach to-channel-id:
    //   N servers subscribed to channel X
    //   M servers skipped attempting to subscribe to channel X
    // (yes, this can be a lot of messages...)
    protected ActionMessages buildMessages(User u, Map<Long, List<Long>> successes,
            Map<Long, List<Long>> skipped) {

        ActionMessages msgs = new ActionMessages();

        for (Long toId : successes.keySet()) {
            Channel c = null;
            if (toId != null && toId != -1L) {
                c = ChannelManager.lookupByIdAndUser(toId, u);
            }

            ActionMessage am;

            // Success messages
            List<Long> srvrs = successes.get(toId);
            if (srvrs.isEmpty()) {
                continue;
            }
            else if (toId == -1L) {
                am = new ActionMessage("basesub.jsp.success-default", srvrs.size());
                msgs.add(ActionMessages.GLOBAL_MESSAGE, am);
            }
            else {
                am = new ActionMessage("basesub.jsp.success", srvrs.size(), c.getName());
                msgs.add(ActionMessages.GLOBAL_MESSAGE, am);
            }

            // Skipped messages
            srvrs = skipped.get(toId);
            if (srvrs.isEmpty()) {
                continue;
            }
            else if (toId == -1L) {
                am = new ActionMessage("basesub.jsp.skip-default", srvrs.size());
                msgs.add(ActionMessages.GLOBAL_MESSAGE, am);
            }
            else {
                am = new ActionMessage("basesub.jsp.skip", srvrs.size(), c.getName());
                msgs.add(ActionMessages.GLOBAL_MESSAGE, am);
            }
        }
        return msgs;
    }
}
