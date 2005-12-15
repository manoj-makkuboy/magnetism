package com.dumbhippo.server.formatters;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.postinfo.FlickrPostInfo;
import com.dumbhippo.postinfo.PostInfo;
import com.dumbhippo.server.PostView;
import com.dumbhippo.services.FlickrPhotosetData.PhotoData;

public class FlickrFormatter extends DefaultFormatter {
	
	@Override
	public String getTextAsHtml(PostView postView) {
		PostInfo postInfo = postView.getPost().getPostInfo();

		if (postInfo == null)
			return super.getTextAsHtml(postView);
		
		FlickrPostInfo itemData = (FlickrPostInfo) postInfo;
		
		XmlBuilder xml = new XmlBuilder();		
		List<PhotoData> data = itemData.getPhotoData();
		for (PhotoData photo : data) {
			String pageUrl = photo.getPageUrl();
			if (pageUrl != null) {
				xml.append("<a href=\"");
				xml.appendEscaped(photo.getPageUrl());
			}
			xml.append("<img class=\"dh-flickr-thumbnail\" src=\"");
			xml.appendEscaped(photo.getThumbnailUrl());
			xml.append("\"></img>");
			if (pageUrl != null) {
				xml.append("</a>");
			}
		}
		xml.append("<p class=\"dh-flickr-description\">");
		xml.appendTextAsHtml(postView.getPost().getText(), postView.getSearchTerms());
		xml.append("</p>");		
		return xml.toString();
	}
}
