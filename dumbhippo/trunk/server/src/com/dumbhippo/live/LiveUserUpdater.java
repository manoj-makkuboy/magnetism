package com.dumbhippo.live;

import javax.ejb.Local;

/**
 * Create and update LivePost objects using information from the
 * data store.
 */
@Local
public interface LiveUserUpdater {
	/**
	 * Does initialization of a newly created LiveUser object.
	 * 
	 * @param user the LiveUser object to initialize
	 */
	void initialize(LiveUser user);
}
