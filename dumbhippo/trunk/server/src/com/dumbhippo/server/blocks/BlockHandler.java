package com.dumbhippo.server.blocks;

import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.views.Viewpoint;

/** This interface has to be implemented for each BlockType */
public interface BlockHandler {
	
	/**
	 * Creates a block view for the given viewpoint, block, and user block data.
	 * The block view should be "minimal" and cheaply created; i.e. not fill in 
	 * any extra details or information except as needed to check visibility.
	 * populateBlockView() will be called later and extra expensive work 
	 * should be done there.
	 * 
	 * FIXME - what is the user block data if the viewpoint is anonymous?
	 * are there guarantees about viewpoint to ubd relationship?
	 * 
	 * @param viewpoint viewpoint looking at the block
	 * @param block the block
	 * @param ubd the user block data
	 * @return new block view
	 * @throws BlockNotVisibleException if the viewpoint can't see this block or user block data
	 */
	public BlockView getUnpopulatedBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws BlockNotVisibleException;
	
	/**
	 * Do any more work to get a displayable BlockView that was not done during prepareBlockView().
	 * The new work cannot reduce the visibility of the block view; this block view should 
	 * already be known visible to its viewpoint due to prepareBlockView()'s checks.
	 * 
	 * @param blockView a block view
	 */
	public void populateBlockView(BlockView blockView);
}
