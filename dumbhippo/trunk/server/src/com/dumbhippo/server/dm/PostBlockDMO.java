package com.dumbhippo.server.dm;

import com.dumbhippo.dm.annotations.DMO;
import com.dumbhippo.dm.annotations.DMProperty;
import com.dumbhippo.dm.store.StoreKey;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.blocks.PostBlockView;

@DMO(classId="http://mugshot.org/p/o/postBlock")
public abstract class PostBlockDMO extends BlockDMO {
	protected PostBlockDMO(BlockDMOKey key) {
		super(key);
	}

	@DMProperty(defaultInclude=true, defaultChildren="+")
	public PostDMO getPost() {
		Post post = ((PostBlockView)blockView).getPostView().getPost();
		return session.findUnchecked(PostDMO.class, post.getGuid());
	}
	
	@Override
	public StoreKey<?,?> getVisibilityDelegate() {
		return getPost().getStoreKey();
	}
}
