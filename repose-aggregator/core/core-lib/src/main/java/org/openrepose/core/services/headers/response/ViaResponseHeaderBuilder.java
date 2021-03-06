package org.openrepose.core.services.headers.response;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;

public class ViaResponseHeaderBuilder extends ViaHeaderBuilder {

    private final String reposeVersion;
    private final String viaReceivedBy;

    public ViaResponseHeaderBuilder(String reposeVersion, String configuredViaReceivedBy) {
        this.reposeVersion = reposeVersion;
        this.viaReceivedBy = StringUtilities.isBlank(configuredViaReceivedBy) ? "Repose" : configuredViaReceivedBy;
    }

    @Override
    protected String getViaValue(MutableHttpServletRequest request) {
        StringBuilder builder = new StringBuilder(" ");

        return builder.append(viaReceivedBy).append(" (Repose/").append(reposeVersion).append(")").toString();
    }
}
