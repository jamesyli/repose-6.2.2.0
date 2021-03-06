/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.core.services.datastore;

import org.openrepose.commons.utils.Destroyable;
import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.routing.RoutingService;
import org.openrepose.services.datastore.DatastoreService;


public interface DistributedDatastoreLauncherService extends Destroyable{
   
   void initialize(ConfigurationService configurationService, ReposeInstanceInfo instanceInfo, DatastoreService datastoreService,
           ServicePorts servicePorts,RoutingService routingService, String configDirectory);
   void startDistributedDatastoreServlet();
   void stopDistributedDatastoreServlet();
}
