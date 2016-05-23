package com.kbdunn.nimbus.api.client;

import java.io.File;
import java.util.List;

import javax.ws.rs.core.GenericType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.FileAddEvent;
import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.FileMoveEvent;
import com.kbdunn.nimbus.api.client.model.PostResponse;
import com.kbdunn.nimbus.api.client.model.PutResponse;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.NimbusResponse;
import com.kbdunn.nimbus.common.util.StringUtil;

public class FileSyncManager {

	private static final Logger log = LoggerFactory.getLogger(FileSyncManager.class);
	
	private static final String SYNC_ENDPOINT = "/sync";
	private static final String SYNC_FILES_ENDPOINT = SYNC_ENDPOINT + "/files";
	
	private final NimbusClient client;
	
	protected FileSyncManager(NimbusClient client) {
		this.client = client;
	}
	
	public SyncFile getSyncFile(String path) throws InvalidRequestException, InvalidResponseException, TransportException {
		NimbusResponse<SyncFile> response = client.process(new NimbusRequest<Void, SyncFile>(
				client.getCredentials(), 
				NimbusRequest.Method.GET, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT + StringUtil.encodeUriUtf8(path),
				new GenericType<SyncFile>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
		return response.getEntity();
	}
	
	public List<SyncFile> getSyncFileList() throws InvalidRequestException, InvalidResponseException, TransportException {
		NimbusResponse<List<SyncFile>> response = client.process(new NimbusRequest<Void, List<SyncFile>>(
				client.getCredentials(), 
				NimbusRequest.Method.GET, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT,
				new GenericType<List<SyncFile>>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
		return response.getEntity();
	}
	
	public void createDirectory(SyncFile syncDirectory) throws InvalidRequestException, InvalidResponseException, TransportException {
		log.info("Processing createDirectory for " + syncDirectory);
		NimbusResponse<PutResponse> response = client.process(new NimbusRequest<FileAddEvent, PutResponse>(
				client.getCredentials(), 
				NimbusRequest.Method.PUT, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT + StringUtil.encodeUriUtf8(syncDirectory.getPath()),
				new FileAddEvent(syncDirectory),
				new GenericType<PutResponse>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
	}
	
	public void move(SyncFile source, SyncFile target) throws InvalidRequestException, InvalidResponseException, TransportException {
		NimbusResponse<PostResponse> response = client.process(new NimbusRequest<FileMoveEvent, PostResponse>(
				client.getCredentials(), 
				NimbusRequest.Method.POST, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT + "/move",
				new FileMoveEvent(source, target),
				new GenericType<PostResponse>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
	}
	
	public void copy(SyncFile source, SyncFile target) throws InvalidRequestException, InvalidResponseException, TransportException {
		NimbusResponse<PostResponse> response = client.process(new NimbusRequest<FileCopyEvent, PostResponse>(
				client.getCredentials(), 
				NimbusRequest.Method.POST, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT + "/copy",
				new FileCopyEvent(source, target),
				new GenericType<PostResponse>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
	}
	
	public void delete(SyncFile syncFile) throws InvalidRequestException, InvalidResponseException, TransportException {
		NimbusResponse<Void> response = client.process(new NimbusRequest<Void, Void>(
				client.getCredentials(), 
				NimbusRequest.Method.DELETE, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT + StringUtil.encodeUriUtf8(syncFile.getPath()),
				new GenericType<Void>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
	}
	
	public void upload(SyncFile syncFile, File file) throws InvalidRequestException, InvalidResponseException, TransportException {
		NimbusResponse<Void> response = client.processUpload(new NimbusRequest<File, Void>(
				client.getCredentials(), 
				NimbusRequest.Method.POST, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT + "/upload" + StringUtil.encodeUriUtf8(syncFile.getPath()),
				file,
				new GenericType<Void>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
	}
	
	public File download(SyncFile syncFile) throws InvalidRequestException, InvalidResponseException, TransportException {
		NimbusResponse<File> response = client.processDownload(new NimbusRequest<Void, File>(
				client.getCredentials(), 
				NimbusRequest.Method.GET, 
				client.getApiEndpoint(), 
				SYNC_FILES_ENDPOINT + "/download" + StringUtil.encodeUriUtf8(syncFile.getPath()),
				new GenericType<File>(){}
			), 0);
		if (!response.succeeded()) {
			throw new TransportException(response.getError().getMessage());
		}
		return response.getEntity();
	}
}
