/* HippoScrollbar.cpp: scrollbar control
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"

#include "HippoScrollbar.h"

HippoScrollbar::HippoScrollbar()
    : orientation_(HIPPO_ORIENTATION_VERTICAL), minPos_(0), maxPos_(0), pageSize_(0)
{
    // standard Windows control
    setClassName(L"SCROLLBAR");
    setWindowStyle(WS_CHILD | SBS_VERT);
}

bool
HippoScrollbar::create()
{
    if (!HippoAbstractControl::create())
        return false;

    syncBounds();

    return true;
}

void
HippoScrollbar::onSizeChanged()
{
}

void
HippoScrollbar::setOrientation(HippoOrientation orientation)
{
    if (orientation_ == orientation)
        return;

    orientation_ = orientation;
    setWindowStyle(WS_CHILD | (orientation_ == HIPPO_ORIENTATION_VERTICAL ? 
                   SBS_VERT : SBS_HORZ));
}

void
HippoScrollbar::setBounds(int minPos,
                          int maxPos,
                          int pageSize)
{
    if (minPos_ == minPos && maxPos_ == maxPos && pageSize_ == pageSize)
        return;
    minPos_ = minPos;
    maxPos_ = maxPos;
    pageSize_ = pageSize;
    syncBounds();
}

void
HippoScrollbar::syncBounds()
{
    if (isCreated()) {
        SCROLLINFO si;
        si.cbSize = sizeof(si);
        si.fMask = SIF_DISABLENOSCROLL | SIF_PAGE | SIF_RANGE;
        si.nMin = minPos_;
        si.nMax = maxPos_;
        si.nPage = pageSize_;
        SetScrollInfo(window_, SB_CTL, &si, true);
    }
}

int
HippoScrollbar::handleScrollMessage(UINT   message,
                                    WPARAM wParam,
                                    LPARAM lParam)
{
    g_return_val_if_fail((message == WM_HSCROLL && orientation_ == HIPPO_ORIENTATION_HORIZONTAL) ||
                         (message == WM_VSCROLL && orientation_ == HIPPO_ORIENTATION_VERTICAL), 0);

    // Note, Windows packs a 16-bit scroll position into the message 
    // params, but we want to get the 32-bit position instead with GetScrollInfo

    // The "track position" is where the user moved the bar, and the 
    // "position" is where we set it. If we don't set it, then user movements
    // have no effect (the scrollbar will "bounce back" on mouse release).

    SCROLLINFO si;
    si.cbSize = sizeof(si);
    si.fMask = SIF_POS | SIF_TRACKPOS;
    GetScrollInfo(window_, SB_CTL, &si);

    int currentPos = si.nPos;
    int currentTrackPos = si.nTrackPos;
    int newPos = currentPos;

    switch (LOWORD(wParam)) {
    case SB_PAGEUP:
        newPos -= MAX((pageSize_ * 0.9), 1);
        break;
    case SB_PAGEDOWN:
        newPos += MAX((pageSize_ * 0.9), 1);
        break;
    case SB_LINEUP:
        newPos -= MAX((pageSize_ * 0.1), 1);
        break;
    case SB_LINEDOWN:
        newPos += MAX((pageSize_ * 0.1), 1);
        break;
    case SB_THUMBPOSITION:
        // this is an update when we set the scroll position ourselves
        break;
    case SB_THUMBTRACK:
        newPos = currentTrackPos;
        break;
    default:
        break;
    }
    
    if (newPos > (maxPos_ - pageSize_ - 1))
        newPos = maxPos_ - pageSize_ - 1;

    if (newPos < 0) 
        newPos = 0;

    if (newPos == currentPos)
        return currentPos;

    si.fMask = SIF_POS;
    si.nPos = newPos;
    SetScrollInfo(window_, SB_CTL, &si, true);

    return newPos;
}

bool 
HippoScrollbar::processMessage(UINT   message,
                               WPARAM wParam,
                               LPARAM lParam)
{
    g_warning("We aren't expecting our window proc to be called on common control class SCROLLBAR");
    return HippoAbstractControl::processMessage(message, wParam, lParam);
}

int
HippoScrollbar::getWidthRequestImpl()
{
    if (orientation_ == HIPPO_ORIENTATION_VERTICAL) {
        return GetSystemMetrics(SM_CXVSCROLL); // width of vscrollbar
    } else {
        return GetSystemMetrics(SM_CXHSCROLL) * 2 + 5; // width of two scroll arrows plus arbitrary 5 for the bar
    }
}

int
HippoScrollbar::getHeightRequestImpl(int forWidth)
{
    if (orientation_ == HIPPO_ORIENTATION_VERTICAL) {
        return GetSystemMetrics(SM_CYVSCROLL) * 2 + 5; // height of two scroll arrows plus arbitrary 5 for the bar
    } else {
        return GetSystemMetrics(SM_CYHSCROLL); // height of hscrollbar
    }
}
