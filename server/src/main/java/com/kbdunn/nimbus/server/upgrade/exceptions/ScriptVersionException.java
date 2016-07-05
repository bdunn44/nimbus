package com.kbdunn.nimbus.server.upgrade.exceptions;

public class ScriptVersionException extends RuntimeException {

	private static final long serialVersionUID = 2979975752666602929L;
	
	public ScriptVersionException(int minVersionRequired) {
		super("This upgrade requires at least version " + minVersionRequired + " of the upgrade.sh script. "
				+ "Please download the latest version from http://cloudnimbus.org/dist/latest/upgrade.sh and try again.");
	}
}
