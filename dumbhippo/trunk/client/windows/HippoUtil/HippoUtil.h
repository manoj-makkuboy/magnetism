#pragma once

#include "stdafx.h"
#include <ole2.h>
#include <assert.h>
#include <strsafe.h>
#include <new>
#include <string>
#include <sstream>
#include "HippoUtil_h.h"

#ifdef min
#error "min macro should not be defined"
#endif

#ifdef BUILDING_HIPPO_UTIL
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT __declspec(dllimport)
#endif

class HResultException : public std::exception {
private:
    HRESULT hResult_;
    mutable std::string what_;

    HResultException() {
    }

    void appendHResultMessage() const {
        try {
            char *s;

            std::ostringstream ost;

            if (what_.size() > 0) {
                ost << what_;
                ost << ": ";
            }

            if (!FormatMessageA (FORMAT_MESSAGE_ALLOCATE_BUFFER | 
                FORMAT_MESSAGE_FROM_SYSTEM,
                NULL,
                hResult_,
                MAKELANGID (LANG_NEUTRAL, SUBLANG_DEFAULT),
                reinterpret_cast<LPSTR>(&s),
                0, NULL)) {
                ost << "HRESULT: Failed to format hresult (";
                ost << hResult_ << ")";
            } else {
                ost << "HRESULT: ";
                ost << s;
                ost << " (";
                ost << hResult_ << ")";
                LocalFree (s);
            }
            what_ = ost.str();
        } catch (std::bad_alloc) {
            return;
        }
    }

public:

    HRESULT result() const { 
        return hResult_;
    }

    virtual const char* what() const throw() {
        if (what_.empty()) {
            appendHResultMessage();
        }
        return what_.c_str();
    }
    
    HResultException(HRESULT hResult)
        : hResult_(hResult)
    {
    }

    HResultException(HRESULT hResult, const std::string & what)
        : hResult_(hResult), what_(what)
    {
        appendHResultMessage();
    }
};

#define HIPPO_DECLARE_REFCOUNTING           \
    STDMETHODIMP_(DWORD) AddRef();          \
    STDMETHODIMP_(DWORD) Release()

#define HIPPO_DEFINE_REFCOUNTING(C) \
   STDMETHODIMP_(DWORD)             \
   C::AddRef()                      \
   {                                \
       return ++refCount_;          \
   }                                \
                                    \
   STDMETHODIMP_(DWORD)             \
   C::Release()                     \
   {                                \
       refCount_--;                 \
                                    \
        if (refCount_ != 0)         \
            return refCount_;       \
                                    \
        delete this;                \
        return 0;                   \
    }

/*
 * Define a very simple set of smart pointers modelled after the ATL CComPtr, CComQIPtr
 *
 * We do this to avoid dragging in ATL; if we later decide that we actually wanted
 * to use ATL, a simple search-and-replace should fix things up.
 */
template<class T>
class HippoPtr
{
public:
    HippoPtr() : raw_(0) {
    }
    HippoPtr(T *t) : raw_(t) {
        if (raw_)
            raw_->AddRef();
    }
	HippoPtr(const HippoPtr &other) : raw_(0) {
		assign(other.raw_);
	}
    ~HippoPtr() {
        if (raw_) {
            raw_->Release();
        }
    }
    operator T *(){
        return raw_;
    }
    operator const T *() const{
        return raw_;
    }

    T* operator->() {
        return raw_;
    }
	const T* operator->() const {
        return raw_;
    }

    T **operator&() {
        assert(raw_ == NULL);
        return &raw_;
    }
    HippoPtr& operator=(T *t) {
		assign(t);
		return *this;
    }
    HippoPtr& operator=(const HippoPtr &other) {
		assign(other.raw_);
		return *this;
    }

protected:
    T *raw_;

private:
	void assign(T *t) {
		// ref first to protect against self-assignment
		if (t)
			t->AddRef();
        if (raw_)
            raw_->Release();
        raw_ = t;
	}
};

template<class T, const IID *piid = &__uuidof(T)>
class HippoQIPtr : public HippoPtr<T> 
{
public:
    HippoQIPtr(IUnknown *unknown) : HippoPtr<T>() {
        if (unknown)
            unknown->QueryInterface(*piid, (LPVOID *)&raw_);
    }
};

// Very simple version of CComBSTR

class HippoBSTR
{
private:
    void assign(const OLECHAR *str, int len = -1) {
        if (str != m_str) {
            BSTR copy;
            if (str != 0) {
                if (len >= 0)
                    copy = ::SysAllocStringLen(str, len);
                else
                    copy = ::SysAllocString(str);
                if (copy == 0)
                    throw std::bad_alloc();
            } else {
                copy = 0;
            }
            ::SysFreeString(m_str);
            m_str = copy;
        }
    }

public:
    HippoBSTR() : m_str(0) {
    }

    HippoBSTR(const OLECHAR *str) throw (std::bad_alloc)
        : m_str(0) {
        assign(str);
    }

    HippoBSTR(const HippoBSTR &str) throw (std::bad_alloc)
        : m_str(0){
            assign(str.m_str);
    }

    HippoBSTR(unsigned int   len,
              const OLECHAR *str) throw (std::bad_alloc)
        : m_str(0) {
            assign(str, len);
    }

    ~HippoBSTR() {
        if (m_str)
            ::SysFreeString(m_str);
    }

    void Append(const OLECHAR *str) throw (std::bad_alloc) {
        UINT oldlen = ::SysStringLen(m_str);
        size_t appendlen = wcslen(str);
        
        if (oldlen + appendlen >= oldlen) {// check for overflow
            if (::SysReAllocStringLen(&m_str, m_str, oldlen + appendlen)) {
                memcpy(m_str + oldlen, str, sizeof(OLECHAR) * (appendlen + 1));
            } else {
                throw std::bad_alloc();
            }
        } else {
            throw std::bad_alloc();
        }
    }

    void Append(const HippoBSTR &str) throw (std::bad_alloc) {
		Append(str.m_str);
	}

    void Append(OLECHAR c) throw (std::bad_alloc) {
        OLECHAR str[2];
        str[0] = c;
        str[1] = 0;
        return Append(str);
    }

    void CopyTo(BSTR *str) const throw (std::bad_alloc) {
        if (m_str) {
            *str = ::SysAllocString(m_str);
            if (*str == 0)
                throw std::bad_alloc();
        } else {
            *str = 0;
        }
    }

    unsigned int Length() const {
        if (m_str == 0)
            return 0;
        else
            return ::SysStringLen(m_str);
    }

    void setUTF8(const char *utf8) throw (std::bad_alloc) {
        setUTF8(utf8, -1);
    }

    void setUTF8(const char *utf8, int len) throw (std::bad_alloc, HResultException) {
        if (utf8 == 0) {
            throw HResultException(E_INVALIDARG, "null UTF-8 string");
        }
        if (len < 0)
            len = static_cast<int>(strlen(utf8));

        if (len == 0) {
            assign(L"");
            return;
        }
        // "len" is WITHOUT nul. That means that MultiByteToWideChar will not 
        // nul-terminate the wide char string it returns.

        int reqlen = MultiByteToWideChar(CP_UTF8, 0, utf8, len, NULL, 0);
        if (reqlen == 0)
            throw HResultException(GetLastError(), "MultiByteToWideChar returned 0");
        WCHAR *buf = new WCHAR[reqlen+1]; // add 1 so we can nul-terminate.
        buf[reqlen] = 0; // nul-terminate
        int ret = MultiByteToWideChar(CP_UTF8, 0, utf8, len, buf, reqlen);
        assert(buf[reqlen] == 0); // still nul
        if (ret == 0) {
            delete [] buf;
            throw HResultException(GetLastError(), "MultiByteToWideChar returned 0");
        }
        assert(ret == reqlen);
        assign(buf, ret);
        delete [] buf;
    }

#if 1
    // FIXME this is almost certainly a Bad Idea. Just type .m_str, just like std::string::c_str()
    // unfortunately it's already used all over the place so fixing it is sort of a PITA
    // an example of what this breaks is "HippoBSTR s; if (s == 0) {}" since the implicit 
    // conversion makes it ambiguous whether to use the conversion or operator== (or something)
    operator BSTR () {
        return m_str;
    }
#endif

    BSTR *operator&() {
        assert(m_str == 0);
        return &m_str;
    }

    HippoBSTR & operator=(const OLECHAR *str) throw (std::bad_alloc) {
        assign(str);
        return *this;
    }
    
    HippoBSTR & operator=(const HippoBSTR &other) throw (std::bad_alloc) {
        assign(other.m_str);
        return *this;
    }

    bool operator==(const HippoBSTR& other) const {
        if (&other == this)
            return true;
        if ((m_str != 0) != (other.m_str != 0))
            return false;
        if (m_str == 0)
            return true;
        if (::SysStringLen(m_str) != ::SysStringLen(other.m_str)) // O(1) for a BSTR
            return false;
        return wcscmp(m_str, other.m_str) == 0;
    }

    BSTR m_str;
};

inline bool
hippoHresultToString(HRESULT hr, HippoBSTR &str)
{
    WCHAR *buf;

    if (!FormatMessageW (FORMAT_MESSAGE_ALLOCATE_BUFFER | 
                         FORMAT_MESSAGE_FROM_SYSTEM,
                         NULL,
                         hr,
                         MAKELANGID (LANG_NEUTRAL, SUBLANG_DEFAULT),
                         (LPWSTR) &buf,
                         0, NULL)) 
    {
        return false;
    } else {
        str = (WCHAR*) buf;
        LocalFree (buf);
        return true;
    }
}

DLLEXPORT void
hippoDebug(WCHAR *format, ...);

DLLEXPORT void 
hippoDebugLastErr(WCHAR *fmt, ...);

DLLEXPORT void 
hippoDebugLogW(const WCHAR *format, ...); // UTF-16

DLLEXPORT void 
hippoDebugLogU(const char *format, ...);  // UTF-8

/* Avoid (incorrect) warnings that you get because SetWindowLongPtr()
 * is #defined to SetWindowLong() on win32
 */
#pragma warning(push)
#pragma warning(disable : 4244 4312)
template <class T>
inline void hippoSetWindowData(HWND window, T *data)
{
    // Could do SetLastError(0) ... GetLastError() to detect error here
    SetWindowLongPtr(window, GWLP_USERDATA, (::LONG_PTR)data);
}

template <class T>
inline T *hippoGetWindowData(HWND window)
{
    return (T *)GetWindowLongPtr(window, GWLP_USERDATA);
}
#pragma warning(pop)

// Put here to avoid circular references between HippoUI and HippoIEWindow
class HippoMessageHook
{
public:
    virtual bool hookMessage(MSG *msg) = 0;
};

DLLEXPORT HRESULT hippoLoadTypeInfo(const WCHAR *libraryName, 
                                    ...);

DLLEXPORT HRESULT  hippoLoadRegTypeInfo(const GUID    &libraryId, 
                                        unsigned short majorVersion, 
                                        unsigned short minorVersion 
                                        ...);
