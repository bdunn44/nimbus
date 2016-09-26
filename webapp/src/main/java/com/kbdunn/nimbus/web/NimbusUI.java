package com.kbdunn.nimbus.web;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.nimbusphere.NimbusphereStatus;
import com.kbdunn.nimbus.common.server.AsyncService;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.common.server.MediaLibraryService;
import com.kbdunn.nimbus.common.server.NimbusphereService;
import com.kbdunn.nimbus.common.server.OAuthService;
import com.kbdunn.nimbus.common.server.PropertiesService;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.server.UserService;
import com.kbdunn.nimbus.web.dashboard.DashboardView;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.error.ErrorView;
import com.kbdunn.nimbus.web.event.EventRouter;
import com.kbdunn.nimbus.web.files.FileManagerController;
import com.kbdunn.nimbus.web.files.editor.TextEditorController;
import com.kbdunn.nimbus.web.header.MediaPlayer;
import com.kbdunn.nimbus.web.landing.LandingView;
import com.kbdunn.nimbus.web.landing.WelcomeView;
import com.kbdunn.nimbus.web.media.MediaView;
import com.kbdunn.nimbus.web.settings.SettingsController;
import com.kbdunn.nimbus.web.share.ExternalShareController;
import com.kbdunn.nimbus.web.share.ShareController;
import com.kbdunn.nimbus.web.tasks.TaskController;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;

@Theme("nimbus")
@PreserveOnRefresh
@Push(PushMode.MANUAL)
public class NimbusUI extends UI {
	
	private static final long serialVersionUID = -4635222069250718877L;
	private static final Logger log = LogManager.getLogger(NimbusUI.class.getName());
	
	// UI Resources and Controllers
	private Navigator navigator;
	private ErrorView errorView;
	private NimbusLayout uiLayout;
	private EventRouter eventRouter;
	private TaskController taskController;
	private FileManagerController fileController;
	private ShareController shareController;
	private ExternalShareController extShareController;
	private TextEditorController textEditorController;
	private SettingsController settingsController;
	
	@Override
	protected void init(VaadinRequest request) {
		
		VaadinSession.getCurrent().setErrorHandler(ee -> {
			VaadinSession.getCurrent().access(() -> {
				// In some cases the current UI is not accessible, so set as directly as possible
				// Issues encountered when attempting to set the Throwable as a session attribute
				log.error(ee.getThrowable(), ee.getThrowable());
				errorView.setCurrentError(ee.getThrowable()); 
				//addView(ErrorView.NAME, errorView); // Ensure error view is added
				getNavigator().navigateTo(Error.UNCAUGHT_EXCEPTION.getPath());
				push();
			});
		});
		
		eventRouter = new EventRouter();
		
		getPage().setTitle("Nimbus - Your Personal Cloud");
		uiLayout = new NimbusLayout();
		setContent(uiLayout);
		
		// Show main layout, set navigator
		/*if (isDevMode()) {
			log.info("Nimbus is in DEV mode!");
			getSession().setAttribute("user", getUserService().getUserByNameOrEmail("Bryson"));
			setAuthenticated(true);
			
		} else*/ if (isDemoMode()) {
			log.info("Nimbus is in DEMO mode!");
			getSession().setAttribute("user", getUserService().getUserByNameOrEmail(getPropertiesService().getDemoUsername()));
			setAuthenticated(true);
			
		} else {
			setAuthenticated(getSession().getAttribute("user") != null);
		}
		
		getSession().setAttribute("ip", getPage().getWebBrowser().getAddress());
		refresh();
	}
	
	public static NimbusUI getCurrent() {
		return (NimbusUI) UI.getCurrent();
	}
	
	public static NimbusUser getCurrentUser() {
		final Object o = VaadinSession.getCurrent().getAttribute("user");
		if (o == null) return null;
		return (NimbusUser) o;
	}
	
	public static String getCurrentIP() {
		final Object o = VaadinSession.getCurrent().getAttribute("ip");
		if (o == null) return null;
		return (String) o;
	}
	
	public static boolean isLocationAccessible() {
		String url = "http://isup.me/";
		try {
			url += URLEncoder.encode(Page.getCurrent().getLocation().getAuthority(), "UTF-8");
			Document html = Jsoup.connect(url).get();
			String desc = html.getElementById("container").ownText();
			return desc.contains("It's just you.");
		} catch (IOException e) {
			log.error("Error determining if current location (" + url + ") is accessible", e);
			return false;
		}
	}
	
	public EventRouter getEventRouter() {
		return eventRouter;
	}
	
	public static EventRouter getCurrentEventRouter() {
		return getCurrent().getEventRouter();
	}
	
	public static UserService getUserService() {
		return NimbusVaadinServlet.getCurrent().getUserService();
	}
	
	public static FileService getFileService() {
		return NimbusVaadinServlet.getCurrent().getFileService();
	}
	
	public static StorageService getStorageService() {
		return NimbusVaadinServlet.getCurrent().getStorageService();
	}
	
	public static MediaLibraryService getMediaLibraryService() {
		return NimbusVaadinServlet.getCurrent().getMediaLibraryService();
	}

	public static FileShareService getFileShareService() {
		return NimbusVaadinServlet.getCurrent().getFileShareService();
	}
	
	public static PropertiesService getPropertiesService() {
		return NimbusVaadinServlet.getCurrent().getPropertiesService();
	}

	public static AsyncService getAsyncService() {
		return NimbusVaadinServlet.getCurrent().getAsyncService();
	}

	public static OAuthService getOAuthService() {
		return NimbusVaadinServlet.getCurrent().getOAuthService();
	}
	
	public static NimbusphereService getNimbusphereService() {
		return NimbusVaadinServlet.getCurrent().getNimbusphereService();
	}
	
	public static String getUrlGuess() {
		final NimbusphereStatus ns = getPropertiesService().getNimbusphereStatus();
		if (ns != null && ns.getAddress() != null && !ns.isDeleted()) {
			return ns.getAddress();
		}
		String l = Page.getCurrent().getLocation().toASCIIString();
		return l.substring(0, l.indexOf("#!"));
	}
	
	public boolean isDevMode() {
		return getPropertiesService().isDevMode();
	}
	
	public boolean isDemoMode() {
		return getPropertiesService().isDemoMode();
	}
	
	public void showLoadingOverlay() {
		uiLayout.showLoadingOverlay();
	}
	
	public void hideLoadingOverlay() {
		uiLayout.hideLoadingOverlay();
	}

	public void refresh() {
		uiLayout.refresh();
	}
	
	public void addView(String name, View view) {
		navigator.addView(name, view);
	}
	
	public void removeView(String name) {
		navigator.removeView(name);
	}
	
	public MediaPlayer getMediaController() {
		return uiLayout.getHeaderPanel().getPlayerPanel().getMediaController();
	}
	
	public TaskController getTaskController() {
		return taskController;
	}
	
	public ShareController getShareController() {
		return shareController;
	}
	
	public ExternalShareController getExternalShareController() {
		return extShareController;
	}
	
	public FileManagerController getFileController() {
		return fileController;
	}
	
	public TextEditorController getTextEditorController() {
		return textEditorController;
	}
	
	public SettingsController getSettingsController() {
		return settingsController;
	}

	/* hidden components are needed for uploads, where an exception 
	 * is thrown if the component is detached when the upload finishes */
	public void addHiddenComponent(AbstractComponent component) {
		uiLayout.addHiddenComponent(component);
	}
	
	public void removeHiddenComponent(AbstractComponent component) {
		uiLayout.removeHiddenComponent(component);
	}
	
	// Changes the state of the navigator. If a user isn't logged in, do not allow navigation to views
	// that require authentication
	public void setAuthenticated(boolean authenticated) {
		if (getNavigator() != null) getNavigator().destroy();
		navigator = new Navigator(this, uiLayout.getContentContainer());
		navigator.addViewChangeListener(new NimbusViewChangeListener(this));
		
		extShareController = new ExternalShareController();
		log.debug("Creating ErrorView...");
		errorView = new ErrorView();
		addView(ErrorView.NAME, errorView);
		
		if (authenticated) {
			removeView(LandingView.NAME);
			removeView(WelcomeView.NAME);
			
			taskController = new TaskController(); // Taskbar display controller
			uiLayout.setTaskbar(taskController.getTaskbar());
			fileController = new FileManagerController();
			shareController = new ShareController();
			textEditorController = new TextEditorController();
			settingsController = new SettingsController();
			
			log.debug("Creating DashboardView...");
			DashboardView dView = new DashboardView();
			addView("", dView);
			addView(DashboardView.NAME, dView);
			
			log.debug("Creating PlayerView...");
			addView(MediaView.NAME, new MediaView());
			
			navigator.setErrorView(ErrorView.class);
			
		} else {
			if (getUserService().getUserCount() == 0) {
				log.debug("Creating WelcomeView...");
				WelcomeView welcome = new WelcomeView();
				addView(WelcomeView.NAME, welcome);
				addView("", welcome);
				navigator.setErrorView(WelcomeView.class);
			} else {
				LandingView landingView = new LandingView();
				addView(LandingView.NAME, landingView);
				addView("", landingView);
				navigator.setErrorView(LandingView.class);
			}
		}
	}
}