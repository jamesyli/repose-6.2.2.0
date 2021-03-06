package org.openrepose.filters.ratelimiting.write;

import org.openrepose.filters.ratelimiting.exception.RateLimitingSerializationException;
import org.openrepose.filters.ratelimiting.util.LimitsEntityStreamTransformer;
import org.openrepose.filters.ratelimiting.util.combine.LimitsTransformPair;
import org.openrepose.services.ratelimit.config.RateLimitList;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CombinedLimitsWriter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(CombinedLimitsWriter.class);
   private static final LimitsEntityStreamTransformer RESPONSE_TRANSFORMER = new LimitsEntityStreamTransformer();

   public CombinedLimitsWriter() {
   }

   public MediaType write(RateLimitList activeRateLimits, MediaType mediaType, InputStream absoluteLimits, OutputStream outputStream) {

      try {
         final LimitsTransformPair transformPair = new LimitsTransformPair(absoluteLimits, activeRateLimits);

         final ByteArrayOutputStream bos = new ByteArrayOutputStream();
         RESPONSE_TRANSFORMER.combine(transformPair, bos);

         final LimitsResponseMimeTypeWriter responseWriter = new LimitsResponseMimeTypeWriter(RESPONSE_TRANSFORMER);
         
         return responseWriter.writeLimitsResponse(bos.toByteArray(), mediaType, outputStream);
      } catch (Exception ex) {
         LOG.error("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
         throw new RateLimitingSerializationException("Failed to serialize limits upon user request. Reason: " + ex.getMessage(), ex);
      }
   }
}
