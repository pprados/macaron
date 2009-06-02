/*
 * Copyright 2009 Philippe Prados.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 * $Log$
 */
package com.googlecode.macaron.audit;

import java.util.Collections;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

/**
 * Builder for XML manipulations.
 * 
 * @author Philippe PRADOS
 */
public final class XMLContext
{
	/**
	 * name-space alias.
	 */
	private static final String ALIAS = "audit";

	/**
	 * Name-space URI.
	 */
	public static final String NAMESPACESTR = "http://macaron.googlecode.com/1.0/";


	/**
	 * The name space context.
	 */
	public static final NamespaceContext NAMESPACE = new NamespaceContext()
	{
		public String getPrefix(final String namespaceURI)
		{
			return (NAMESPACESTR.equals(namespaceURI)) ? ALIAS : null;
		}

		public String getNamespaceURI(final String prefix)
		{
			return (ALIAS.equals(prefix)) ? NAMESPACESTR : null;
		}

		public Iterator<?> getPrefixes(final String namespaceURI)
		{
			return Collections.EMPTY_LIST.iterator();
		}

	};

	/**
	 * The XML document builder factory.
	 */
	public static final DocumentBuilderFactory DOC_BUILDER_FACTORY;

	/**
	 * The XPath factory.
	 */
	public static final XPathFactory XPATH_FACTORY;

	/**
	 * Initialize factories.
	 */
	static
	{
		DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
		// DOC_BUILDER_FACTORY.setValidating(true);
		DOC_BUILDER_FACTORY.setAttribute(
			"http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
		XPATH_FACTORY = XPathFactory.newInstance();
	};

	/**
	 * Not used.
	 */
	private XMLContext()
	{

	}
}
