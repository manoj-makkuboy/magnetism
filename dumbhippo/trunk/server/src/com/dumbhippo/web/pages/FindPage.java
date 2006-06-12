package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostSearchResult;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.TrackSearchResult;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;

/**
 * Backing bean for find.jsp ... as find.jsp is a temporary name for a 
 * search.jsp replacement, this is a temporary name for a SearchPage 
 * replacement.
 * 
 * @author otaylor
 */
public class FindPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FindPage.class);
	
	// We override the default values from Pageable
	static private final int INITIAL_PER_PAGE = 3;
	static private final int SUBSEQUENT_PER_PAGE = 10;
	
	@Signin
	private SigninBean signin;
	
	@PagePositions
	private PagePositionsBean pagePositions;

	private PostingBoard postBoard;
	private MusicSystem musicSystem;
	private IdentitySpider identitySpider;
	
	private String searchText;

	private PostSearchResult postSearchResult;
	private Pageable<PostView> posts;
	private TrackSearchResult trackSearchResult;
	private Pageable<TrackView> tracks;
	
	private Pageable<PersonView> people;
	
	public FindPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	}

	private void ensurePostSearchResult() {
		if (postSearchResult == null) {
			postSearchResult = postBoard.searchPosts(signin.getViewpoint(), searchText);
			posts = pagePositions.createPageable("posts");
			posts.setInitialPerPage(INITIAL_PER_PAGE);
			posts.setSubsequentPerPage(SUBSEQUENT_PER_PAGE);

			List<PostView> resultList = postBoard.getPostSearchPosts(signin.getViewpoint(), postSearchResult, posts.getStart(), posts.getCount());
			
			posts.setTotalCount(postSearchResult.getApproximateCount());
			posts.setResults(resultList);
		}
	}
	
	public String getPostError() {
		ensurePostSearchResult();

		return postSearchResult.getError();
	}
	
	public Pageable<PostView> getPosts() {
		ensurePostSearchResult();
		
		return posts;
	}

	private void ensureTrackSearchResult() {
		if (trackSearchResult == null) {
			trackSearchResult = musicSystem.searchTracks(signin.getViewpoint(), searchText);
			tracks = pagePositions.createPageable("songs");
			tracks.setInitialPerPage(INITIAL_PER_PAGE);
			tracks.setSubsequentPerPage(SUBSEQUENT_PER_PAGE);

			List<TrackView> resultList = musicSystem.getTrackSearchTracks(signin.getViewpoint(), trackSearchResult, tracks.getStart(), tracks.getCount());
			
			tracks.setTotalCount(trackSearchResult.getApproximateCount());
			tracks.setResults(resultList);
		}
	}
	
	public String getTrackError() {
		ensureTrackSearchResult();

		return trackSearchResult.getError();
	}
	
	public Pageable<TrackView> getTracks() {
		ensureTrackSearchResult();
		
		return tracks;
	}

	public Pageable<PersonView> getPeople() {
		if (people == null) {
			people = pagePositions.createPageable("people");
			people.setInitialPerPage(INITIAL_PER_PAGE);
			people.setSubsequentPerPage(SUBSEQUENT_PER_PAGE);
			ArrayList<PersonView> results = new ArrayList<PersonView>();
			
			Viewpoint viewpoint = signin.getViewpoint();			
			
			if (viewpoint instanceof UserViewpoint && searchText != null) {
				// look up the search text as an email or AIM
				User user;
				if (searchText.contains("@"))
					user = identitySpider.lookupUserByEmail(viewpoint, searchText);
				else
					user = identitySpider.lookupUserByAim(viewpoint, searchText);
				
				if (user != null) {
					PersonView pv = identitySpider.getPersonView(viewpoint, user, PersonViewExtra.ALL_RESOURCES);
					results.add(pv);
				}
			}
			people.setTotalCount(results.size());
			people.setResults(results);
		}
		
		return people;
	}	
	
	public SigninBean getSignin() {
		return signin;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		if (searchText != null)
			searchText = searchText.trim();
		this.searchText = searchText;
	}
}
