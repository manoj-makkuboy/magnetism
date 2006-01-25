/*
 * These abstract interfaces are intended to hide iTunes/WMP/etc. (various ways someone might be playing music)
 */
#pragma once

#include <assert.h>
#include "HippoArray.h"
#include "HippoUtil.h"

#include <vector>

#define HIPPO_TRACK_INFO_PROP(prop)                                                     \
public:                                                                                 \
    bool has ## prop() const { return prop ## _.Length() > 0; }                         \
    const HippoBSTR& get ## prop() const { assert(has ## prop()); return prop ## _; }   \
    void set ## prop(const HippoBSTR& val) { prop ## _ = val; }                         \
private:                                                                                \
    HippoBSTR prop ## _

#define HIPPO_TRACK_INFO_PROP_LONG(prop)                                                \
    HIPPO_TRACK_INFO_PROP(prop);                                                        \
public:                                                                                 \
    void set ## prop(long val) { fromLong(prop ## _, val); }

// has to match Java TrackType enum
#define HIPPO_TRACK_TYPE_UNKNOWN        L"UNKNOWN"
#define HIPPO_TRACK_TYPE_FILE           L"FILE"
#define HIPPO_TRACK_TYPE_CD             L"CD"
#define HIPPO_TRACK_TYPE_NETWORK_STREAM L"NETWORK_STREAM"
#define HIPPO_TRACK_TYPE_PODCAST        L"PODCAST"

// has to match Java MediaFileFormat enum
#define HIPPO_MEDIA_FILE_FORMAT_UNKNOWN L"UNKNOWN"
#define HIPPO_MEDIA_FILE_FORMAT_MP3     L"MP3"
#define HIPPO_MEDIA_FILE_FORMAT_WMA     L"WMA"
#define HIPPO_MEDIA_FILE_FORMAT_AAC     L"AAC"

// Keep the fields here in sync with Java Track class on the server,
// and with the code that stuffs HippoTrackInfo into an XMPP message,
// and with any music player backends...
class HippoTrackInfo
{
public:
	HippoTrackInfo() 
    {
	}

    HIPPO_TRACK_INFO_PROP(Type);
    HIPPO_TRACK_INFO_PROP(Format);
    HIPPO_TRACK_INFO_PROP(Name);
    HIPPO_TRACK_INFO_PROP(Artist);
    HIPPO_TRACK_INFO_PROP(Album);
    HIPPO_TRACK_INFO_PROP(Url);
    HIPPO_TRACK_INFO_PROP_LONG(Duration);
    HIPPO_TRACK_INFO_PROP_LONG(FileSize);
    HIPPO_TRACK_INFO_PROP_LONG(TrackNumber);
    HIPPO_TRACK_INFO_PROP(DiscIdentifier);

public:
    void clear() {
        Type_ = 0;
        Format_ = 0;
        Name_ = 0;
        Artist_ = 0;
        Album_ = 0;
        Url_ = 0;
        Duration_ = 0;
        FileSize_ = 0;
        TrackNumber_ = 0;
        DiscIdentifier_ = 0;
    }

    bool operator==(const HippoTrackInfo &other) const {
        if (&other == this)
            return true;
        return Type_ == other.Type_ &&
            Format_ == other.Format_ &&
            Name_ == other.Name_ &&
            Artist_ == other.Artist_ &&
            Album_ == other.Album_ &&
            Url_ == other.Url_ &&
            Duration_ == other.Duration_ &&
            FileSize_ == other.FileSize_ &&
            TrackNumber_ == other.TrackNumber_ && 
            DiscIdentifier_ == other.DiscIdentifier_;
    }

    bool operator!=(const HippoTrackInfo &other) const {
        return !(*this == other);
    }

    // default operator= and copy should be OK in theory...

private:
    void fromLong(HippoBSTR &s, long val) {
        WCHAR buf[32];
        StringCchPrintfW(buf, sizeof(buf), L"%ld", val);
        buf[31]='\0';
        s = buf;
    }
};

class HippoMusicListener;

class HippoMusicMonitor
{
public:
	
	virtual bool hasCurrentTrack() const = 0;
	virtual const HippoTrackInfo& getCurrentTrack() const = 0;
    virtual const std::vector<HippoTrackInfo> getPrimingData() const = 0;

	void addListener(HippoMusicListener *listener);
	void removeListener(HippoMusicListener *listener);

	virtual ~HippoMusicMonitor() {}

	static HippoMusicMonitor* createITunesMonitor();
    static HippoMusicMonitor* createYahooMonitor();

protected:
	void fireCurrentTrackChanged(bool haveTrack, const HippoTrackInfo & newTrack);

	HippoMusicMonitor() {}

private:
	HippoArray<HippoMusicListener*> listeners_;

	// private so they aren't used
	HippoMusicMonitor(const HippoMusicMonitor &other);
	HippoMusicMonitor& operator=(const HippoMusicMonitor &other);
};

class HippoMusicListener
{
public:
	virtual void onCurrentTrackChanged(HippoMusicMonitor *monitor, bool haveTrack, const HippoTrackInfo & newTrack) = 0;
	virtual ~HippoMusicListener() {}
};
