package com.kbdunn.nimbus.server.async;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.model.NimbusUser;

public class UpdateMediaLibraryOperation extends AsyncServerOperation {
	
	private static final Logger log = LogManager.getLogger(UpdateMediaLibraryOperation.class.getName());
	private static List<NimbusUser> runningLibraryUpdates = new ArrayList<NimbusUser>(); 
	
	private NimbusUser user;
	private List<LibraryUpdateFinishedListener> listeners = new ArrayList<LibraryUpdateFinishedListener>();
	
	public UpdateMediaLibraryOperation(AsyncConfiguration config, NimbusUser user) {
		super(config);
		this.user = user;
		super.getConfiguration().setName("Updating media library");
	}
	
	public void addLibraryUpdateFinishedListener(LibraryUpdateFinishedListener listener) {
		listeners.add(listener);
	}
	
	public static boolean libraryUpdateIsRunning(NimbusUser user) {
		return runningLibraryUpdates.contains(user);
	}
	
	protected static synchronized void setLibraryUpdateRunning(NimbusUser user) {
		runningLibraryUpdates.add(user);
	}
	
	protected static synchronized void setLibraryUpdateFinished(NimbusUser user) {
		runningLibraryUpdates.remove(user);
	}
	
	@Override
	public void doOperation() throws Exception {
		if (libraryUpdateIsRunning(user)) {
			log.warn("Library update is already running for " + user.getName());
			return;
		}
		log.info("Updating " + user.getName() + "'s media library....");
		setLibraryUpdateRunning(user);
		
		// Delete user's groups
		/*NMediaDAO.deleteArtistsAndAlbums(user);
		setProgress(.1f);
		
		// Loop through Drives, determine if isSong or isVideo, insert into NFile
		// Don't update media library!!
		
		
		// Derive artist groups, create them
		// This is faster than file.reconcile(userRoot, true, *true*)
		for (MediaGroup g : MediaGroup.derive(user)) 
			NMediaDAO.insertGroup(g);
		setProgress(.9f);
		
		NFileDAO.deleteUnusedFiles();
		NMediaDAO.deleteEmptyArtistsAndAlbums();
		
		log.info("Done updating " + user.getName() + "'s media library.");
		setProgress(1f);
		setLibraryUpdateFinished(user);*/
		
		// Reconcile all files on user's drive
		// Don't update media library, we'll do that manually (faster)
		//LocalUserService userService = new LocalUserService();
		//LocalFileService fileService = new LocalFileService();
		
		/*if (!userService.getActiveUserDrive(user).isReconciled()) {
			log.info("Reconciling the N_FILE table for user " + user.getName());
			fileService.reconcile(userService.getActiveDriveUserHomeFile(user), true);
		}*/
		/*progress = .6f;
		setProgress(progress);
		
		List<Artist> artists = Playlist.derive(user);
		final float artistProgress = (.95f - progress) / artists.size();
		
		for (Artist artist : artists) {
			artist.save();
			progress += artistProgress;
			setProgress(progress);
		}
		
		MediaLibraryDAO.deleteEmptyArtistsAndAlbums();*/
		super.setSucceeded(true);
		
		for (LibraryUpdateFinishedListener listener : listeners)
			listener.libraryUpdateFinished(user);
		
		setLibraryUpdateFinished(user);
		log.info("Done updating " + user.getName() + "'s media library.");
	}
	
	public interface LibraryUpdateFinishedListener {
		public void libraryUpdateFinished(NimbusUser user);
	}
}