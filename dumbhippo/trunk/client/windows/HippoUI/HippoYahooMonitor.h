#pragma once
#include "hippomusicmonitor.h"

class HippoYahooMonitor :
    public HippoMusicMonitor
{
public:
    HippoYahooMonitor();
    virtual ~HippoYahooMonitor();

    virtual bool hasCurrentTrack() const;
	virtual const HippoTrackInfo& getCurrentTrack() const;
    virtual const std::vector<HippoTrackInfo> getPrimingData() const;

private:
	friend class HippoYahooMonitorImpl;
	HippoPtr<HippoYahooMonitorImpl> impl_;
};
