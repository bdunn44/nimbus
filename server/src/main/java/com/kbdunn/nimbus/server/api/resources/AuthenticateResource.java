package com.kbdunn.nimbus.server.api.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.kbdunn.nimbus.api.client.model.AuthenticateResponse;

@Path("/authenticate")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class AuthenticateResource {
	
	@GET
	public Response authenticate() {
		// Actual authentication is done by the HMAC request filter.
		// If the request isn't filtered they're authenticated.
		return Response.ok(new AuthenticateResponse("Authenticated")).build();
	}
}
