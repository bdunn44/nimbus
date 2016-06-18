package com.kbdunn.nimbus.common.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kbdunn.nimbus.common.util.ComparatorUtil;

@JsonIgnoreProperties({
	"usedPercentage"
	,"available"
})
public class HardDrive implements StorageDevice, NimbusRecord {
	
	private Long id;
	private String name;
	private String path;
	private String devicePath;
	private String label;
	private String uuid;
	private Boolean connected;
	private Boolean mounted;
	private Boolean reconciled;
	private Boolean autonomous;
	private String type;
	private Long size;
	private Long used;
	private Date created;
	private Date updated;
	
	public HardDrive() { 
		autonomous = false; // default to false
	}
	
	public HardDrive(Long id, String name, String path, String devicePath, String label, String uuid, String type, Boolean connected, 
			Boolean mounted, Boolean reconciled, Boolean autonomous, Long size, Long used, Date createDate, Date lastUpdateDate) {
		this.id = id;
		this.name = name;
		this.path = path;
		this.devicePath = devicePath;
		this.label = label;
		this.uuid = uuid;
		this.type = type;
		this.connected = connected;
		this.mounted = mounted;
		this.reconciled = reconciled;
		this.autonomous = autonomous == null ? false : autonomous; // default to false
		this.size = size;
		this.used = used;
		this.created = createDate;
		this.updated = lastUpdateDate;
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
		return name == null || name.isEmpty() ? label : name;
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
	
	public String getDevicePath() {
		return devicePath;
	}
	
	public void setDevicePath(String devicePath) {
		this.devicePath = devicePath;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public boolean isConnected() {
		return connected != null && connected;
	}
	
	public void setConnected(Boolean isConnected) {
		this.connected = isConnected;
	}
	
	public boolean isMounted() {
		return mounted != null && mounted;
	}
	
	public void setMounted(Boolean isMounted) {
		this.mounted = isMounted;
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
	public String getType() {
		return type == null ? "unknown" : type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public Long getSize() {
		return size;
	}
	
	public void setSize(Long size) {
		this.size = size;
	}

	public Long getUsed() {
		return used;
	}
	
	public void setUsed(Long used) {
		this.used = used;
	}
	
	public Long getAvailable() {
		return size - used;
	}

	public Double getUsedPercentage() {
		return (double) (used / size * 100);
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
		result = prime * result
				+ ((devicePath == null) ? 0 : devicePath.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((connected == null) ? 0 : connected.hashCode());
		result = prime * result
				+ ((mounted == null) ? 0 : mounted.hashCode());
		result = prime * result
				+ ((reconciled == null) ? 0 : reconciled.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((size == null) ? 0 : size.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((used == null) ? 0 : used.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HardDrive))
			return false;
		HardDrive other = (HardDrive) obj;
		if (devicePath == null) {
			if (other.devicePath != null)
				return false;
		} else if (!devicePath.equals(other.devicePath))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (connected == null) {
			if (other.connected != null)
				return false;
		} else if (!connected.equals(other.connected))
			return false;
		if (mounted == null) {
			if (other.mounted != null)
				return false;
		} else if (!mounted.equals(other.mounted))
			return false;
		if (reconciled == null) {
			if (other.reconciled != null)
				return false;
		} else if (!reconciled.equals(other.reconciled))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
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
		if (size == null) {
			if (other.size != null)
				return false;
		} else if (!size.equals(other.size))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (used == null) {
			if (other.used != null)
				return false;
		} else if (!used.equals(other.used))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HardDrive [id=" + id + ", name=" + name + ", path=" + path
				+ ", devicePath=" + devicePath + ", label=" + label + ", uuid="
				+ uuid + ", connected=" + connected + ", mounted="
				+ mounted + ", reconciled=" + reconciled
				+ ", autonomous=" + autonomous + ", type=" + type
				+ ", size=" + size + ", used=" + used + "]";
	}
	
	@Override
	public int compareTo(StorageDevice o) {
		int i = 0;
		i = this.created.compareTo(o.getCreated());
		i = i == 0 ? ComparatorUtil.nullSafeStringComparator(this.name, o.getName()) : i;
		return i;
	}
}