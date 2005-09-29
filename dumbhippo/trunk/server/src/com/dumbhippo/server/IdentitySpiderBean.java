package com.dumbhippo.server;

import org.hibernate.Query;
import org.hibernate.Session;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;
/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
public class IdentitySpiderBean implements IdentitySpider {
	
	private static final String BASE_LOOKUP_PERSON_EMAIL_QUERY = "select p from Person p, ResourceOwnershipClaim c where p.id = c.claimedOwner and c.resource = :email ";

	public Person lookupPersonByEmail(EmailResource email) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();		
		return (Person) hsession.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and c.assertedBy is null").setParameter("email", email).uniqueResult();
	}
	
	public Person lookupPersonByEmail(Person viewpoint, EmailResource email) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();		
		return (Person) hsession.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and (c.assertedBy.id = :viewpointguid or c.assertedBy.id is null)")
		.setString("viewpointguid", viewpoint.getId()).setParameter("email", email).uniqueResult();
	}
	
	public Person addPersonWithEmail(EmailResource email) {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		Session hsession = sess.getSession();		
		Person p = new Person();
		hsession.save(p);
		addOwnershipClaim(hsession, email, p);
		return p;
	}

	private void addOwnershipClaim(Session hsession, EmailResource email, Person p) {
		ResourceOwnershipClaim claim = new ResourceOwnershipClaim(p, email);
		hsession.save(claim);
	}
	
	public String getHumanReadableId(Person inviter) {
		// TODO look up full name too
		return getEmailAddress(inviter).getEmail();
	}
	
	private static final String BASE_LOOKUP_EMAIL_QUERY 
		= "select e from EmailResource e, ResourceOwnershipClaim c where e.id = c.resource and c.claimedOwner = :personid and (c.assertedBy.id is null ";	

	public EmailResource getEmailAddress(Person viewpoint, Person p) {
		EmailResource res;
		Session hsession = Storage.getGlobalPerThreadSession().getSession();
		Query q;
		if (viewpoint == null) {
			q = hsession.createQuery(BASE_LOOKUP_EMAIL_QUERY + ")");
		} else {
			q = hsession.createQuery(BASE_LOOKUP_EMAIL_QUERY + "or c.assertedBy.id = :viewpointguid)").setString("viewpointguid", viewpoint.getId());
		}
		q.setParameter("personid", p.getId());
		res = (EmailResource) q.uniqueResult();

		return res;
	}

	public EmailResource getEmailAddress(Person p) {
		return getEmailAddress(null, p);
	}

	public EmailResource getEmail(String email) {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		Session hsession = sess.getSession();
		
		Query q;
		
		q = hsession.createQuery("from EmailResource e where e.email = :email");
		q.setParameter("email", email);
		
		EmailResource res = (EmailResource) q.uniqueResult();
		if (res == null) {
			res = new EmailResource(email);
			hsession.save(res);
		}
		
		return res;	
	}
	
	private static final String theManGuid = "8716baa63bef600797fbc59e06010000a35ad1637e6a7f87";
	private static final String theManEmail = "theman@dumbhippo.com";

	public Person getTheMan() {
		SessionWrapper sess = Storage.getGlobalPerThreadSession();
		Session hsession = sess.getSession();
		Person ret = (Person) hsession.createQuery("from Person p where p.id = :id").setParameter("id", theManGuid).uniqueResult();
		if (ret == null) {
			EmailResource res = getEmail(theManEmail);
			ret = new Person(new Guid(theManGuid));
			addOwnershipClaim(hsession, res, ret);
		}
		return ret;
	}
}

