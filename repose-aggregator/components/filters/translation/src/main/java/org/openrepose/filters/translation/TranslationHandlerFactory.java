package org.openrepose.filters.translation;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.filters.translation.config.*;
import org.openrepose.filters.translation.xslt.XsltParameter;
import org.openrepose.filters.translation.xslt.xmlfilterchain.XmlChainPool;
import org.openrepose.filters.translation.xslt.xmlfilterchain.XmlFilterChain;
import org.openrepose.filters.translation.xslt.xmlfilterchain.XmlFilterChainBuilder;
import org.openrepose.filters.translation.xslt.xmlfilterchain.XmlFilterChainFactory;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.core.services.config.ConfigurationService;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<TranslationHandler> {

  public static final String SAXON_HE_FACTORY_NAME = "net.sf.saxon.TransformerFactoryImpl";
  public static final String SAXON_EE_FACTORY_NAME = "com.saxonica.config.EnterpriseTransformerFactory";

  private final List<XmlChainPool> responseProcessorPools;
  private final List<XmlChainPool> requestProcessorPools;
  private final String configurationRoot;
  private final Object lock = new Object();
  private final XslUpdateListener xslListener;
  private final String config;
  private SAXTransformerFactory transformerFactory;
  private TranslationConfig configuration;
  private XmlFilterChainBuilder xsltChainBuilder;

  public TranslationHandlerFactory(ConfigurationService configService, String configurationRoot, String config) {

    transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance(SAXON_HE_FACTORY_NAME, null);

    requestProcessorPools = new ArrayList<XmlChainPool>();
    responseProcessorPools = new ArrayList<XmlChainPool>();
    this.configurationRoot = configurationRoot;
    this.config = config;
    xslListener = new XslUpdateListener(this, configService, configurationRoot);
  }

  @Override
  protected Map<Class, UpdateListener<?>> getListeners() {
    return new HashMap<Class, UpdateListener<?>>() {
      {
        put(TranslationConfig.class, new TranslationConfigurationListener());
      }
    };
  }

  @Override
  protected TranslationHandler buildHandler() {

    if (!this.isInitialized()) {
      return null;
    }
    synchronized (lock) {
      return new TranslationHandler(new ArrayList<XmlChainPool>(requestProcessorPools), new ArrayList<XmlChainPool>(responseProcessorPools), configuration.isMultiMatch());
    }
  }

  private List<XsltParameter> buildXslParamList(TranslationBase translation) {
    final List<XsltParameter> params = new ArrayList<XsltParameter>();
    if (translation.getStyleSheets() != null) {
      for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
        for (StyleParam param : sheet.getParam()) {
          params.add(new XsltParameter<String>(sheet.getId(), param.getName(), param.getValue()));
        }
      }
    }
    return params;
  }

  private ObjectPool<XmlFilterChain> buildChainPool(final TranslationBase translation) {
      return new SoftReferenceObjectPool<XmlFilterChain>(new XmlFilterChainFactory(xsltChainBuilder, translation, configurationRoot, config));
  }

  private void addStyleSheetsToWatchList(final TranslationBase translation) {
    if (translation.getStyleSheets() != null) {
      for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
        if (sheet.getHref() != null) {
          xslListener.addToWatchList(sheet.getHref());
        }
      }
    }
  }

  public void buildProcessorPools() {
    synchronized (lock) {
      requestProcessorPools.clear();
      responseProcessorPools.clear();

      if (configuration.getResponseTranslations() != null) {
        for (final ResponseTranslation translation : configuration.getResponseTranslations().getResponseTranslation()) {
          addStyleSheetsToWatchList(translation);
        }
      }

      if (configuration.getRequestTranslations() != null) {
        for (final RequestTranslation translation : configuration.getRequestTranslations().getRequestTranslation()) {
          addStyleSheetsToWatchList(translation);
        }
      }

      if (configuration.getResponseTranslations() != null) {
        for (final ResponseTranslation translation : configuration.getResponseTranslations().getResponseTranslation()) {
          List<XsltParameter> params = buildXslParamList(translation);
          ObjectPool<XmlFilterChain> pool = buildChainPool(translation);

          responseProcessorPools.add(new XmlChainPool(
                  translation.getContentType(),
                  translation.getAccept(),
                  null,
                  translation.getCodeRegex(),
                  translation.getTranslatedContentType(),
                  params,
                  pool));
        }
      }

      if (configuration.getRequestTranslations() != null) {
        for (final RequestTranslation translation : configuration.getRequestTranslations().getRequestTranslation()) {

          List<XsltParameter> params = buildXslParamList(translation);
          ObjectPool<XmlFilterChain> pool = buildChainPool(translation);

          requestProcessorPools.add(new XmlChainPool(
                  translation.getContentType(),
                  translation.getAccept(),
                  translation.getHttpMethods(),
                  null,
                  translation.getTranslatedContentType(),
                  params,
                  pool));
        }
      }

    }
  }

  private void updateTransformerPool (String transFactoryClass) {
     if (!transformerFactory.getClass().getCanonicalName().equals(transFactoryClass)) {
        transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance(transFactoryClass, null);
     }
  }

  class TranslationConfigurationListener implements UpdateListener<TranslationConfig> {

    private boolean isInitialized = false;

    @Override
    public void configurationUpdated(TranslationConfig newConfig) {
      synchronized (lock) {
        configuration = newConfig;

        if (configuration.getXslEngine() == XSLEngine.SAXON_EE ) {
           updateTransformerPool( SAXON_EE_FACTORY_NAME );
        } else {
            updateTransformerPool(SAXON_HE_FACTORY_NAME);
        }

        xslListener.unsubscribe();
        try {
          xsltChainBuilder = new XmlFilterChainBuilder(transformerFactory, false, configuration.isAllowDoctypeDecl());
          buildProcessorPools();
        } finally {
          xslListener.listen();
        }
      }
      isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
      return isInitialized;

    }
  }
}
