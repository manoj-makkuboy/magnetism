/* HippoIcon.cpp: notification icon
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx.h"
#include <strsafe.h>
#include "HippoIcon.h"
#include "HippoUI.h"
#include <HippoUtil.h>
#include <HippoUtil_h.h>
#include "Resource.h"
#include "Guid.h"

// Note that timeout values are clamped between 10 and 30 seconds
// by current versions of Windows

// Timeout for balloons for new posts (30 seconds)
static const int NEW_POST_NOTIFY_TIMEOUT = 30 * 1000;

// Timeout for ballons from someone clicking on an existing post (10 seconds)
static const int CLICKED_ON_NOTIFY_TIMEOUT = 10 * 1000;

HippoIcon::HippoIcon()
{
    ignoreNextClick_ = false;
}

HippoIcon::~HippoIcon()
{
}

void
HippoIcon::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoIcon::create(HWND window)		  
{
    NOTIFYICONDATA notifyIconData = { 0 };
    HINSTANCE instance = GetModuleHandle(NULL);
   
    window_ = window;
    
    message_ = RegisterWindowMessage(TEXT("HippoNotifyMessage"));

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_ICON | NIF_MESSAGE;
    notifyIconData.uCallbackMessage = message_;
    notifyIconData.hIcon = icon_;

    Shell_NotifyIcon(NIM_ADD, &notifyIconData);

	displayState_ = DISPLAYING_NONE;

    return true;
}

void
HippoIcon::destroy(void)
{
   NOTIFYICONDATA notifyIconData = { 0 };

   notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
   notifyIconData.hWnd = window_;
   notifyIconData.uID = 0;
   
   Shell_NotifyIcon(NIM_DELETE, &notifyIconData);
}

void
HippoIcon::setIcon(HICON icon)
{
    icon_ = icon;
}

void
HippoIcon::updateIcon(HICON icon)
{	
	setIcon(icon);
    NOTIFYICONDATA notifyIconData = { 0 };
    notifyIconData.uID = 0;
	notifyIconData.hWnd = window_;
    notifyIconData.uFlags = NIF_ICON;
    notifyIconData.hIcon = icon_;

    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}

UINT
HippoIcon::getMessage()
{
    return message_;
}

void 
HippoIcon::processMessage(WPARAM wParam,
			  LPARAM lParam)
		          
{
    switch (lParam) {
    case WM_LBUTTONDOWN:
    case NIN_SELECT:
	if (ignoreNextClick_) {
	    ignoreNextClick_ = false;
	    return;
	}
        ui_->showMenu(TPM_LEFTBUTTON);
        break;
    case WM_RBUTTONDOWN:
    case WM_CONTEXTMENU:
        ui_->showMenu(TPM_RIGHTBUTTON);
        break;
    case NIN_BALLOONSHOW:
	break;
    case NIN_BALLOONUSERCLICK:
		if (displayState_ == DISPLAYING_LINK) {
			ignoreNextClick_ = true;
			ui_->showURL(currentPostId_, currentURL_);
		}
        break;
    case NIN_BALLOONHIDE:
    case NIN_BALLOONTIMEOUT:
		displayState_ = DISPLAYING_NONE;
	break;
    }
    
}

// XP SP2 addition
#ifndef NIIF_USER
#define NIIF_USER 4
#endif

void
HippoIcon::showURL(HippoLinkShare &linkshare)
{
    currentURL_ = linkshare.url;
	currentPostId_ = linkshare.postId;

	displayState_ = DISPLAYING_LINK;

	{
		HippoPtr<HippoBubble> bubble = new HippoBubble;
		bubble->setUI(ui_);
		bubble->setLinkNotification(linkshare);
		bubble->show();
	}

    NOTIFYICONDATA notifyIconData = { 0 };

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_INFO;
    const size_t infoLen = sizeof(notifyIconData.szInfo) / sizeof(notifyIconData.szInfo[0]);

	StringCchCopy(notifyIconData.szInfo, infoLen, TEXT(""));
    StringCchCat(notifyIconData.szInfo, infoLen, linkshare.url);
	StringCchCat(notifyIconData.szInfo, infoLen, TEXT("\n\n"));
	StringCchCat(notifyIconData.szInfo, infoLen, linkshare.description);
	StringCchCat(notifyIconData.szInfo, infoLen, TEXT("\n\nSent from "));
	StringCchCat(notifyIconData.szInfo, infoLen, linkshare.senderName);
	StringCchCat(notifyIconData.szInfo, infoLen, TEXT(" to "));
	for (unsigned int i = 0; i < linkshare.personRecipients.length(); i++) {
		StringCchCat(notifyIconData.szInfo, infoLen, linkshare.personRecipients[i]);
		if (i < linkshare.personRecipients.length() - 1)
			StringCchCat(notifyIconData.szInfo, infoLen, TEXT(", "));
	}
	if (linkshare.personRecipients.length() > 0 
		&& linkshare.groupRecipients.length() > 0)
		StringCchCat(notifyIconData.szInfo, infoLen, TEXT(", and the groups "));
	else if (linkshare.groupRecipients.length() > 0) 
		StringCchCat(notifyIconData.szInfo, infoLen, TEXT("the groups "));
	for (unsigned int i = 0; i < linkshare.groupRecipients.length(); i++) {
		StringCchCat(notifyIconData.szInfo, infoLen, linkshare.groupRecipients[i]);
		if (i < linkshare.groupRecipients.length() - 1)
			StringCchCat(notifyIconData.szInfo, infoLen, TEXT(", "));
	}
    StringCchCat(notifyIconData.szInfo, infoLen, TEXT("\n(click to win)"));

    notifyIconData.uTimeout = NEW_POST_NOTIFY_TIMEOUT;
    const size_t titleLen = sizeof(notifyIconData.szInfoTitle) / sizeof(notifyIconData.szInfoTitle[0]);
	StringCchCopy(notifyIconData.szInfoTitle, titleLen, TEXT("New link"));
	StringCchCat(notifyIconData.szInfoTitle, titleLen, TEXT(": "));
	StringCchCat(notifyIconData.szInfoTitle, titleLen, linkshare.title);
    notifyIconData.dwInfoFlags = NIIF_USER;
   
    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}


void
HippoIcon::showURLClicked(const WCHAR *postId,
				          const WCHAR *clickerName,
		                  const WCHAR *title)
{
    currentURL_ = NULL;
	currentPostId_ = postId;

	displayState_ = DISPLAYING_CLICK;

	ui_->debugLogU("Showing clicked bubble");

    NOTIFYICONDATA notifyIconData = { 0 };

    notifyIconData.cbSize = sizeof(NOTIFYICONDATA);
    notifyIconData.hWnd = window_;
    notifyIconData.uID = 0;
    notifyIconData.uFlags = NIF_INFO;
    const size_t infoLen = sizeof(notifyIconData.szInfo) / sizeof(notifyIconData.szInfo[0]);

	StringCchCopy(notifyIconData.szInfo, infoLen, TEXT(""));
	StringCchCat(notifyIconData.szInfo, infoLen, clickerName);
	// FIXME i18n
	StringCchCat(notifyIconData.szInfo, infoLen, TEXT(" has viewed your share \""));
    StringCchCat(notifyIconData.szInfo, infoLen, title);
	StringCchCat(notifyIconData.szInfo, infoLen, TEXT("\""));

    notifyIconData.uTimeout = CLICKED_ON_NOTIFY_TIMEOUT;
    const size_t titleLen = sizeof(notifyIconData.szInfoTitle) / sizeof(notifyIconData.szInfoTitle[0]);
	StringCchCopy(notifyIconData.szInfoTitle, titleLen, TEXT("XMPP Remoted Hardware Event 0x23A"));
    notifyIconData.dwInfoFlags = NIIF_USER;
   
    Shell_NotifyIcon(NIM_MODIFY, &notifyIconData);
}
