package org.openrepose.services.ratelimit;

import org.openrepose.services.ratelimit.config.RateLimitList;
import org.openrepose.services.ratelimit.exception.OverLimitException;

import java.util.List;
import java.util.Map;

public interface RateLimitingService {

   RateLimitList queryLimits(String user, List<String> groups);

   void trackLimits(String user, List<String> groups, String uri, Map<String, String[]> parameterMap, String httpMethod, int datastoreWarnLimit) throws OverLimitException;
}
