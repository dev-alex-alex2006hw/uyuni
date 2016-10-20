/**
 * Copyright (c) 2013 SUSE LLC
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
package com.redhat.rhn.frontend.xmlrpc.audit;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.redhat.rhn.FaultException;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.frontend.xmlrpc.BaseHandler;
import com.redhat.rhn.frontend.xmlrpc.MethodInvalidParamException;
import com.redhat.rhn.frontend.xmlrpc.UnknownCVEIdentifierFaultException;
import com.redhat.rhn.manager.audit.CVEAuditManager;
import com.redhat.rhn.manager.audit.CVEAuditSystem;
import com.redhat.rhn.manager.audit.PatchStatus;
import com.redhat.rhn.manager.audit.UnknownCVEIdentifierException;

/**
 * CVESearchHandler
 *
 * @version $Rev$
 * @xmlrpc.namespace audit
 * @xmlrpc.doc Methods to audit systems.
 */
public class CVEAuditHandler extends BaseHandler {
    /**
     * List visible systems with their patch status regarding a given CVE
     * identifier.
     *
     * Please note that the query code relies on data that is pre-generated
     * by the 'cve-server-channels' taskomatic job.
     * @param sessionKey the session key
     * @param cveIdentifier the CVE number to search for
     * @return a list of systems with their patch status
     *
     * @xmlrpc.doc List visible systems with their patch status regarding a given CVE
     * identifier. Please note that the query code relies on data that is pre-generated
     * by the 'cve-server-channels' taskomatic job.
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param("string", "cveIdentifier")
     * @xmlrpc.returntype #array() $CVEAuditSystemSerializer #array_end()
     */
    public List<CVEAuditSystem> listSystemsByPatchStatus(String sessionKey,
            String cveIdentifier) {
        return listSystemsByPatchStatus(sessionKey, cveIdentifier, null);
    }

    /**
     * List visible systems with their patch status regarding a given CVE
     * identifier. Filter the results by passing in a list of patch status
     * labels. Please note that the query code relies on data that is
     * pre-generated by the 'cve-server-channels' taskomatic job.
     * @param sessionKey the session key
     * @param cveIdentifier the CVE number to search for
     * @param patchStatusLabels patch status labels to filter, will only return
     *            results with those patch statuses
     * @return a list of systems with their patch status
     * @throws FaultException if the CVE number is not known
     *
     * @xmlrpc.doc List visible systems with their patch status regarding a given CVE
     * identifier. Filter the results by passing in a list of patch status labels.
     * Please note that the query code relies on data that is pre-generated by the
     * 'cve-server-channels' taskomatic job.
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param("string", "cveIdentifier")
     * @xmlrpc.param #array_single("string", "patchStatusLabel")
     *  #options()
     *      #item_desc ("AFFECTED_PATCH_INAPPLICABLE",
     *          "Affected, patch available in unassigned channel")
     *      #item_desc ("AFFECTED_PATCH_APPLICABLE",
     *          "Affected, patch available in assigned channel")
     *      #item_desc ("NOT_AFFECTED", "Not affected")
     *      #item_desc ("PATCHED", "Patched")
     *  #options_end()
     * @xmlrpc.returntype #array() $CVEAuditSystemSerializer #array_end()
     */
    public List<CVEAuditSystem> listSystemsByPatchStatus(String sessionKey,
            String cveIdentifier, List<String> patchStatusLabels) throws FaultException {
        User user = getLoggedInUser(sessionKey);

        // Convert list of strings to patch status objects
        EnumSet<PatchStatus> patchStatuses = EnumSet.noneOf(PatchStatus.class);
        if (patchStatusLabels == null) {
            patchStatuses = EnumSet.allOf(PatchStatus.class);
        }
        else {
            for (String label : patchStatusLabels) {
                try {
                    patchStatuses.add(PatchStatus.valueOf(label));
                }
                catch (IllegalArgumentException e) {
                    throw new MethodInvalidParamException(e);
                }
            }
        }

        try {
            List<CVEAuditSystem> result = CVEAuditManager.listSystemsByPatchStatus(
                    user, cveIdentifier, patchStatuses);

            Collections.sort(result, new Comparator<CVEAuditSystem>() {
                @Override
                public int compare(CVEAuditSystem s1, CVEAuditSystem s2) {
                    return s1.getPatchStatusRank() - s2.getPatchStatusRank();
                }
            });

            return result;
        }
        catch (UnknownCVEIdentifierException e) {
            throw new UnknownCVEIdentifierFaultException();
        }
    }
}