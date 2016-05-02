package com.kbdunn.nimbus.web.header;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.dashboard.DashboardView;
import com.kbdunn.nimbus.web.files.FileView;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.media.MediaView;
import com.kbdunn.nimbus.web.share.ShareListView;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

public class HeaderPanel extends Panel implements Refreshable {
	
	private static final long serialVersionUID = 1L;
	
	// These will be the navigation links
	private static final String[][] navLinkToViewMap = { 
		{DashboardView.NAME, "Dashboard"}, 
		{FileView.NAME, "Files"},
		{MediaView.NAME, "Media"},
		{ShareListView.NAME, "Sharing"} 
	};
	
	private CssLayout content;
	private CssLayout leftLayout;
	private CssLayout rightLayout;
	private CssLayout navigation;
	
	private NimbusUser user;
	private MediaPlayerPanel playerPanel;
	private AboutPopup aboutPopup;
	
	public HeaderPanel() {
		buildLayout();
		refresh();
	}
	
	public MediaPlayerPanel getPlayerPanel() {
		return playerPanel;
	}

	@Override
	public void refresh() {
		user = NimbusUI.getCurrentUser();
		
		navigation.removeAllComponents();
		rightLayout.removeAllComponents();
		
		if (user != null) {
			showLoggedInHeader();			
		} else {
			showLoggedOutHeader();
		}
	}
	
	private void buildLayout() {
		setWidth("100%");
		setHeight("90px");
		addStyleName("header");
		
		// Content
		content = new CssLayout();
		setContent(content);
		content.addStyleName("content");
		
		// Left layout for logo and navigation
		leftLayout = new CssLayout();
		leftLayout.addStyleName("left-layout");
		content.addComponent(leftLayout);
		
		// Right layout for CssLayout button or user menu
		rightLayout = new CssLayout();
		rightLayout.addStyleName("right-layout");
		content.addComponent(rightLayout);
		
		// Nimbus Logo
		/*Label nimbusLogo = new Label("Nimbus");
		nimbusLogo.addStyleName("nimbus-logo");
		Label cloud = FontAwesome.CLOUD.getLabel();
		cloud.addStyleName("nimbus-cloud");
		cloud.addStyleName(ValoTheme.LABEL_COLORED);
		leftLayout.addComponent(cloud);
		leftLayout.addComponent(nimbusLogo);*/
		leftLayout.addComponent(new NimbusLogo());
		
		// Navigation Layout
		navigation = new CssLayout();
		navigation.addStyleName("nav-layout");
		navigation.setSizeUndefined();
		leftLayout.addComponent(navigation);
		
		aboutPopup = new AboutPopup();
	}
	
	private void showLoggedInHeader() {
		
		// Add navigation buttons				
		for (String[] link: navLinkToViewMap) {
			Link l = new Link(link[1], new ExternalResource("#!" + link[0]));
			l.addStyleName(ValoTheme.LINK_LARGE);
			navigation.addComponent(l);
		}
		
		// Media Player Panel //
		MediaPlayer mediaPlayer = new MediaPlayer();
		playerPanel = new MediaPlayerPanel(mediaPlayer);
		mediaPlayer.setPlayerPanel(playerPanel);
		rightLayout.addComponent(playerPanel);
		
		// User MenuBar
		MenuBar userMenu = new MenuBar();
		userMenu.addStyleName("user-menu");
		userMenu.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
		rightLayout.addComponent(userMenu);
		
		// Top-level item
		String greeting = user.getName() == null || user.getName().isEmpty() ?
				"Hi, " + user.getEmail() :
				"Hi, " + user.getName();
		MenuItem userItem = userMenu.addItem(greeting, null);
		
		// Invite Friends Link
		/*userItem.addItem("Invite", FontAwesome.ENVELOPE_O, new Command() {
			private static final long serialVersionUID = 1L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				navigator.navigateTo(InviteView.NAME);
			}
		});*/
		
		// Update Media Library Link
		// The update library functionality does nothing currently
		/*userItem.addItem("Update Media Library", FontAwesome.HEADPHONES, new Command() {
			private static final long serialVersionUID = 1L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				if (!NimbusUI.getCurrentPropertiesService().isDemoMode()) {
					NimbusUI.getCurrent().getTaskController().addAndStartTask(new UpdateLibraryOperation(user));
				}
			}
		});*/
		
		// Settings link
		userItem.addItem("Settings", FontAwesome.COGS, new Command() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void menuSelected(MenuItem selectedItem) {
				NimbusUI.getCurrent().getSettingsController().openSettingsHome();
			}
		});
		
		// About link
		userItem.addItem("About", FontAwesome.INFO_CIRCLE, new Command(){
			private static final long serialVersionUID = 1L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				aboutPopup.open();
			}
		});
		
		// Logout link
		userItem.addItem("Logout", FontAwesome.SIGN_OUT, new Command(){
			private static final long serialVersionUID = 1L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				if (!NimbusUI.getPropertiesService().isDemoMode()) logout();
			}
		});
	}
	
	private void showLoggedOutHeader() {
		if (navigation != null) navigation.removeAllComponents();
		
		// Login Button
		Button login = new Button("Login", new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				new LoginDialog().showDialog();
			}
		});
		
		login.addStyleName("login-link");
		login.addStyleName(ValoTheme.BUTTON_LINK);
		login.addStyleName(ValoTheme.BUTTON_LARGE);
		rightLayout.addComponent(login);
	}
	
	private void logout() {
		//UI.getCurrent().getSession().setAttribute("user", null);
		UI.getCurrent().getSession().close();
		//((NimbusUI) UI.getCurrent()).setAuthenticated(false);
		Page.getCurrent().setLocation("");
		//UI.getCurrent().getNavigator().navigateTo("");
		//refresh();
	}
}
