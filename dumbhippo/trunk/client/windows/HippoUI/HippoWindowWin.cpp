#include "stdafx-hippoui.h"
#include "HippoWindowWin.h"
#include "HippoAbstractControl.h"
#include "HippoUIUtil.h"
#include "HippoCanvas.h"
#include "HippoGSignal.h"

class HippoWindowImpl : public HippoAbstractControl {
public:
    HippoWindowImpl() {
        setClassName(L"HippoWindow");
        setClassStyle(CS_HREDRAW | CS_VREDRAW);
        setWindowStyle(WS_POPUP); // change to WS_OVERLAPPEDWINDOW if you want resize/maximize etc. controls
        //setExtendedStyle(WS_EX_TOPMOST);
        setTitle(L"Mugshot");
        setResizable(HIPPO_ORIENTATION_VERTICAL, false);
        setResizable(HIPPO_ORIENTATION_HORIZONTAL, false);
        contentsControl_ = new HippoCanvas();
        contentsControl_->Release(); // remove extra ref
        contentsControl_->setParent(this);
    }

    virtual void show(bool activate);

    void setContents(HippoCanvasItem *item);
    void setVisible(bool visible);
    void setPosition(int x, int y);
    void getSize(int *x_p, int *y_p);
    void beginResize(HippoSide side);

    virtual bool create();
    virtual void onSizeChanged();

protected:

    virtual void queueResize();
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    virtual void destroy();

private:
    GIdle resizeIdle_;

    bool idleResize();

    HippoGObjectPtr<HippoCanvasItem> contents_;
    HippoPtr<HippoCanvas> contentsControl_;
};

static void      hippo_window_win_init                (HippoWindowWin       *window_win);
static void      hippo_window_win_class_init          (HippoWindowWinClass  *klass);
static void      hippo_window_win_iface_init          (HippoWindowClass     *window_class);
static void      hippo_window_win_finalize            (GObject              *object);

static void hippo_window_win_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_window_win_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);


/* Window methods */
static void     hippo_window_win_set_contents       (HippoWindow     *window,
                                                     HippoCanvasItem *item);
static void     hippo_window_win_set_visible        (HippoWindow     *window,
                                                     gboolean         visible);
static void     hippo_window_win_set_position       (HippoWindow     *window,
                                                     int              x,
                                                     int              y);
static void     hippo_window_win_get_size           (HippoWindow     *window,
                                                     int             *width_p,
                                                     int             *height_p);
static void     hippo_window_win_set_resizable      (HippoWindow     *window,
                                                     HippoOrientation orientation,
                                                     gboolean         value);
static void     hippo_window_win_begin_resize_drag  (HippoWindow     *window,
                                                     HippoSide        side,
                                                     HippoEvent      *event);

/* internal stuff */

struct _HippoWindowWin {
    GObject object;
    HippoWindowImpl *impl;
};

struct _HippoWindowWinClass {
    GObjectClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoWindowWin, hippo_window_win, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_WINDOW, hippo_window_win_iface_init));

static void
hippo_window_win_iface_init(HippoWindowClass *window_class)
{
    window_class->set_contents = hippo_window_win_set_contents;
    window_class->set_visible = hippo_window_win_set_visible;
    window_class->set_position = hippo_window_win_set_position;
    window_class->get_size = hippo_window_win_get_size;
    window_class->set_resizable = hippo_window_win_set_resizable;
    window_class->begin_resize_drag = hippo_window_win_begin_resize_drag;
}

static void
hippo_window_win_class_init(HippoWindowWinClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_window_win_set_property;
    object_class->get_property = hippo_window_win_get_property;

    object_class->finalize = hippo_window_win_finalize;
}

static void
hippo_window_win_init(HippoWindowWin *window_win)
{
    window_win->impl = new HippoWindowImpl();
}

static void
hippo_window_win_finalize(GObject *object)
{
    HippoWindowWin *win = HIPPO_WINDOW_WIN(object);

    win->impl->Release();

    G_OBJECT_CLASS(hippo_window_win_parent_class)->finalize(object);
}

HippoWindow*
hippo_window_win_new(HippoUI *ui)
{
    HippoWindowWin *win;

    win = HIPPO_WINDOW_WIN(g_object_new(HIPPO_TYPE_WINDOW_WIN,
                           NULL));
    win->impl->setUI(ui);

    return HIPPO_WINDOW(win);
}

static void
hippo_window_win_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoWindowWin *win;

    win = HIPPO_WINDOW_WIN(object);

    switch (prop_id) {

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_window_win_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoWindowWin *win;

    win = HIPPO_WINDOW_WIN (object);

    switch (prop_id) {

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_window_win_set_contents(HippoWindow     *window,
                              HippoCanvasItem *item)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setContents(item);
}

static void
hippo_window_win_set_visible(HippoWindow     *window,
                             gboolean         visible)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setVisible(visible != FALSE);
}


static void
hippo_window_win_set_position(HippoWindow     *window,
                              int              x,
                              int              y)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setPosition(x, y);
}

static void
hippo_window_win_get_size(HippoWindow     *window,
                          int             *width_p,
                          int             *height_p)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->getSize(width_p, height_p);
}

static void
hippo_window_win_set_resizable(HippoWindow      *window,
                               HippoOrientation  orientation,
                               gboolean          value)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

    window_win->impl->setResizable(orientation, value != FALSE);
}

static void
hippo_window_win_begin_resize_drag(HippoWindow      *window,
                                   HippoSide         side,
                                   HippoEvent       *event)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    
    // the coordinates in the HippoEvent are on any random canvas item,
    // so not useful.
    window_win->impl->beginResize(side);
}

void
HippoWindowImpl::destroy()
{
    contentsControl_ = NULL;
    HippoAbstractControl::destroy();
}

void
HippoWindowImpl::setContents(HippoCanvasItem *item)
{
    contents_ = item;
    contentsControl_->setRoot(item);
}

bool
HippoWindowImpl::create()
{
    bool result;

    // we need to create the parent window first
    result = HippoAbstractControl::create();

    contentsControl_->create();

    return result;
}

void
HippoWindowImpl::show(bool activate)
{
    create();              // so we can do the resizing
    contentsControl_->show(false);
    idleResize();          // so we have the right size prior to showing
    HippoAbstractControl::show(activate);
}

void
HippoWindowImpl::queueResize()
{
    resizeIdle_.add(slot(this, &HippoWindowImpl::idleResize), (G_PRIORITY_DEFAULT_IDLE + G_PRIORITY_LOW) / 2);
}

bool
HippoWindowImpl::idleResize()
{
    //g_debug("idleResize");
    int w = getWidthRequest();
    int h = getHeightRequest(w);

    int oldW = getWidth();
    int oldH = getHeight();

    int newW, newH;

    if (isHResizable()) {
        newW = MAX(w, oldW);
    } else {
        newW = w;
    }

    if (isVResizable()) {
        newH = MAX(h, oldH);
    } else {
        newH = h;
    }

    resize(newW, newH);

    // kind of a lame hack - if we short-circuited the 
    // size allocate, send it out anyway since a resize
    // was queued
    if (newW == oldW && newH == oldH) {
        onSizeChanged();
    }

    resizeIdle_.remove();

    return false; // remove idle
}

void
HippoWindowImpl::setVisible(bool visible)
{
    if (visible) {
        show(false);
    } else {
        hide();
    }
}

void
HippoWindowImpl::setPosition(int x, int y)
{
    move(x, y);
}

void
HippoWindowImpl::getSize(int *width_p, int *height_p)
{
    if (width_p)
        *width_p = getWidth();
    if (height_p)
        *height_p = getHeight();
}

void
HippoWindowImpl::beginResize(HippoSide side)
{
    // Simulate a button click on the window frame.
    // There's an official WM_SYSCOMMAND to start a resize, but it's the 
    // keyboard resize as if you chose it from the window menu.

    WPARAM wParam;
    switch (side) {
        case HIPPO_SIDE_LEFT:
            wParam = HTLEFT;
            break;
        case HIPPO_SIDE_TOP:
            wParam = HTTOP;
            break;
        case HIPPO_SIDE_RIGHT:
            wParam = HTRIGHT;
            break;
        case HIPPO_SIDE_BOTTOM:
            wParam = HTBOTTOM;
            break;
        default:
            g_warning("bad window side");
            return;
    }
    
    // this is slightly bogus, since there may not be a message for 
    // GetMessagePos() to get the coords from, but I think it will work
    // for when we use it. Otherwise we probably need to add HippoWindow coords
    // to HippoEvent. It seems a bit like these coords aren't used anyway - 
    // I had them totally wrong at one point and things still worked.
    DefWindowProc(window_, WM_NCLBUTTONDOWN, wParam, GetMessagePos());
}

int
HippoWindowImpl::getWidthRequestImpl()
{
    return contentsControl_->getWidthRequest();
}

int
HippoWindowImpl::getHeightRequestImpl(int forWidth)
{
    return contentsControl_->getHeightRequest(forWidth);
}

void
HippoWindowImpl::onSizeChanged()
{
    contentsControl_->resize(getWidth(), getHeight());
}

bool 
HippoWindowImpl::processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    switch (message) {
        case WM_PAINT:
            RECT region;
            if (GetUpdateRect(window_, &region, true)) {
                PAINTSTRUCT paint;
                HDC hdc = BeginPaint(window_, &paint);
#if 0
                FillRect(hdc, &region, (HBRUSH) (COLOR_WINDOW+1));

                TextOut(hdc, 0, 0, L"Hello, Windows!", 15);
#endif
                EndPaint(window_, &paint);
            }
            return true;
    }

    return HippoAbstractControl::processMessage(message, wParam, lParam);
}
