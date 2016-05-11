package com.kbdunn.nimbus.server.api;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

import com.kbdunn.nimbus.api.model.NimbusUserDecorator;
import com.kbdunn.nimbus.common.model.NimbusUser;

//@Provider
@Priority(100)
public class EntityDecorationFilter implements ContainerResponseFilter {

	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		if (Response.Status.fromStatusCode(responseContext.getStatus()).getFamily().equals(Response.Status.Family.CLIENT_ERROR)
				|| !responseContext.hasEntity()) {
			return;
		}
		
		Object entity = responseContext.getEntity();
		
		if (entity instanceof NimbusUser) {
			responseContext.setEntity(decorateNimbusUser((NimbusUser) entity));
		}
	}
	
	private NimbusUserDecorator decorateNimbusUser(NimbusUser user) {
		NimbusUserDecorator wrapped = new NimbusUserDecorator(user);
		//List<StorageDevice> activated = storageService.getStorageDevicesAssignedToUser(user);
		
		/*for (StorageDevice sd : storageService.getAccessibleStorageDevices(user)) {
			NimbusUserStorageDevice usd = new NimbusUserStorageDevice(user, sd);
			usd.setActivated(activated.contains(sd));
			if (usd.isActivated()) 
				usd.setHomeFolder(userService.getUserHomeFolder(user, sd));
			wrapped.addStorageDevice(usd);
		}*/
		
		return wrapped;
	}
}