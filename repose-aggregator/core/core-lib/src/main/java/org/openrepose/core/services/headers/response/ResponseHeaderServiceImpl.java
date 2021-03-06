package org.openrepose.core.services.headers.response;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;
import java.net.MalformedURLException;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("responseHeaderService")
public class ResponseHeaderServiceImpl implements ResponseHeaderService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ResponseHeaderServiceImpl.class);
    private ViaHeaderBuilder viaHeaderBuilder;
    private LocationHeaderBuilder locationHeaderBuilder;

    @Override
    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder, LocationHeaderBuilder locationHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @Override
    public void setVia(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        final String existingVia = response.getHeader(CommonHttpHeader.VIA.toString());
        final String myVia = viaHeaderBuilder.buildVia(request);
        final String via = StringUtilities.isBlank(existingVia) ? myVia : existingVia + ", " + myVia ;

        response.setHeader(CommonHttpHeader.VIA.name(), via);
    }

    @Override
    public void fixLocationHeader(HttpServletRequest originalRequest, MutableHttpServletResponse response, RouteDestination destination, String destinationLocationUri, String proxiedRootContext) {
        String destinationUri = cleanPath(destinationLocationUri);
        if (!destinationUri.matches("^https?://.*")) {
            // local dispatch
            destinationUri = proxiedRootContext;
        }
        try {
            locationHeaderBuilder.setLocationHeader(originalRequest, response, destinationUri, destination.getContextRemoved(), proxiedRootContext);
        } catch (MalformedURLException ex) {
            LOG.warn("Invalid URL in location header processing", ex);
        }
    }

    private String cleanPath(String uri) {
        return uri == null ? "" : uri.split("\\?")[0];
    }
}
