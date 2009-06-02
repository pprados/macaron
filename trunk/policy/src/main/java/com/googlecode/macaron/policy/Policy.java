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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import com.googlecode.macaron.policy.VariableWriter.VariableNotFound;


/**
 * For each component, extract policy file in META-INF/jar.policy or request a database, and generate a global policy file
 * to add privileges in JVM.
 * 
 * @author Philippe PRADOS
 */
public class Policy
{
	private static final String DEFAULT_DATABASE="http://macaron-policy.googlecode.com/svn/trunk/{}.policy";
	private static final String DEFAULT_PREFIX="${webapps.home}/";
	
	private static final Pattern LOG_ACCESS=Pattern.compile("^access: access (allowed|denied) \\((.+)\\)$");
	private static final Pattern LOG_DOMAINIFALLOWED=Pattern.compile("^access: domain .*\\((.+)\\)$");
	private static final Pattern LOG_DOMAINIFDENIED=Pattern.compile("^access: domain that failed ProtectionDomain +\\((.+)\\)$");
	private static final Pattern LOG_REMOVE_PERMISSION=Pattern.compile(
		"^java\\.(lang\\.RuntimePermission \"?(define|access)ClassInPackage\\.(javax?|sun)\\.)"+
		"|(security\\.AllPermission)"+
		"|(io\\.FilePermission .+\\.(jar|class) read)");
//	private static final Pattern LOG_ACCEPT_CODEBASE=Pattern.compile("/WEB-INF/");
	private static final Pattern LOG_ACCEPT_CODEBASE=Pattern.compile("");
	
	/** The logger. Use to explain steps. */
	private static Logger log=Logger.getLogger(Policy.class.getName());
	
	/** Temp directory. */
	private static final String TEMP;
	static
	{
		final String t=System.getProperty("java.io.tmpdir");
		TEMP=(t==null) ? "." : t;
	}


	private final File outFilename_;
	/** Writer to write report. */
	private final PrintWriter out_;
	
	/** Privileges are merged ? */
	private final String merge_;
	
	/** All properties. */
	private final Properties prop_;

	/** All inverse properties. */
	private final Properties inverseProp_;
	
	/** Database url with {} */
	private final String database_;
	private boolean databaseHS=false;

	/** Log file to analyse */
	private final File log_;
	/** Policy file to update. */
	private final File policyFile_;
	private final boolean updateOutput_;
	
	/** JavaEE component to analyse. */
	private final File component_;

	/** Policy file. */
	private final PolicyFile policy_=new PolicyFile(log);
	/** Extract prefix. */
	private final String extract_;
	
	/**
	 * Exception if error in command line.
	 */
	public static class CommandLineException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public CommandLineException(String s)
		{
			super(s);
		}
	}
	
	/**
	 * Visitor in EAR,WAR ou JAR file.
	 */
	private interface Visitor
	{
		void doFileName(String idname,JarFile jarFile,ZipEntry entry) throws IOException;
	}
	
	/**
	 * A policy extractor.
	 * 
	 * @param config All parameters.
	 * @throws IOException If it's impossible to write the result.
	 */
	public Policy(ParamsConfig config) throws IOException
	{
		prop_=config.properties;
		extract_=config.extract;
		inverseProp_=config.inverseProperties;
		updateOutput_=(config.policy!=null) && config.policy.getAbsoluteFile().equals(config.output.getAbsoluteFile());
		outFilename_=(updateOutput_) ? new File(config.output.getPath()+".tmp")
				: config.output;

		if (!outFilename_.isDirectory())
		{
			final Writer out=(outFilename_.getName().equals("-"))
				? new PrintWriter(System.out)
				: new PrintWriter(new FileWriter(outFilename_));
			
			class ExVariableWriter extends VariableWriter
			{
				ExVariableWriter(Properties prop)
				{
					super(out,prop);
				}
				public void onError(final String msg, final String varname) throws VariableNotFound
				{
				}
			};
			out_=new PrintWriter(new ExVariableWriter(prop_));
		}
		else
			out_=null;
		log_=config.log;
		merge_=config.merge;
		policy_.setAcceptAllPrivileg(config.acceptAllPermission);
		policyFile_=config.policy;
		final String envDatabase=System.getenv("POLICY_DATABASE");
		database_=(config.database!=null) ? config.database 
				: (envDatabase!=null) ? envDatabase : DEFAULT_DATABASE;
		component_=config.name;
	}
	
	/**
	 * Analyse all input file and generate output policy.
	 * 
	 * @return <code>true</code> if add something.
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public boolean analyse() throws IllegalArgumentException, IOException
	{
		// Load current policy
		if (policyFile_!=null)
		{
			policy_.load(new FileReader(policyFile_));
		}
		
		// Analyse log_
		if (log_!=null)
		{
			analyseLog();
		}
		
		// Analyse component
		if (component_!=null)
			analyseComponent();
		
		if (merge_!=null && merge_.length()!=0) // Pas de merge si ""
		{
			policy_.merge(merge_);
		}
		// Save policy
		if (extract_!=null)
		{
			policy_.extractDatabase(outFilename_,extract_);
			return false;
		}
		else
		{
			final boolean update=policy_.save(out_);
			if (updateOutput_)
				outFilename_.renameTo(policyFile_);
			return update;
		}
	}

	/**
	 * Copy stream.
	 * @param in Source.
	 * @param out Destination.
	 * @throws IOException If error.
	 */
	private static void copyStream(InputStream in,OutputStream out) throws IOException
	{
		byte[] tampon=new byte[4096];
	    int len;
        while ((len = in.read(tampon)) > 0) {
            out.write(tampon, 0, len);
        }
	    out.flush();
	}

	/**
	 * Audit component.
	 * @param idname Identity name.
	 * @param jar Jar file.
	 * @param visitor Visitor.
	 * @throws IOException If error.
	 */
	private void auditOneComponent(String idname,File jar,Visitor visitor) throws IOException
	{
		JarFile jarFile=new JarFile(jar);
		try
		{
			for (Enumeration<?> entrie=jarFile.entries();entrie.hasMoreElements();)
			{
				ZipEntry entry=(ZipEntry)entrie.nextElement();
				if (entry.isDirectory()) continue;
	
				visitor.doFileName(idname,jarFile,entry);
			}
		}
		finally
		{
			jarFile.close();
		}
	}
	
	/**
	 * Analyze a jar file.
	 * 
	 * @param idName The identity name.
	 * @param baseIdName The base identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doJar(final String idName,final String baseIdName,File jar) throws IOException
	{
		log.info("Analyse "+idName);
		JarFile j=new JarFile(jar);
		ZipEntry entry=j.getEntry("META-INF/jar.policy");
		int idx=idName.indexOf('!');
		final String prefix=VariableWriter.convVariable("${prefix}", prop_, new OnConvVariable()
		{
			public void onError(final String msg, final String varname) throws VariableNotFound
			{
			}

			public void onVariable(String varname, String value)
			{
			}
		});
		
		String ctx=prefix + ((idx==-1) ? idName : idName.substring(idx+1));
		if (entry!=null)
		{
			log.fine("Insert "+entry);
			policy_.insert(new InputStreamReader(j.getInputStream(entry)),"*** Policy extract from "+ctx,ctx,merge_);
		}
		else
		{
			// Extraction depuis la database
			if (!databaseHS) // Optimize if firewall or invalid URL
			{
				try
				{
					URL url=new URL(database_.replaceFirst("\\{\\}", baseIdName));
					URLConnection con=url.openConnection();
					con.connect();
					if ("text/text".equals(con.getContentType()) || "content/unknown".equals(con.getContentType()))
					{
						log.fine("Insert "+url);
						policy_.insert(new InputStreamReader(con.getInputStream()),"*** Policy extract from "+url,ctx,merge_);
					}
				}
				catch (IOException e)
				{
					databaseHS=true;
				}
			}
		}
	}
	
	/**
	 * Analyze WAR file.
	 * 
	 * @param idName The identity name.
	 * @param baseIdName The base identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doWar(final String idName,final String baseIdName,File jar) throws IOException
	{
		log.info("Analyse "+idName);
		final int len="WEB-INF/lib/".length();
		auditOneComponent(idName,jar, new Visitor()
		{
			public void doFileName(String idName,JarFile jarFile,ZipEntry entry) throws IOException
			{
				if (entry.getName().startsWith("WEB-INF/lib/"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(TEMP+File.separator+new File(jarFile.getName()).getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						// Audit de l'archive
						doJar(idName+"!"+entry.getName(),entry.getName().substring(len),jarfile);
					}
					finally
					{
						jarfile.delete();
					}
				}
			}
		});
	}
	
	/**
	 * Analyze EAR file.
	 * 
	 * @param idName The identity name.
	 * @param baseIdName The base identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doEar(String idName,String baseIdName,File jar) throws IOException
	{
		log.info("Analyse "+idName);
		auditOneComponent(idName,jar, new Visitor()
		{
			public void doFileName(String idName,JarFile jarFile,ZipEntry entry) throws IOException
			{
				if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".war"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(jarFile.getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						if (entry.getName().endsWith(".jar"))
							doJar(idName+"!"+entry.getName(),entry.getName(),jarfile);
						else
							doWar(idName+"!"+entry.getName(),entry.getName(),jarfile);
					}
					finally
					{
						jarfile.delete();
					}
				}
			}
		});
	}
	
	/**
	 * Analyze RAR file.
	 * 
	 * @param idName The identity name.
	 * @param baseIdName The base identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doRar(String idName,String baseIdName,File jar) throws IOException
	{
		log.info("Analyse "+idName);
		auditOneComponent(idName,jar, new Visitor()
		{
			public void doFileName(String idName,JarFile jarFile,ZipEntry entry) throws IOException
			{
				if (entry.getName().endsWith(".jar"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(jarFile.getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						doJar(idName+"!"+entry.getName(),entry.getName(),jarfile);
					}
					finally
					{
						jarfile.delete();
					}
				}
			}
		});
	}

	/**
	 * Analyze all files.
	 * 
	 * @param name The component.
	 * 
	 * @throws IOException If error.
	 */
	private boolean analyseComponent() throws IOException
	{
		if (prop_.get("basename")==null)
		{
			prop_.put("basename", component_.getName().substring(0,component_.getName().lastIndexOf('.')));
		}
		if (prop_.get("prefix")==null)
		{
			prop_.put("prefix",DEFAULT_PREFIX);
		}
		if (component_.getName().endsWith(".war"))
		{
			doWar(component_.getName(),component_.getName(),component_);
		}
		else if (component_.getName().endsWith(".ear"))
		{
			doEar(component_.getName(),component_.getName(),component_);
		}
		else if (component_.getName().endsWith(".rar"))
		{
			doRar(component_.getName(),component_.getName(),component_);
		}
		else if (component_.getName().endsWith(".jar"))
		{
			doJar(component_.getName(),component_.getName(),component_);
		}
		else
		{
			throw new IllegalArgumentException("Unknown file type for "+component_.getName());
		}
		return true;
	}

	// -Djava.security.debug=access,domain,failure
	/* La démarche est la suivante, indiquer all-privilege pour chaque composant.
	 * Injecter les privileges ressorties et relancer
	 * Après chaque relance, ajouter les nouveaux droits et ainsi de suite.
	 * Pour pb XML, voir l'intégration d'un parseur std dans le WAR.
	 */
	private void analyseLog() throws IOException
	{
		boolean onlyDenied=true;
		BufferedReader reader=null;
		try
		{
			if (!log_.canRead()) return;
			reader=new BufferedReader(new FileReader(log_));
			log.info("Analyse "+log_);
			String line;
			String privilegeIfAllowed=null;
			String privilegeIfDenied=null;
			
			// Creation d'une liste de valeur des propriétés, classé par ordre de taille.
			SortedSet<String> propValues=new TreeSet<String>(new Comparator<String>()
					{
						public int compare(String o1, String o2)
						{
							return o2.length()-o1.length();
						}
					});
			// Property in inverse mode
			final Map<String,List<String>> inverseProp=new HashMap<String,List<String>>();
			for (Map.Entry<Object,Object> e:inverseProp_.entrySet())
			{
				propValues.add((String)e.getValue());
				List<String> l=inverseProp.get((String)e.getValue());
				if (l==null) l=new ArrayList<String>(); 
				l.add((String)e.getKey());
				inverseProp.put((String)e.getValue(), l);
			}
				
			while ((line=reader.readLine())!=null)
			{
				if (line.startsWith("access:"))
				{
					Matcher matcher;
					if ((privilegeIfAllowed!=null) || (privilegeIfDenied!=null))
					{
						matcher=(privilegeIfAllowed!=null) ? LOG_DOMAINIFALLOWED.matcher(line) : LOG_DOMAINIFDENIED.matcher(line);
						if (matcher.find())
						{
							String codebase=matcher.group(1);
							if (LOG_ACCEPT_CODEBASE.matcher(codebase).find())
							{
								String basename;
								if ("context is null".equals(codebase))
								{
									basename="";
								}
								else
								{
									int i=codebase.indexOf('<');
									basename=(i!=-1) ? codebase.substring(0,i-1).trim() : codebase;
								}
								//System.out.println(codebase+" "+privilegeIfAllowed);
								basename = inverseVariable(basename, propValues, inverseProp,true);
								if ((privilegeIfAllowed!=null) && !onlyDenied )
								{
									privilegeIfAllowed = inverseVariable(privilegeIfAllowed, propValues, inverseProp,false);
									policy_.addCodeBase(basename, "*** Policy extract from "+log_);
									policy_.addPerm(basename, privilegeIfAllowed,"*** Policy extract from "+log_);
									log.fine("Add "+privilegeIfAllowed+" for codebase "+basename);
								}
								if (privilegeIfDenied!=null)
								{
									privilegeIfDenied = inverseVariable(privilegeIfDenied, propValues, inverseProp,false);
									policy_.addCodeBase(basename, "*** Policy extract from "+log_);
									policy_.addPerm(basename, privilegeIfDenied,"*** Policy extract from "+log_);
									log.fine("Add "+privilegeIfDenied+" for codebase "+basename);
								}
								
							}
							if (privilegeIfAllowed!=null)
								privilegeIfAllowed=null;
							else
								privilegeIfDenied=null;
						}
					}
					matcher=LOG_ACCESS.matcher(line);
					if (matcher.find())
					{
						// Trouve un access
						String privilege=matcher.group(2);
						if (LOG_REMOVE_PERMISSION.matcher(privilege).find())
							continue;
						String[] split=privilege.split(" ");
						StringBuilder policyPrivilege=new StringBuilder();
						boolean first=true;
						for (String s:split)
						{
							if (first)
							{
								first=false;
								policyPrivilege.append(split[0]).append(' ');
							}
							else
								policyPrivilege.append('"').append(s).append("\",");
						}
						policyPrivilege.setLength(policyPrivilege.length()-1);
						if ("denied".equals(matcher.group(1)))
							privilegeIfDenied=policyPrivilege.toString();
						else
							privilegeIfAllowed=policyPrivilege.toString();
					}
				}
			}
		}
		finally
		{
			if (reader!=null)
				reader.close();
		}
		
	}

	/**
	 * Detect variable name from the value.
	 * @param policy
	 * @param stringToUpdate
	 * @param propValues
	 * @param inverseProp
	 * @param codebase
	 * @return
	 */
	private String inverseVariable(String stringToUpdate, SortedSet<String> propValues,
			final Map<String, List<String>> inverseProp,boolean codebase)
	{
		for (String value:propValues)
		{
			List<String> l=inverseProp.get(value);
			String varname=null;
			if (codebase)
			{
				for (String v:l)
				{
					varname=stringToUpdate.replace(value, "${"+v+"}");
					if (policy_.getCodebase(varname)!=null) // Trouve une variable candidate
						break;
				}
			}
			else
			{
				varname=stringToUpdate.replace(value, "${"+l.get(0)+"}");
			}
			stringToUpdate=varname;
		}
		if (stringToUpdate.startsWith("java.io.FilePermission"))
		{
			stringToUpdate=stringToUpdate.replace("/", "${file.separator}");
		}
		return stringToUpdate;
	}

	/**
	 * Print help screen.
	 * 
	 * @param out Stream to use.
	 */
	private static void help(final PrintStream out)
	{
		out.println(
		//             1         2         3        4         5         6         7
		//   0123456789012345678901234567890123467890123456789012345678901234567890123456789
			"Usage: macaron-policy [-h] \\\n"+
			"                      [-D<key>=<val>]* [-P <file.properties>]* \\\n"+
			"                      [-d <url>] \\\n"+
			"                      [-m] [--accept-all-permission] \\\n"+
			"                      [-o <-|file.policy>] \\\n"+
			"                      [-l <access.log>] \\\n"+
			"                      [--policy <file.policy>] \\\n"+
			"                      [--loglevel <severe|warning|info|...>] \\\n"+
			"                      [-x <prefix>] \\\n"+
			"                      [<file(.jar|.war|.ear|.rar)>*]\n"+
			"(-o|--output) <-|file.policy>       : The policy result file or stdout.\n"+
			"(-m|--merge)                        : Merge all privileges in\n"+
			"                                      global codebase.\n"+
			"--accept-all-permission             : Accept privileges with\n"+
			"                                      all permissions.\n"+
			"-D<key>=<val>                       : A property\n"+
			"(-P|--properties) <file.properties> : A properties file\n"+
			"(-d|--database) <url>               : The URL to database.\n"+
			"                                      {} will be convert to jar name.\n"+
			"(-l|--logaccess) <access.log>       : Log file generate with \n"+
			"                                      -Djava.security.debug=access,failure\n"+
			"--policy <file.policy>              : Policy file to update.\n"+
			"(-x|--extract) <prefix>             : extract database from all\n"+
			"                                      codebase with the prefix to output\n"+
			"                                      directory.\n"+
			"--loglevel <str>                    : Java Log level\n"+
			"                                      (severe, warning, info,fine, ...)\n"+
			"(-h|--help)                         : This help\n"+
			"\n"+
		//   0123456789012345678901234567890123467890123456789012345678901234567890123456789
			"Analyzes the component (JAR, EAR, WAR or RAR), extracts all\n"+
			"META-INF/jar.policy (or uses data from database) and generates an extract of\n"+
			"policy file to be used to launch a JVM with Java security. The result can be\n"+
			"merged in a global security context or to dispatch privileges to each jar\n"+
			"file in the component (recommended).\n"+ 
			"The variables can be converted to properties when the tool generates the\n"+
			"output file. If not already set, the variable ${basename} is set to the\n"+
			"basename of the current component to analyze. The variable ${prefix} is set\n"+
			"to the \"${webapp.home}/\" to identify the context in codebase. By convention,\n"+
			"the variables ${server.home}, ${server.lib}, ${webapp.base} and\n"+
			"${webapp.home} represent the application server directories.\n"+
			"A sample properties file to use to set this variables for Tomcat is included\n"+
			"in the distribution.\n"+
			"The environment variable POLICY_DATABASE can be set to the default database\n"+
			"URL. The database can be initialized with -x '' -o dir."
			);
		out.println();
	}
	/** Error in command line. */
	private static final int ERROR = -1;

	/** Help in command line. */
	private static final int ASK_HELP = -2;

	/**
	 * All parameters for Policy.
	 */
	public static class ParamsConfig
	{
		File name;
		File output;
		String merge;
		boolean acceptAllPermission;
		Properties properties=new Properties();
		Properties inverseProperties=new Properties();
		String database;
		File log;
		File policy;
		
		String extract;
	}	

	/**
	 * Parse command line.
	 * 
	 * @param params The config to set.
	 * @param args The args.
	 * @param pos The current position.
	 * @return ASK_HELP, ERROR or current position.
	 * @throws CommandLineException If syntax error.
	 */
	private static int parseArg(final ParamsConfig params, final String[] args, final int pos) throws CommandLineException
	{
		int position = pos;
		final String arg = args[position];
		if (arg.startsWith("-D") || arg.startsWith("-I"))
		{

			final int idx = args[position].indexOf('=');
			if (idx < 0)
				throw new CommandLineException("Syntax error with -D or -I parameter");
			final String value = args[position].substring(idx + 1);
			final String param = args[position].substring(
				2, idx);
			if (arg.charAt(1)=='D') params.properties.put(param, value);
			else params.inverseProperties.put(param, value);
		}
		else if (("-P".equals(arg) || "--properties".equals(arg)) && position < args.length - 1)
		{
			try
			{
				FileReader reader=new FileReader(args[++position]);
				params.properties.load(reader);
				reader.close();
			}
			catch (IOException e)
			{
				throw new CommandLineException(e.getMessage());
			}
		}
		else if (("-o".equals(arg) || "--output".equals(arg)) && position < args.length - 1)
		{
			params.output=new File(args[++position]);
		}
		else if (("-d".equals(arg) || "--database".equals(arg)) && position < args.length - 1)
		{
			final String value=args[++position];
			try
			{
				new URL(value);
				params.database=value;
			}
			catch (MalformedURLException x)
			{
				try
				{
					params.database = new File(value).toURI().toURL().toExternalForm();
				}
				catch (MalformedURLException xx)
				{
					throw new CommandLineException(xx.getLocalizedMessage());
				}
			}	
			params.database=params.database.replace("%7B%7D", "{}");
		}
		else if (("-m".equals(arg) || "--merge".equals(arg)) && position < args.length - 1)
		{
			params.merge=args[++position];
		}
		else if ("--accept-all-permission".equals(arg))
		{
			params.acceptAllPermission=true;
		}
		else if ("-l".equals(arg) || "--accesslog".equals(arg) && position < args.length - 1)
		{
			params.log=new File(args[++position]);
		}
		else if ("--loglevel".equals(arg) && position < args.length - 1)
		{
			log.setLevel(Level.parse(args[++position].toUpperCase()));
		}
		else if ("--policy".equals(arg) && position < args.length - 1)
		{
			params.policy=new File(args[++position]);
		}
		else if ("-x".equals(arg) || "--extract".equals(arg))
		{
			params.extract=args[++position];	
			params.extract=params.extract.replace('#','$');	// For eclipse	
		}
		else if ("-h".equals(arg) || "--help".equals(arg))
		{
			help(System.out);
			return ASK_HELP;
		}
		else
		{
			if (arg.charAt(0)=='-')
				return ERROR;
			params.name=new File(arg);
		}
		return position;
	}
	
	/**
	 * Parse command line and return configuration.
	 * 
	 * @param args The args.
	 * @return The configuration.
	 * 
	 * @throws CommandLineException If syntax error.
	 */
	public static ParamsConfig parseArgs(final String[] args) throws CommandLineException
	{
		final ParamsConfig params = new ParamsConfig();

		if (args.length==0)
		{
			help(System.out);
			return null;
		}
		for (int i = 0, j; i < args.length; i = ++j)
		{
			j = parseArg(params, args, i);
			if (j < 0)
			{
				if (j == ASK_HELP)
					return null;
				throw new CommandLineException("Unknow parameters " + args[i]);
			}
		}
		if ((params.log==null) && (params.policy==null) && (params.name==null))
		{
			throw new CommandLineException("Missing --log, --policy ou component parameter.");
		}
		if (params.output==null)
		{
			params.output=(params.policy==null) ? new File("-") : params.policy;
		}
		return params;
	}

	
	/**
	 * Analyse arguments and invoque policy. 
	 * @param args The args.
	 */
	public static int mainRet(final String[] args)
	{
		try
		{
			ParamsConfig config=parseArgs(args);
			if (config==null) return 0;
			Policy audit=new Policy(config);
			boolean update=audit.analyse();
			return (update ? 1 : 0);
		}
		catch (CommandLineException e)
		{
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		}
		catch (ZipException e)
		{
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		}
		catch (IOException e)
		{
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		}
		catch (IllegalArgumentException e)
		{
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		}
		catch (Exception e)
		{
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return 2;
	}
	
	/**
	 * JDK5 log formater.
	 * @author pprados
	 *
	 */
	public static class TinyFormatter extends Formatter 
	{
		private static final int max(int a,int b)
		{
			return (a>b) ? a : b;
		}
		private static final String format_; 
		static
		{
			int w=max(Level.SEVERE.getLocalizedName().length(),Level.WARNING.getLocalizedName().length());
			w=max(w,Level.INFO.getLocalizedName().length());
			w=max(w,Level.CONFIG.getLocalizedName().length());
			w=max(w,Level.FINE.getLocalizedName().length());
			w=max(w,Level.FINER.getLocalizedName().length());
			w=max(w,Level.FINEST.getLocalizedName().length());
			format_="%1$"+w+"s: %2$s%n";
		}
	    /**
	     * Format the given LogRecord.
	     * @param record the log record to be formatted.
	     * @return a formatted log record
	     */
	    public synchronized String format(LogRecord record) 
	    {
			final StringBuffer sb = new StringBuffer();
			final java.util.Formatter formatter = new java.util.Formatter(sb, Locale.US);
	    	formatter.format(format_,new Object[]{record.getLevel().getLocalizedName(),formatMessage(record)});
			return sb.toString();
	    }
	}

	/**
	 * Command line main.
	 * @param args
	 */
	public static void main(final String[] args)
	{
		try
		{
			LogManager.getLogManager().readConfiguration(Policy.class.getResourceAsStream("/META-INF/logging.properties"));
			System.exit(mainRet(args));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
