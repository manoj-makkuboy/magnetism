package com.dumbhippo.dm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.annotations.Inject;
import com.dumbhippo.identity20.Guid;

public class DMClassHolder<T> {
	@SuppressWarnings("unused")
	static private Logger logger = GlobalSetup.getLogger(DMClassHolder.class);
	
	private DataModel model;
	private Class<T> baseClass;
	private Class<?> keyClass;
	private Constructor keyConstructor;
	private Constructor wrapperConstructor;
	private DMPropertyHolder[] properties;
	private Map<String, Integer> propertiesMap = new HashMap<String, Integer>();

	public DMClassHolder(DataModel model, Class<T> clazz) {
		this.model = model;
		this.baseClass = clazz;

		for (Constructor c : clazz.getConstructors()) {
			Class<?>[] parameterTypes = c.getParameterTypes();
			if (parameterTypes.length != 1)
				continue;
			
			if (!(parameterTypes[0].equals(Guid.class) ||
				  parameterTypes[0].equals(String.class) ||
				  DMKey.class.isAssignableFrom(parameterTypes[0])))
				  continue;
			
			if (keyConstructor != null)
				throw new RuntimeException("Multiple candidate constructors found for class " + 
										   clazz.getName() + ": " +
										   keyConstructor.toGenericString() + ", " +
										   c.toGenericString());
			
			keyConstructor = c;
			keyClass = parameterTypes[0];
		}
		
		buildWrapperClass();
	}

	// FIXME: do we need this?
	public Class<?> getKeyClass() {
		return keyClass;
	}
	
	public Class<T> getBaseClass() {
		return baseClass;
	}
	
	public int getPropertyIndex(String name) {
		Integer index = propertiesMap.get(name);
		if (index == null)
			throw new IllegalArgumentException("No property named " + name);
		
		return index;
	}
	
	public int getPropertyCount() {
		return properties.length;
	}
	
	public DMPropertyHolder getProperty(int propertyIndex) {
		return properties[propertyIndex];
	}
	
	public T createInstance(Object key, DMSession session) {
		try {
			@SuppressWarnings("unchecked")
			T result = (T)wrapperConstructor.newInstance(key, session);
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Error creating instance of class " + baseClass.getName(), e);
		}
	}
	
	public void processInjections(DMSession session, T t) {
		for (Field field : baseClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Inject.class)) {
				injectField(session, t, field);
			}
		}
	}

	private void injectField(DMSession session, T t, Field field) {
		if (field.getType().equals(EntityManager.class)) {
			setField(t, field, session.getInjectableEntityManager());
		} else if (DMViewpoint.class.isAssignableFrom(field.getType())) {
			// We use a isAssignableFrom check here to allow people to @Inject fields
			// that are subclasses of DMViewpoint. If the type of the @Inject field
			// is a subtype of DMViewpoint not compatible with the the viewpoint of
			// the DMSession, then this we'll get a ClassCastException here
			setField(t, field, session.getViewpoint());
		} else { 
			throw new RuntimeException("@Inject annotation found field of unknown type " + field.getType().getName());
		}
	}

	private void setField(T t, Field field, Object value) {
		try {
			// Like EJB3, we support private-field injection
			field.setAccessible(true);
			field.set(t, value);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error injecting object", e);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////

	static final Pattern TEMPLATE_PARAMETER = Pattern.compile("%([A-Za-z_][A-Za-z0-9_]+)%");

	// Simple string-template facility for the generated methods
	private static class Template {
		private String template;
		private Map<String, String> parameters = new HashMap<String, String>();
		
		public Template(String template) {
			this.template = template;
		}
		
		public void setParameter(String name, String value) {
			parameters.put(name, value);
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			Matcher m = TEMPLATE_PARAMETER.matcher(template);
			while (m.find()) {
				String replacement = parameters.get(m.group(1));
				if (replacement == null)
					throw new RuntimeException("No replacement for template parameter '" + m.group(1) + "'");
			    m.appendReplacement(sb, replacement);
			}
			 m.appendTail(sb);
			 return sb.toString();
		}
	}
	
	private CtClass ctClassForClass(Class<?> c) {
		ClassPool classPool = model.getClassPool();
		String className = c.getName();

		try {
			return classPool.get(className);
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't find the bytecode for" + className);
		}
	}
	
	private void addCommonFields(CtClass wrapperCtClass) throws CannotCompileException {
		CtField field;
		
		CtClass dmSessionCtClass = ctClassForClass(DMSession.class);
		
		field = new CtField(dmSessionCtClass, "_dm_session", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);
		
		field = new CtField(CtClass.booleanType, "_dm_initialized", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);
	}
	
	private void addConstructor(CtClass wrapperCtClass) throws CannotCompileException {
		CtClass dmSessionCtClass = ctClassForClass(DMSession.class);
		CtClass keyCtClass = ctClassForClass(keyClass);
		
		CtConstructor constructor = new CtConstructor(new CtClass[] { keyCtClass, dmSessionCtClass }, wrapperCtClass);
		
		constructor.setBody("{ super($1); _dm_session = $2;}");
		
		wrapperCtClass.addConstructor(constructor);
	}
	
	private void addInitMethod(CtClass baseCtClass, CtClass wrapperCtClass) throws CannotCompileException {
		CtMethod wrapperMethod = new CtMethod(CtClass.voidType, "_dm_init", new CtClass[] {}, wrapperCtClass);
		Template body = new Template(
			"{" +
			"    if (!_dm_initialized) {" +
			"        _dm_session.internalInit(%className%.class, $0);" +
			"        _dm_initialized = true;" +
			"    }" +
			"}");
		body.setParameter("className", baseCtClass.getName());
		wrapperMethod.setBody(body.toString());
		
		wrapperCtClass.addMethod(wrapperMethod);
	}
	
	private void addWrapperGetter(CtClass baseCtClass, CtClass wrapperCtClass, DMPropertyHolder property, int propertyIndex) throws CannotCompileException, NotFoundException {
		CtField field;
			
		field = new CtField(CtClass.booleanType, "_dm_" + property.getName() + "Initialized", wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);

		field = new CtField(property.getCtClass(), "_dm_" + property.getName(), wrapperCtClass);
		field.setModifiers(Modifier.PRIVATE);
		wrapperCtClass.addField(field);
		
		CtMethod wrapperMethod = new CtMethod(property.getCtClass(), property.getMethodName(), new CtClass[] {}, wrapperCtClass);
		
		// TODO: Deal with primitive types, where we need to box/unbox
		
		Template body = new Template(
			"{" +
			"    if (!_dm_%propertyName%Initialized) {" +
			"    	 try {" +
			"           _dm_%propertyName% = (%propertyTypeName%)_dm_session.fetchAndFilter(%className%.class, getKey(), %propertyIndex%);" +
			"    	 } catch (com.dumbhippo.dm.NotCachedException e) {" +
			"           _dm_init();" +
			"           _dm_%propertyName% = (%propertyTypeName%)_dm_session.storeAndFilter(%className%.class, getKey(), %propertyIndex%, super.%methodName%());" +
			"        }" +
			"        _dm_%propertyName%Initialized = true;" +
			"    }" +
			"    return _dm_%propertyName%;" +
			"}");

		body.setParameter("propertyName", property.getName());
		body.setParameter("propertyTypeName", property.getCtClass().getName());
		body.setParameter("propertyIndex", Integer.toString(propertyIndex));
		body.setParameter("methodName", property.getMethodName());
		body.setParameter("className", baseCtClass.getName());
		wrapperMethod.setBody(body.toString());
		
		wrapperCtClass.addMethod(wrapperMethod);
	}

	private void addWrapperGetters(CtClass baseCtClass, CtClass wrapperCtClass) throws CannotCompileException, NotFoundException {
		List<DMPropertyHolder> foundProperties = new ArrayList<DMPropertyHolder>();
		
		int propertyIndex = 0;
		for (CtMethod method : baseCtClass.getMethods()) {
			DMPropertyHolder property = DMPropertyHolder.getForMethod(this, method);
			if (property != null) {
				foundProperties.add(property);
				propertiesMap.put(property.getName(), propertyIndex);
				addWrapperGetter(baseCtClass, wrapperCtClass, property, propertyIndex);
				propertyIndex++;
			}
		}
		
		properties = foundProperties.toArray(new DMPropertyHolder[foundProperties.size()]);
	}
	
	private void buildWrapperClass() {
		String className = baseClass.getName();
		ClassPool classPool = model.getClassPool();
		CtClass baseCtClass = ctClassForClass(baseClass);
		
		CtClass wrapperCtClass = classPool.makeClass(className + "_DMWrapper", baseCtClass);
		
		try {
			addCommonFields(wrapperCtClass);
			addConstructor(wrapperCtClass);
			addInitMethod(baseCtClass, wrapperCtClass);
			addWrapperGetters(baseCtClass, wrapperCtClass);
			
			Class<?> wrapperClass  = wrapperCtClass.toClass();
			wrapperConstructor = wrapperClass.getDeclaredConstructors()[0]; 
		} catch (CannotCompileException e) {
			throw new RuntimeException("Error compiling wrapper for " + className, e);
		} catch (NotFoundException e) {
			throw new RuntimeException("Cannot look up class compiling wrapper for " + className, e);
		}
	}
}
