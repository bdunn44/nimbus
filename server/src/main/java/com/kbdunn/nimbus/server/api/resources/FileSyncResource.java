package com.kbdunn.nimbus.server.api.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.kbdunn.nimbus.api.client.model.FileAddEvent;
import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.FileMoveEvent;
import com.kbdunn.nimbus.api.client.model.NimbusError;
import com.kbdunn.nimbus.api.client.model.PostResponse;
import com.kbdunn.nimbus.api.client.model.PutResponse;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.service.FileSyncService;
import com.kbdunn.nimbus.server.service.LocalUserService;

@Path("/sync")
@Produces(MediaType.APPLICATION_JSON)
public class FileSyncResource {
	
	private static final Logger log = LogManager.getLogger(FileSyncResource.class);
	
	@HeaderParam(NimbusHttpHeaders.Key.REQUESTOR)
	private String requestor;
	
	@HeaderParam(NimbusHttpHeaders.Key.ORIGINATION_ID)
	private String originationId;
	
	private final FileSyncService syncService;
	private final LocalUserService userService;
	
	public FileSyncResource() {
		syncService = NimbusContext.instance().getFileSyncService();
		userService = NimbusContext.instance().getUserService();
	}
	
	@GET
	@Path("/files")
	public Response getSyncFiles() {
		long start = System.nanoTime();
		final NimbusUser user = getUser();
		log.debug("Building sync file list for " + user);
		try {
			List<SyncFile> result = syncService.getSyncFileList(user);
			log.debug("Took " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + "ms to build sync file list");
			return Response.ok(result).build();
		} catch (Exception e) {
			log.error("Error generating sync file list", e);
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
					.entity(new NimbusError("Error generating sync file list. " + e.getClass() + ": " + e.getMessage()))
					.build();
		}
	}
	
	@GET
	@Path("/files/{path:.+}")
	public Response getSyncFile(@PathParam("path") String path) {
		path = StringUtil.decodeUriUtf8(path);
		NimbusUser user = getUser();
		try {
			SyncFile file = syncService.toSyncFile(user, path);
			if (file == null) {
				return Response.noContent().build();
			}
			return Response.ok(file).build();
		} catch (Exception e) {
			log.error("Error retrieving sync file " + path, e);
			return Response.serverError()
					.entity(new NimbusError("Error retrieving sync file " + path + ". " + e.getClass() + ": " + e.getMessage()))
					.build();
		}
	}
	
	@PUT
	@Path("/files/{path:.+}")
	public Response createDirectory(@PathParam("path") String path, FileAddEvent addEvent) {
		if (addEvent.getOriginationId() == null) addEvent.setOriginationId(originationId);
		path = StringUtil.decodeUriUtf8(path);
		final NimbusUser user = getUser();
		if (!syncService.userHasSyncRoot(user)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NimbusError("User '" + user.getName() + "' does not have file synchronization configured."))
					.build();
		}
		log.info("Processing " + addEvent);
		log.info("File is " + addEvent.getFile());
		try {
			final NimbusFile file = syncService.processCreateDirectory(user, addEvent);
			if (file == null) throw new IOException("Failed to create directory");
			return Response.ok(new PutResponse(file.getId(), file.getCreated(), file.getUpdated(), getUri(file))).build();
		} catch (Exception e) {
			log.error("Error processing create directory request", e);
			return Response.status(e instanceof FileConflictException ? Status.CONFLICT : Status.INTERNAL_SERVER_ERROR)
					.entity(new NimbusError(e.getClass() + ":" + e.getMessage()))
					.build();
		}
	}
	
	@DELETE
	@Path("/files/{path:.+}")
	public Response deleteFile(@PathParam("path") String path) {
		path = StringUtil.decodeUriUtf8(path);
		final NimbusUser user = getUser();
		if (!syncService.userHasSyncRoot(user)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NimbusError("User '" + user.getName() + "' does not have file synchronization configured."))
					.build();
		}
		log.info("Processing delete " + path + " for " + getUser());
		try {
			if (syncService.processFileDelete(user, path, originationId)) {
				return Response.ok().build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new NimbusError("File does not exist"))
						.build();
			}
		} catch (Exception e) {
			log.error("Error processing delete request", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new NimbusError("Unable to delete file. " + e.getClass() + ": " + e.getMessage()))
					.build();
		} 
	}
	
	@POST
	@Path("/files/move")
	public Response moveFile(FileMoveEvent moveEvent) {
		if (moveEvent.getOriginationId() == null) moveEvent.setOriginationId(originationId);
		final NimbusUser user = getUser();
		if (!syncService.userHasSyncRoot(user)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NimbusError("User '" + user.getName() + "' does not have file synchronization configured."))
					.build();
		}
		log.info("Processing " + moveEvent);
		try {
			final NimbusFile file = syncService.processFileMove(user, moveEvent);
			return Response.ok(new PostResponse(file.getId(), file.getCreated(), file.getUpdated(), getUri(file))).build();
		} catch (Exception e) {
			log.error("Error processing file move request", e);
			return Response.status(e instanceof FileConflictException ? Status.CONFLICT : Status.INTERNAL_SERVER_ERROR)
					.entity(new NimbusError(e.getClass() + ": " + e.getMessage()))
					.build();
		}
	}
	
	@POST
	@Path("/files/copy")
	public Response copyFile(FileCopyEvent copyEvent) {
		if (copyEvent.getOriginationId() == null) copyEvent.setOriginationId(originationId);
		final NimbusUser user = getUser();
		if (!syncService.userHasSyncRoot(user)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NimbusError("User '" + user.getName() + "' does not have file synchronization configured."))
					.build();
		}
		log.info("Processing " + copyEvent);
		try {
			final NimbusFile copy = syncService.processFileCopy(user, copyEvent);
			return Response.ok(new PostResponse(copy.getId(), copy.getCreated(), copy.getUpdated(), getUri(copy))).build();
		} catch (Exception e) {
			log.error("Error processing file copy request", e);
			return Response.status(e instanceof FileConflictException ? Status.CONFLICT : Status.INTERNAL_SERVER_ERROR)
					.entity(new NimbusError(e.getClass() + ": " + e.getMessage()))
					.build();
		}
	}
	
	@POST
	@Path("/files/upload/{path:.+}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@PathParam("path") String path, @FormDataParam("file") InputStream in) {
		final NimbusUser user = getUser();
		if (!syncService.userHasSyncRoot(user)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NimbusError("User '" + user.getName() + "' does not have file synchronization configured."))
					.build();
		}
		path = StringUtil.decodeUriUtf8(path);
		try {
			syncService.processFileUpload(user, path, originationId, in);
		} catch (Exception e) {
			log.error("Error processing file upload request", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new NimbusError(e.getClass() + ": " + e.getMessage()))
					.build();
		}
		return Response.ok().build();
	}
	
	@GET
	@Path("/files/download/{path:.+}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadFile(@PathParam("path") String path) {
		final NimbusUser user = getUser();
		if (!syncService.userHasSyncRoot(user)) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new NimbusError("User '" + user.getName() + "' does not have file synchronization configured."))
					.build();
		}
		path = StringUtil.decodeUriUtf8(path);
		try {
			final NimbusFile file = syncService.toNimbusFile(getUser(), path);
			if (file.isDirectory()) {
				throw new IllegalArgumentException("Cannot request the download of a directory: " + file.getPath());
			}
			if (!syncService.isTrackedFile(user, file)) {
				throw new IllegalArgumentException("Requested file is not in the user's sync directory: " + file.getPath());
			}
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream out) throws IOException, WebApplicationException {
					try (InputStream in = new FileInputStream(new File(file.getPath()))) {
						byte[] buffer = new byte[2048];
						int length = 0;
						while ((length = in.read(buffer)) != -1) {
							out.write(buffer, 0, length);
						}
						out.flush();
					} catch (Exception e) {
						log.error("Error writing to download output stream", e);
					}
				}
			};
			return Response.ok(stream).build();
		} catch (Exception e) {
			log.error("Error processing file download request", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new NimbusError(e.getClass() + ": " + e.getMessage()))
					.build();
		}
	}
	
	private NimbusUser getUser() {
		return userService.getUserByNameOrEmail(requestor);
	}
	
	private String getUri(NimbusFile file) throws IOException {
		return "sync/files" + StringUtil.encodeUriUtf8(syncService.toSyncFile(file).getPath());
	}
}
