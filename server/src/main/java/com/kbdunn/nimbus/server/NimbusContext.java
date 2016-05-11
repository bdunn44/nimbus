package com.kbdunn.nimbus.server;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereResourceFactory;
import org.atmosphere.cpr.BroadcasterFactory;

import com.kbdunn.nimbus.server.service.LocalAsyncService;
import com.kbdunn.nimbus.server.service.LocalFileService;
import com.kbdunn.nimbus.server.service.LocalFileShareService;
import com.kbdunn.nimbus.server.service.LocalMediaLibraryService;
import com.kbdunn.nimbus.server.service.LocalOAuthService;
import com.kbdunn.nimbus.server.service.LocalPropertiesService;
import com.kbdunn.nimbus.server.service.LocalStorageService;
import com.kbdunn.nimbus.server.service.LocalUserService;

public class NimbusContext {
	
	// Singleton
	private static NimbusContext instance;
	
	// Nimbus Services
	private final LocalUserService userService;
	private final LocalFileService fileService;
	private final LocalStorageService storageService;
	private final LocalMediaLibraryService mediaLibraryService;
	private final LocalFileShareService fileShareService;
	private final LocalPropertiesService propertiesService;
	private final LocalAsyncService asyncService;
	private final LocalOAuthService oAuthService;
	
	// Atmosphere
	private AtmosphereFramework atmosphereFramework;

	private NimbusContext() {
		userService = new LocalUserService();
		fileService = new LocalFileService();
		storageService = new LocalStorageService();
		mediaLibraryService = new LocalMediaLibraryService();
		fileShareService = new LocalFileShareService();
		propertiesService = new LocalPropertiesService();
		asyncService = new LocalAsyncService();
		oAuthService = new LocalOAuthService();
		
		userService.initialize(this);
		fileService.initialize(this);
		storageService.initialize(this);
		mediaLibraryService.initialize(this);
		fileShareService.initialize(this);
		asyncService.initialize(this);
		oAuthService.initialize(this);
	}
	
	public static NimbusContext instance() {
		if (instance == null) {
			instance = new NimbusContext();
		}
		return instance;
	}
	
	public LocalUserService getUserService() {
		return userService;
	}

	public LocalFileService getFileService() {
		return fileService;
	}

	public LocalStorageService getStorageService() {
		return storageService;
	}

	public LocalMediaLibraryService getMediaLibraryService() {
		return mediaLibraryService;
	}

	public LocalFileShareService getFileShareService() {
		return fileShareService;
	}

	public LocalPropertiesService getPropertiesService() {
		return propertiesService;
	}

	public LocalAsyncService getAsyncService() {
		return asyncService;
	}

	public LocalOAuthService getOAuthService() {
		return oAuthService;
	}
	
	public AtmosphereFramework getAtmosphereFramework() {
		return atmosphereFramework;
	}
	
	public void setAtmosphereFramework(AtmosphereFramework atmosphereFramework) {
		this.atmosphereFramework = atmosphereFramework;
	}
	
	public BroadcasterFactory getAtmosphereBroadcasterFactory() {
		if (atmosphereFramework != null) return atmosphereFramework.getBroadcasterFactory();
		return null;
	}
	
	public AtmosphereResourceFactory getAtmosphereResourceFactory() {
		if (atmosphereFramework != null) return atmosphereFramework.atmosphereFactory();
		return null;
	}
}