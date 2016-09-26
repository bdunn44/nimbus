package com.kbdunn.nimbus.web;

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.kbdunn.nimbus.common.server.AsyncService;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.common.server.MediaLibraryService;
import com.kbdunn.nimbus.common.server.NimbusphereService;
import com.kbdunn.nimbus.common.server.OAuthService;
import com.kbdunn.nimbus.common.server.PropertiesService;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.server.UserService;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.BootstrapFragmentResponse;
import com.vaadin.server.BootstrapListener;
import com.vaadin.server.BootstrapPageResponse;
import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionDestroyEvent;
import com.vaadin.server.SessionDestroyListener;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.server.VaadinServlet;
import com.vaadin.util.FileTypeResolver;

@WebServlet(value = "/*", asyncSupported = true)
@VaadinServletConfiguration(productionMode = true, ui = NimbusUI.class, widgetset = "com.kbdunn.nimbus.web.NimbusWidgetset")
public class NimbusVaadinServlet extends VaadinServlet implements SessionInitListener, SessionDestroyListener, ServletContextListener {
	
	private static final long serialVersionUID = 1L;
	//private static final Logger log = Logger.getLogger(VdnServlet.class.getName());
	private static final AtomicInteger activeSessions = new AtomicInteger(0);

	// Nimbus Services
	private UserService userService;
	private FileService fileService;
	private StorageService storageService;
	private MediaLibraryService mediaLibraryService;
	private FileShareService fileShareService;
	private PropertiesService propertiesService;
	private AsyncService asyncService;
	private OAuthService oAuthService;
	private NimbusphereService nimbusphereService;
	
	/*public AsyncService getAsyncService() {
		return asyncService;
	}
	
	public void setAsyncService(AsyncService asyncService) {
		this.asyncService = asyncService;
	}*/

	@Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        getService().addSessionInitListener(this);
        getService().addSessionDestroyListener(this);
        
        // Set custom error messages
 		getService().setSystemMessagesProvider(new SystemMessagesProvider() {
 			private static final long serialVersionUID = 1L;
 			
 			@Override 
 		    public SystemMessages getSystemMessages(
 		        SystemMessagesInfo systemMessagesInfo) {
 		        CustomizedSystemMessages messages = new CustomizedSystemMessages();
 		        messages.setCommunicationErrorCaption("Communication Problem");
 		        messages.setCommunicationErrorMessage("Click here to continue.");
 		        messages.setCommunicationErrorNotificationEnabled(true);
 		        messages.setCommunicationErrorURL(null);
 		        messages.setSessionExpiredCaption("Session Expired");
 		        messages.setSessionExpiredMessage("Click here to continue.");
 		        messages.setSessionExpiredNotificationEnabled(true);
 		        messages.setSessionExpiredURL(null);
 		        return messages;
 		    }
 		});
 		
 		// Get pointers to services
 		asyncService = (AsyncService) getServletContext().getAttribute(AsyncService.class.getName());
 		fileService = (FileService) getServletContext().getAttribute(FileService.class.getName());
 		fileShareService = (FileShareService) getServletContext().getAttribute(FileShareService.class.getName());
 		mediaLibraryService = (MediaLibraryService) getServletContext().getAttribute(MediaLibraryService.class.getName());
 		propertiesService = (PropertiesService) getServletContext().getAttribute(PropertiesService.class.getName());
 		storageService = (StorageService) getServletContext().getAttribute(StorageService.class.getName());
 		userService = (UserService) getServletContext().getAttribute(UserService.class.getName());
 		oAuthService = (OAuthService) getServletContext().getAttribute(OAuthService.class.getName());
 		nimbusphereService = (NimbusphereService) getServletContext().getAttribute(NimbusphereService.class.getName());
 		
 		// Set MIME Types
 		FileTypeResolver.addExtension("ogg", "audio/ogg");
 		FileTypeResolver.addExtension("ogv", "video/ogg");
 		FileTypeResolver.addExtension("mp4", "video/mp4");
 		FileTypeResolver.addExtension("webm", "video/webm");
 		FileTypeResolver.addExtension("wmv", "video/x-ms-wmv");
 		FileTypeResolver.addExtension("wma", "audio/x-ms-wma");
 		FileTypeResolver.addExtension("flv", "video/x-flv");
 		FileTypeResolver.addExtension("avi", "video/x-msvideo");
 		FileTypeResolver.addExtension("ac3", "audio/ac3");
 		FileTypeResolver.addExtension("flac", "audio/flac");
 		FileTypeResolver.addExtension("mka", "audio/x-matroska");
 		FileTypeResolver.addExtension("mkv", "video/x-matroska");
    }
	
	@Override
	public void sessionDestroy(SessionDestroyEvent event) {
		if (activeSessions.decrementAndGet() == 0)
			asyncService.resumeStorageDeviceReconciliations();
	}
	
	@Override
	public void sessionInit(SessionInitEvent event) throws ServiceException {
		activeSessions.incrementAndGet();
		asyncService.pauseStorageDeviceReconciliations();
		
		event.getSession().addBootstrapListener(new BootstrapListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void modifyBootstrapFragment(BootstrapFragmentResponse response) {  }

			@Override
			public void modifyBootstrapPage(BootstrapPageResponse response) {
				final String ver = propertiesService.getNimbusVersion();
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "57x57").attr("href", "/images/apple-icon-57x57.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "60x60").attr("href", "/images/apple-icon-60x60.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "72x72").attr("href", "/images/apple-icon-72x72.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "76x76").attr("href", "/images/apple-icon-76x76.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "114x114").attr("href", "/images/apple-icon-114x114.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "120x120").attr("href", "/images/apple-icon-120x120.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "144x144").attr("href", "/images/apple-icon-144x144.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "152x152").attr("href", "/images/apple-icon-152x152.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "apple-touch-icon").attr("sizes", "180x180").attr("href", "/images/apple-icon-180x180.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "icon").attr("type", "image/png").attr("sizes", "192x192" ).attr("href", "/images/android-icon-192x192.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "icon").attr("type", "image/png").attr("sizes", "32x32").attr("href", "/images/favicon-32x32.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "icon").attr("type", "image/png").attr("sizes", "96x96").attr("href", "/images/favicon-96x96.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "icon").attr("type", "image/png").attr("sizes", "16x16").attr("href", "/images/favicon-16x16.png?v=" + ver);
				response.getDocument().head().appendElement("link").attr("rel", "manifest").attr("href", "/images/manifest.json");
				response.getDocument().head().appendElement("meta").attr("name", "msapplication-TileColor").attr("content", "#ffffff");
				response.getDocument().head().appendElement("meta").attr("name", "msapplication-TileImage").attr("content", "/images/ms-icon-144x144.png?v=" + ver);
				response.getDocument().head().appendElement("meta").attr("name", "theme-color").attr("content", "#ffffff");
			}
		});
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// Set the root path system property - used by Log4j file appender
		ServletContext context = sce.getServletContext();
		System.setProperty("rootPath", context.getRealPath("/"));
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Do nothing
	}
	
	public static NimbusVaadinServlet getCurrent() {
		return (NimbusVaadinServlet) VaadinServlet.getCurrent();
	}
	
	protected UserService getUserService() {
		return userService;
	}

	protected FileService getFileService() {
		return fileService;
	}

	protected StorageService getStorageService() {
		return storageService;
	}

	protected MediaLibraryService getMediaLibraryService() {
		return mediaLibraryService;
	}

	protected FileShareService getFileShareService() {
		return fileShareService;
	}

	protected PropertiesService getPropertiesService() {
		return propertiesService;
	}

	protected AsyncService getAsyncService() {
		return asyncService;
	}
	
	protected OAuthService getOAuthService() {
		return oAuthService;
	}
	
	protected NimbusphereService getNimbusphereService() {
		return nimbusphereService;
	}
}
