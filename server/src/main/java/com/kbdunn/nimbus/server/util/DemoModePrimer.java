package com.kbdunn.nimbus.server.util;

import java.util.Date;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.exception.ShareBlockNameConflictException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.FilesystemLocation;
import com.kbdunn.nimbus.common.model.HardDrive;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.async.UpdateMediaLibraryOperation;
import com.kbdunn.nimbus.server.service.LocalFileService;
import com.kbdunn.nimbus.server.service.LocalFileShareService;
import com.kbdunn.nimbus.server.service.LocalMediaLibraryService;
import com.kbdunn.nimbus.server.service.LocalPropertiesService;
import com.kbdunn.nimbus.server.service.LocalStorageService;
import com.kbdunn.nimbus.server.service.LocalUserService;

public class DemoModePrimer {
	
	private static final Logger log = LogManager.getLogger(DemoModePrimer.class.getName());
	//public static HardDrive demoDrive;
	
	/*static {
		LogManager.getRootLogger().removeAllAppenders();
		Properties log4jprops = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(new File("/nimbus/log4j.properties"));
			log4jprops.load(is);
		} catch (IOException e) {
			log.error(e, e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		PropertyConfigurator.configure(log4jprops);
	}*/

	public void go() {
		log.info("Starting Demo Primer....");
		LocalStorageService storageService = NimbusContext.instance().getStorageService();
		LocalUserService userService = NimbusContext.instance().getUserService();
		LocalFileService fileService = NimbusContext.instance().getFileService();
		LocalMediaLibraryService mediaService = NimbusContext.instance().getMediaLibraryService();
		LocalFileShareService shareService = NimbusContext.instance().getFileShareService();
		
		HardDrive demoDrive = new HardDrive(null, "Demo Hard Drive", "/media/nimbus-123456", "/dev/sda1", "DRIVE LABEL", "123456", "ntfs", 
				true, true, false, false, 500000000L, 100000L, new Date(), new Date());
		storageService.save(demoDrive);
		
		FilesystemLocation demoFs = new FilesystemLocation(null, "Folder Storage", "/nimbus", false, false, new Date(), new Date());
		storageService.save(demoFs);
		
		NimbusUser u = new NimbusUser();
		u = new NimbusUser();
		u.setName(new LocalPropertiesService().getDemoUsername());
		u.setEmail("demo@cloudnimbus.org");
		u.setPasswordDigest("1000:934c26f1f0f8d03bcf4af7c3ab1fe1755a8e3d8c2e1ccdfc:7736a5ac51ccf0466c641a52e79cf1882cbfa4b674c45d52");
		u.setAdministrator(true);
		try {
			userService.save(u);
		} catch (UsernameConflictException | EmailConflictException
				| FileConflictException e) {
			log.error(e, e);
		}
		
		storageService.assignDriveToUser(demoDrive, u);
		storageService.assignDriveToUser(demoFs, u);
		
		// Sleep to fake out the ownership check (checks for the first created user by date)
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Ignore
		}
		
		NimbusUser o = new NimbusUser();
		o = new NimbusUser();
		o.setName("OtherUser");
		o.setEmail("otheruser@cloudnimbus.org");
		o.setPasswordDigest("1000:934c26f1f0f8d03bcf4af7c3ab1fe1755a8e3d8c2e1ccdfc:7736a5ac51ccf0466c641a52e79cf1882cbfa4b674c45d52");
		o.setAdministrator(false);
		try {
			userService.save(o);
		} catch (UsernameConflictException | EmailConflictException
				| FileConflictException e) {
			log.error(e, e);
		}
		
		storageService.assignDriveToUser(demoDrive, o);
		log.info("Demo Drive and Users saved");
		
		log.info("Starting reconciliation");
		Thread t = new Thread(new UpdateMediaLibraryOperation(null, u));
		t.start();
		try {
			t.join();
			log.info("Media library update finished");
		} catch (InterruptedException e) {
			// Ignore
		}
		
		// Create demo playlist
		Playlist playlist = new Playlist(u.getId(), "Demo Playlist");
		mediaService.save(playlist);
		mediaService.setPlaylistSongs(playlist, mediaService.getSongs(u));
		log.info("Demo playlist created");
		
		// Create demo user share
		NimbusFile hdHome = userService.getUserHomeFolder(u, demoDrive);
		NimbusFile fsHome = userService.getUserHomeFolder(u, demoFs);
		ShareBlock share = new ShareBlock();
		share.setUserId(u.getId());
		share.setName("My First Share");
		share.setMessage("Hey there, this is my first file share.");
		share.setExternal(true);
		try {
			shareService.save(share);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(hdHome, "Pictures/nimbus_logo.JPG"));
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(hdHome, "Pictures/C No Class.JPG"));
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(fsHome, "Music"));
		
		// Read other user share
		ShareBlock readshare = new ShareBlock();
		readshare.setUserId(o.getId());
		NimbusFile oHome = userService.getUserHomeFolder(o, demoDrive);
		readshare.setName("Nimbus Website Files");
		readshare.setMessage("These are the website files for cloudnimbus.org.");
		try {
			shareService.save(readshare);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}
		shareService.addFileToShareBlock(readshare, fileService.resolveRelativePath(oHome, "index.html"));
		shareService.addFileToShareBlock(readshare, fileService.resolveRelativePath(oHome, "favicon.ico"));
		shareService.addFileToShareBlock(readshare, fileService.resolveRelativePath(oHome, "css"));
		ShareBlockAccess readaccess = shareService.getShareBlockAccess(readshare, u);
		shareService.save(readaccess);
		
		// Update other user share
		ShareBlock updateshare = new ShareBlock();
		updateshare.setUserId(o.getId());
		updateshare.setName("Vaadin Resources");
		updateshare.setMessage("This is the book of Vaadin.");
		try {
			shareService.save(updateshare);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}
		shareService.addFileToShareBlock(updateshare, fileService.resolveRelativePath(oHome, "book-of-vaadin.pdf"));
		ShareBlockAccess updateaccess = shareService.getShareBlockAccess(updateshare, u);
		updateaccess.setCanUpdate(true);
		shareService.save(updateaccess);
		
		log.info("Demo file shares created");
		log.info("Done.");
	}
}