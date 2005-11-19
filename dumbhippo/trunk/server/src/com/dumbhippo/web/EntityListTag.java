package com.dumbhippo.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class EntityListTag extends SimpleTagSupport {
	List<Object> entities;
	private String skipRecipientId;
	
	public void doTag() throws IOException {
		JspWriter writer = getJspContext().getOut();
		
		if (entities == null)
			return;
		
		boolean first = true;
		for (Object o : entities) {
			String html = EntityTag.entityHTML(o, skipRecipientId);
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
}
