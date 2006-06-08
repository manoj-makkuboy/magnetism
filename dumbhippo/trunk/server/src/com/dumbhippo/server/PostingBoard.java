package com.dumbhippo.server;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.lucene.DocumentBuilder;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.User;
import com.dumbhippo.postinfo.PostInfo;

@Local
public interface PostingBoard {

	public URL parsePostURL(String url);
	
	public int getPostsForCount(Viewpoint viewpoint, Person forPerson);	
	
	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, int start, int max);

	public void pagePostsFor(Viewpoint viewpoint, Person poster, Pageable<PostView> pageable);

	public int getReceivedPostsCount(UserViewpoint viewpoint, User recipient);
	
	public List<PostView> getReceivedPosts(UserViewpoint viewpoint, User recipient, int start, int max);
	
	/**
	 * Gets information about received posts for display in pageable fashion; currently
	 * posts must be retrieved from the viewpoint of the receiving user.
	 * 
	 * @param viewpoint the viewpoint retrieving the information
	 * @param recipient the user receiving the posts
	 * @param pageable provides information about what posts to view and receives the result
	 */
	public void pageReceivedPosts(UserViewpoint viewpoint, User recipient, Pageable<PostView> pageable);
	
	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, int start, int max);
	
	/**
	 * Gets information about posts sent to a group in a pageable fashion.
	 * 
	 * @param viewpoint the viewpoint retrieving the information
	 * @param recipient the group that received the posts
	 * @param pageable provides information about what posts to view and receives the result
	 */
	public void getGroupPosts(Viewpoint viewpoint, Group recipient, Pageable<PostView> pageable);

	public List<PostView> getContactPosts(UserViewpoint viewpoint, User user, boolean include_received, int start, int max);

	/**
	 * Retrieve posts that a user has marked as "favorites"
	 * @param viewpoint the viewpoint retrieving the information
	 * @param user the user whose favorites to retrieve
	 * @param pageable provides information about what posts to view and receives the result
	 */
	public void pageFavoritePosts(Viewpoint viewpoint, User user, Pageable<PostView> pageable);	
	
	public boolean canViewPost(UserViewpoint viewpoint, Post post);
	
	public int getPostsForCount(Viewpoint viewpoint, Person forPerson, String search);
	
	public List<PostView> getPostsFor(Viewpoint viewpoint, Person poster, String search, int start, int max);
	
	public int getReceivedPostsCount(UserViewpoint viewpoint, User recipient, String search);
	
	public List<PostView> getReceivedPosts(UserViewpoint viewpoint, User recipient, String search, int start, int max);

	public int getGroupPostsCount(Viewpoint viewpoint, Group recipient, String search);
	
	public List<PostView> getGroupPosts(Viewpoint viewpoint, Group recipient, String search, int start, int max);
	
	/**
	 * Count the number of posts sent to a group that are visible to a 
	 * particular viewpoint. Passing in the system viewpoint here will
	 * be faster, but the count may be greater than the number of
	 * posts that a user can actually see.
	 * 
	 * @param viewpoint viewpoint retrieving the information
	 * @param group a group
	 * @return count of the number of posts sent to the group that are visible to the viewpoint.
	 */
	public int getGroupPostsCount(Viewpoint viewpoint, Group group);
	
	public void pageHotPosts(Viewpoint viewpoint, Pageable<PostView> pageable);
	
	enum InviteRecipients {
		DONT_INVITE,         // Just send out plain emails without invitation links
		INVITE_IF_POSSIBLE,  // Send out plain emails if sender has no invitations  
		MUST_INVITE          // Skip sending email if sender has no invitations
	}
	
	public Post doLinkPost(User poster, PostVisibility visibility, String title, String text, URL link, Set<GuidPersistable> recipients, InviteRecipients inviteRecipients, PostInfo postInfo)
		throws NotFoundException;

	public Post doShareGroupPost(User poster, Group group, String title, String text, Set<GuidPersistable> recipients, InviteRecipients inviteRecipients)
		throws NotFoundException;

	public void doGroupInvitationPost(User owner, Group group);	
	
	public void doShareLinkTutorialPost(User recipient);
	
	public void doNowPlayingTutorialPost(User recipient);
	
	public Post loadRawPost(Viewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public PostView loadPost(Viewpoint viewpoint, Guid guid) throws NotFoundException;
	
	public PostView getPostView(Viewpoint viewpoint, Post post);
	
	/**
	 * Returns a set of EntityView (i.e. PersonView and GroupView) which contains the entities directly
	 * referenced by this post.  At the moment, this is just the person recipients, the poster, and the
	 * group recipients.  This does not include indirectly referenced entities (e.g. the members of a group).
	 * 
	 * @param viewpoint viewpoint from which the post is viewed
	 * @param post the post in question
	 * @return set of EntityView
	 */
	public Set<EntityView> getReferencedEntities(Viewpoint viewpoint, Post post);
	
	public List<PersonPostData> getPostViewers(Viewpoint viewpoint, Guid guid, int max);
	
	public int getPostViewerCount(Guid guid);

	/**
	 * Notifies system that the post was viewed by the given person.
	 * If the postId is bad, silently does nothing.  This also
	 * un-ignores the post if the user had it ignored.
	 * 
	 * @param postId the ID of the post that was clicked
	 * @param clicker person who clicked on the post
	 */
	public void postViewedBy(String postId, User clicker);
	
	/**
	 * Get all messages that were posted in the chatroom about this post.
	 * 
	 * @param post the post the look up the messages for
	 * @return the list of mesages, sorted by date (newest last)
	 */
	public List<PostMessage> getPostMessages(Post post);
	
	/**
	 * Get recent messages that were posted in the chatroom about this post.
	 * 
	 * @param post the post the look up the messages for
	 * @return the list of mesages, sorted by date (newest last)
	 */
	public List<PostMessage> getRecentPostMessages(Post post, int seconds);	
	
	/**
	 * Add a new message that was sent to the chatroom about this post
	 * 
	 * @param post the post the message is about.
	 * @param fromUser the user who sent the message
	 * @param text the text of the message
	 * @param timestamp the time when the message was posted
	 * @param serial counter (starts at zero) of messages for the post
	 */
	public void addPostMessage(Post post, User fromUser, String text, Date timestamp, int serial);
	
	/**
	 * Search the database of posts using Lucene.
	 * 
	 * @param viewpoint the viewpoint being searched from
	 * @param queryString the search string to use, in Lucene syntax. The search
	 *   will be done across both the title and description fields
	 * @return a PostSearchResult object representing the search; you should
	 *    check the getError() method of this object to determine if an error
	 *    occurred (such as an error parsing the query string) 
	 */
	public PostSearchResult searchPosts(Viewpoint viewpoint, String queryString);
	
	/**
	 * Get a range of posts from the result object returned from searchPosts(). 
	 * This is slightly more efficient than calling PostSearchResult getPosts(),
	 * because we avoid some EJB overhead.
	 * 
	 * @param viewpoint the viewpoint for the returned PostView objects; must be the same 
	 *        as the viewpoint passed in when calling searchPosts()
	 * @param
	 * @param start the index of the first post to retrieve (starting at zero)
	 * @param count the maximum number of items desired 
	 * @return a list of PostView objects; may have less than count items when no more
	 *        are available. 
	 */
	public List<PostView> getPostSearchPosts(Viewpoint viewpoint, PostSearchResult searchResult, int start, int count);
	
	/**
	 * Add the posts given by a set of ids to the specified Lucene index. This is used internally
	 * when new posts are created.
	 * 
	 * @param writer a Lucene IndexWriter
	 * @param builder a DocumentBuilder to use to create Lucene Document objects from Posts
	 * @param ids a list of Post Guids to index
	 * @throws IOException
	 */
	public void indexPosts(IndexWriter writer, DocumentBuilder<Post> builder, List<Object> ids) throws IOException;
	
	/**
	 * Add all posts in the database to the specified Lucene index. This is an internal implementation
	 * detail of PostIndex.reindex().
	 * 
	 * @param writer a Lucene IndexWriter
	 * @param builder a DocumentBuilder to use to create Lucene Document objects from Posts
	 * @throws IOException
	 */
	public void indexAllPosts(IndexWriter writer, DocumentBuilder<Post> builder) throws IOException;
	
	/**
	 * Sets whether the given post is a favorite of the given viewpoint user.
	 * @param viewpoint
	 * @param post
	 * @param favorite
	 */
	public void setFavoritePost(UserViewpoint viewpoint, Post post, boolean favorite);

	/**
	 * Record whether ornot the specified user wants to ignore future notifications about
	 * a particular post.
	 * 
	 * @param user user who wishes to ignore a post
	 * @param post post to be ignored
	 * @param ignore true iff future notifications should be ignored
	 */
	public void setPostIgnored(User user, Post post, boolean ignore);
	
	/**
	 * Returns whether or not the specified user wants to ignore notifications
	 * about a post.
	 * 
	 * @param user user in question
	 * @param post post in question
	 * @return true iff post notifications should be ignored
	 */
	public boolean getPostIgnored(User user, Post post);
}
