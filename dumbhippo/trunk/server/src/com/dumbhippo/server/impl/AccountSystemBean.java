package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;

@Stateless
public class AccountSystemBean implements AccountSystem {
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;

	public HippoAccount createAccountFromResource(Resource res) {
		Person person = new Person();
		em.persist(person);
		spider.addVerifiedOwnershipClaim(person, res);
		HippoAccount account = new HippoAccount(person);
		em.persist(account);
		return account;
	}

	public HippoAccount createAccountFromEmail(String email) {
		Resource res = spider.getEmail(email);
		return createAccountFromResource(res);
	}

	public Client authorizeNewClient(HippoAccount acct, String name) {
		Client c = new Client(name);
		em.persist(c);
		acct.authorizeNewClient(c);
		return c;
	}
	
	public boolean checkClientCookie(Person user, String authKey) {
		HippoAccount account = lookupAccountByPerson(user);
		return account.checkClientCookie(authKey);
	}

	public HippoAccount lookupAccountByPerson(Person person) {
		HippoAccount ret;
		try {
			ret = (HippoAccount) em.createQuery("from HippoAccount a where a.owner = :person").setParameter("person", person).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public HippoAccount lookupAccountByPersonId(String personId) {
		HippoAccount ret;
		try {
			ret = (HippoAccount) em.createQuery("from HippoAccount a where a.owner.id = :id").setParameter("id", personId).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public long getNumberOfActiveAccounts() {
		long count = (Long) em.createQuery("SELECT SIZE(*) FROM HippoAccount a").getSingleResult();
		return count;
	}

	public Set<HippoAccount> getActiveAccounts() {
		Query q = em.createQuery("FROM HippoAccount");
		
		Set<HippoAccount> accounts = new HashSet<HippoAccount>();
		List list = q.getResultList();
		
		for (Object o : list) {
			accounts.add((HippoAccount) o);
		}
		
		return accounts;
	}
}
