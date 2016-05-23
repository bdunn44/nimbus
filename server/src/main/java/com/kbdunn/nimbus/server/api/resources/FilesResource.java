package com.kbdunn.nimbus.server.api.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.util.FileUtil;

@Path("/files")
@Produces(MediaType.APPLICATION_JSON)
public class FilesResource {
	
	@Context ServletContext context;
	
	private FileService getFileService() {
		return (FileService) context.getAttribute(FileService.class.getName());
	}
	
	@GET
	@Path("/{id: \\d+}")
	public Response getFile(@PathParam("id") Long id) {
		NimbusFile file = getFileService().getFileById(id);
		if (file != null)
			return Response.ok(file).build();
		return Response.status(Status.NOT_FOUND).build();
	}
	
	@GET
	@Path("/{path: (?!\\d+).+}") // Not numeric
	public Response getFile(@PathParam("path") String path) {
		if (FileUtil.pathContainsInvalidCharacters(path)) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		if (!path.startsWith("/")) path = "/" + path;
		NimbusFile file = getFileService().getFileByPath(path);
		if (file != null && getFileService().fileExistsOnDisk(file))
			return Response.ok(file).build();
		return Response.status(Status.NOT_FOUND).build();
	}
}
