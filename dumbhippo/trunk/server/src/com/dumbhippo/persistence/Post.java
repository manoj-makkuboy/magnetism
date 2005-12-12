package com.dumbhippo.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.postinfo.PostInfo;

@Entity
public class Post extends GuidPersistable {
	
	static private final Log logger = GlobalSetup.getLog(Post.class);
	static public final int MAX_INFO_LENGTH = 2048;
	
	private static final long serialVersionUID = 0L;

	private User poster;
	private PostVisibility visibility;
	private String explicitTitle;
	private long postDate;
	private String text;
	private String info;
	private long infoDate;
	transient private PostInfo cachedPostInfo;
	transient private boolean leaveInfoUnmodified;
	private Set<Resource> personRecipients;
	private Set<Group> groupRecipients;
	private Set<Resource> resources;
	private Set<Resource> expandedRecipients;
	private ChatRoom chatRoom;
	transient private boolean cachedUrlUpdated;
	transient private URL cachedUrl;
	
	private void initMissing() {
		if (visibility == null)
			visibility = PostVisibility.ANONYMOUSLY_PUBLIC;
		if (personRecipients == null)
			personRecipients = new HashSet<Resource>();
		if (groupRecipients == null)
			groupRecipients = new HashSet<Group>();
		if (resources == null)
			resources = new HashSet<Resource>();
		if (expandedRecipients == null)
			expandedRecipients = new HashSet<Resource>();
	}
	
	protected Post() {
		initMissing();
	}
	
	/**
	 * @param poster
	 * @param visibility 
	 * @param explicitTitle
	 * @param text
	 * @param personRecipients
	 * @param groupRecipients
	 * @param expandedRecipients
	 * @param resources
	 */
	public Post(User poster, PostVisibility visibility, String explicitTitle, String text, Set<Resource> personRecipients,
			Set<Group> groupRecipients, Set<Resource> expandedRecipients, Set<Resource> resources) {
		this.poster = poster;
		this.visibility = visibility;
		this.explicitTitle = explicitTitle;
		this.text = text;
		this.personRecipients = personRecipients;
		this.groupRecipients = groupRecipients;
		this.expandedRecipients = expandedRecipients;
		this.resources = resources;
		this.postDate = System.currentTimeMillis();
		initMissing();
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public User getPoster() {
		return poster;
	}
	public void setPoster(User poster) {
		this.poster = poster;
	}
	
	@Column(nullable=false)
	public PostVisibility getVisibility() {
		return visibility;
	}

	public void setVisibility(PostVisibility visibility) {
		this.visibility = visibility;
	}

	@ManyToMany
	@JoinTable(table=@Table(name="Post_PersonRecipient"))
	public Set<Resource> getPersonRecipients() {
		return personRecipients;
	}
	protected void setPersonRecipients(Set<Resource> recipients) {
		if (recipients == null)
			throw new IllegalArgumentException("null");
		this.personRecipients = recipients;
	}
	public void addPersonRecipients(Set<Resource> newRecipients) {
		this.personRecipients.addAll(newRecipients);
	}
	
	@ManyToMany
	public Set<Group> getGroupRecipients() {
		return groupRecipients;
	}
	protected void setGroupRecipients(Set<Group> recipients) {
		if (recipients == null)
			throw new IllegalArgumentException("null");
		this.groupRecipients = recipients;
	}
	public void addGroupRecipients(Set<Group> newRecipients) {
		this.groupRecipients.addAll(newRecipients);
	}
	
	@ManyToMany
	public Set<Resource> getResources() {
		return resources;
	}
	protected void setResources(Set<Resource> resources) {
		if (resources == null)
			throw new IllegalArgumentException("null");
		this.resources = resources;
	}
	
	// This results in the "text" type on mysql. 
	// The Postgresql docs I've read basically say 
	// "varchar's fixed lengthness is stupid, text is always
	// a better choice unless you have a real reason for a fixed length"
	// on postgresql "text" is the same as "varchar" but doesn't run 
	// the code to truncate to the fixed length.
	// But some mysql stuff says mysql text type is screwy and it 
	// involves some kind of out-of-band lookaside table.
	// 
	// Anyhow, Hibernate seems to be broken, it doesn't 
	// properly convert the clob to a string, so we can't 
	// use this anyway. 255 char limit it is.
	//@Lob(type=LobType.CLOB, fetch=FetchType.EAGER)
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getExplicitTitle() {
		return explicitTitle;
	}
	public void setExplicitTitle(String title) {
		this.explicitTitle = title;
	}

	@ManyToMany
	@JoinTable(table=@Table(name="Post_ExpandedRecipient"))
	public Set<Resource> getExpandedRecipients() {
		return expandedRecipients;
	}

	public void setExpandedRecipients(Set<Resource> expandedRecipients) {
		if (expandedRecipients == null)
			throw new IllegalArgumentException("null");
		this.expandedRecipients = expandedRecipients;
	}
	
	@Transient
	public String getTitle() {
		if (explicitTitle != null)
			return explicitTitle;
		else if (resources != null && !resources.isEmpty()) {
			// FIXME look for an url and use its title
		
			return "";
		} else {
			return "";
		}
	}
	
	@Column(nullable=false)
	public Date getPostDate() {
		return new Date(postDate);
	}

	protected void setPostDate(Date postDate) {
		this.postDate = postDate.getTime();
	}
	
	@OneToOne(cascade = { CascadeType.ALL }, mappedBy="post")
	@JoinColumn(nullable=true)
	public ChatRoom getChatRoom() {
		return chatRoom;
	}

	public void setChatRoom(ChatRoom chatRoom) {
		this.chatRoom = chatRoom;
	}

	@Column(nullable=true,length=MAX_INFO_LENGTH)
	public String getInfo() {
		return info;
	}

	protected void setInfo(String info) {
		logger.debug(this + " setting info: " + info + " was: " + this.info);
		
		// we probably need some better way to handle this, but 
		// we definitely don't want to let the database to silently truncate it
		// producing corrupt xml
		if (info != null && info.length() > MAX_INFO_LENGTH)
			throw new IllegalArgumentException("info XML string exceeds max length");
		
		if (this.info != null && info != null && this.info.equals(info))
			return;
		
		cachedPostInfo = null;
		
		// silently don't change it... probably the XML parser is broken
		if (leaveInfoUnmodified) {
			return;
		}
		
		this.info = info;
		
		
	}

	@Column(nullable=true)
	public Date getInfoDate() {
		if (infoDate < 0)
			return null;
		else
			return new Date(infoDate);
	}

	public void setInfoDate(Date infoDate) {
		if (infoDate == null)
			this.infoDate = -1;
		else
			this.infoDate = infoDate.getTime();
	}
	
	@Transient
	public PostInfo getPostInfo() {
		
		if (cachedPostInfo != null)
			return cachedPostInfo;
		
		String info = getInfo();
		if (info != null) {
			try {
				cachedPostInfo = PostInfo.parse(info);
				cachedPostInfo.makeImmutable();
				return cachedPostInfo;
			} catch (SAXException e) {
				logger.trace(e);
				logger.error("post " + getId() + " appears to have corrupt PostInfo XML string: " + e.getMessage());
				
				// We enter a "do no harm" mode when this happens; it's probably some bug
				// in our XML parser and we don't want to lose any data from the PostInfo
				// because we wouldn't be able to get it back; if we can't parse the 
				// info we can't round trip the data
				
				leaveInfoUnmodified = true;
				
				return null;
			}
		} else {
			return null;
		}
	}
	
	public void setPostInfo(PostInfo postInfo) {
		if (postInfo != null) {
			
			if (cachedPostInfo == postInfo)
				throw new RuntimeException("Don't modify the post info from Post.getPostInfo() and then set it back. Make a copy.");
			
			if (cachedPostInfo != null && cachedPostInfo.equals(postInfo)) {
				logger.debug("Cached post info is the same as new post info, doing nothing. old: " + cachedPostInfo + " new: " + postInfo);
				return; // nothing to do
			}
			
			setInfo(postInfo.toXml());
		} else {
			setInfo(null);
		}
		// note that setInfo just set cachedPostInfo = null
		
		// not copying this is probably going to cause a bug, but 
		// for now it seems like a worthwhile efficiency hack
		cachedPostInfo = postInfo;
		if (cachedPostInfo != null)
			cachedPostInfo.makeImmutable();
	}
	
	@Transient
	public URL getUrl() {
		if (!cachedUrlUpdated) {
			String link = null;
			Set<Resource> resources = getResources();
			if (resources != null) {
				for (Resource r : resources) {
					if (r instanceof LinkResource) {
						link = ((LinkResource)r).getUrl();
						break;
					}
				}
			}
			cachedUrl = null;
			if (link != null) {
				try {
					cachedUrl = new URL(link);
				} catch (MalformedURLException e) {
					logger.debug("Invalid link in database: " + link);
				}
			}
			cachedUrlUpdated = true;
		}
		return cachedUrl;
	}
	
	public String toString() {
		return "{Post id=" + getId() + "}";
	}
}
