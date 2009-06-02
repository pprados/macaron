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
package com.googlecode.macaron.policy;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;
import java.util.Stack;

/**
 * A writer with an on-the-fly variables conversion.
 * 
 * @author Philippe PRADOS
 */
public class VariableWriter extends Writer implements OnConvVariable
{
	/**
	 * The next reader.
	 */
	private final Writer next_;
	
	/**
	 * The current properties.
	 */
	private final Properties props_;

	/**
	 * Error if the variable is not found.
	 */
	public static class VariableNotFound extends IOException
	{

		/**
		 * The serial version.
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * The variable name in error.
		 */
		private String varname_;

		/**
		 * Constructor.
		 * 
		 * @param msg The message of exception.
		 * @param varname The variable name.
		 */
		public VariableNotFound(final String msg, final String varname)
		{
			super(msg);
			varname_ = varname;
		}

		/**
		 * Return the variable name in error.
		 * 
		 * @return The variable name in error.
		 */
		public String getVariableName()
		{
			return varname_;
		}

	}

	/**
	 * Constructor.
	 * 
	 * @param next The next writer.
	 * @param props The properties to change the variables to values.
	 */
	public VariableWriter(final Writer next,final Properties props)
	{
		super();
		next_ = next;
		props_=props;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException
	{
		next_.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException
	{
		next_.flush();
	}

	private StringBuilder variableBuffer_=new StringBuilder();
	private int previous_=-1;
	private int inVariable_=0;
	
	/**
	 * The recursive transformation of variable buffer.
	 * @throws VariableNotFound 
	 */
	static String convVariable(String var,Properties props,OnConvVariable events) throws VariableNotFound
	{
		StringBuilder variableBuffer=new StringBuilder(var);
		final Stack<Integer> startvar = new Stack<Integer>();
		
		int previous=-1;
		boolean invariable=false;
		int inner=0;
		for (int i=0;i<variableBuffer.length();++i) 
		{
			final char cur=variableBuffer.charAt(i);
			if (previous==-1)
			{
				previous=cur;
				continue;
			}
			if (previous=='$' && cur=='{')
			{
				invariable=true;
				++inner;
				startvar.push(i+1);
			}
			else if (cur=='}' && invariable)
			{
				int prev=startvar.pop();
				final String varname=variableBuffer.substring(prev,i);
				String value=props.getProperty(varname);
				if (value==null)
				{
					events.onError("Variable " + varname + " not found!", varname);
				}
				else
				{
					events.onVariable(varname, value);
					variableBuffer.replace(prev-2,i+1, value);
					i=prev-2;
				}
			}
		}
		return variableBuffer.toString();
	}
	private void convVariable() throws VariableNotFound
	{
		String v=convVariable(variableBuffer_.toString(),props_,this);
		variableBuffer_.setLength(0);
		variableBuffer_.append(v);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(char[] data, int start, int len) throws IOException, VariableNotFound
	{
		assert(data!=null);
		assert(start>=0);
		assert(len<=data.length);
		
		int st=(inVariable_!=0) ? -1 : start;
		for (int i=start;i<start+len;++i)
		{
			final char cur=data[i];
			if (previous_==-1) 
			{
				previous_=cur;
				continue;
			}
			if (previous_=='$' && (cur=='{'))
			{
				if (inVariable_==0)
				{
					next_.write(data,st,i-st-1); // Ecrit le prefix
					variableBuffer_.append('$');
				}
				variableBuffer_.append('{');
				++inVariable_;
			}
			else if ((inVariable_!=0) && cur=='}')
			{
				if (inVariable_!=0) variableBuffer_.append("}");
				--inVariable_;
				if (inVariable_==0)
				{
					convVariable();
					next_.write(variableBuffer_.toString().toCharArray());
					variableBuffer_.setLength(0);
				}
				st=i+1;
			}
			else if (inVariable_!=0)
			{
				variableBuffer_.append(cur);
			}
			previous_=cur;
		}
		if ((inVariable_==0) && (st!=-1))
		{
			next_.write(data,st,len-st); // ecrit la fin
		}
	}
	/**
	 * If variable is not found, generate a exception.
	 * You can extends this class and propose another implementation of this method,
	 * for ask the user, continue to detect all errors, etc.
	 * 
	 * @param msg The error message.
	 * @param varname The variable name.
	 * @throws VariableNotFound If variable not found.
	 */
	public void onError(final String msg, final String varname) throws VariableNotFound
	{
		throw new VariableNotFound(msg, varname);
	}
	
	/**
	 * Invoked on variable set.
	 * 
	 * @param varname The variable name.
	 * @param value The value to set.
	 */
	public void onVariable(final String varname,final String value)
	{
		
	}

//	public static void main(String[] args) throws IOException
//	{
//		final Properties props=new Properties();
//		props.put("var1", "value1");
//		props.put("var2", "value2");
//		props.put("var3", "ar");
//		props.put("var4", "${v${var3}2}/${var2}");
//		props.put("var5", "${ignore}${v${ignore}2}");
//		props.put("var6", "${var1}");
//		final CharArrayWriter result=new CharArrayWriter();
//		class NoErrorVariableWriter extends VariableWriter
//		{
//			NoErrorVariableWriter()
//			{
//				super(result,props);
//			}
//			protected void onError(final String msg, final String varname) throws VariableNotFound
//			{
//			}
//			protected void onVariable(final String varname,final String value)
//			{
//				System.out.println(varname+"="+value);
//			}
//		};
//		Writer writer=new NoErrorVariableWriter();
//		char[] tst;
//		String r;
//
//		tst="hello\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("hello\n"));
//
//		tst="a${var1}b\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("avalue1b\n"));
//
//		tst="a$v{ar1}b\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("a$v{ar1}b\n"));
//
//		tst="cut${var1".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		tst="}end\n".toCharArray();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("cutvalue1end\n"));
//
//		tst="${var4}\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("value2/value2\n"));
//		
//		tst="${v${var3}2}\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("value2\n"));
//		
//		tst="c${v".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		tst="ar".toCharArray();
//		writer.write(tst,0,tst.length);
//		tst="1}e\n".toCharArray();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("cvalue1e\n"));
//
//		tst="${ignore}\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("${ignore}\n"));
//		
//		tst="${var5}\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("${ignore}${v${ignore}2}\n"));
//		
//		tst="${var6}".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("value1"));
//		
//		tst="$${var6}\n".toCharArray();
//		System.out.println(tst);
//		result.reset();
//		writer.write(tst,0,tst.length);
//		writer.flush();
//		r=new String(result.toCharArray());
//		System.out.println(r);
//		assert(r.equals("$value1\n"));
//		
//	}
}
