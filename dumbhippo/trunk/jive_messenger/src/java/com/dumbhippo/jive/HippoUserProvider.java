package com.dumbhippo.jive;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.messenger.user.User;
import org.jivesoftware.messenger.user.UserAlreadyExistsException;
import org.jivesoftware.messenger.user.UserNotFoundException;
import org.jivesoftware.messenger.user.UserProvider;
import org.jivesoftware.util.Log;

import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.MessengerGlueRemote.JabberUser;

public class HippoUserProvider implements UserProvider {

	static final public boolean ENABLE_ADMIN_USER = true;
	static final public String ADMIN_USERNAME = "admin";
	
	private UserNotFoundException createUserNotFound(String username, Exception root) {
		return new UserNotFoundException ("No account has username '" + username + "'", root);
	}
	
	public User loadUser(String username) throws UserNotFoundException {
		
		Log.debug("loadUser() username = " + username);
		
		MessengerGlueRemote glue = Server.getMessengerGlue();
	
		if (ENABLE_ADMIN_USER) {
			if (username.equals(ADMIN_USERNAME)) {
				return new User(ADMIN_USERNAME, "Administrator", null, null, null);
			}
		}
		
		JabberUser remote = null;
		try {
			remote = glue.loadUser(username);
		} catch (JabberUserNotFoundException e) {
			throw createUserNotFound(username, e);
		}

		Log.debug("loaded user " + remote);
		
		return new User(username, remote.getName(), remote.getEmail(), null, null);
	}

	public User createUser(String username, String password, String name,
			String email) throws UserAlreadyExistsException {
		
		Log.debug("createUser() username = " + username);
		
		throw new UnsupportedOperationException("Users must be created on the web site");
	}

	public void deleteUser(String username) {
		
		Log.debug("deleteUser() username = " + username);
		
		throw new UnsupportedOperationException("Users must be deleted on the web site");
	}

	public int getUserCount() {
		
		Log.debug("getUserCount()");
		
		MessengerGlueRemote glue = Server.getMessengerGlue();
	
		long result = glue.getJabberUserCount();
		// Is there such a thing as optimistic paranoia?
		if (result > Integer.MAX_VALUE)
			throw new Error("Too many users for JiveMessenger's mind!");
		
		if (ENABLE_ADMIN_USER)
			result = result + 1;
	
		Log.debug(" count is " + result);
		
		return (int) result;
	}

	public Collection<User> getUsers() {
		
		Log.debug("getUsers()");
		
		// Whatever is calling this should be replaced by a call to 
		// our server which does a database query or something instead...
		throw new UnsupportedOperationException("Bug! getUsers() was called; we can't implement this, there are too many, so the caller will need to be changed");
	}

	public Collection<User> getUsers(int startIndex, int numResults) {
		
		Log.debug("incremental getUsers()");
		
		// At the moment, this function is never used in the JiveMessenger source.
		throw new UnsupportedOperationException("Bug! Incremental getUsers() called; but it's not implemented yet");
	}

	public String getPassword(String username) throws UserNotFoundException,
			UnsupportedOperationException {
		
		Log.debug("getPassword()");
		
		throw new UnsupportedOperationException("Can't get password, have to use digest");
	}

	public void setPassword(String username, String password)
			throws UserNotFoundException, UnsupportedOperationException {
		
		Log.debug("setPassword()");
		
		throw new UnsupportedOperationException("You have to set your password on the web site");
	}

	public void setName(String username, String name)
			throws UserNotFoundException {
		
		Log.debug("setName() username = " + username + " name = " + name);
		
		if (ENABLE_ADMIN_USER) {
			if (username.equals(ADMIN_USERNAME))
				return;
		}
		
		MessengerGlueRemote glue = Server.getMessengerGlue();
		try {
			glue.setName(username, name);
		} catch (JabberUserNotFoundException e) {
			throw createUserNotFound(username, e);
		}
	}

	public void setEmail(String username, String email)
			throws UserNotFoundException {
		
		Log.debug("setEmail() username = " + username + " email = " + email);
		
		if (ENABLE_ADMIN_USER) {
			if (username.equals(ADMIN_USERNAME))
				return;
		}
		
		MessengerGlueRemote glue = Server.getMessengerGlue();
		try {
			glue.setEmail(username, email);
		} catch (JabberUserNotFoundException e) {
			throw createUserNotFound(username, e);
		}
	}

	public void setCreationDate(String username, Date creationDate)
			throws UserNotFoundException {
		
		Log.debug("setCreationDate() username = " + username);
		
		throw new UnsupportedOperationException();
	}

	public void setModificationDate(String username, Date modificationDate)
			throws UserNotFoundException {
		
		Log.debug("setModificationDate() username = " + username);
		
		throw new UnsupportedOperationException();

	}

	public Set<String> getSearchFields() throws UnsupportedOperationException {
		
		Log.debug("getSearchFields()");
		
		// TODO should probably implement this sometime
		throw new UnsupportedOperationException();
	}

	public Collection<User> findUsers(Set<String> fields, String query)
			throws UnsupportedOperationException {
		
		Log.debug("findUsers()");
		
		//		 TODO should probably implement this sometime
		throw new UnsupportedOperationException();
	}

	public Collection<User> findUsers(Set<String> fields, String query,
			int startIndex, int numResults)
			throws UnsupportedOperationException {
		
		Log.debug("incremental findUsers()");
		
		//		 TODO should probably implement this sometime
		throw new UnsupportedOperationException();
	}

	public boolean isReadOnly() {
		
		Log.debug("isReadOnly()");
		
		// We don't support a lot of the modification operations, so maybe we should 
		// set this to true. But we do support a couple of them, so...
		return false;
	}
}
