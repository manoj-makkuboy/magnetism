package com.dumbhippo.dm.fetch;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMClassHolder;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMPropertyHolder;


public class PropertyFetchNode {
	static private final Logger logger = GlobalSetup.getLogger(PropertyFetchNode.class); 
	
	private String property;
	private FetchNode children;
	private FetchAttributeNode[] attributes;
	
	public PropertyFetchNode(String property, FetchAttributeNode[] attributes, FetchNode children) {
		this.property = property;
		this.attributes = attributes;
		this.children = children;
	}

	public FetchNode getChildren() {
		return children;
	}

	public String getProperty() {
		return property;
	}
	
	public Object getAttribute(FetchAttributeType type) {
		for (FetchAttributeNode attr : attributes) {
			if (attr.getType() == type)
				return attr.getValue();
		}
		
		return null;
	}
	

	/**
	 * Finds all properties in the given class and in *subclasses* of the given class
	 * that match this node, bind them and return the result.
	 * 
	 * TODO: Implement the subclass part
	 * 
	 * @param classHolder
	 * @param whether to skip properties that are marked as defaultInclude 
	 *   (the property will still not be skipped if the children overridden)
	 * @param resultList list to append the results to 
	 */
	public void bind(DMClassHolder<? extends DMObject> classHolder, boolean skipDefault, List<PropertyFetch> resultList) {
		boolean notify = true;
		for (FetchAttributeNode attribute : attributes) {
			switch (attribute.getType()) {
			case NOTIFY:
				// FIXME: We probably should make bind() throw an exception and make this fatal
				if (!(attribute.getValue() instanceof Boolean)) {
					logger.warn("Ignoring non-boolean notify attribute");
					continue;
				}
				notify = ((Boolean)(attribute.getValue())).booleanValue();
				break;
			}
		}
		
		int propertyIndex = classHolder.getPropertyIndex(property);
		if (propertyIndex >= 0) {
			DMPropertyHolder propertyHolder = classHolder.getProperty(propertyIndex);
			boolean maybeSkip = skipDefault && propertyHolder.getDefaultInclude(); 
			
			Fetch boundChildren = null;
			if (propertyHolder.isResourceValued()) {
				Fetch defaultChildren = propertyHolder.getDefaultChildren();
				
				if (maybeSkip && children == null && defaultChildren == null) {
					return;
				}

				if (children != null) {
					DMClassHolder<? extends DMObject> childClassHolder = propertyHolder.getResourceClassHolder();
					boundChildren = children.bind(childClassHolder);
				}

				if (maybeSkip && boundChildren != null && defaultChildren != null) {
					if (boundChildren.equals(defaultChildren))
						return;
				}
				
			} else {
				if (maybeSkip)
					return;
			}

			resultList.add(new PropertyFetch(propertyHolder, boundChildren, notify));
		}
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append(property);
		
		if (attributes.length > 0) {
			b.append('(');
			for (int i = 0; i < attributes.length; i++) {
				if (i != 0)
					b.append(',');
				b.append(attributes[i].toString());
			}
			b.append(')');
		}
		
		if (children != null) {
			b.append(' ');
			if (children.getProperties().length == 1) {
				b.append(children.getProperties()[0].toString());
			} else {
				b.append('[');
				b.append(children.toString());
				b.append(']');
			}
		}
		
		return b.toString();
	}
}