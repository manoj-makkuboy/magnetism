/**
 * $RCSfile$
 * $Revision: 128 $
 * $Date: 2004-10-25 19:42:00 -0400 (Mon, 25 Oct 2004) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * <p>Indicates a route does not exist or is invalid (cannot be reached).</p>
 *
 * @author Iain Shigeoka
 */
public class NoSuchRouteException extends Exception {
    private Throwable nestedThrowable = null;

    public NoSuchRouteException() {
        super();
    }

    public NoSuchRouteException(String msg) {
        super(msg);
    }

    public NoSuchRouteException(Throwable nestedThrowable) {
        this.nestedThrowable = nestedThrowable;
    }

    public NoSuchRouteException(String msg, Throwable nestedThrowable) {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

    public void printStackTrace() {
        super.printStackTrace();
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace();
        }
    }

    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(ps);
        }
    }

    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(pw);
        }
    }
}
