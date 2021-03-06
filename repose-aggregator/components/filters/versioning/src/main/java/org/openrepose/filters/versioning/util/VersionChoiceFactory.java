package org.openrepose.filters.versioning.util;

import org.openrepose.filters.versioning.config.MediaType;
import org.openrepose.filters.versioning.config.ServiceVersionMapping;
import org.openrepose.filters.versioning.schema.MediaTypeList;
import org.openrepose.filters.versioning.schema.VersionChoice;
import org.openrepose.filters.versioning.schema.VersionStatus;

/**
 * @author fran
 */
public class VersionChoiceFactory {
    private final ServiceVersionMapping serviceVersionMapping;

    public VersionChoiceFactory(ServiceVersionMapping serviceVersionMapping) {
        this.serviceVersionMapping = serviceVersionMapping;
    }

    public VersionChoice create() {
        VersionChoice versionChoice = new VersionChoice();

        versionChoice.setId(serviceVersionMapping.getId());
        versionChoice.setStatus(serviceVersionMapping.getStatus() == null ? null : VersionStatus.fromValue(serviceVersionMapping.getStatus().value()));
        versionChoice.setMediaTypes(convertMediaTypes());

        return versionChoice;
    }

    private MediaTypeList convertMediaTypes() {
        MediaTypeList mediaTypeList = null;

        if (serviceVersionMapping.getMediaTypes() != null) {
            mediaTypeList = new MediaTypeList();

            for (MediaType configuredMediaType : serviceVersionMapping.getMediaTypes().getMediaType()){
                org.openrepose.filters.versioning.schema.MediaType responseMediaType = new org.openrepose.filters.versioning.schema.MediaType();

                responseMediaType.setBase(configuredMediaType.getBase());
                responseMediaType.setType(configuredMediaType.getType());

                mediaTypeList.getMediaType().add(responseMediaType);
            }
        }

        return mediaTypeList;
    }
}
