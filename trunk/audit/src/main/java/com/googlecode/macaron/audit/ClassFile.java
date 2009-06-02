/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */
package com.googlecode.macaron.audit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * This class is encapsulating binary .class file information as
 * defined at
 * http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html
 *
 * This is used by the annotation frameworks to quickly scan .class files
 * for the presence of annotations. This avoid the annotation framework having
 * to load each .class file in the class loader.
 *
 * @author Jerome Dochez
 */
// http://google.com/codesearch/p?hl=fr#h0n4vxU5-X4/glassfish/appserv-commons/src/java/com/sun/enterprise/deployment/annotation/introspection/ClassFile.java&q=glassfish%20AnnotationProcessor
public class ClassFile
{

	ByteBuffer header;

	ConstantPoolInfo constantPoolInfo = new ConstantPoolInfo();

	/** Creates a new instance of ClassFile */
	public ClassFile()
	{
		header = ByteBuffer.allocate(12000);
	}

	public void setConstantPoolInfo(ConstantPoolInfo poolInfo)
	{
		constantPoolInfo = poolInfo;
	}

	/**
	 * Read the input channel and initialize instance data structure.
	 */
	public boolean containsAnnotation(ReadableByteChannel in, long size) throws IOException
	{

		/**
		 * this is the .class file layout
		 * 
		 * ClassFile { u4 magic; u2 minor_version; u2 major_version; u2
		 * constant_pool_count; cp_info constant_pool[constant_pool_count-1]; u2
		 * access_flags; u2 this_class; u2 super_class; u2 interfaces_count; u2
		 * interfaces[interfaces_count]; u2 fields_count; field_info
		 * fields[fields_count]; u2 methods_count; method_info
		 * methods[methods_count]; u2 attributes_count; attribute_info
		 * attributes[attributes_count]; }
		 **/
		header.clear();
		if (size != -1 && size > header.capacity())
		{
			// time to expand...
			header = ByteBuffer.allocate((int) size);
		}
		long read = (long) in.read(header);
		if (size != -1 && read != size)
		{
			return false;
		}
		header.rewind();

		if (header.getInt() != magic)
		{
			return false;
		}

		majorVersion = header.getShort();
		minorVersion = header.getShort();
		int constantPoolSize = header.getShort();

		return constantPoolInfo.containsAnnotation(
			constantPoolSize, header);

	}

	public short majorVersion;

	public short minorVersion;

	public ConstantPoolInfo constantPool[];

	public short accessFlags;

	public ConstantPoolInfo thisClass;

	public ConstantPoolInfo superClass;

	public ConstantPoolInfo interfaces[];

	/**
	 * bunch of stuff I really don't care too much for now.
	 * 
	 * FieldInfo fields[]; MethodInfo methods[]; AttributeInfo attributes[];
	 */
	boolean isValidClass = false;

	private static final int magic = 0xCAFEBABE;

	public static final int ACC_PUBLIC = 0x1;

	public static final int ACC_PRIVATE = 0x2;

	public static final int ACC_PROTECTED = 0x4;

	public static final int ACC_STATIC = 0x8;

	public static final int ACC_FINAL = 0x10;

	public static final int ACC_SYNCHRONIZED = 0x20;

	public static final int ACC_THREADSAFE = 0x40;

	public static final int ACC_TRANSIENT = 0x80;

	public static final int ACC_NATIVE = 0x100;

	public static final int ACC_INTERFACE = 0x200;

	public static final int ACC_ABSTRACT = 0x400;

}
