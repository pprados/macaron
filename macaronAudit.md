# Macaron-Audit #
This tool audit a JavaEE component and generate an XML file to identify all
suspicious files. The result file can be open with a navigator because it is
associated with a XSLT file.

![http://macaron.googlecode.com/svn/trunk/images/audit.png](http://macaron.googlecode.com/svn/trunk/images/audit.png)

A previous result can be the "ignore file", to remove all false positives alerts.
```
Usage: macaron-audit [-R] [-i <file.xml|url>] [-h] \
                    [--xslt <str>] [-o <-|file.xml>] \
                    [--loglevel <severe|warning|info|...>] \
                    <dir>|<files(.jar|.war|.ear|.rar)>)+
(-R|--recursive)             : Audit all component in directory and
                               sub-directory.
(-i|--ignore) <file.xml|url> : The xml result for ignore warning.
(-o|--output) <-|file.xml>   : The xml result file or stdout.
--xslt <str>                 : The xslt to inject.
--loglevel <str>             : Java Log level
                               (severe, warning, info,fine, ...)
(-h|--help)                  : This help

Audit JavaEE component and jar to detect suspicious files.
The environment variable AUDIT_XSLT may be the default XSLT.
```

[macaron-policy](macaronPolicy.md) [macaron-seal](macaronSeal.md)