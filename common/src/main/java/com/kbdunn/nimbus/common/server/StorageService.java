package com.kbdunn.nimbus.common.server;

import java.util.List;

import com.kbdunn.nimbus.common.model.LinuxBlock;
import com.kbdunn.nimbus.common.model.MemoryInformation;
import com.kbdunn.nimbus.common.model.HardDrive;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;

public interface StorageService {

	HardDrive getHardDriveByUuid(String uuid);

	StorageDevice getStorageDeviceById(long driveId);

	StorageDevice getStorageDeviceByPath(String path);

	long getUsedBytes(StorageDevice device);

	boolean storageDeviceIsAvailable(StorageDevice device);

	boolean isValidNewFilesystemLocationPath(String path);

	HardDrive getDriveFromBlock(LinuxBlock block);

	List<StorageDevice> getAllStorageDevices();

	// Get all drives the user is allowed to use
	//List<StorageDevice> getAccessibleStorageDevices(NimbusUser user);

	// Get the drives that the user is currently using
	List<StorageDevice> getStorageDevicesAssignedToUser(NimbusUser user);
	
	List<NimbusUser> getUsersAssignedToStorageDevice(StorageDevice device);
	
	// Allow the user to store files an the drive. User Drive is not active until the user activates it.
	boolean assignDriveToUser(StorageDevice device, NimbusUser user);
	
	boolean revokeDriveFromUser(StorageDevice device, NimbusUser user);

	boolean setAssignedUserStorageDevices(NimbusUser user, List<StorageDevice> devices);

	boolean delete(StorageDevice device);

	boolean save(StorageDevice device);

	// Activate a Drive for a User that is allowed to access it.
	//boolean activateUserDrive(StorageDevice device, NimbusUser user);

	// Assigned to at least one user
	List<StorageDevice> getAssignedStorageDevices();

	// Connected, not necessarily mounted
	List<HardDrive> getConnectedHardDrives();

	// Connected and mounted
	List<HardDrive> getAvailableHardDrives();

	// Returns a list of drives that have been assigned and activated for at least one user
	//List<StorageDevice> getActivatedStorageDevices();

	void scanAndMountUSBHardDrives();

	void resetReconciliation();

	MemoryInformation getSystemMemoryInformation();

}