# Macaron-Seal #
This tool add `sealed:true` in all `META-INF/MANIFEST.MF` in a JavaEE component.

```
Usage: macaron-seal [-i] [-k] [-s] [-R] [-a] [-l <int>] \
                   [-I <-|file|url>] [-o <file>] [-h] \
                   [--xslt <str>] \
                   [--loglevel <severe|warning|info|...>] \
                   <dir>|<files(.jar|.war|.ear|.rar)>)+
(-o|--output <file>)       : The output jar.
(-i|--in-place)            : Update the archive in place.
(-I|--ignore) <-|file|url> : Update the archive in place.
(-k|--keep-original)       : Keep a copy of original in <name>.original.
(-s|--sha1)                : If present, update the <name>.sha file.
(-R|--recursive)           : Seale all component in directory and
                             sub-directory.
(-a|--audit)               : Audit the component.
--xslt <str>               : The xslt to inject in xml result.
(-l|--loglevel) <str>      : Java Log level (severe, warning, info, fine, ...)
(-h|--help)                : This help

Add "Sealed: true" attribute in each META-INF/MANIFEST.MF in jar or inner jar.

```
It's possible to combine [macaron-seal](macaronSeal.md) with [macaron-audit](macaronAudit.md) to seal only the package not shared
in different components.
For example, to have a harderned Tomcat, use
```
macaron-audit ‑‑output ‑ ‑R $CATALINA_HOME | \
 macaron-seal  ‑‑ignore ‑ ‑R $CATALINA_HOME ‑‑in‑place
```

[macaron-audit](macaronAudit.md) [macaron-policy](macaronPolicy.md)