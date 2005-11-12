package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.FullName;
import com.dumbhippo.identity20.Guid;


@Entity
public class Person extends GuidPersistable {

	private static final long serialVersionUID = 0L;

	private FullName name;
	private String nickname;
	
	protected Person() { 
		super();
		name = FullName.parseDatabaseString("");
	}

	protected Person(Guid guid) {
		super(guid);
		name = FullName.parseDatabaseString("");
	}

	@Override
	public String toString() {
		return "{Person " + "guid = " + getId() + " name = " + name + "}";
	}
	
	/**
	 * The database stores names in encoded form, so the 
	 * full name of a person is just a single string. 
	 * This accessor returns that encoded form and is
	 * used for persistence.
	 * @return the database-encoded name.
	 */
	public String getEncodedName() {
		if (name != null)
			return name.getDatabaseString();
		return null;
	}
	
	public void setEncodedName(String text) {
		name = FullName.parseDatabaseString(text);
	}
	
	@Transient
	public FullName getName() {
		// because FullName is immutable, no need to copy it here
		return name;
	}
	
	@Transient
	public void setName(FullName name) {
		// because FullName is immutable, no need to copy it here
		this.name = name;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
