package org.openrepose.filters.authz;

import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.common.auth.openstack.AuthenticationService;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.HttpStatusCode;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.components.authz.rackspace.config.IgnoreTenantRoles;
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint;
import org.openrepose.filters.authz.cache.CachedEndpoint;
import org.openrepose.filters.authz.cache.EndpointListCache;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RequestAuthorizationHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAuthorizationHandler.class);
    private final AuthenticationService authenticationService;
    private final EndpointListCache endpointListCache;
    private final ServiceEndpoint myEndpoint;
    private final List<String> ignoreTenantRoles;

    public RequestAuthorizationHandler(AuthenticationService authenticationService, EndpointListCache endpointListCache,
                                       ServiceEndpoint myEndpoint, IgnoreTenantRoles ignoreTenantRoles) {
        this.authenticationService = authenticationService;
        this.endpointListCache = endpointListCache;
        this.myEndpoint = myEndpoint;
        this.ignoreTenantRoles = getListOfRoles(ignoreTenantRoles);
    }

    private List<String> getListOfRoles(IgnoreTenantRoles ignoreTenantRoles) {
        List<String> roles = new ArrayList<>();
        if(ignoreTenantRoles != null) {
            roles.addAll(ignoreTenantRoles.getIgnoreTenantRole());
            roles.addAll(ignoreTenantRoles.getRole());
        }
        return roles;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.RETURN);
        myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);

        if (authenticationWasDelegated(request)) {
            // We do not support delegation
            myDirector.setResponseStatus(HttpStatusCode.FORBIDDEN);
            LOG.debug("Authentication delegation is not supported by the Rackspace authorization filter. Rejecting request.");
        } else {
            authorizeRequest(myDirector, request);
        }

        return myDirector;
    }

    public void authorizeRequest(FilterDirector director, HttpServletRequest request) {
        final String authenticationToken = request.getHeader(CommonHttpHeader.AUTH_TOKEN.toString());

        if (StringUtilities.isBlank(authenticationToken)) {
            // Reject if no token
            LOG.debug("Authentication token not found in X-Auth-Token header. Rejecting request.");
            director.setResponseStatus(HttpStatusCode.UNAUTHORIZED);
        } else if (!ignoreTenantRoles.isEmpty()) {
            //if service admin roles from cfg populated then compare to x-roles header
            final List<String> xRolesHeaderValueList = Collections.list(request.getHeaders(OpenStackServiceHeader.ROLES.toString()));

            if (checkForAdminRoles(xRolesHeaderValueList)) {
                director.setFilterAction(FilterAction.PASS);
            } else {
                checkTenantEndpoints(director, authenticationToken);
            }
        } else {
            checkTenantEndpoints(director, authenticationToken);
        }
    }

    private boolean checkForAdminRoles(List<String> xRolesHeaderValueStringList) {

        if (xRolesHeaderValueStringList.size() > 0) {
            return adminRoleMatchIgnoringCase(xRolesHeaderValueStringList);
        }

        return false;
    }

    private boolean adminRoleMatchIgnoringCase(List<String> roleStringList) {

        for (String ignoreTenantRole : ignoreTenantRoles) {
            for (String role : roleStringList) {
                if (ignoreTenantRole.equalsIgnoreCase(role)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void checkTenantEndpoints(FilterDirector director, String userToken) {

        try {
            final List<CachedEndpoint> authorizedEndpoints = getEndpointsForToken(userToken);

            if (isEndpointAuthorized(authorizedEndpoints)) {
                director.setFilterAction(FilterAction.PASS);
            } else {
                LOG.info("User token: " + userToken +
                         ": The user's service catalog does not contain an endpoint that matches " +
                         "the endpoint configured in openstack-authorization.cfg.xml: \"" +
                         myEndpoint.getHref() + "\".  User not authorized to access service.");
                director.setResponseStatus(HttpStatusCode.FORBIDDEN);
            }
        } catch (AuthServiceException ex) {
            LOG.error("Failure in authorization component" + ex.getMessage(), ex);
            director.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            LOG.error("Failure in authorization component: " + ex.getMessage(), ex);
            director.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isEndpointAuthorized(final List<CachedEndpoint> authorizedEndpoints) {
        boolean authorized = false;

        for (CachedEndpoint authorizedEndpoint : authorizedEndpoints) {

            if (StringUtilities.isBlank(authorizedEndpoint.getPublicUrl())) {
                LOG.warn("Endpoint Public URL is null.  This is a violation of the OpenStack Identity Service contract.");
            }

            if (StringUtilities.isBlank(authorizedEndpoint.getType())) {
                LOG.warn("Endpoint Type is null.  This is a violation of the OpenStack Identity Service contract.");
            }

            if (StringUtilities.nullSafeStartsWith(authorizedEndpoint.getPublicUrl(), myEndpoint.getHref())) {
                authorized = true;
                break;
            }
        }

        return authorized;
    }

    private List<CachedEndpoint> getEndpointsForToken(String userToken) {
        List<CachedEndpoint> cachedEndpoints = endpointListCache.getCachedEndpointsForToken(userToken);

        if (cachedEndpoints == null || cachedEndpoints.isEmpty()) {
            cachedEndpoints = requestEndpointsForTokenFromAuthService(userToken);

            try {
                endpointListCache.cacheEndpointsForToken(userToken, cachedEndpoints);
            } catch (IOException ioe) {
                LOG.error("Caching failure. Reason: " + ioe.getMessage(), ioe);
            }
        }

        return cachedEndpoints;
    }

    private List<CachedEndpoint> requestEndpointsForTokenFromAuthService(String userToken) {
        final List<Endpoint> authorizedEndpoints = authenticationService.getEndpointsForToken(userToken);
        final LinkedList<CachedEndpoint> serializable = new LinkedList<CachedEndpoint>();

        for (Endpoint ep : authorizedEndpoints) {
            serializable.add(new CachedEndpoint(ep.getPublicURL(), ep.getRegion(), ep.getName(), ep.getType()));
        }

        return serializable;
    }

    // The X-Identity-Status header gets set if client authentication is in delegated mode.  If the token is valid, the
    // value of the X-Identity-Status header is "Confirmed".  If the token is not valid, then the X-Identity-Status
    // header is set to "Indeterminate".  In the future, we may want to allow authorization for delegated requests
    // that have a "Confirmed" status since we know the token is valid in that case.
    public boolean authenticationWasDelegated(HttpServletRequest request) {
        return StringUtilities.isNotBlank(request.getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString()));
    }
}
