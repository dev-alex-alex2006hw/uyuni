/**
 * Copyright (c) 2015 SUSE LLC
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

package com.redhat.rhn.domain.server.virtualhostmanager;

import com.redhat.rhn.testing.BaseTestCaseWithUser;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PropertyValueException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * VirtualHostManagerFactory Test
 */
public class VirtualHostManagerFactoryTest extends BaseTestCaseWithUser {

    private VirtualHostManagerFactory factory;
    private static final String SUSE_CLOUD = "SUSECloud";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        factory = new VirtualHostManagerFactory() {
            @Override
            Set<String> getAvailableGathererModules() {
                return new HashSet<String>() {{
                        add(SUSE_CLOUD);
                }};
            }
        };
    }

    /**
     * Tests creating and retrieving a VirtualHostManager.
     * @throws Exception if anything goes wrong
     */
    public void testCreateAndGetVHM() throws Exception {
        factory.createVirtualHostManager("mylabel", user.getOrg(), SUSE_CLOUD, null);
        VirtualHostManager fromDb = factory.lookupByLabel("mylabel");

        assertEquals("mylabel", fromDb.getLabel());
        assertEquals(user.getOrg(), fromDb.getOrg());
        assertEquals(SUSE_CLOUD, fromDb.getGathererModule());
        assertNull(fromDb.getConfigs());
    }

    /**
     * Tests creating and retrieving a VirtualHostManager with credentials.
     * @throws Exception if anything goes wrong
     */
    public void testCreateAndGetVHManagerWithCreds() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("user", "FlashGordon");
        config.put("pass", "The savior of the universe");

        factory.createVirtualHostManager("mylabel", user.getOrg(), SUSE_CLOUD, config);
        VirtualHostManager virtualHostManager = factory.lookupByLabel("mylabel");

        assertEquals("FlashGordon", virtualHostManager.getCredentials().getUsername());
        assertEquals("The savior of the universe",
                virtualHostManager.getCredentials().getPassword());
        // user and pass should be deleted from configs
        assertTrue(virtualHostManager.getConfigs().isEmpty());
    }

    /**
     * Tests creating and retrieving a VirtualHostManager with config.
     * @throws Exception if anything goes wrong
     */
    public void testCreateAndGetVHMWithConfigs() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("testkey", "43");

        factory.createVirtualHostManager("mylabel", user.getOrg(), SUSE_CLOUD, config);
        VirtualHostManager virtualHostManager = factory.lookupByLabel("mylabel");

        assertEquals(1, virtualHostManager.getConfigs().size());
        assertEquals("43", virtualHostManager.getConfigs().iterator().next().getValue());
    }

    /**
     * Tests creating and retrieving a VirtualHostManager with null label.
     */
    public void testCreateAndGetVHMNullLabel() {
        try {
            factory.createVirtualHostManager(null, user.getOrg(), SUSE_CLOUD, null);
        }
        catch (PropertyValueException e) {
            return; // we've caught exception about violating not-null constraint
        }
        fail("PQLException should have been thrown.");
    }

    /**
     * Tests creating and retrieving a VirtualHostManager with null organization.
     */
    public void testCreateAndGetVHMNullOrg() {
        try {
            factory.createVirtualHostManager("mylabel", null, SUSE_CLOUD, null);
        }
        catch (PropertyValueException e) {
            return; // we've caught exception about violating not-null constraint
        }
        fail("PQLException should have been thrown.");
    }

    /**
     * Tests retrieving non-existing Virtual Host Manager.
     */
    public void testCreateAndGetNonExistentVHM() {
        try {
            factory.lookupByLabel("idontexist");
        }
        catch (ObjectNotFoundException e) {
            return; // we've caught exception about non existing object
        }
        fail("ObjectNotFoundException should have been thrown.");
    }

    /**
     * Tests deleting an existing Virtual Host Manager.
     */
    public void testDeleteVirtualHostManager() {
        Map<String, String> config = new HashMap<>();
        config.put("testkey", "43");
        String myLabel = "myLabel";
        VirtualHostManager vhm = factory
                .createVirtualHostManager(myLabel, user.getOrg(), SUSE_CLOUD, config);
        assertNotEmpty(factory.lookupByLabel(myLabel).getConfigs());
        assertNotNull(factory.lookupByLabel(myLabel));

        factory.delete(vhm);

        try {
            factory.lookupByLabel(myLabel);
        }
        catch (ObjectNotFoundException e) {
            return; // we've caught exception about non existing object
        }
        fail("ObjectNotFoundException should have been thrown.");
    }

    /**
     * Tests throwing the exception when creating Virtual Host Manager with non-existing
     * gatherer module
     */
    public void testCreateVHMInvalidGathererModule() {
        VirtualHostManagerFactory customFactory = new VirtualHostManagerFactory() {
            @Override
            Set<String> getAvailableGathererModules() {
                return Collections.emptySet();
            }
        };

        try {
            customFactory
                    .createVirtualHostManager("myLabel", user.getOrg(), SUSE_CLOUD, null);
        }
        catch (IllegalArgumentException e) {
            return; // we've caught exception invalid gatherer module
        }
        fail("IllegalArgumentException should have been thrown.");
    }
}
