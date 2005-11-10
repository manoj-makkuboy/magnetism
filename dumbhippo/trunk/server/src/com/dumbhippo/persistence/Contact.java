package com.dumbhippo.persistence;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.dumbhippo.persistence.Person;

/**
 * Contact represents an entry in the list of contacts for an
 * account.
 * 
 * @author otaylor
 */
@Entity
public class Contact extends Person {
	private static final long serialVersionUID = 1L;
	
	private Account account;
	private Set<ContactClaim> resources;
	
	protected Contact() {}
	
	public Contact(Account account) {
		this.account = account;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Account getAccount() {
		return account;
	}
	
	protected void setAccount(Account account) {
		this.account = account;
	}
	
	// The main reason for the inverse mapping is so that we can
	// cascade removal to resources if we remove a contact.
	
	@OneToMany(cascade={ CascadeType.REMOVE }, mappedBy="contact")
	public Set<ContactClaim> getResources() {
		return resources;
	}
	
	public void setResources(Set<ContactClaim> resources) {
		this.resources = resources;
	}
}
