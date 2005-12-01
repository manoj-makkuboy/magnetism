/* HippoBubble.cpp: Display notifications
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#import <msxml3.dll>  named_guids

#include <mshtml.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include "HippoUI.h"
#include "HippoIE.h"
#include <HippoUtil.h>
#include "HippoBubble.h"
#include "Guid.h"

static const TCHAR *CLASS_NAME = TEXT("HippoBubbleClass");
static const int BASE_WIDTH = 400;
static const int BASE_HEIGHT = 150;

using namespace MSXML2;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoBubble::HippoBubble(void)
{
    refCount_ = 1;
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    idle_ = FALSE;
    viewerSpace_ = 0;
    ie_ = NULL;

    HippoPtr<ITypeLib> typeLib;
    HRESULT hr = LoadRegTypeLib(LIBID_HippoUtil, 
                                0, 1, /* Version */
                                0,    /* LCID */
                                &typeLib);
    if (SUCCEEDED (hr)) {
        typeLib->GetTypeInfoOfGuid(IID_IHippoBubble, &ifaceTypeInfo_);
        typeLib->GetTypeInfoOfGuid(CLSID_HippoBubble, &classTypeInfo_);
    } else
        hippoDebug(L"Failed to load type lib: %x\n", hr);
}

HippoBubble::~HippoBubble(void)
{
}

void 
HippoBubble::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoBubble::createWindow(void)
{
    window_ = CreateWindowEx(WS_EX_TOPMOST, CLASS_NAME, L"Hippo Notification", WS_POPUP,
                             CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                             NULL, NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }
    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    moveResizeWindow();

    hippoSetWindowData<HippoBubble>(window_, this);

    return true;
}

void
HippoBubble::moveResizeWindow() 
{
    int width = BASE_WIDTH;
    int height = BASE_HEIGHT + viewerSpace_;

    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    MoveWindow(window_, 
               (desktopRect.right - width), (desktopRect.bottom - height), 
               width, height, 
               TRUE);

    if (ie_) {
        RECT rect;

        rect.top = 0;
        rect.left = 0;
        rect.bottom = height;
        rect.right = width;

        HippoQIPtr<IOleInPlaceObject> inPlace = ie_;
        inPlace->SetObjectRects(&rect, &rect);
    }
}

bool
HippoBubble::embedIE(void)
{
    RECT rect;
    GetClientRect(window_,&rect);
    OleCreate(CLSID_WebBrowser,IID_IOleObject,OLERENDER_DRAW,0,this,this,(void**)&ie_);
    HippoQIPtr<IWebBrowser2> browser(ie_);
    browser_ = browser;
    ie_->SetHostNames(L"Web Host",L"Web View");
    OleSetContainedObject(ie_,TRUE);

    ie_->DoVerb(OLEIVERB_SHOW,NULL,this,-1,window_,&rect);

    VARIANT url;
    url.vt = VT_BSTR;
    url.bstrVal = L"about:blank";
    VARIANT vempty;
    vempty.vt = VT_EMPTY;

    browser->Navigate2(&url, &vempty, &vempty, &vempty, &vempty);
    browser->put_Resizable(VARIANT_FALSE);

    appendTransform(L"notification.xml", L"clientstyle.xml", NULL);

    // Kind of a hack
    HippoBSTR serverURLStr;
    ui_->getRemoteURL(HippoBSTR(L""), &serverURLStr);
    HippoBSTR appletURLStr;
    ui_->getAppletURL(HippoBSTR(L""), &appletURLStr);
    variant_t serverUrl(serverURLStr.m_str);
    variant_t appletUrl(appletURLStr.m_str);
    variant_t result;
    invokeJavascript(L"dhInit", &result, 2, &serverUrl, &appletUrl);

    // Set the initial value of the idle state
    setIdle(idle_);

    return true;
}

bool
HippoBubble::appendTransform(BSTR src, BSTR style, ...)
{
    va_list vap;
    va_start (vap, style);

    variant_t xmlResult;
    MSXML2::IXMLDOMDocumentPtr xmlsrc(MSXML2::CLSID_DOMDocument);
    MSXML2::IXMLDOMDocumentPtr clientXSLT(CLSID_FreeThreadedDOMDocument);
    try {
        HippoBSTR xmlsrcUrl;
        ui_->getAppletURL(src, &xmlsrcUrl);
        
        xmlResult = xmlsrc->load(xmlsrcUrl.m_str);

        HippoBSTR styleSrcUrl;
        ui_->getAppletURL(style, &styleSrcUrl); 
        xmlResult = clientXSLT->load(styleSrcUrl.m_str);
    } catch(_com_error &e) {
        hippoDebug(L"Error loading XML files : %s\n",
                   (const char*)_bstr_t(e.Description()));
    }

    IXSLProcessorPtr processor;
    IXSLTemplatePtr xsltTemplate(CLSID_XSLTemplate);
    try{
        xmlResult = xsltTemplate->putref_stylesheet(clientXSLT);
        processor = xsltTemplate->createProcessor();
    } catch(_com_error &e) {
        hippoDebug(L"Error setting XSL style sheet : %s\n", (const char*)_bstr_t(e.Description()));
    }
    IStream *iceCream;
    CreateStreamOnHGlobal(NULL,TRUE,&iceCream); // This is equivalent to Java's StringWriter
    processor->put_output(_variant_t(iceCream));

    xmlResult = processor->put_input(_variant_t(static_cast<IUnknown*>(xmlsrc)));

    // Always set appleturl as parameter
    HippoBSTR appletURL;
    ui_->getAppletURL(L"", &appletURL);
    processor->addParameter(_bstr_t("appleturl"), variant_t(appletURL.m_str), _bstr_t(""));
    // Append all the other specified parameters
    {
        WCHAR* key;
        WCHAR* val;
        while ((key = va_arg (vap, WCHAR *)) != NULL) {
            val = va_arg (vap, WCHAR *);
            processor->addParameter(key, variant_t(val), _bstr_t(""));
        }
    }
    processor->transform();

    // Append NUL character so we can treat it as C string
    iceCream->Write((void const*)"\0",1,0);
    // Retrieve buffer, written in whatever encoding the XSLT stylesheet specified
    HGLOBAL hg = NULL;
    void *buf;
    GetHGlobalFromStream(iceCream, &hg);
    buf = GlobalLock(hg);

    HippoQIPtr<IWebBrowser2> browser(ie_);
    IDispatch *docDispatch;
    browser->get_Document(&docDispatch);
    HippoQIPtr<IHTMLDocument2> doc(docDispatch);
    HRESULT hresult = S_OK;
    VARIANT *param;
    SAFEARRAY *sfArray;
    HippoBSTR actualData;

    // The XSLT must have written out UTF-8.  Otherwise we lose...
    actualData.setUTF8((LPSTR) buf);

    // I have no idea why the write method takes an array of variants...crack!
    // We create a singleton array of a variant with a BSTR value
    sfArray = SafeArrayCreateVector(VT_VARIANT, 0, 1);
    hresult = SafeArrayAccessData(sfArray,(LPVOID*) & param);
    param->vt = VT_BSTR;
    param->bstrVal = actualData;
    hresult = SafeArrayUnaccessData(sfArray);
    // Append the transformed XML to the document
    hresult = doc->write(sfArray);

    GlobalFree(hg);

    va_end (vap);
    return true;
}

bool
HippoBubble::create(void)
{
    if (window_ != NULL) {
        return true;
    }
    if (!registerClass()) {
        ui_->debugLogW(L"Failed to register window class");
        return false;
    }
    if (!createWindow()) {
        ui_->debugLogW(L"Failed to create window");
        return false;
    }
    if (!embedIE()) {
        ui_->debugLogW(L"Failed to embed IE");
        return false;
    }
    return true;
}

static SAFEARRAY *
hippoStrArrayToSafeArray(HippoArray<HippoBSTR> &args)
{
    // I swear the SAFEARRAY API was *designed* to be painful
    SAFEARRAYBOUND dim[1];
    dim[0].lLbound= 0;
    dim[0].cElements = args.length();
    SAFEARRAY *ret = SafeArrayCreate(VT_VARIANT, 1, dim);
    for (unsigned int i = 0; i < args.length(); i++) {
        VARIANT *data;
        _variant_t argv;
        SafeArrayAccessData(ret, (void**)&data);
        argv.vt = VT_BSTR;
        argv.bstrVal = args[i];
        VariantCopy(&(data[i]), &argv);
        SafeArrayUnaccessData(ret);
    }
    return ret;
}

void 
HippoBubble::setLinkNotification(HippoLinkShare &share)
{
    if (window_ == NULL) {
        ui_->debugLogW(L"Creating new window");
        if (!create()) {
            ui_->debugLogW(L"Failed to create window");
            return;
        }
    }

    variant_t senderName(share.senderName);
    variant_t senderId(share.senderId);
    variant_t postId(share.postId);
    variant_t linkTitle(share.title);
    variant_t linkURL(share.url);
    variant_t linkDescription(share.description);
    SAFEARRAY *personRecipients = hippoStrArrayToSafeArray(share.personRecipients);
    VARIANT personRecipientsArg;
    personRecipientsArg.vt = VT_ARRAY | VT_VARIANT;
    personRecipientsArg.parray = personRecipients;
    SAFEARRAY *groupRecipients = hippoStrArrayToSafeArray(share.groupRecipients);
    VARIANT groupRecipientsArg;
    groupRecipientsArg.vt = VT_ARRAY | VT_VARIANT;
    groupRecipientsArg.parray = groupRecipients;
    SAFEARRAY *viewers = hippoStrArrayToSafeArray(share.viewers);
    VARIANT viewersArg;
    viewersArg.vt = VT_ARRAY | VT_VARIANT;
    viewersArg.parray = viewers;

    VARIANT result;
    ui_->debugLogW(L"Invoking dhAddLinkShare");
    invokeJavascript(L"dhAddLinkShare", &result, 9, &senderName,
                     &senderId, &postId, &linkTitle, &linkURL, &linkDescription,
                     &personRecipientsArg, &groupRecipientsArg, &viewersArg);
    SafeArrayDestroy(personRecipients);
    SafeArrayDestroy(groupRecipients);

    show();
}

bool
HippoBubble::invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...)
{
    va_list args;
    va_start (args, nargs);
    HRESULT result = HippoIE::invokeJavascript(browser_, funcName, invokeResult, nargs, args);
    bool ret = SUCCEEDED(result);
    if (!ret)
        ui_->logError(L"failed to invoke javascript", result);
    va_end (args);
    return ret;
}

void 
HippoBubble::setIdle(bool idle)
{
    // Note that we count on this not short-circuiting at window creation time,
    // where we call it with idle_ as the parameter to pass the value to the
    // Javascript

    idle_ = idle;

    if (window_) {
        variant_t idleVariant(idle);
        variant_t result;
    
        HippoIE::invokeJavascript(browser_, HippoBSTR(L"dhSetIdle"), &result, 1, &idleVariant);
    }
}

bool
HippoBubble::registerClass()
{
    WNDCLASSEX wcex;

    ZeroMemory(&wcex, sizeof(WNDCLASSEX));
    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = windowProc;
    wcex.cbClsExtra = 0;
    wcex.cbWndExtra = 0;
    wcex.hInstance  = instance_;
    wcex.hCursor    = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = CLASS_NAME;

    if (RegisterClassEx(&wcex) == 0) {
        if (GetClassInfoEx(instance_, CLASS_NAME, &wcex) != 0)
            return true;
        return false;
    }
    return true;
}

void
HippoBubble::show(void) 
{   
    ui_->debugLogW(L"doing bubble show");
    //if (!AnimateWindow(window_, 400, AW_BLEND))
    //  ui_->logLastError(L"Failed to invoke AnimateWindow");
    if (!ShowWindow(window_, SW_SHOW))
        ui_->logLastError(L"Failed to invoke ShowWindow");
    if (!RedrawWindow(window_, NULL, NULL, RDW_UPDATENOW))
        ui_->logLastError(L"Failed to invoke RedrawWindow");
    if (!BringWindowToTop(window_))
        ui_->logLastError(L"Failed to invoke BringWindowToTop");
}


bool
HippoBubble::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    switch (message) 
    {
    case WM_CLOSE:
        Close();
        return true;
    default:
        return false;
    }
}

LRESULT CALLBACK 
HippoBubble::windowProc(HWND   window,
                        UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    HippoBubble *bubbleWindow = hippoGetWindowData<HippoBubble>(window);
    if (bubbleWindow) {
        if (bubbleWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

// IHippoBubble

STDMETHODIMP
HippoBubble::DebugLog(BSTR str)
{
    ui_->debugLogW(L"%s", str);
    return S_OK;
}

STDMETHODIMP 
HippoBubble::DisplaySharedLink(BSTR linkId)
{
    ui_->displaySharedLink(linkId);
    return S_OK;
}

STDMETHODIMP
HippoBubble::OpenExternalURL(BSTR url)
{
    HippoPtr<IWebBrowser2> browser;
    ui_->launchBrowser(url, browser);
    return S_OK;
}

STDMETHODIMP
HippoBubble::Close()
{
    AnimateWindow(window_, 200, AW_BLEND | AW_HIDE);
    ui_->debugLogU("closing link notification");
    return S_OK;
}

STDMETHODIMP 
HippoBubble::SetViewerSpace(DWORD viewerSpace)
{
    if (viewerSpace != viewerSpace_) {
        viewerSpace_ = viewerSpace;
        if (window_)
            moveResizeWindow();
    }

    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetXmlHttp(IXMLHttpRequest **request)
{
    CoCreateInstance(CLSID_XMLHTTPRequest, NULL, CLSCTX_INPROC,
        IID_IXMLHTTPRequest, (void**) request);
    return S_OK;
}

// IDocHostUIExternal
STDMETHODIMP 
HippoBubble::EnableModeless(BOOL enable)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::FilterDataObject(IDataObject *dobj, IDataObject **dobjRet)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetDropTarget(IDropTarget *dropTarget, IDropTarget **dropTargetRet)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetExternal(IDispatch **dispatch)
{
    *dispatch = this;
    this->AddRef();
    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetHostInfo(DOCHOSTUIINFO *info)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetOptionKeyPath(LPOLESTR *chKey, DWORD dw)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::HideUI(VOID)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::OnDocWindowActivate(BOOL activate)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::OnFrameWindowActivate(BOOL activate)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::ResizeBorder(LPCRECT border, IOleInPlaceUIWindow *uiWindow, BOOL frameWindow)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::ShowContextMenu(DWORD id, POINT *pt, IUnknown *cmdtReserved, IDispatch *dispReserved)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::ShowUI(DWORD id, IOleInPlaceActiveObject *activeObject, IOleCommandTarget *commandTarget, IOleInPlaceFrame *frame, IOleInPlaceUIWindow *doc)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::TranslateAccelerator(LPMSG msg, const GUID *guidCmdGroup, DWORD cmdID)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::TranslateUrl(DWORD translate, OLECHAR *chURLIn, OLECHAR **chURLOut)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::UpdateUI(VOID)
{
    return S_OK;
}

// IStorage
STDMETHODIMP 
HippoBubble::CreateStream(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStream ** ppstm)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::OpenStream(const WCHAR * pwcsName,void * reserved1,DWORD grfMode,DWORD reserved2,IStream ** ppstm)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::CreateStorage(const WCHAR * pwcsName,DWORD grfMode,DWORD reserved1,DWORD reserved2,IStorage ** ppstg)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::OpenStorage(const WCHAR * pwcsName,IStorage * pstgPriority,DWORD grfMode,SNB snbExclude,DWORD reserved,IStorage ** ppstg)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::CopyTo(DWORD ciidExclude,IID const * rgiidExclude,SNB snbExclude,IStorage * pstgDest)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::MoveElementTo(const OLECHAR * pwcsName,IStorage * pstgDest,const OLECHAR* pwcsNewName,DWORD grfFlags)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::Commit(DWORD grfCommitFlags)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::Revert(void)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::EnumElements(DWORD reserved1,void * reserved2,DWORD reserved3,IEnumSTATSTG ** ppenum)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::DestroyElement(const OLECHAR * pwcsName)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::RenameElement(const WCHAR * pwcsOldName,const WCHAR * pwcsNewName)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::SetElementTimes(const WCHAR * pwcsName,FILETIME const * pctime,FILETIME const * patime,FILETIME const * pmtime)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::SetClass(REFCLSID clsid)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::SetStateBits(DWORD grfStateBits,DWORD grfMask)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::Stat(STATSTG * pstatstg,DWORD grfStatFlag)
{
    NOTIMPLEMENTED;
}

// IOleWindow

STDMETHODIMP 
HippoBubble::GetWindow(HWND FAR* lphwnd)
{
    *lphwnd = window_;

    return S_OK;
}

STDMETHODIMP 
HippoBubble::ContextSensitiveHelp(BOOL fEnterMode)
{
    NOTIMPLEMENTED;
}

// IOleInPlaceUIWindow
STDMETHODIMP 
HippoBubble::GetBorder(LPRECT lprectBorder)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::RequestBorderSpace(LPCBORDERWIDTHS pborderwidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::SetBorderSpace(LPCBORDERWIDTHS pborderwidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::SetActiveObject(IOleInPlaceActiveObject *pActiveObject,LPCOLESTR pszObjName)
{
    return S_OK;
}


// IOleInPlaceFrame
STDMETHODIMP 
HippoBubble::InsertMenus(HMENU hmenuShared,LPOLEMENUGROUPWIDTHS lpMenuWidths)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::SetMenu(HMENU hmenuShared,HOLEMENU holemenu,HWND hwndActiveObject)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::RemoveMenus(HMENU hmenuShared)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::SetStatusText(LPCOLESTR pszStatusText)
{
    return S_OK;
}

// EnableModeless already covered

STDMETHODIMP HippoBubble::TranslateAccelerator(  LPMSG lpmsg,WORD wID)
{
    NOTIMPLEMENTED;
}

// IOleClientSite

STDMETHODIMP 
HippoBubble::SaveObject()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::GetMoniker(DWORD dwAssign,DWORD dwWhichMoniker,IMoniker ** ppmk)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::GetContainer(LPOLECONTAINER FAR* ppContainer)
{
    // We are a simple object and don't support a container.
    *ppContainer = NULL;

    return E_NOINTERFACE;
}

STDMETHODIMP 
HippoBubble::ShowObject()
{
    return NOERROR;
}

STDMETHODIMP 
HippoBubble::OnShowWindow(BOOL fShow)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::RequestNewObjectLayout()
{
    NOTIMPLEMENTED;
}

// IOleInPlaceSite


STDMETHODIMP 
HippoBubble::CanInPlaceActivate()
{
    // Yes we can
    return S_OK;
}

STDMETHODIMP
HippoBubble::OnInPlaceActivate()
{
    // Why disagree.
    return S_OK;
}

STDMETHODIMP 
HippoBubble::OnUIActivate()
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetWindowContext(
    LPOLEINPLACEFRAME FAR* ppFrame,
    LPOLEINPLACEUIWINDOW FAR* ppDoc,
    LPRECT prcPosRect,
    LPRECT prcClipRect,
    LPOLEINPLACEFRAMEINFO lpFrameInfo)
{
    *ppFrame = this;
    *ppDoc = NULL;
    GetClientRect(window_,prcPosRect);
    GetClientRect(window_,prcClipRect);

    lpFrameInfo->fMDIApp = FALSE;
    lpFrameInfo->hwndFrame = window_;
    lpFrameInfo->haccel = NULL;
    lpFrameInfo->cAccelEntries = 0;

    return S_OK;
}

STDMETHODIMP 
HippoBubble::Scroll(SIZE scrollExtent)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::OnUIDeactivate(BOOL fUndoable)
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::OnInPlaceDeactivate()
{
    return S_OK;
}

STDMETHODIMP 
HippoBubble::DiscardUndoState()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::DeactivateAndUndo()
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::OnPosRectChange(LPCRECT lprcPosRect)
{
    return S_OK;
}

// IParseDisplayName
STDMETHODIMP 
HippoBubble::ParseDisplayName(IBindCtx *pbc,LPOLESTR pszDisplayName,ULONG *pchEaten,IMoniker **ppmkOut)
{
    NOTIMPLEMENTED;
}

// IOleContainer
STDMETHODIMP 
HippoBubble::EnumObjects(DWORD grfFlags,IEnumUnknown **ppenum)
{
    NOTIMPLEMENTED;
}

STDMETHODIMP 
HippoBubble::LockContainer(BOOL fLock)
{
    NOTIMPLEMENTED;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoBubble::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoBubble*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoBubble)) 
        *result = static_cast<IHippoBubble *>(this);
    else if (IsEqualIID(ifaceID, IID_IDocHostUIHandler))
        *result = static_cast<IDocHostUIHandler*>(this);
    else if (IsEqualIID(ifaceID, IID_IOleClientSite))
        *result = static_cast<IOleClientSite*>(this);
    else if (IsEqualIID(ifaceID, IID_IOleInPlaceSite)) // || riid == IID_IOleInPlaceSiteEx || riid == IID_IOleInPlaceSiteWindowless)
        *result = static_cast<IOleInPlaceSite*>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoBubble)


//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

    STDMETHODIMP
HippoBubble::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetTypeInfo(UINT        iTInfo,
                         LCID        lcid,
                         ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ifaceTypeInfo_->AddRef();
    *ppTInfo = ifaceTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoBubble::GetIDsOfNames (REFIID    riid,
                            LPOLESTR *rgszNames,
                            UINT      cNames,
                            LCID      lcid,
                            DISPID   *rgDispId)
{
    HRESULT ret;
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    
    ret = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
    return ret;
}
        
STDMETHODIMP
HippoBubble::Invoke (DISPID        member,
                     const IID    &iid,
                     LCID          lcid,              
                     WORD          flags,
                     DISPPARAMS   *dispParams,
                     VARIANT      *result,
                     EXCEPINFO    *excepInfo,  
                     unsigned int *argErr)
{
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    HippoQIPtr<IHippoBubble> hippoBubble(static_cast<IHippoBubble *>(this));
    HRESULT hr = DispInvoke(hippoBubble, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
