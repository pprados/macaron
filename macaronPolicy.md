# Macaron-Policy #
This tool helps to generate the policy file to start the Java Servlet Engine with
the security set to on.

You can watch this [video](http://prados.eu/macaron/macaron.html) to understand the usage.

The shared database of policy for component is present [here](http://code.google.com/p/macaron-policy/)
```
Usage: macaron-policy [-h] \
                     [-D<key>=<val>]* [-P <file.properties>]* \
                     [-d <url>] \
                     [-m] [--accept-all-permission] \
                     [-o <-|file.policy>] \
                     [-l <access.log>] \
                     [--policy <file.policy>] \
                     [--loglevel <severe|warning|info|...>] \
                     [-x <prefix>] \
                     [<file(.jar|.war|.ear|.rar)>*]
(-o|--output) <-|file.policy>       : The policy result file or stdout.
(-m|--merge)                        : Merge all privileges in
                                      global codebase.
--accept-all-permission             : Accept privileges with
                                      all permissions.
-D<key>=<val>                       : A property
(-P|--properties) <file.properties> : A properties file
(-d|--database) <url>               : The URL to database.
                                      {} will be convert to jar name.
(-l|--logaccess) <access.log>       : Log file generate with
                                     -Djava.security.debug=access,failure
--policy <file.policy>              : Policy file to update.
(-x|--extract) <prefix>             : extract database from all
                                      codebase with the prefix to output
                                      directory.
--loglevel <str>                    : Java Log level
                                      (severe, warning, info,fine, ...)
(-h|--help)                         : This help

Analyzes the component (JAR, EAR, WAR or RAR), extracts all
META-INF/jar.policy (or uses data from database) and generates an extract of
policy file to be used to launch a JVM with Java security. The result can be
merged in a global security context or to dispatch privileges to each jar
file in the component (recommended).
The variables can be converted to properties when the tool generates the
output file. If not already set, the variable ${basename} is set to the
basename of the current component to analyze. The variable ${prefix} is set
to the "${webapp.home}/" to identify the context in codebase. By convention,
the variables ${server.home}, ${server.lib}, ${webapp.base} and
${webapp.home} represent the application server directories.
A sample properties file to use to set this variables for Tomcat is included
in the distribution.
The environment variable POLICY_DATABASE can be set to the default database
URL. The database can be initialized with -x '' -o dir.
```

[macaron-audit](macaronAudit.md) [macaron-seal](macaronSeal.md)