package com.dumbhippo.aim;

public interface RawListener extends BaseListener {
    public void handleMessage(ScreenName buddy, String htmlMessage) throws FilterException;

    public void handleSetEvilAmount(ScreenName whoEviledUs, int amount);

    public void handleBuddySignOn(ScreenName buddy, String htmlInfo);
    public void handleBuddySignOff(ScreenName buddy, String htmlInfo);

    public void handleBuddyUnavailable(ScreenName buddy, String htmlMessage);
    public void handleBuddyAvailable(ScreenName buddy, String htmlMessage);
    
    // may add a new buddy too
    public void handleUpdateBuddy(ScreenName buddy, String group);
    public void handleAddPermitted(ScreenName buddy);
    public void handleAddDenied(ScreenName buddy);
}
