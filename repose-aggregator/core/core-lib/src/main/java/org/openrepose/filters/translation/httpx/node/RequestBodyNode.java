package org.openrepose.filters.translation.httpx.node;

import org.openrepose.core.httpx.Body;
import org.openrepose.core.httpx.Request;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;
import org.openrepose.filters.translation.httpx.processor.TranslationPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author fran
 */
public class RequestBodyNode extends ObjectFactoryUser implements Node {

    private static final Logger LOG = LoggerFactory.getLogger(RequestBodyNode.class);
    private final HttpServletRequest request;
    private final Request messageRequest;
    private final boolean jsonProcessing;

    public RequestBodyNode(HttpServletRequest request, Request messageRequest, boolean jsonProcessing) {
        this.request = request;
        this.messageRequest = messageRequest;
        this.jsonProcessing = jsonProcessing;
    }

    @Override
    public void build() {
        Body body = getObjectFactory().createBody();

        try {
            MimeType contentMimeType = MimeType.getMatchingMimeType(request.getHeader("content-type"));
            MediaType contentType = new MediaType(contentMimeType);
            TranslationPreProcessor processor = new TranslationPreProcessor(request.getInputStream(), contentType, jsonProcessing);
            body.getContent().add(processor.getBodyStream());
        } catch (IOException e) {
            LOG.error("Error adding body stream. Reason: " + e.getMessage(), e);
        }

        messageRequest.setBody(body);
    }
}
