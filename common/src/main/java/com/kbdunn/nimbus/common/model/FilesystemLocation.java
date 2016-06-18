package com.kbdunn.nimbus.common.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kbdunn.nimbus.common.util.ComparatorUtil;

@JsonIgnoreProperties({
	"type"
})
public class FilesystemLocation implements StorageDevice, NimbusRecord {
	
	public static final String TYPE = "FS";
	
	private Long id;
	private String name;
	private String path;
	private Boolean reconciled;
	private Boolean autonomous;
	private Date created;
	private Date updated;
	
	public FilesystemLocation() {
		autonomous = true; // default to true
	}
	
	public FilesystemLocation(Long id, String name, String path, Boolean reconciled, Boolean autonomous, Date createDate, Date lastUpdateDate) {
		this.id = id;
		this.name = name;
		this.path = path;
		this.reconciled = reconciled;
		this.autonomous = autonomous == null ? true : autonomous; // default to true
		this.created = createDate;
		this.updated = lastUpdateDate;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getPath() {
		return path;
	}
	
	@Override
	public void setPath(String path) {
		this.path = path;
	}
	
	@Override
	public boolean isReconciled() {
		return reconciled != null && reconciled;
	}
	
	@Override
	public void setReconciled(boolean isReconciled) {
		this.reconciled = isReconciled;
	}
	
	@Override
	public boolean isAutonomous() {
		return autonomous != null && autonomous;
	}
	
	@Override
	public void setAutonomous(boolean isAutonomous) {
		this.autonomous = isAutonomous;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	@Override
	public void setCreated(Date createDate) {
		this.created = createDate;
	}

	@Override
	public Date getUpdated() {
		return updated;
	}

	@Override
	public void setUpdated(Date lastUpdateDate) {
		this.updated = lastUpdateDate;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((reconciled == null) ? 0 : reconciled.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FilesystemLocation))
			return false;
		FilesystemLocation other = (FilesystemLocation) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (reconciled == null) {
			if (other.reconciled != null)
				return false;
		} else if (!reconciled.equals(other.reconciled))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FilesystemLocation [id=" + id + ", name=" + name + ", path="
				+ path + ", isReconciled=" + reconciled + ", isAutonomous="
				+ autonomous + "]";
	}

	@Override
	public int compareTo(StorageDevice o) {
		int i = 0;
		i = this.created.compareTo(o.getCreated());
		i = i == 0 ? ComparatorUtil.nullSafeStringComparator(this.name, o.getName()) : i;
		return i;
	}
}
