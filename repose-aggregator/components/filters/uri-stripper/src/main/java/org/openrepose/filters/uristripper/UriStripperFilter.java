package org.openrepose.filters.uristripper;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.filters.uristripper.config.UriStripperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

public class UriStripperFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(UriStripperFilter.class);
    private static final String DEFAULT_CONFIG = "uri-stripper.cfg.xml";
    private String config;
    private UriStripperHandlerFactory handlerFactory;
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
        handlerFactory = new UriStripperHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/uri-stripper.xsd");
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, UriStripperConfig.class);
    }
}
