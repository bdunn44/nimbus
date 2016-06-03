package com.kbdunn.nimbus.common.server;

import java.util.List;

import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.SMTPSettings;
import com.kbdunn.nimbus.common.model.StorageDevice;

public interface UserService {

	Integer getUserCount();

	List<NimbusUser> getAllUsers();

	NimbusUser getUserById(Long id);

	NimbusUser getUserByNameOrEmail(String name);
	
	// TODO: Implement all FileContainer methods?
	NimbusFile resolveRelativePath(NimbusUser user, StorageDevice device, String relativePath);

	SMTPSettings getSmtpSettings(NimbusUser user);

	void updateSmtpSettings(SMTPSettings smtpSettings);

	String getUserRootFolderPath(NimbusUser user, StorageDevice device);

	String getUserHomeFolderPath(NimbusUser user, StorageDevice device);

	NimbusFile getUserRootFolder(NimbusUser user, StorageDevice device);

	NimbusFile getUserHomeFolder(NimbusUser user, StorageDevice device);

	String getTempFolderPath(NimbusUser user, StorageDevice device);

	NimbusFile getTempFolder(NimbusUser user, StorageDevice device);

	List<NimbusFile> getAllTempFolders(NimbusUser user);

	Boolean hasDuplicateEmail(NimbusUser user);

	Boolean hasDuplicateName(NimbusUser user);

	String generateTemporaryPassword();
	
	void resetApiToken(NimbusUser user) throws UsernameConflictException, EmailConflictException, FileConflictException;

	String getDigestedPassword(String clearTextPassword);
	
	boolean validatePassword(String clearTextPassword, String passwordDigest);

	boolean save(NimbusUser user) throws UsernameConflictException, EmailConflictException, FileConflictException;

	boolean delete(NimbusUser user);
	
	EmailTransport getEmailTransport(NimbusUser user);

}