package com.kbdunn.nimbus.web.error;

import com.kbdunn.nimbus.web.settings.SettingsView;
import com.kbdunn.nimbus.web.settings.drives.StorageSettingsTab;
import com.kbdunn.nimbus.web.settings.profile.ProfileSettingsTab;
import com.kbdunn.nimbus.web.settings.users.UserSettingsTab;
import com.kbdunn.nimbus.web.share.ShareListView;
import com.vaadin.server.VaadinServlet;

public enum Error {
	
	UNKNOWN(
			"Sorry, an error occurred!",
			"Please try again",
			"Please try again"),
	
	NO_DRIVES(
			"There are no hard drives connected!", 
			"Connect a hard drive and try again.",
			"Contact an administrator of this cloud for assistance."),
			
	DRIVE_DISCONNECTED( 
			"Your hard drive is disconnected!", 
			"Connect your drive or <a href=\"" 
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + SettingsView.NAME + "/" + StorageSettingsTab.FRAGMENT + "\">click here</a> to manage drives.",
			"Contact an administrator of this cloud for assistance."),
					
	NO_DRIVES_ASSIGNED(
			"You are not assigned to a drive!", 
			"Click <a href=\"" 
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + SettingsView.NAME + "/" + UserSettingsTab.FRAGMENT + "\">here</a> to assign drives to users.",
			"Contact an administrator of this cloud for assistance, they can assign you a hard drive."),
			
	DRIVE_NOT_ACTIVATED(
			"You haven't activated a drive yet!",
			"Click <a href=\"" 
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + SettingsView.NAME + "/" + StorageSettingsTab.FRAGMENT + "\">here</a> to activate a drive.",
			"Click <a href=\"" 
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + SettingsView.NAME + "/" + StorageSettingsTab.FRAGMENT + "\">here</a> to activate a drive."),
	
	USER_DIRECTORIES_MISSING(
			"The folders used to store your files are gone!",
			"Sorry, this should be taken care of for you. Visit the <a href=\""
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + SettingsView.NAME + "/" + StorageSettingsTab.FRAGMENT + "\">drives</a> page to fix this.",
			"Contact an administrator of this cloud for assistance."),
					
	SHARE_NOT_FOUND(
			"The file share was not found!",
			"Check the URL and try again, or <a href=\""
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + ShareListView.NAME + "\">click here</a> to view your shares.",
			"Check the URL and try again, or <a href=\""
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + ShareListView.NAME + "\">click here</a> to view your shares."),
			
	ACCESS_DENIED(
			"You don't have access to that!",
			"", ""),
			
	INVALID_FILE(
			"File or folder not found!",
			"Check the URL and try again.",
			"Check the URL and try again."),
		
	EMAIL_CONFIGURATION(
			"Your email configuration isn't working!",
			"Visit the <a href=\""
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + SettingsView.NAME + "/" + ProfileSettingsTab.FRAGMENT + "\">profile settings</a> page to fix this.",
			"Visit the <a href=\""
					+ VaadinServlet.getCurrent().getServletContext().getContextPath() 
					+ "#!" + SettingsView.NAME + "/" + ProfileSettingsTab.FRAGMENT + "\">profile settings</a> page to fix this."),
	
	UNCAUGHT_EXCEPTION(
			"Something went wrong!", 
			"Please submit this error to our developers in the <a href=\"https://groups.google.com/forum/#!forum/nimbuscloud\">Nimbus forum</a>.", 
			"Please submit this error to our developers in the <a href=\"https://groups.google.com/forum/#!forum/nimbuscloud\">Nimbus forum</a>.");
	
	private final String fragment, title, adminMessage, userMessage;
	
	Error(String title, String adminMessage, String userMessage) {
		this.fragment = toString().toLowerCase().replace("_", "-");
		this.title = title;
		this.adminMessage = adminMessage;
		this.userMessage = userMessage;
	}
	
	public String getPath() {
		return ErrorView.NAME + "/" + fragment;
	}
	
	public static Error fromFragment(String fragment) {
		return Error.valueOf(fragment.toUpperCase().replace("-", "_"));
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getAdminMessage() {
		return adminMessage;
	}
	
	public String getUserMessage() {
		return userMessage;
	}
}
