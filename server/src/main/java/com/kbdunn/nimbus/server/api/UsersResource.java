package com.kbdunn.nimbus.server.api;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.rest.entity.NimbusUserStorageDevice;
import com.kbdunn.nimbus.common.rest.entity.PostResponse;
import com.kbdunn.nimbus.common.rest.entity.PutResponse;
import com.kbdunn.nimbus.common.rest.entity.StorageDeviceList;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.server.UserService;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UsersResource {
	
	private static final Logger log = LogManager.getLogger(UsersResource.class.getName());
	
	@Context ServletContext context;
	
	private UserService getUserService() {
		return (UserService) context.getAttribute(UserService.class.getName());
	}
	
	private StorageService getStorageService() {
		return (StorageService) context.getAttribute(StorageService.class.getName());
	}
	
	private FileService getFileService() {
		return (FileService) context.getAttribute(FileService.class.getName());
	}
	
	@GET
	public Response getUsers() {
		return Response.ok(getUserService().getAllUsers()).build();
	}
	
	@POST
	public Response postUser(NimbusUser user) {
		log.debug("POST content is: " + user.toString());
		
		return Response.status(Status.CREATED)
				.entity(new PostResponse(222l, new Date(), new Date(), "users/222", "users/" + user.getName()))
				.build();
	}
	
	@PUT
	@Path("/{id : [0-9]+}")
	public Response putUser(@PathParam("id") Long id, NimbusUser user) {
		log.debug("PUT content is: " + user.toString());
		return Response.status(Status.CREATED)
				.entity(new PutResponse(444l, new Date(), new Date(), "users/444", "users/" + user.getName()))
				.build();
	}
	
	@GET
	@Path("/{id : [0-9]+}")
	public Response getUser(@PathParam("id") Long id) {
		NimbusUser user = getUserService().getUserById(id);
		if (user != null)
			return Response.ok(user).build();
		return Response.status(Status.NOT_FOUND).build();
	}
	
	@GET
	@Path("/{key: (?!\\d+).+}") //// Not numeric
	public Response getUser(@PathParam("key") String key) {
		NimbusUser user = getUserService().getUserByNameOrEmail(key);
		if (user != null)
			return Response.ok(user).build();
		return Response.status(Status.NOT_FOUND).build();
	}
	
	@GET
	@Path("/{userId}/devices")
	public Response getUserDevices(@PathParam("userId") Long userId, 
			@QueryParam("available") Boolean available) {
		NimbusUser user = getUserService().getUserById(userId);
		if (user == null)
			return Response.status(Status.NOT_FOUND).build();
		List<StorageDevice> sds = getStorageService().getStorageDevicesAssignedToUser(user);
		
		// Filter based on query params
		if (available != null) {
			Iterator<StorageDevice> it = sds.iterator();
			while (it.hasNext()) {
				StorageDevice sd = it.next();
				if (available != null && available && !getStorageService().storageDeviceIsAvailable(sd)) {
					it.remove();
				}
			}
		}
		return Response.ok(new StorageDeviceList(sds)).build();
	}
	
	@GET
	@Path("/{userId}/devices/{deviceId}")
	public Response getUserDevice(@PathParam("userId") Long userId, @PathParam("deviceId") Long deviceId) {
		NimbusUser user = getUserService().getUserById(userId);
		StorageDevice sd = getStorageService().getStorageDeviceById(deviceId);
		if (user == null || sd == null)
			return Response.status(Status.NOT_FOUND).build();
		NimbusUserStorageDevice usd = new NimbusUserStorageDevice(user, sd);
		usd.setActivated(getStorageService().getStorageDevicesAssignedToUser(user).contains(sd));
		return Response.ok(usd).build();
	}
	
	@GET
	@Path("/{userId}/devices/{deviceId}/files")
	public Response getUserDeviceHomeContents(@PathParam("userId") Long userId, @PathParam("deviceId") Long deviceId) {
		NimbusUser user = getUserService().getUserById(userId);
		StorageDevice sd = getStorageService().getStorageDeviceById(deviceId);
		if (user == null || sd == null)
			return Response.status(Status.NOT_FOUND).build();
		if (!getStorageService().getStorageDevicesAssignedToUser(user).contains(sd)) {
			// Device is not active
			return Response.noContent().build();
		}
		return Response.ok(
				getFileService().getContents(getUserService().getUserHomeFolder(user, sd))
			).build();
	}
}
