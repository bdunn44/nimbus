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
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.service.LocalFileService;
import com.kbdunn.nimbus.server.service.LocalFileShareService;
import com.kbdunn.nimbus.server.service.LocalStorageService;
import com.kbdunn.nimbus.server.service.LocalUserService;

public class DevModePrimer {
	
	private static final Logger log = LogManager.getLogger(DevModePrimer.class.getName());
	
	public void go() {
		
		log.info("Starting Dev Mode Primer....");
		LocalStorageService storageService = NimbusContext.instance().getStorageService();
		LocalUserService userService = NimbusContext.instance().getUserService();
		LocalFileService fileService = NimbusContext.instance().getFileService();
		LocalFileShareService shareService = NimbusContext.instance().getFileShareService();
		
		// Dev Users
		NimbusUser bryson = new NimbusUser();
		bryson.setName("Bryson");
		bryson.setEmail("bryson@cloudnimbus.org");
		bryson.setPasswordDigest("1000:934c26f1f0f8d03bcf4af7c3ab1fe1755a8e3d8c2e1ccdfc:7736a5ac51ccf0466c641a52e79cf1882cbfa4b674c45d52"); // bryson
		bryson.setHmacKey("769fbec5b880e85f5519e9c05f2a76aa630dbb95cf536ef125faae3b401464f9");
		bryson.setApiToken("d6c48e3208bfed94c100b0d5aa7232793f60c2630816d15fc35895a65c18992b");
		bryson.setAdministrator(true);
		try {
			userService.save(bryson);
			log.info("Created user " + bryson.getName() + "(" + bryson.getId() + ")");
		} catch (UsernameConflictException | EmailConflictException
				| FileConflictException e) {
			log.error(e, e);
		}
		
		// Sleep to fake out the ownership check (checks for the first created user by date)
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Ignore
		}
		
		NimbusUser createuser = new NimbusUser();
		createuser.setName("CreateUser");
		createuser.setEmail("create@cloudnimbus.org");
		createuser.setPasswordDigest("1000:934c26f1f0f8d03bcf4af7c3ab1fe1755a8e3d8c2e1ccdfc:7736a5ac51ccf0466c641a52e79cf1882cbfa4b674c45d52"); // bryson
		createuser.setHmacKey("162bf17e42ab8db3bf6c67cc795d69ffd1e958127433fe60b4216abef6b77e24");
		createuser.setAdministrator(false);
		try {
			userService.save(createuser);
			log.info("Created user " + createuser.getName() + "(" + createuser.getId() + ")");
		} catch (UsernameConflictException | EmailConflictException
				| FileConflictException e) {
			log.error(e, e);
		}
		
		NimbusUser updateuser = new NimbusUser();
		updateuser.setName("UpdateUser");
		updateuser.setEmail("update@cloudnimbus.org");
		updateuser.setPasswordDigest("1000:934c26f1f0f8d03bcf4af7c3ab1fe1755a8e3d8c2e1ccdfc:7736a5ac51ccf0466c641a52e79cf1882cbfa4b674c45d52");
		updateuser.setHmacKey("48af1b481cb9fb9f5b9da59dcda8af360549597f70b47a4e34baeb915f6c6eff");
		updateuser.setAdministrator(false);
		try {
			userService.save(updateuser);
			log.info("Created user " + updateuser.getName() + "(" + updateuser.getId() + ")");
		} catch (UsernameConflictException | EmailConflictException
				| FileConflictException e) {
			log.error(e, e);
		}
		
		NimbusUser deleteuser = new NimbusUser();
		deleteuser.setName("DeleteUser");
		deleteuser.setEmail("delete@cloudnimbus.org");
		deleteuser.setPasswordDigest("1000:934c26f1f0f8d03bcf4af7c3ab1fe1755a8e3d8c2e1ccdfc:7736a5ac51ccf0466c641a52e79cf1882cbfa4b674c45d52");
		deleteuser.setHmacKey("bfe547fd7658278c86e7d1d568d5d2ddb34712c8cd804d717a728d4d640b47ee");
		deleteuser.setAdministrator(false);
		try {
			userService.save(deleteuser);
			log.info("Created user " + deleteuser.getName() + "(" + deleteuser.getId() + ")");
		} catch (UsernameConflictException | EmailConflictException
				| FileConflictException e) {
			log.error(e, e);
		}
		
		log.info("Users created");
		
		//devDrives = new ArrayList<StorageDevice>();
		
		// Primary testing drive
		HardDrive primary = new HardDrive(null, "", "/media/nimbus-123456", "/dev/sda1", "NIMBUS01", "123456", "ntfs", 
				true, true, false, false, 500000000L, 100000L, new Date(), new Date());
		storageService.save(primary);
		//devDrives.add(primary);
		
		// Secondary testing storage device
		FilesystemLocation secondary = new FilesystemLocation(null, "FS Location", "/media/nimbus-fsloc", false, true, new Date(), new Date());
		storageService.save(secondary);
		//devDrives.add(secondary);
		
		// Unconnected drive
		HardDrive unconnected = new HardDrive(null, "", "/media/nimbus-unconnected", "/dev/sdc1", "Unconnected Drive", "unconnected", "ext4", 
				false, false, false, false, 1000000000L, 12000000L, new Date(), new Date());
		storageService.save(unconnected);
		//devDrives.add(unconnected);
		
		// Unmounted drive
		HardDrive unmounted = new HardDrive(null, "",  "/media/nimbus-unmounted", "/dev/sda2", "Unmounted Drive", "unmounted", "fat32", 
				true, false, false, false, 1024000000L, 5120000L, new Date(), new Date());
		storageService.save(unmounted);
		//devDrives.add(unmounted);
		
		// Assign Drives
		storageService.assignDriveToUser(primary, bryson);
		storageService.assignDriveToUser(secondary, bryson);
		storageService.assignDriveToUser(primary, createuser);
		storageService.assignDriveToUser(primary, updateuser);
		storageService.assignDriveToUser(primary, deleteuser);
		
		log.info("Drives created and assigned");
		
		// My share
		NimbusFile bHome = userService.getUserHomeFolder(bryson, primary);
		ShareBlock share = new ShareBlock();
		share.setUserId(bryson.getId());
		share.setName("My Share");
		share.setExternal(true);
		share.setExternalUploadAllowed(true);
		share.setMessage("This is a really long message. This is a really long message. This is a really long message. "
				+ "This is a really long message. This is a really long message. This is a really long message. This is a really long message. "
				+ "This is a really long message. This is a really long message. This is a really long message. This is a really long message. "
				+ "This is a really long message. This is a really long message. This is a really long message. This is a really long message. "
				+ "This is a really long message. Th");
		try {
			shareService.save(share);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}
		fileService.getContents(fileService.resolveRelativePath(bHome, "Music")); // reconcile
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(bHome, "Pictures"));
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(bHome, "Music/Pretty Lights"));
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(bHome, "Videos"));
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(bHome, "04 Little Black Submarines.mp3"));
		shareService.addFileToShareBlock(share, fileService.resolveRelativePath(bHome, "COPY (1).java"));
		
		// All access share
		NimbusFile cHome = userService.getUserHomeFolder(createuser, primary);
		ShareBlock allaccessshare = new ShareBlock();
		allaccessshare.setUserId(createuser.getId());
		allaccessshare.setName("All Access Share");
		try {
			shareService.save(allaccessshare);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}	
		shareService.addFileToShareBlock(allaccessshare, fileService.resolveRelativePath(cHome, "Music"));
		shareService.addFileToShareBlock(allaccessshare, fileService.resolveRelativePath(cHome, "05 Short Change Hero.mp3"));
		shareService.addFileToShareBlock(allaccessshare, fileService.resolveRelativePath(cHome, "09 Vibe Vendetta (GRiZ Remix).mp3"));
		ShareBlockAccess allaccess = shareService.getShareBlockAccess(allaccessshare, bryson);
		allaccess.setCanCreate(true);
		allaccess.setCanUpdate(true);
		allaccess.setCanDelete(true);
		shareService.save(allaccess);
		
		// Create access share
		ShareBlock createshare = new ShareBlock();
		createshare.setUserId(createuser.getId());
		createshare.setName("Create Access Share");
		try {
			shareService.save(createshare);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}
		shareService.addFileToShareBlock(createshare, fileService.resolveRelativePath(cHome, "Music"));
		shareService.addFileToShareBlock(createshare, fileService.resolveRelativePath(cHome, "05 Short Change Hero.mp3"));
		shareService.addFileToShareBlock(createshare, fileService.resolveRelativePath(cHome, "09 Vibe Vendetta (GRiZ Remix).mp3"));
		ShareBlockAccess createaccess = shareService.getShareBlockAccess(createshare, bryson);
		createaccess.setCanCreate(true);
		shareService.save(createaccess);
		
		// Update access share
		NimbusFile uHome = userService.getUserHomeFolder(updateuser, primary);
		ShareBlock updateshare = new ShareBlock();
		updateshare.setUserId(updateuser.getId());
		updateshare.setName("Update Access Share");
		try {
			shareService.save(updateshare);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}
		shareService.addFileToShareBlock(updateshare, fileService.resolveRelativePath(uHome, "Music"));
		shareService.addFileToShareBlock(updateshare, fileService.resolveRelativePath(uHome, "05 Short Change Hero.mp3"));
		shareService.addFileToShareBlock(updateshare, fileService.resolveRelativePath(uHome, "09 Vibe Vendetta (GRiZ Remix).mp3"));
		shareService.addFileToShareBlock(updateshare, fileService.resolveRelativePath(uHome, "COPY (1).java"));
		ShareBlockAccess updateaccess = shareService.getShareBlockAccess(updateshare, bryson);
		updateaccess.setCanUpdate(true);
		shareService.save(updateaccess);
		
		// Delete access share
		NimbusFile dHome = userService.getUserHomeFolder(deleteuser, primary);
		ShareBlock deleteshare = new ShareBlock();
		deleteshare.setUserId(deleteuser.getId());
		deleteshare.setName("Delete Access Share");
		try {
			shareService.save(deleteshare);
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
		}
		shareService.addFileToShareBlock(deleteshare, fileService.resolveRelativePath(dHome, "Music"));
		shareService.addFileToShareBlock(deleteshare, fileService.resolveRelativePath(dHome, "05 Short Change Hero.mp3"));
		shareService.addFileToShareBlock(deleteshare, fileService.resolveRelativePath(dHome, "09 Vibe Vendetta (GRiZ Remix).mp3"));
		ShareBlockAccess deleteaccess = shareService.getShareBlockAccess(deleteshare, bryson);
		deleteaccess.setCanDelete(true);
		shareService.save(deleteaccess);
		
		log.info("Share blocks created");
		log.info("Done.");
	}
}