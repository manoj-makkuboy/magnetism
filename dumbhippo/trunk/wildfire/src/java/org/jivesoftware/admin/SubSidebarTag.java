/**
 * $RCSfile$
 * $Revision: 297 $
 * $Date: 2004-11-10 14:13:33 -0600 (Wed, 10 Nov 2004) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.admin;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

/**
 * <p>A simple tag to gather its body content and pass it to the parent tag. This tag must be used
 * as a child of the {@link SidebarTag}.</p>
 *
 * <p>Sample usage:<ul><tt>
 *
 *      &lt;jive:sidebar bean="jivepageinfo"&gt; <br>
 *          &nbsp;&nbsp;&nbsp;&lt;a href="[url]" title="[description]"&gt;[name]&lt;/a&gt; <br>
 *          &nbsp;&nbsp;&nbsp;&lt;jive:subsidebar&gt; <br>
 *          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;a href="[url]"&gt;[name]&lt;/a&gt; <br>
 *          &nbsp;&nbsp;&nbsp;&lt;/jive:subsidebar&gt; <br>
 *      &lt;/jive:sidebar&gt;</tt></ul></p>
 *
 * <p>Note, this class has no attributes.</p>
 */
public class SubSidebarTag extends SidebarTag {

    private SidebarTag parent;
    private String body;

    /**
     * Returns the body content of this tag.
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the body content of this tag.
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Looks for the parent SidebarTag class, throws an error if not found. If found,
     * {@link #EVAL_BODY_BUFFERED} is returned.
     *
     * @return {@link #EVAL_BODY_BUFFERED} if no errors.
     * @throws javax.servlet.jsp.JspException if a parent SidebarTag is not found.
     */
    public int doStartTag() throws JspException {
        // The I18nTag should be our parent Tag
        parent = (SidebarTag)findAncestorWithClass(this, SidebarTag.class);

        // If I18nTag was not our parent, throw Exception
        if (parent == null) {
            throw new JspTagException("SubSidebarTag with out a parent which is expected to be a SidebarTag");
        }
        return EVAL_BODY_BUFFERED;
    }

    /**
     * Sets the 'body' property to be equal to the body content of this tag. Calls the
     * {@link SidebarTag#setSubSidebar(SubSidebarTag)} method of the parent tag.
     * @return {@link #EVAL_PAGE}
     * @throws JspException if an error occurs.
     */
    public int doEndTag() throws JspException {
        setBody(bodyContent.getString());
        parent.setSubSidebar(this);
        return EVAL_PAGE;
    }
}