package com.kbdunn.nimbus.common.model;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import org.apache.commons.io.FileUtils;

public class NimbusVersion implements Comparator<NimbusVersion>, Comparable<NimbusVersion> {
	
	private final int major, minor, dot, build;

	public NimbusVersion(int major, int minor, int dot) {
		this(major, minor, dot, -1);
	}
	
	public NimbusVersion(int major, int minor, int dot, int build) {
		this.major = major;
		this.minor = minor;
		this.dot = dot;
		this.build = build;
	}
	
	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getDot() {
		return dot;
	}

	public int getBuild() {
		return build;
	}
	
	public static NimbusVersion fromString(String version) throws NumberFormatException {
		String[] split = version.trim().split("\\.");
		int major = Integer.valueOf(split[0]);
		int minor = Integer.valueOf(split[1]);
		int dot = Integer.valueOf(split[2]);
		int build = split.length > 3 ? Integer.valueOf(split[3]) : -1;
		return new NimbusVersion(major, minor, dot, build);
	}
	
	public static NimbusVersion fromNimbusInstallation(File nimbusHome) throws IllegalArgumentException, NumberFormatException, IOException {
		File versionFile = null;
		if (nimbusHome == null || !nimbusHome.isDirectory() || !(versionFile = new File(nimbusHome, "logs/version.txt")).isFile()) {
			throw new IllegalArgumentException("Invalid Nimbus Home directory (" + nimbusHome + ")");
		}
		return NimbusVersion.fromString(FileUtils.readFileToString(versionFile));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + build;
		result = prime * result + dot;
		result = prime * result + major;
		result = prime * result + minor;
		return result;
	}

	public boolean equalsIgnoreBuild(NimbusVersion v2) {
		if (this == v2) 
			return true;
		if (v2 == null) 
			return false;
		return this.getMajor() == v2.getMajor()
				&& this.getMinor() == v2.getMinor()
				&& this.getDot() == v2.getDot();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NimbusVersion))
			return false;
		NimbusVersion other = (NimbusVersion) obj;
		if (build != other.build)
			return false;
		if (dot != other.dot)
			return false;
		if (major != other.major)
			return false;
		if (minor != other.minor)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return + major + "." + minor + "." + dot + "." + build;
	}
	
	public String toDotString() {
		return + major + "." + minor + "." + dot;
	}

	@Override
	public int compare(NimbusVersion o1, NimbusVersion o2) {
		int r = 0;
		r = o1.getMajor() > o2.getMajor() ? 1 : o1.getMajor() < o2.getMajor() ? -1 : 0;
		r = r == 0 ? (o1.getMinor() > o2.getMinor() ? 1 : o1.getMinor() < o2.getMinor() ? -1 : 0) : r;
		r = r == 0 ? (o1.getDot() > o2.getDot() ? 1 : o1.getDot() < o2.getDot() ? -1 : 0) : r;
		r = r == 0 ? (o1.getBuild() > o2.getBuild() ? 1 : o1.getBuild() < o2.getBuild() ? -1 : 0) : r;
		return r;
	}

	@Override
	public int compareTo(NimbusVersion o) {
		return compare(this, o);
	}
}
