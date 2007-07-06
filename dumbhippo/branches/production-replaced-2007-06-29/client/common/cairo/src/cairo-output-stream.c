/* cairo-output-stream.c: Output stream abstraction
 *
 * Copyright © 2005 Red Hat, Inc
 *
 * This library is free software; you can redistribute it and/or
 * modify it either under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation
 * (the "LGPL") or, at your option, under the terms of the Mozilla
 * Public License Version 1.1 (the "MPL"). If you do not alter this
 * notice, a recipient may use your version of this file under either
 * the MPL or the LGPL.
 *
 * You should have received a copy of the LGPL along with this library
 * in the file COPYING-LGPL-2.1; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * You should have received a copy of the MPL along with this library
 * in the file COPYING-MPL-1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTY
 * OF ANY KIND, either express or implied. See the LGPL or the MPL for
 * the specific language governing rights and limitations.
 *
 * The Original Code is cairo_output_stream.c as distributed with the
 *   cairo graphics library.
 *
 * The Initial Developer of the Original Code is Red Hat, Inc.
 *
 * Author(s):
 *	Kristian Høgsberg <krh@redhat.com>
 */

#include <stdio.h>
#include <locale.h>
#include <ctype.h>
#include <errno.h>
#include "cairoint.h"
#include "cairo-output-stream-private.h"

#ifdef _MSC_VER
#define snprintf _snprintf
#endif /* _MSC_VER */


cairo_private void
_cairo_output_stream_init (cairo_output_stream_t            *stream,
			   cairo_output_stream_write_func_t  write_func,
			   cairo_output_stream_close_func_t  close_func)
{
    stream->write_func = write_func;
    stream->close_func = close_func;
    stream->position = 0;
    stream->status = CAIRO_STATUS_SUCCESS;
    stream->closed = FALSE;
}

cairo_private cairo_status_t
_cairo_output_stream_fini (cairo_output_stream_t *stream)
{
    return _cairo_output_stream_close (stream);
}

const cairo_output_stream_t cairo_output_stream_nil = {
    NULL, /* write_func */
    NULL, /* close_func */
    0,    /* position */
    CAIRO_STATUS_NO_MEMORY,
    FALSE /* closed */
};

static const cairo_output_stream_t cairo_output_stream_nil_write_error = {
    NULL, /* write_func */
    NULL, /* close_func */
    0,    /* position */
    CAIRO_STATUS_WRITE_ERROR,
    FALSE /* closed */
};

typedef struct _cairo_output_stream_with_closure {
    cairo_output_stream_t	 base;
    cairo_write_func_t		 write_func;
    cairo_close_func_t		 close_func;
    void			*closure;
} cairo_output_stream_with_closure_t;


static cairo_status_t
closure_write (cairo_output_stream_t *stream,
	       const unsigned char *data, unsigned int length)
{
    cairo_output_stream_with_closure_t *stream_with_closure =
	(cairo_output_stream_with_closure_t *) stream;

    return stream_with_closure->write_func (stream_with_closure->closure,
					    data, length);
}

static cairo_status_t
closure_close (cairo_output_stream_t *stream)
{
    cairo_output_stream_with_closure_t *stream_with_closure =
	(cairo_output_stream_with_closure_t *) stream;

    if (stream_with_closure->close_func != NULL)
	return stream_with_closure->close_func (stream_with_closure->closure);
    else
	return CAIRO_STATUS_SUCCESS;
}

cairo_output_stream_t *
_cairo_output_stream_create (cairo_write_func_t		write_func,
			     cairo_close_func_t		close_func,
			     void			*closure)
{
    cairo_output_stream_with_closure_t *stream;

    stream = malloc (sizeof (cairo_output_stream_with_closure_t));
    if (stream == NULL)
	return (cairo_output_stream_t *) &cairo_output_stream_nil;

    _cairo_output_stream_init (&stream->base, closure_write, closure_close);
    stream->write_func = write_func;
    stream->close_func = close_func;
    stream->closure = closure;

    return &stream->base;
}

cairo_status_t
_cairo_output_stream_close (cairo_output_stream_t *stream)
{
    cairo_status_t status;

    if (stream->closed)
	return stream->status;

    if (stream == &cairo_output_stream_nil ||
	stream == &cairo_output_stream_nil_write_error)
    {
	return stream->status;
    }

    if (stream->close_func) {
	status = stream->close_func (stream);
	/* Don't overwrite a pre-existing status failure. */
	if (stream->status == CAIRO_STATUS_SUCCESS)
	    stream->status = status;
    }

    stream->closed = TRUE;

    return stream->status;
}

cairo_status_t
_cairo_output_stream_destroy (cairo_output_stream_t *stream)
{
    cairo_status_t status;

    if (stream == NULL)
	return CAIRO_STATUS_NULL_POINTER;

    status = _cairo_output_stream_fini (stream);
    free (stream);

    return status;
}

void
_cairo_output_stream_write (cairo_output_stream_t *stream,
			    const void *data, size_t length)
{
    if (length == 0)
	return;

    if (stream->status)
	return;

    stream->status = stream->write_func (stream, data, length);
    stream->position += length;
}

void
_cairo_output_stream_write_hex_string (cairo_output_stream_t *stream,
				       const char *data,
				       size_t length)
{
    const char hex_chars[] = "0123456789abcdef";
    char buffer[2];
    unsigned int i, column;

    if (stream->status)
	return;

    for (i = 0, column = 0; i < length; i++, column++) {
	if (column == 38) {
	    _cairo_output_stream_write (stream, "\n", 1);
	    column = 0;
	}
	buffer[0] = hex_chars[(data[i] >> 4) & 0x0f];
	buffer[1] = hex_chars[data[i] & 0x0f];
	_cairo_output_stream_write (stream, buffer, 2);
    }
}

/* Format a double in a locale independent way and trim trailing
 * zeros.  Based on code from Alex Larson <alexl@redhat.com>.
 * http://mail.gnome.org/archives/gtk-devel-list/2001-October/msg00087.html
 *
 * The code in the patch is copyright Red Hat, Inc under the LGPL, but
 * has been relicensed under the LGPL/MPL dual license for inclusion
 * into cairo (see COPYING). -- Kristian Høgsberg <krh@redhat.com>
 */

int
_cairo_dtostr (char *buffer, size_t size, double d)
{
  struct lconv *locale_data;
  const char *decimal_point;
  int decimal_point_len;
  char *p;
  int decimal_len;

  snprintf (buffer, size, "%f", d);

  locale_data = localeconv ();
  decimal_point = locale_data->decimal_point;
  decimal_point_len = strlen (decimal_point);

  assert (decimal_point_len != 0);
  p = buffer;

  if (*p == '+' || *p == '-')
      p++;

  while (isdigit (*p))
      p++;

  if (strncmp (p, decimal_point, decimal_point_len) == 0) {
      *p = '.';
      decimal_len = strlen (p + decimal_point_len);
      memmove (p + 1, p + decimal_point_len, decimal_len);
      p[1 + decimal_len] = 0;

      /* Remove trailing zeros and decimal point if possible. */
      for (p = p + decimal_len; *p == '0'; p--)
	  *p = 0;

      if (*p == '.') {
	  *p = 0;
	  p--;
      }
  }

  return p + 1 - buffer;
}

enum {
    LENGTH_MODIFIER_LONG = 0x100
};

/* Here's a limited reimplementation of printf.  The reason for doing
 * this is primarily to special case handling of doubles.  We want
 * locale independent formatting of doubles and we want to trim
 * trailing zeros.  This is handled by dtostr() above, and the code
 * below handles everything else by calling snprintf() to do the
 * formatting.  This functionality is only for internal use and we
 * only implement the formats we actually use.
 */
void
_cairo_output_stream_vprintf (cairo_output_stream_t *stream,
			      const char *fmt, va_list ap)
{
    char buffer[512], single_fmt[32];
    char *p, *end;
    const char *f, *start;
    int length_modifier;

    if (stream->status)
	return;

    f = fmt;
    p = buffer;
    while (*f != '\0') {
	if (p == buffer + sizeof (buffer)) {
	    _cairo_output_stream_write (stream, buffer, sizeof (buffer));
	    p = buffer;
	}

	if (*f != '%') {
	    *p++ = *f++;
	    continue;
	}

	start = f;
	f++;

	if (*f == '0')
	    f++;

	if (isdigit (*f)) {
	    strtol (f, &end, 10);
	    f = end;
	}

	length_modifier = 0;
	if (*f == 'l') {
	    length_modifier = LENGTH_MODIFIER_LONG;
	    f++;
	}

	/* Reuse the format string for this conversion. */
	memcpy (single_fmt, start, f + 1 - start);
	single_fmt[f + 1 - start] = '\0';

	/* Flush contents of buffer before snprintf()'ing into it. */
	_cairo_output_stream_write (stream, buffer, p - buffer);
	p = buffer;

	/* We group signed and unsigned together in this switch, the
	 * only thing that matters here is the size of the arguments,
	 * since we're just passing the data through to sprintf(). */
	switch (*f | length_modifier) {
	case '%':
	    buffer[0] = *f;
	    buffer[1] = 0;
	    break;
	case 'd':
	case 'u':
	case 'o':
	case 'x':
	case 'X':
	    snprintf (buffer, sizeof buffer, single_fmt, va_arg (ap, int));
	    break;
	case 'd' | LENGTH_MODIFIER_LONG:
	case 'u' | LENGTH_MODIFIER_LONG:
	case 'o' | LENGTH_MODIFIER_LONG:
	case 'x' | LENGTH_MODIFIER_LONG:
	case 'X' | LENGTH_MODIFIER_LONG:
	    snprintf (buffer, sizeof buffer,
		      single_fmt, va_arg (ap, long int));
	    break;
	case 's':
	    snprintf (buffer, sizeof buffer,
		      single_fmt, va_arg (ap, const char *));
	    break;
	case 'f':
	    _cairo_dtostr (buffer, sizeof buffer, va_arg (ap, double));
	    break;
	case 'c':
	    buffer[0] = va_arg (ap, int);
	    buffer[1] = 0;
	    break;
	default:
	    ASSERT_NOT_REACHED;
	}
	p = buffer + strlen (buffer);
	f++;
    }

    _cairo_output_stream_write (stream, buffer, p - buffer);
}

void
_cairo_output_stream_printf (cairo_output_stream_t *stream,
			     const char *fmt, ...)
{
    va_list ap;

    va_start (ap, fmt);

    _cairo_output_stream_vprintf (stream, fmt, ap);

    va_end (ap);
}

long
_cairo_output_stream_get_position (cairo_output_stream_t *stream)
{
    return stream->position;
}

cairo_status_t
_cairo_output_stream_get_status (cairo_output_stream_t *stream)
{
    return stream->status;
}

/* Maybe this should be a configure time option, so embedded targets
 * don't have to pull in stdio. */


typedef struct _stdio_stream {
    cairo_output_stream_t	 base;
    FILE			*file;
} stdio_stream_t;

static cairo_status_t
stdio_write (cairo_output_stream_t *base,
	     const unsigned char *data, unsigned int length)
{
    stdio_stream_t *stream = (stdio_stream_t *) base;

    if (fwrite (data, 1, length, stream->file) != length)
	return CAIRO_STATUS_WRITE_ERROR;

    return CAIRO_STATUS_SUCCESS;
}

static cairo_status_t
stdio_flush (cairo_output_stream_t *base)
{
    stdio_stream_t *stream = (stdio_stream_t *) base;

    fflush (stream->file);

    if (ferror (stream->file))
	return CAIRO_STATUS_WRITE_ERROR;
    else
	return CAIRO_STATUS_SUCCESS;
}

static cairo_status_t
stdio_close (cairo_output_stream_t *base)
{
    cairo_status_t status;
    stdio_stream_t *stream = (stdio_stream_t *) base;

    status = stdio_flush (base);

    fclose (stream->file);

    return status;
}

cairo_output_stream_t *
_cairo_output_stream_create_for_file (FILE *file)
{
    stdio_stream_t *stream;

    if (file == NULL)
	return (cairo_output_stream_t *) &cairo_output_stream_nil_write_error;

    stream = malloc (sizeof *stream);
    if (stream == NULL)
	return (cairo_output_stream_t *) &cairo_output_stream_nil;

    _cairo_output_stream_init (&stream->base, stdio_write, stdio_flush);
    stream->file = file;

    return &stream->base;
}

cairo_output_stream_t *
_cairo_output_stream_create_for_filename (const char *filename)
{
    stdio_stream_t *stream;
    FILE *file;

    file = _cairo_fopen (filename, "wb");
    if (file == NULL)
	return (cairo_output_stream_t *) &cairo_output_stream_nil_write_error;

    stream = malloc (sizeof *stream);
    if (stream == NULL) {
	fclose (file);
	return (cairo_output_stream_t *) &cairo_output_stream_nil;
    }

    _cairo_output_stream_init (&stream->base, stdio_write, stdio_close);
    stream->file = file;

    return &stream->base;
}


typedef struct _memory_stream {
    cairo_output_stream_t	base;
    cairo_array_t		array;
} memory_stream_t;

static cairo_status_t
memory_write (cairo_output_stream_t *base,
	      const unsigned char *data, unsigned int length)
{
    memory_stream_t *stream = (memory_stream_t *) base;

    return _cairo_array_append_multiple (&stream->array, data, length);
}

static cairo_status_t
memory_close (cairo_output_stream_t *base)
{
    memory_stream_t *stream = (memory_stream_t *) base;

    _cairo_array_fini (&stream->array);

    return CAIRO_STATUS_SUCCESS;
}

cairo_output_stream_t *
_cairo_memory_stream_create (void)
{
    memory_stream_t *stream;

    stream = malloc (sizeof *stream);
    if (stream == NULL)
	return (cairo_output_stream_t *) &cairo_output_stream_nil;

    _cairo_output_stream_init (&stream->base, memory_write, memory_close);
    _cairo_array_init (&stream->array, 1);

    return &stream->base;
}

void
_cairo_memory_stream_copy (cairo_output_stream_t *base,
			   cairo_output_stream_t *dest)
{
    memory_stream_t *stream = (memory_stream_t *) base;

    _cairo_output_stream_write (dest, 
				_cairo_array_index (&stream->array, 0),
				_cairo_array_num_elements (&stream->array));
}

int
_cairo_memory_stream_length (cairo_output_stream_t *base)
{
    memory_stream_t *stream = (memory_stream_t *) base;

    return _cairo_array_num_elements (&stream->array);
}

/**
 * _cairo_fopen:
 * @filename: filename to open
 * @mode: mode string with which to open the file
 * 
 * Exactly like the C library function, but possibly doing encoding
 * conversion on the filename. On Unix platforms, the filename is
 * passed directly to the system, but on Windows, the filename is
 * interpreted as UTF-8, rather than in a codepage that would depend
 * on system settings.
 * 
 * Return value: The newly opened file, or NULL if an error occured.
 **/
FILE *
_cairo_fopen (char *filename, char *mode)
{
#ifdef USE_UTF16_WFOPEN
    uint16_t *filename_w;
    uint16_t *mode_w;
    FILE *result;

    /* This check is a convenience if someone omits a check on the
     * the return value cairo_win32_filename_from_unicode(), since that can
     * return NULL on OOM.
     */
    if (filename == NULL || mode == NULL) {
	errno = EINVAL;
	return NULL;
    }
    
    if (_cairo_utf8_to_utf16 (filename, -1, &filename_w, NULL) != CAIRO_STATUS_SUCCESS) {
	errno = EINVAL;
	return NULL;
    }

    if (_cairo_utf8_to_utf16 (mode, -1, &mode_w, NULL) != CAIRO_STATUS_SUCCESS) {
	free (filename_w);
	errno = EINVAL;
	return NULL;
    }

#ifdef HAVE__WFOPEN
    result = _wfopen(filename_w, mode_w);
#elif HAVE_WFOPEN
    result = wfopen(filename_w, mode_w);
#else
#error "USE_UTF16_WFOPEN is defined but neither HAVE__WFOPEN or HAVE_WFOPEN was defined"
#endif /* USE_UTF16_WFOPEN */

    free (filename_w);
    free (mode_w);

    return result;
    
#else /* Use fopen directly */
    return fopen (filename, mode);
#endif  
}
