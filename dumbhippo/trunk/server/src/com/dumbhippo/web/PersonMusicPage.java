package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.TrackView;

public class PersonMusicPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PersonMusicPage.class);
	
	static private final int LIST_SIZE = 5;
	
	private Configuration configuration;
	private InvitationSystem invitationSystem;
	private MusicSystem musicSystem;
	private ListBean<TrackView> latestTracks;
	private ListBean<TrackView> frequentTracks;
	private ListBean<TrackView> popularTracks;
	private boolean musicSharingEnabled; 
	private int selfInvitations;
	private ListBean<NowPlayingTheme> exampleThemes;
	
	public PersonMusicPage() {
		selfInvitations = -1;
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}

	public ListBean<TrackView> getFrequentTracks() {
		if (frequentTracks == null) {
			frequentTracks = new ListBean<TrackView>(getMusicSystem().getFrequentTrackViews(getSignin().getViewpoint(), getViewedUser(), LIST_SIZE));
		}
		
		return frequentTracks;
	}

	public ListBean<TrackView> getLatestTracks() {
		if (latestTracks == null) {
			latestTracks = new ListBean<TrackView>(getMusicSystem().getLatestTrackViews(getSignin().getViewpoint(), getViewedUser(), LIST_SIZE));
		}

		return latestTracks;
	}
	
	public ListBean<TrackView> getPopularTracks() {
		if (popularTracks == null) {
			popularTracks = new ListBean<TrackView>(getMusicSystem().getPopularTrackViews(getSignin().getViewpoint(), LIST_SIZE));
		}
		
		return popularTracks;
	}
	
	public boolean isMusicSharingEnabled() {
		return musicSharingEnabled;
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	@Override
	public void setViewedUser(User user) {
		super.setViewedUser(user);
		musicSharingEnabled = getIdentitySpider().getMusicSharingEnabled(user);
	}
	
	public int getSelfInvitations() {
		if (selfInvitations < 0) {
			selfInvitations = invitationSystem.getInvitations(getIdentitySpider().getCharacter(Character.MUSIC_GEEK));
		}
		return selfInvitations;
	}
	
	public String getPromotion() {
		return PromotionCode.MUSIC_INVITE_PAGE_200602.getCode();
	}
	
	public ListBean<NowPlayingTheme> getExampleThemes() {
		if (exampleThemes == null) {
			exampleThemes = new ListBean<NowPlayingTheme>(musicSystem.getExampleNowPlayingThemes(getSignin().getViewpoint(), 5));
		}
		return exampleThemes;
	}
}
