/*
 * Copyright 2005 Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planetj.taste.impl.recommender;

import com.planetj.taste.model.Item;
import com.planetj.taste.recommender.ItemFilter;

/**
 * A simple {@link ItemFilter} which always returns <code>true</code>, thus
 * accepting all {@link Item}s.
 *
 * @author Sean Owen
 */
public final class AllItemFilter implements ItemFilter {

	private static final ItemFilter instance = new AllItemFilter();

	public static ItemFilter getInstance() {
		return instance;
	}

	private AllItemFilter() {
		// do nothing
	}

	/**
	 * @param item
	 * @return true always
	 */
	public boolean isAccepted(final Item item) {
		return true;
	}

}