/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.integration;

import static org.geoserver.geofence.core.model.enums.AdminGrantType.ADMIN;
import static org.geoserver.geofence.core.model.enums.AdminGrantType.USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.GeofenceAccessManager;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class GeofenceAccessManagerIntegrationTest extends GeoServerSystemTestSupport {

    public GeofenceIntegrationTestSupport support;

    private GeofenceAccessManager accessManager;
    private GeoFenceConfigurationManager configManager;
    private RuleAdminService ruleService;

    private static final String AREA_WKT =
            "MULTIPOLYGON(((0.0016139656066815888 -0.0006386457758059581,0.0019599705696027314 -0.0006386457758059581,0.0019599705696027314 -0.0008854090051601674,0.0016139656066815888 -0.0008854090051601674,0.0016139656066815888 -0.0006386457758059581)))";

    private static final String AREA_WKT_2 =
            "MULTIPOLYGON(((0.0011204391479413545 -0.0006405065746780663,0.0015764146804730927 -0.0006405065746780663,0.0015764146804730927 -0.0014612625330857614,0.0011204391479413545 -0.0014612625330857614,0.0011204391479413545 -0.0006405065746780663)))";

    @Before
    public void setUp() {
        support =
                new GeofenceIntegrationTestSupport(
                        () -> GeoServerSystemTestSupport.applicationContext);
        support.before();
        accessManager =
                applicationContext.getBean(
                        "geofenceRuleAccessManager", GeofenceAccessManager.class);
        configManager = applicationContext.getBean(GeoFenceConfigurationManager.class);

        ruleService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
        // add rule to grant access to all to everything with a very low priority
        if (ruleService.getRuleByPriority(9999) == null)
            support.addRule(GrantType.ALLOW, null, null, null, null, null, null, 9999);
    }

    @After
    public void clearRules() {
        support.after();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    @Test
    public void testAllowedAreaLayerInTwoGroups() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, if one of the container
        // LayerGroup doesn't have an allowed area, there will be no allowedArea in the final
        // filter.
        Long idRule = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();

            LayerInfo places = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
            LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));

            group1 =
                    createsLayerGroup(
                            catalog,
                            "group21",
                            LayerGroupInfo.Mode.NAMED,
                            null,
                            Arrays.asList(places, forests));
            group2 =
                    createsLayerGroup(
                            catalog,
                            "group22",
                            LayerGroupInfo.Mode.NAMED,
                            null,
                            Arrays.asList(places, forests));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group21",
                            0);

            // limit rule for anonymousUser on LayerGroup group2
            support.addRule(
                    GrantType.LIMIT,
                    "anonymousUser",
                    "ROLE_ANONYMOUS",
                    "WMS",
                    null,
                    null,
                    "group22",
                    1);

            // add allowed Area only to the first layer group
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326);

            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl =
                    (VectorAccessLimits) accessManager.getAccessLimits(user, places);

            assertEquals(vl.getReadFilter(), Filter.INCLUDE);
            assertEquals(vl.getWriteFilter(), Filter.INCLUDE);
            logout();
        } finally {
            removeLayerGroup(group1, group2);
        }
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsModeSingle() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to LayerGroups with SINGLE mode, the LayerGroup
        // is not applied
        Long idRule = null;
        Long idRule2 = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 =
                    createsLayerGroup(
                            catalog,
                            "group31",
                            LayerGroupInfo.Mode.SINGLE,
                            null,
                            Arrays.asList(lakes, fifteen));
            group2 =
                    createsLayerGroup(
                            catalog,
                            "group32",
                            LayerGroupInfo.Mode.SINGLE,
                            null,
                            Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group31",
                            4);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group32",
                            5);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326);
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_2, 4326);

            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            assertEquals(vl.getReadFilter(), Filter.INCLUDE);
            assertEquals(vl.getWriteFilter(), Filter.INCLUDE);
            logout();
        } finally {
            removeLayerGroup(group1, group2);
        }
    }

    @Test
    public void testCiteCannotWriteOnWorkspace() {
        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(false);
        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        // check workspace access
        WorkspaceInfo citeWS = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());
    }

    @Test
    public void testAdminRule_WorkspaceAccessLimits_Role_based_rule() {
        final String citeUserRole = "CITE_USER";
        final String citeAdminRole = "CITE_ADMIN";
        final String sfUserRole = "SF_USER";
        final String sfAdminRole = "SF_ADMIN";

        final Authentication citeUser = getUser("citeuser", "cite", citeUserRole);
        final Authentication citeAdmin = getUser("citeadmin", "cite", citeAdminRole);
        final Authentication sfUser = getUser("sfuser", "sfuser", sfUserRole);
        final Authentication sfAdmin = getUser("sfadmin", "sfadmin", sfAdminRole);

        final WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        final WorkspaceInfo sf = getCatalog().getWorkspaceByName("sf");

        support.addAdminRule(0, null, citeAdminRole, cite.getName(), ADMIN);
        support.addAdminRule(1, null, citeUserRole, cite.getName(), USER);
        support.addAdminRule(2, null, sfAdminRole, sf.getName(), ADMIN);
        support.addAdminRule(3, null, sfUserRole, sf.getName(), USER);

        setUser(citeUser);
        assertAdminAccess(citeUser, cite, false);
        assertAdminAccess(citeUser, sf, false);

        setUser(sfUser);
        assertAdminAccess(sfUser, cite, false);
        assertAdminAccess(sfUser, sf, false);

        setUser(citeAdmin);
        assertAdminAccess(citeAdmin, cite, true);
        assertAdminAccess(citeAdmin, sf, false);

        setUser(sfAdmin);
        assertAdminAccess(sfAdmin, cite, false);
        assertAdminAccess(sfAdmin, sf, true);
    }

    @Test
    public void testAdminRule_WorkspaceAccessLimits_Username_based_rule() {
        final String citeUserRole = "CITE_USER";
        final String citeAdminRole = "CITE_ADMIN";
        final String sfUserRole = "SF_USER";
        final String sfAdminRole = "SF_ADMIN";

        final Authentication citeUser = getUser("citeuser", "cite", citeUserRole);
        final Authentication citeAdmin = getUser("citeadmin", "cite", citeAdminRole);
        final Authentication sfUser = getUser("sfuser", "sfuser", sfUserRole);
        final Authentication sfAdmin = getUser("sfadmin", "sfadmin", sfAdminRole);

        final WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        final WorkspaceInfo sf = getCatalog().getWorkspaceByName("sf");

        support.addAdminRule(0, citeAdmin.getName(), null, cite.getName(), ADMIN);
        support.addAdminRule(1, citeUser.getName(), null, cite.getName(), USER);
        support.addAdminRule(2, sfAdmin.getName(), null, sf.getName(), ADMIN);
        support.addAdminRule(3, sfUser.getName(), null, sf.getName(), USER);

        assertAdminAccess(citeUser, cite, false); // fail
        assertAdminAccess(citeUser, sf, false);
        assertAdminAccess(sfUser, cite, false);
        assertAdminAccess(sfUser, sf, false);

        assertAdminAccess(citeAdmin, cite, true);
        assertAdminAccess(citeAdmin, sf, false);
        assertAdminAccess(sfAdmin, cite, false);
        assertAdminAccess(sfAdmin, sf, true);
    }

    private void assertAdminAccess(Authentication user, WorkspaceInfo ws, boolean expectedAdmin) {
        WorkspaceAccessLimits userAccess = accessManager.getAccessLimits(user, ws);
        assertEquals(expectedAdmin, userAccess.isAdminable());
    }

    protected Authentication getUser(String username, String password, String... roles) {

        List<GrantedAuthority> l = new ArrayList<>();
        for (String role : roles) {
            l.add(new SimpleGrantedAuthority(role));
        }

        return new UsernamePasswordAuthenticationToken(username, password, l);
    }

    protected void setUser(Authentication user) {
        SecurityContextHolder.getContext().setAuthentication(user);
    }

    protected LayerGroupInfo createsLayerGroup(
            Catalog catalog,
            String name,
            LayerGroupInfo.Mode mode,
            LayerInfo rootLayer,
            List<LayerInfo> layers)
            throws Exception {
        return createsLayerGroup(catalog, name, mode, rootLayer, layers, null);
    }

    protected LayerGroupInfo createsLayerGroup(
            Catalog catalog,
            String name,
            LayerGroupInfo.Mode mode,
            LayerInfo rootLayer,
            List<LayerInfo> layers,
            CoordinateReferenceSystem groupCRS)
            throws Exception {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);

        group.setMode(mode);
        if (rootLayer != null) {
            group.setRootLayer(rootLayer);
            group.setRootLayerStyle(rootLayer.getDefaultStyle());
        }
        for (LayerInfo li : layers) group.getLayers().add(li);
        group.getStyles().add(null);
        group.getStyles().add(null);

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.calculateLayerGroupBounds(group);
        if (groupCRS != null) {
            ReferencedEnvelope re = group.getBounds();
            MathTransform transform =
                    CRS.findMathTransform(
                            group.getBounds().getCoordinateReferenceSystem(), groupCRS);
            Envelope bbox = JTS.transform(re, transform);
            ReferencedEnvelope newRe =
                    new ReferencedEnvelope(
                            bbox.getMinX(),
                            bbox.getMaxX(),
                            bbox.getMinY(),
                            bbox.getMaxY(),
                            groupCRS);
            group.setBounds(newRe);
        }
        catalog.add(group);
        return group;
    }

    private void removeLayerGroup(LayerGroupInfo... groups) {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        for (LayerGroupInfo group : groups) {
            if (group != null) {
                getCatalog().remove(group);
            }
        }
        logout();
    }
}
