package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.User;

@Local
public interface MySpaceTracker {
	
	public void updateFriendId(User user);
	
	public void setFriendId(Account acct, String friendId);
	
	public void addMySpaceBlogComment(User user, long commentId, long posterId);
	
	public List<MySpaceBlogComment> getRecentComments(User user);

	public void notifyNewContactComment(UserViewpoint viewpoint, String mySpaceContactName);
}
