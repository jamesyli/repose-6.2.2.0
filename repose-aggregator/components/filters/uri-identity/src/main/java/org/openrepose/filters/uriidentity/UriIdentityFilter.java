package org.openrepose.filters.uriidentity;


import org.openrepose.filters.uriidentity.config.UriIdentityConfig;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServletContextHelper;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UriIdentityFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(UriIdentityFilter.class);
    private static final String DEFAULT_CONFIG = "uri-identity.cfg.xml";
    private String config;
    private UriIdentityHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        configurationManager = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();
        handlerFactory = new UriIdentityHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/uri-identity-configuration.xsd");
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, UriIdentityConfig.class);
    }
}
