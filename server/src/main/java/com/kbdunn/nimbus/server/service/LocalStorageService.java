package com.kbdunn.nimbus.server.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.LinuxBlock;
import com.kbdunn.nimbus.common.model.MemoryInformation;
import com.kbdunn.nimbus.common.model.HardDrive;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.dao.StorageDAO;
import com.kbdunn.nimbus.server.linux.BlockFinder;
import com.kbdunn.nimbus.server.linux.CmdDf;
import com.kbdunn.nimbus.server.linux.CmdMemInfo;
import com.kbdunn.nimbus.server.linux.CmdPmount;
import com.kbdunn.nimbus.server.linux.CmdPumount;
import com.kbdunn.nimbus.server.linux.CmdDf.Filesystem;

public class LocalStorageService implements StorageService {
	
	private static final Logger log = LogManager.getLogger(LocalStorageService.class.getName());
	
	private LocalFileService fileService;
	private LocalUserService userService;
	private LocalPropertiesService propertiesService;
	
	public LocalStorageService() {  }
	
	public void initialize(NimbusContext container) {
		fileService = container.getFileService();
		userService = container.getUserService();
		propertiesService = container.getPropertiesService();
	}
	
	@Override
	public HardDrive getHardDriveByUuid(String uuid) {
		if (uuid == null) throw new NullPointerException("UUID cannot be null");
		return StorageDAO.getHardDriveByUuid(uuid);
	}
	
	@Override
	public StorageDevice getStorageDeviceById(long driveId) {
		return StorageDAO.getById(driveId);
	}
	
	@Override
	public StorageDevice getStorageDeviceByPath(String path) {
		if (path == null) throw new NullPointerException("Path cannot be null");
		return StorageDAO.getByPath(path);
	}
	
	@Override
	public long getUsedBytes(StorageDevice device) {
		if (device.getId() == null) throw new NullPointerException("Drive ID cannot be null");
		return StorageDAO.getUsedBytes(device.getId());
	}
	
	@Override
	public boolean storageDeviceIsAvailable(StorageDevice device) {
		// Hard Drives must be connected and mounted
		if (device instanceof HardDrive && 
				!(((HardDrive) device).isConnected() && ((HardDrive) device).isMounted())) {
			return false;
		}
		if (device.getPath() == null) return false;
		return pathIsReadable(device.getPath());
	}
	
	@Override
	public boolean isValidNewFilesystemLocationPath(String path) {
		if (path == null) throw new NullPointerException("Path cannot be null");
		for (StorageDevice d : StorageDAO.getAll()) {
			String dp = d.getPath();
			if (dp != null && (path.equals(dp) || path.startsWith(dp))) 
				return false;
		}
		return pathIsWritable(path);
	}
	
	private boolean pathIsReadable(String path) {
		if (path == null) throw new NullPointerException("Path cannot be null");
		return Files.isReadable(Paths.get(path));
	}
	
	private boolean pathIsWritable(String path) {
		if (path == null) throw new NullPointerException("Path cannot be null");
		return Files.isWritable(Paths.get(path));
	}
	
	@Override
	public HardDrive getDriveFromBlock(LinuxBlock block) {
		HardDrive d = StorageDAO.getHardDriveByUuid(block.getUuid());
		if (d == null) {
			d = new HardDrive();
		}
		d.setDevicePath(block.getPath());
		d.setLabel(block.getLabel());
		d.setUuid(block.getUuid());
		d.setType(block.getType());
		return d;
	}
	
	@Override
	public List<StorageDevice> getAllStorageDevices() {
		return StorageDAO.getAll();
	}
	
	// Get all drives the user is allowed to use
	/*@Override
	public List<StorageDevice> getAccessibleStorageDevices(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		if (user.isOwner() || user.isAdministrator()) return getAllStorageDevices();
		return StorageDAO.getUserDrives(user.getId());
	}*/
	
	// Get all connected & mounted drives the user is allowed to use
	/*public List<HardDrive> getAvailableUserDrives(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return DriveDAO.getAvailableUserDrives(user.getId());
	}*/
	
	// Get the drives that the user is currently using
	@Override
	public List<StorageDevice> getStorageDevicesAssignedToUser(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return StorageDAO.getStorageDevicesAssignedToUser(user.getId());
	}
	
	@Override
	public StorageDevice getSyncRootStorageDevice(NimbusUser user) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		return StorageDAO.getSyncRootDevice(user.getId());
	}
	
	@Override
	public void setSyncRootStorageDevice(NimbusUser user, StorageDevice device) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		// Device can be set to null, but if it's not-null it should have an ID
		if (device != null && device.getId() == null) throw new NullPointerException("Storage Device ID cannot be null");
		final StorageDevice oldRoot = getSyncRootStorageDevice(user);
		if (device == null || !device.equals(oldRoot)) {
			NimbusContext.instance().getFileSyncService().publishRootChangeEvent(user, oldRoot, device);
			StorageDAO.setSyncRootDevice(user.getId(), device == null ? null : device.getId());
			if (device != null) StorageDAO.resetReconciliation(device.getId());
		}
	}
	
	@Override
	public boolean delete(StorageDevice device) {
		if (device.getId() == null) throw new NullPointerException("Device ID cannot be null");
		return StorageDAO.delete(device.getId());
	}
	
	@Override
	public boolean save(StorageDevice device) {
		if (device.getId() == null) {
			if (!StorageDAO.insert(device)) return false;
			StorageDevice dbo = device instanceof HardDrive ? 
					StorageDAO.getByDevicePath(((HardDrive) device).getDevicePath()) :
					StorageDAO.getByPath(device.getPath());
			device.setId(dbo.getId());
			device.setUpdated(dbo.getUpdated());
			device.setCreated(dbo.getCreated());
		} else {
			if (!StorageDAO.update(device)) return false;
			device.setUpdated(new Date()); // close enough
		}
		return true;
	}
	
	// Allow the user to store files an the drive. User Drive is not active until the user activates it.
	@Override
	public boolean assignDriveToUser(StorageDevice device, NimbusUser user) {
		if (device.getId() == null) throw new NullPointerException("Drive ID cannot be null");
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		
		if (StorageDAO.getUserDrive(device.getId(), user.getId()) == null) {
			log.info("Assigning user " + user.getName() + " to Drive " + device.getPath());
			if (!StorageDAO.insertUserDrive(user.getId(), device.getId())) return false;
			if (!activateUserDrive(device, user)) return false;
		} 
		
		return true;
	}
	
	@Override
	public boolean setAssignedUserStorageDevices(NimbusUser user, List<StorageDevice> devices) {
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		if (!StorageDAO.setUserDrives(user.getId(), devices)) return false;
		
		for (StorageDevice device : devices) {
			if (!activateUserDrive(device, user)) return false;
		}
		return true;
	}
	
	// Activate a Drive for a User that is allowed to access it.
	private boolean activateUserDrive(StorageDevice device, NimbusUser user) {
		if (device.getId() == null) throw new NullPointerException("Drive ID cannot be null");
		if (device.getPath() == null) throw new NullPointerException("Drive path cannot be null");
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		if (user.getName() == null) throw new NullPointerException("User name cannot be null");
		
		if (StorageDAO.getUserDrive(device.getId(), user.getId()) == null) { // user hasn't been assigned the drive
			if (user.isOwner() || user.isAdministrator()) { // ...which is okay if they're admin
				if (!assignDriveToUser(device, user)) return false;
			} else {
				throw new IllegalArgumentException("User does not have permission to use this Drive");
			}
		}
		return createUserDirectories(device, user);
	}
	
	private boolean createUserDirectories(StorageDevice device, NimbusUser user) {
		// Create user directories if they don't exist
		NimbusFile userRoot = fileService.getFileByPath(device.getPath() + "/NimbusUser-" + user.getName());
		if (!fileService.createDirectory(userRoot)) return false;
		NimbusFile userHome = fileService.getFileByPath(userRoot.getPath() + "/Home");
		if (!fileService.createDirectory(userHome)) return false;
		if (!fileService.createDirectory(
				fileService.getFileByPath(userHome.getPath() + "/Documents"))
				) return false;
		if (!fileService.createDirectory(
				fileService.getFileByPath(userHome.getPath() + "/Music"))
				) return false;
		if (!fileService.createDirectory(
				fileService.getFileByPath(userHome.getPath() + "/Videos"))
				) return false;
		if (!fileService.createDirectory(
				fileService.getFileByPath(userHome.getPath() + "/Pictures"))
				) return false;
		
		fileService.reconcileFolder(userHome);
		return true;
	}
	
	@Override
	public boolean revokeDriveFromUser(StorageDevice device, NimbusUser user) {
		if (device.getId() == null) throw new NullPointerException("Drive ID cannot be null");
		if (user.getId() == null) throw new NullPointerException("User ID cannot be null");
		
		if (StorageDAO.getUserDrive(device.getId(), user.getId()) != null) {
			return StorageDAO.deleteUserDrive(user.getId(), device.getId());
		} else {
			return true;
		}
	}
	
	@Override
	public List<NimbusUser> getUsersAssignedToStorageDevice(StorageDevice device) {
		if (device.getId() == null) throw new NullPointerException("Storage device ID cannot be null");
		return StorageDAO.getUsersAssignedToDevice(device.getId());
	}
	
	// Assigned to at least one user, not necessarily activated by any users
	@Override
	public List<StorageDevice> getAssignedStorageDevices() {
		return StorageDAO.getAssigned();
	}
	
	// Connected, not necessarily mounted
	@Override
	public List<HardDrive> getConnectedHardDrives() {
		return StorageDAO.getConnectedHardDrives();
	}
	
	// Connected and mounted
	@Override
	public List<HardDrive> getAvailableHardDrives() {
		return StorageDAO.getAvailable();
	}
	
	// Returns a list of drives that have been assigned and activated for at least one user
	/*@Override
	public List<StorageDevice> getActivatedStorageDevices() {
		return StorageDAO.getActiveUserStorageDevices();
	}*/
	
	@Override
	public void scanAndMountUSBHardDrives() {
		if (propertiesService.isDevMode() || propertiesService.isDemoMode()) {
			return;
		}
		if (!new LocalPropertiesService().isAutoScan()) {
			log.debug("Auto scan is set to false. Skipping automatic drive scan/mount.");
			return;
		}
		log.info("Scanning system for connected USB hard drives...");
		
		// Get all connected devices
		List<HardDrive> connected = new ArrayList<HardDrive>();
		for (LinuxBlock b : new BlockFinder().scan())  {
			if (b.getPath() != null && b.getPath().startsWith("/dev/sd") && (b.getType() == null || !b.getType().equals("swap"))) {
				log.debug("Device scanner found device: " 
						+ b.getPath() + " - " 
						+ b.getLabel() + " (" 
						+ b.getType() + " " 
						+ b.getUuid() + ")");
				HardDrive d = getDriveFromBlock(b);
				d.setConnected(true);
				connected.add(d);
			}
		}
		log.info("Found " + connected.size() + " connected drive(s)");
		
		// Unmount unconnected devices
		log.info("Finding mounted drives which are no longer connected...");
		for (Filesystem f : CmdDf.execute()) {
			if (f.getMountedPath().startsWith("/media/nimbus-")) {
				boolean c = false;
				for (HardDrive d : connected) {
					if (d.getDevicePath().equals(f.getDevicePath())) {
						c = true;
						d.setMounted(true);
						//mounted.add(d);
						break;
					}
				}
				if (!c) {
					log.info("Drive previously mounted to " + f.getDevicePath() + " was not found. Un-mounting drive.");
					CmdPumount.unmount(f.getDevicePath());
				}
			}
		}
		
		// Mount all connected but unmounted devices
		log.info("Attempting to mount all un-mounted drives...");
		for (HardDrive d : connected) {
			if (!d.isMounted()) {
				log.debug("Mounting " + d.getDevicePath());
				if (!CmdPmount.mount(d)) {
					log.warn("Detected error while mounting " + d.getDevicePath());
				}
				d.setReconciled(false);
			}
		}
		
		// Get drive stats and verify everything is mounted
		List<Filesystem> filesystems = CmdDf.execute();
		for (HardDrive d : connected) {
			Filesystem f = null;
			for (Filesystem ff : filesystems) {
				if (d.getDevicePath().equals(ff.getDevicePath())) {
					f = ff;
					break;
				}
			}
			
			if (f == null) {
				log.error("Drive " + d.getPath() + " failed to mount");
				d.setMounted(false);
			} else {
				d.setMounted(true);
				d.setPath(f.getMountedPath());
				d.setSize(f.getSize());
				d.setUsed(f.getUsed());
				d.setType(f.getFilesystemType());
				log.info("Drive " + d.getDevicePath() + " is mounted");
				log.debug("Drive's mounted path is: " + d.getPath());
			}
		}
		log.info("Filesystem statistics retrieved.");
		
		// Update file metadata, Delta detect connected drives, update DB
		log.info("Updating database and validating user directories...");
		for (HardDrive d : connected) {
			log.debug("Retrieving drive " + d.getDevicePath() + " from DB");
			HardDrive dbdrive = (HardDrive) StorageDAO.getByDevicePath(d.getDevicePath());
			if (dbdrive == null) {
				log.debug("Drive not found. Inserting record.");
				save(d);
			} else {
				log.debug("Drive found. Updating record.");
				d.setId(dbdrive.getId());
				save(d);
				
				if (storageDeviceIsAvailable(d)) {
					log.debug("Checking required directories for users assigned to drive");
					for (NimbusUser u : StorageDAO.getUsersAssignedToDevice(d.getId())) {
						NimbusFile uHome = userService.getUserRootFolder(u, d);
						log.debug("User " + u.getName() + " home directory is: " + uHome.getPath());					
						if (!fileService.fileExistsOnDisk(uHome)) {
							log.warn("Home directory for user " + u.getName() + " doesn't exist! Re-creating user directories.");
							createUserDirectories(d, u); 
						}
					}
				}
			}
		}
		
		// Delta detect DB drives, delete old drives that aren't connected
		log.debug("Updating the DB for all unconnected drives");
		for (StorageDevice d : StorageDAO.getAll()) {
			if (d instanceof HardDrive) {
				boolean c = false;
				for (HardDrive cd : connected) {
					if (cd.getDevicePath().equals(((HardDrive)d).getDevicePath())) {
						c = true;
						break;
					}
				}
				if (c) continue;
				
				HardDrive hd = (HardDrive) d;
				log.debug("Unconnected drive " + hd.getDevicePath() + " updated in DB");
				hd.setConnected(false);
				hd.setMounted(false);
				hd.setReconciled(false);
				StorageDAO.update(hd);
			}
		}
		
		log.info("Done.");
	}
	
	@Override
	public void resetReconciliation(StorageDevice device) {
		if (device.getId() == null) throw new NullPointerException("Storage Device ID cannot be null");
		StorageDAO.resetReconciliation(device.getId());
	}
	
	@Override
	public void resetReconciliation() {
		StorageDAO.resetReconciliation();
	}
	
	// TODO: Move to a system service, not here
	@Override
	public MemoryInformation getSystemMemoryInformation() {
		if (propertiesService.isDevMode()) return new MemoryInformation(50L*1014*1024*1024, 100L*1014*1024*1024);
		return CmdMemInfo.execute();
	}
}