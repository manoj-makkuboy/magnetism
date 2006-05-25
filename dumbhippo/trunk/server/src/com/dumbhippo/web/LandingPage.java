package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PromotionCode;

public class LandingPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LandingPage.class);

	private InvitationSystem invitationSystem;
	private int selfInvitations;
	
	public LandingPage() {
		selfInvitations = -1;
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
	}
	
	public int getSelfInvitations() {
		if (selfInvitations < 0) {
			selfInvitations = invitationSystem.getInvitations(getIdentitySpider().getCharacter(Character.MUGSHOT));
		}
		return selfInvitations;
	}
	
	public String getPromotion() {
		return PromotionCode.GENERIC_LANDING_200606.getCode();
	}

	public String getSummitPromotion() {
		return PromotionCode.SUMMIT_LANDING_200606.getCode();
	}
}
