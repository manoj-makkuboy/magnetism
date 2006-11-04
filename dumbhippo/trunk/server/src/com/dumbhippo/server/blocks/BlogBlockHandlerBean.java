package com.dumbhippo.server.blocks;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class BlogBlockHandlerBean extends AbstractBlockHandlerBean<BlogBlockView> implements
		BlogBlockHandler {

	@EJB
	private PersonViewer personViewer;	
	
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private FeedSystem feedSystem;
	
	public BlogBlockHandlerBean() {
		super(BlogBlockView.class);
	}

	@Override
	protected void populateBlockViewImpl(BlogBlockView blockView) throws BlockNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();
		
		User user = identitySpider.lookupUser(block.getData1AsGuid());
		// TODO: check what extras we need to request here
		PersonView userView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.ALL_RESOURCES);
		ExternalAccount blogAccount;
		try {
			blogAccount = externalAccountSystem.lookupExternalAccount(viewpoint, user, ExternalAccountType.BLOG);
		} catch (NotFoundException e) {
			throw new BlockNotVisibleException("external blog account for block not visible", e);
		}  
	    FeedEntry lastEntry = feedSystem.getLastEntry(blogAccount.getFeed());
	    blockView.setUserView(userView);
		blockView.setEntry(lastEntry);
		blockView.setPopulated(true);
	}

	public Set<User> getInterestedUsers(Block block) {
		return getUsersWhoCareAboutData1User(block);
	}
	
	public Set<Group> getInterestedGroups(Block block) {
		return getGroupsData1UserIsIn(block);
	}	
}
