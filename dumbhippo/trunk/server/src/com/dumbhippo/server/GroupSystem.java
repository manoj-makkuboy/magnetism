package com.dumbhippo.server;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@Local
public interface GroupSystem {
	public Group createGroup(User creator, String name, GroupAccess access);
	
	public void deleteGroup(User deleter, Group group);
	
	public void addMember(User adder, Group group, Person person);
	
	public void removeMember(User remover, Group group, Person person);
	
	public int getMembersCount(Viewpoint viewpoint, Group group, MembershipStatus status);
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, PersonViewExtra... extras);
	
	public Set<PersonView> getMembers(Viewpoint viewpoint, Group group, MembershipStatus status, PersonViewExtra... extras);

	public Set<User> getUserMembers(Viewpoint viewpoint, Group group);
	
	public Set<User> getUserMembers(Viewpoint viewpoint, Group group, MembershipStatus status);
	
	public GroupMember getGroupMember(Viewpoint viewpoint, Group group, User member) throws NotFoundException;
	
	public GroupMember getGroupMember(Group group, Resource member) throws NotFoundException;
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, User member);	
	
	public Set<Group> findRawGroups(Viewpoint viewpoint, User member, MembershipStatus status);

	/**
	 * Increase the version number of the group; increasing the group version means
	 * any cached resources for the group (currently, just the group photo) are
	 * no longer valid and must be reloaded. 
	 * 
	 * You must call this function after the corresponding changes are committed;
	 * calling it first could result in stale versions of the resources being
	 * received and cached.
	 * 
	 * Note that if you have a Group object around and call this function with
	 * group.getId(), the version field of the group won't be updated.
	 * 
	 * @param groupId group ID of a group
	 * @return the new version
	 */
	public int incrementGroupVersion(String groupId);

	/**
	 * Find the groups that member is in. The returned GroupView objects
	 * will include information about the user inviting the user to the
	 * group only when the viewpoint is the member's own viewpoint; the
	 * inviter information isn't interesting in other cases, so it's
	 * not worth the expense to retrieve.
	 * 
	 * @param viewpoint the viewpoint of the viewer viewing member
	 * @param member the person being viewed
	 * @return a list of GroupView objects for the groups member is in
	 */
	public Set<GroupView> findGroups(Viewpoint viewpoint, User member);
	
	public int findGroupsCount(Viewpoint viewpoint, User member);
	
	public Group lookupGroupById(Viewpoint viewpoint, String groupId) throws NotFoundException;
	
	public Group lookupGroupById(Viewpoint viewpoint, Guid guid) throws NotFoundException;
	
	/**
	 * Finds the set of contacts of an account owner that aren't already
	 * members of a group (and thus can be added to the group)
	 * 
	 * @param viewpoint viewpoint from which we are viewing the setup
	 * @param owner account owner (must equal viewpoint.getViewer() currently)
	 * @param groupId group ID of a group
	 * @param extras info to put in each PersonView
	 * @return the contacts of owner that aren't already members of the group
	 */
	public Set<PersonView> findAddableContacts(UserViewpoint viewpoint, User owner, String groupId, PersonViewExtra... extras);
	
	/**
	 * Get all messages that were posted in the chatroom about this group.
	 * 
	 * @param group the group the look up the messages for
	 * @return the list of mesages, sorted by date (newest last)
	 */
	public List<GroupMessage> getGroupMessages(Group group);
	
	/**
	 * Add a new message that was sent to the chatroom about this group
	 * 
	 * @param group the group the message is about.
	 * @param fromUser the user who sent the message
	 * @param text the text of the message
	 * @param timestamp the time when the message was posted
	 * @param serial counter (starts at zero) of messages for the group
	 */
	public void addGroupMessage(Group group, User fromUser, String text, Date timestamp, int serial);
}
