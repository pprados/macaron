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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PolicyFile
{
	private static class Perm implements Comparable<Perm>
	{
		String perm;
		String comment;
		
		int start=Integer.MAX_VALUE;
		int stop=Integer.MAX_VALUE;
		enum flag { Nothing,Insert,Delete};
		flag fl=flag.Nothing;
		Perm(String s,int start,int stop)
		{
			perm=s;
			this.start=start;
			this.stop=stop;
		}
		Perm(String s,flag fl,String comment,int start,int stop)
		{
			perm=s;
			this.comment=comment;
			this.fl=fl;
			this.start=start;
			this.stop=stop;
		}
		public String toString()
		{
			return (perm==null) ? "(null)" : perm;
		}
		public boolean equals(Object x)
		{
			return perm.equals(((Perm)x).perm);
		}
		public int compareTo(Perm x)
		{
			return perm.compareTo(x.perm);
		}
	}
	private static class Tag implements Comparable<Tag>
	{
		boolean remove;
		
		String codebase;

		String signedBy;
		
		String comment;

		int start=Integer.MAX_VALUE;

		int stop=Integer.MAX_VALUE;

		List<Perm> perms = new ArrayList<Perm>();

		public int compareTo(Tag tag)
		{
			return start-tag.start;
		}
		public boolean equals(Object x)
		{
			Tag xx=(Tag)x;
			if (codebase==xx.codebase) return true;
			if (codebase!=null) return codebase.equals(xx.codebase);
			return false;
		}
		public String toString()
		{
			return (codebase==null) ? "(null)" : codebase;
		}
	}

	private Logger log;
	private boolean acceptAllPrivilege_=false;
	
	private List<String> file = new ArrayList<String>();

	private List<Tag> tags = new ArrayList<Tag>();

	private static final String javaString="\"[^\"\\\r\n]*(?:\\.[^\"\\\r\n]*)*\"";
	private static final Pattern pattern=Pattern.compile("^\\s*permission\\s+([A-Za-z\\.$]+)\\s*(("+javaString+")(,\\s*"+javaString+")?)?\\s*;");
	private static final Pattern patternPartial=Pattern.compile("^([A-Za-z\\.$]+)\\s*(("+javaString+")(,\\s*"+javaString+")?)?$");
	private static String parsePrivilege(int pos,String line,boolean all) throws IllegalArgumentException
	{
		Matcher matcher=(all) ? pattern.matcher(line) : patternPartial.matcher(line);
		if (matcher.find())
		{
			StringBuilder builder=new StringBuilder();
			builder.append(matcher.group(1));
			if (matcher.group(3)!=null)
				builder.append(' ').append(matcher.group(3));
			if (matcher.group(4)!=null)
				builder.append(matcher.group(4));
			return builder.toString();
		}
		throw new IllegalArgumentException("Syntax error: line "+pos+" "+line);
	}

	PolicyFile(Logger log)
	{
		this.log=log;
	}
	PolicyFile load(final Reader in) throws IOException, IllegalArgumentException
	{
		return load(in,null,null,null);
	}
	PolicyFile insert(final Reader in,String comment,String context,String merge) throws IOException, IllegalArgumentException
	{
		return load(in,comment,context,merge);
	}
	
	void setAcceptAllPrivileg(boolean a)
	{
		acceptAllPrivilege_=a;
	}
	
	private PolicyFile load(final Reader in,String comment,String context,String merge) throws IOException, IllegalArgumentException
	{
		try
		{
			BufferedReader reader = new BufferedReader(in);
			String line;
			StringBuilder builder = new StringBuilder();
			Tag tag = null;
			int start=0;
			int pos = 0;
			while ((line = reader.readLine()) != null)
			{
				++pos;
				if (context==null) 
					file.add(line);
				line = line.trim();
				if (line.startsWith("//") || (line.length() == 0))
				{
					++start;
					continue;
				}
				builder.append(line);
				if (tag != null && (line.charAt(line.length() - 1)) != ';')
				{
					builder.append(' ');
					continue;
				}
				line = builder.toString();
				builder.setLength(0);
				// Find the start block
				String[] items = line.split("[ \t]"); // FIXME : manage \"
				if (tag == null) // No context
				{
					if (!"grant".equals(items[0]))
					{
						throw new IllegalArgumentException("syntax error: missing 'grant' in line " + pos);
					}
					String codebase;
					if ("codeBase".equals(items[1]))
					{
						codebase = items[2].substring(1, items[2].length() - 1);
					}
					else
					{
						codebase=(context==null) ? "" : context;
					}
					if (merge!=null) codebase=merge;
					tag=getCodebase(codebase);
					if (tag==null)
					{
						tag = new Tag();
						tag.codebase=codebase;
						tag.comment=comment;
					}
					if ((context==null) && (merge==null)) tag.start = ++start;
				}
				else
				{
					if ("};".equals(items[0]))
					{
						if ((context==null) && (merge==null)) tag.stop = pos;
						if (!tags.contains(tag)) tags.add(tag);
						tag = null;
					}
					else
					{
						++start;
						if (merge!=null)
						{
							final Perm p=new Perm(parsePrivilege(pos, line,true),Perm.flag.Insert,"Merged from "+context,Integer.MAX_VALUE,Integer.MAX_VALUE);
							if (context==null || acceptAllPrivilege_ || !p.perm.startsWith("java.security.AllPermission"))
							{
								if (!tag.perms.contains(p))
									tag.perms.add(p);
							}
							else
							{
								log.warning("Remove java.security.AllPermission for "+context);
							}
						}
						else
						{
							final Perm p=new Perm(parsePrivilege(pos, line,true),start,pos);
							if (context==null || acceptAllPrivilege_ || !p.perm.startsWith("java.security.AllPermission"))
							{
								tag.perms.add(p);
							}
							else
							{
								log.warning("Remove java.security.AllPermission for "+context);
							}
						}
					}
				}
				start=pos;
			}
	
			return this;
		}
		finally
		{
			in.close();
		}
	}
	
	public Tag getCodebase(String codebase)
	{
		for (Tag t:tags)
		{
			if (codebase==t.codebase || codebase.equals(t.codebase))
				return t;
		}
		return null;
	}
	
	public void addPerm(String codebase,String perm,String comment)
	{
		assert(codebase!=null);
		assert(perm!=null);
		try
		{
			parsePrivilege(0,perm,false); // Check syntax
			Tag tag=getCodebase(codebase);
			Perm p=new Perm(perm,Perm.flag.Insert,comment,Integer.MAX_VALUE,Integer.MAX_VALUE);
			if (!tag.perms.contains(p))
				tag.perms.add(p);
		}
		catch (IllegalArgumentException e)
		{
			// Ignore
			// Case :  java.util.PropertyPermission "fn:contains(name,","searchString)","read"
		}
	}
	
	public void removePerm(String codebase,String perm)
	{
		Tag tag=getCodebase(codebase);
		for (Perm p:tag.perms)
		{
			if (p.perm.equals(perm))
			{
				p.fl=Perm.flag.Delete;
				return;
			}
		}
		throw new IllegalArgumentException(codebase+" not found");
	}
	
	public Tag addCodeBase(String codebase,String comment)
	{
		Tag tag=getCodebase(codebase);
		if (tag==null) 
		{
			tag=new Tag();
			tag.codebase=codebase;
			tag.comment=comment;
			tags.add(tag);
		}
		return tag;
	}
	
	// Reunie tous dans le même codebase
	public void merge(String codebase)
	{
		String filter=codebase;
		if (codebase.endsWith("-") || codebase.endsWith("*"))
		{
			filter=codebase.substring(0,codebase.length()-1);
		}
		Tag tag=getCodebase(codebase);
		if (tag==null)
		{
			tag=addCodeBase(codebase, "*** Merged "+codebase);
		}
		for (Tag t:tags)
		{
			if ((codebase.equals(t.codebase) || t.codebase.startsWith(filter)) && t!=tag)
			{
				for (Perm p:t.perms)
				{
					if (!tag.perms.contains(p))
					{
						log.finest("merge "+p+" in \""+tag+"\" form "+t);
						tag.perms.add(p);
					}
				}
				t.remove=true;
			}
		}
		Collections.sort(tag.perms);
	}
	
	public boolean save(final Writer w)
	{
		final PrintWriter writer=new PrintWriter(w,true);
		try
		{
			boolean update=false;
			Collections.sort(tags);
			Iterator<Tag> i=tags.iterator();
			if (!i.hasNext())
			{
				log.warning("Empty policy");
				return false;
			}
			Tag tag=i.next();
			boolean logtag=false;
			boolean endlogtag=false;
			Iterator<Perm> j=tag.perms.iterator();
			Perm perm=j.hasNext() ? j.next() : new Perm("",tag.start,tag.start+1);
			int l=0;
			for (String line:file)
			{
				++l; // line driving
				if ((l>=tag.start) && (l<=tag.stop))
				{
					if (tag.remove)
						writer.print("// ");
					if ((l==perm.start) && perm.fl==Perm.flag.Delete)
					{
						if (!logtag)
						{
							if (endlogtag) log.fine("}");
							logtag=endlogtag=true;
							log.fine("grant codeBase \""+tag.codebase+"\" {");
						}
						log.fine("- permission "+perm.perm+';');
						writer.println("\t// DELETED permission "+perm.perm);
						update=true;
					}
					if (l==perm.stop)
					{
						if (perm.fl!=Perm.flag.Delete)
							writer.println(line);
						if (j.hasNext()) perm=j.next();
						continue;
					}
					if ((l>=perm.start) && (l<=perm.stop) && perm.fl==Perm.flag.Delete)
						continue;
					if (l==tag.stop)
					{
						while (perm.fl==Perm.flag.Insert)
						{
							update=true;
							if (perm.comment!=null) writer.println("\t// "+perm.comment);
							writer.println("\tpermission "+perm.perm+';');
							if (!logtag)
							{
								if (endlogtag) log.fine("}");
								logtag=endlogtag=true;
								if ((tag.codebase==null) || (tag.codebase.length()==0))
								{
									log.fine("grant {");
								}
								else
									log.fine("grant codeBase \""+tag.codebase+"\" {");
							}
							log.fine("+ permission "+perm.perm+';');
							if (!j.hasNext()) break;
							perm=j.next();
						}
						if (i.hasNext()) tag=i.next();
						logtag=false;
						j=tag.perms.iterator();
						if (j.hasNext()) perm=j.next();
					}
				}
				writer.println(line); // Comment, etc.
			}
			if (endlogtag) log.fine("}");
			
			// Ajout des nouveautées
			if (tag.start==Integer.MAX_VALUE)
			{
				update=true;
				for (;;)
				{
					if (tag.comment!=null) writer.println("// "+tag.comment);
					if ((tag.codebase==null) || (tag.codebase.length()==0))
					{
						writer.println("grant {");
						log.fine("grant {");
					}
					else
					{
						writer.println("grant codeBase \""+tag.codebase+"\" {");
						log.fine("grant codeBase \""+tag.codebase+"\" {");
					}
					for (Perm p:tag.perms)
					{
						writer.println("\tpermission "+p.perm+";");
						log.fine("+ permission "+p.perm+';');
					}
					writer.println("};");
					log.fine("}");
					if (i.hasNext()) tag=i.next();
					else break;
				}
			}

			//for (PolicyTag t:tags)
			//{
			//	log.fine(t.toString());
			//}
			
			return update;
		}
		finally
		{
			writer.close();
		}
	}
	
	public void extractDatabase(File dir,String prefix) throws FileNotFoundException
	{
		if (!dir.isDirectory())
			throw new IllegalArgumentException(dir+" must be a directory");
		PrintWriter writer=null;
		try
		{
			for (Tag tag:tags)
			{
				log.finest("Try to extract "+tag.codebase);				
				if ((tag.codebase!=null) && tag.codebase.startsWith(prefix))
				{
					int idx=tag.codebase.lastIndexOf('/');
					String basename=tag.codebase.substring(idx+1);
					if (basename.endsWith(".jar"))
					{
						if (basename.startsWith("${prefix}"))
							basename=basename.substring("${prefix}".length());
						log.info("Extract "+basename);						
						writer=new PrintWriter(new File(dir,basename+".policy"));
						writer.println("// "+basename);
						writer.println("grant {");
						Collections.sort(tag.perms);
						for (Perm perm:tag.perms)
						{
							writer.println("\tpermission "+perm.perm+" ;");
						}
						writer.println("};");
						writer.close();
					}
				}
			}
		}
		finally
		{
			if (writer!=null)
				writer.close();
		}
	}
	
//	public static void main(String[] args) throws IllegalArgumentException, IOException
//	{
//		Reader reader=new FileReader("src/test/policy/test.policy");
//		PolicyFile policy=new PolicyFile().load(reader);
//		reader.close();
//		String codebase="file:${java.home}/toto";
//		policy.removePerm(codebase, "java.security.AllPermission");
//		policy.removePerm(codebase, "java.util.PropertyPermission \"java.util.logging.config.file\", \"read\"");
//
//		policy.addPerm(codebase,"my.permission TOTO,\"tutu\"","Add by main");
//		policy.addCodeBase("file:${java.home}/toto.tutu",null);
//		policy.addPerm("file:${java.home}/toto.tutu","my.permission TOTO,\"tutu\"",null);
//		policy.save(new OutputStreamWriter(System.out));
//		
//	}
}
