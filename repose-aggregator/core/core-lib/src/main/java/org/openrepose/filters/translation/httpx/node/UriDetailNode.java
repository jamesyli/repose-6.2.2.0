package org.openrepose.filters.translation.httpx.node;

import org.openrepose.core.httpx.ComplexParameter;
import org.openrepose.core.httpx.RequestHead;
import org.openrepose.core.httpx.URIDetail;
import org.openrepose.filters.translation.httpx.ObjectFactoryUser;

import java.util.Map;

/**
 * @author fran
 */
public class UriDetailNode extends ObjectFactoryUser implements Node {
    private final Map<String, String[]> parameterMap;
    private final RequestHead requestHead;

    public UriDetailNode(Map<String, String[]> parameterMap, RequestHead requestHead) {
        this.parameterMap = parameterMap;
        this.requestHead = requestHead;
    }

    @Override
    public void build() {
        URIDetail uriDetail = getObjectFactory().createURIDetail();
        uriDetail.setFragment("where do we get this?");
        
        if (parameterMap != null) {
            
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                ComplexParameter parameter = getObjectFactory().createComplexParameter();
                parameter.setName(entry.getKey());

                for (String value : entry.getValue()) {
                    parameter.getValue().add(value);
                }

                uriDetail.getQueryParameter().add(parameter);
            }
        }

        requestHead.setUriDetail(uriDetail);
    }
}
