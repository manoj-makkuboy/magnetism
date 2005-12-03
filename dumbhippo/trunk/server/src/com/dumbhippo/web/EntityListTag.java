package com.dumbhippo.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class EntityListTag extends SimpleTagSupport {
	private List<Object> entities;
	private String skipRecipientId;
	private boolean showInviteLinks;
	private boolean photos;
	
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		
		if (entities == null)
			return;
		
		String buildStamp;
		try {
			buildStamp = (String) getJspContext().getVariableResolver().resolveVariable("buildStamp");
		} catch (ELException e) {
			throw new RuntimeException(e);
		}
		
		boolean first = true;
		for (Object o : entities) {
			String html = EntityTag.entityHTML(o, buildStamp, skipRecipientId, showInviteLinks, photos);
			if (html == null)
				continue;
			
			if (!first)
				writer.print(", ");
			
			writer.print(html);
			
			first = false;
		}
	}
	
	public void setValue(List<Object> value) {
		entities = value;
	}
	
	public void setSkipRecipientId(String skipRecipientId) {
		this.skipRecipientId = skipRecipientId;
	}
	
	public void setShowInviteLinks(boolean showInviteLinks) {
		this.showInviteLinks = showInviteLinks;
	}
	
	public void setPhotos(boolean photos) {
		this.photos = photos;
	}
}
