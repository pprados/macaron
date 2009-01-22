<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<!-- 
	* Copyright 2008 Philippe Prados.
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

    $Id$
    $Log$
-->

<xsl:stylesheet version="1.0"
	xmlns:audit="http://macaron.googlecode.com/1.0/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation=
		"http://www.w3.org/1999/XSL/Transform http://www.w3.org/2007/schema-for-xslt20.xsd
		 http://macaron.googlecode.com/1.0/ http://macaron-policy.googlecode.com/svn/trunk/audit/src/main/resources/com/googlecode/macaron/audit/audit.xsd"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://www.w3.org/1999/xhtml"
>
	<xsl:strip-space elements="*" />
	<xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no" />

	<xsl:variable name="sealed" select="count(//audit:packages/audit:package[@sealed = 'true']) &gt; 0"/>

	<xsl:template match="/">
		<html>
			<head>
				<style type="text/css">
@media screen {				
	body,html {
		font-size: medium;
		font-family: arial, sans-serif
	}
	a:link {
		color: #00c
	}
	a:active {
		color: red
	}

	.bar {
		border-top: 1px solid #c9d7f1;
		font-size: 1px;
		left: 0
	}
	.comment {
		font-style:italic
	}
	.view {
		cursor: pointer;
		text-align: center;
		vertical-align: top
	}

	.view-open {
	}

	.view-closed {
		display:none
	}

	.expander {
		cursor: pointer;
		vertical-align: top
	}

	.expander-open {
	}
	.expander-closed {
		display: none
	}

	.logo  {
		float:right
	}
	img 
	{ 
		border:none 
	}
	
	.menu {
		height: 22px;
		margin-right: .73em;
		vertical-align: top;
		text-align: center;
		cursor: pointer
	}
	.menu-selected  {
		height: 22px;
		margin-right: .73em;
		vertical-align: top;
		text-align: center;
		font-weight:bold;
		cursor: pointer;
		color:#00f
	}
	.cmd {
		height: 22px;
		margin-right: .73em;
		vertical-align: top;
		text-align: right;
		color:#00f
	}
	.header {
		display: none
	}
	.item {
		font-weight:bold
	}
	:focus 
	{ 
		outline: none 
	}
}
@media print {
	body,html {
		font-size: small;
		font-family: arial, sans-serif
	}

	a:link {
		color: #00c
	}
	a:active {
		color: red
	}

	.bar {
		display: none;
	}
	.comment {
		font-style:italic;
		page-break-after: avoid;
	}
	.view {
		vertical-align: top;
		text-align: center;
	}

	.view-open {
	}

	.view-closed {
	}

	.expander {
		vertical-align: top;
	}

	.expander-open {
	}
	.expander-closed {
	}

	.menu {
		display: none;
	}
	.menu-selected {
		display: none;
		text-align: center;
	}
	.cmd {
		display: none;
		text-align: right;
	}
	.header {
		height: 22px;
		margin-right: .73em;
		vertical-align: top;
		font-weight:bolder;
		color:#00f;
		page-break-after: avoid;
	}
	.item {
		font-weight:bold;
	}
	.never-print {
		display: none;
	}
	.logo {
		display:none;
	}
}

</style>
</head>
<body>
	<script type="text/javascript"><![CDATA[
var macaron=
{
	menus:["Components","SealedPackages","Packages","Filenames","Basenames","Services"],
	selectMenu:function(id)
	{
		for (var i=0;i != this.menus.length;++i)
		{
			var div=document.getElementById("menu"+this.menus[i]);
			if (div)
			{
				if (id==div.id)
				{
					div.className='menu-selected';
					document.getElementById(this.menus[i]).className='view-open';
				}
				else
				{
					div.className='menu';
					document.getElementById(this.menus[i]).className='view-closed';
				}
			}
		}
	},

	selectHash:function(hash)
	{
		if (hash!="")
		{
			var parent=document.getElementById(hash).parentNode;
			var next=parent;
			do { next=next.nextSibling; } while(next.nodeName=="#text");
			next.className='expender-open';
			//parent.parentNode.previousSibling.firstChild.data='\u2212';
			var prev=parent.parentNode;
			do { prev=prev.previousSibling; } while (prev.nodeName=="#text");
			prev.firstChild.data='\u2212';
			macaron.selectMenu("menu"+parent.parentNode.parentNode.parentNode.parentNode.parentNode.id);
		}
	}
}

window.onload=function()
{
	macaron.selectHash(document.location.hash.substring(1));
}

document.onclick=function(event)
{
	try 
	{
		var par=(window.event) ? window.event.srcElement : event.originalTarget;
		if (par.nodeName.toLowerCase()=='a')
		{
			if (par.hash!='')
			{
				macaron.selectHash(par.hash.substring(1));
			}
		}
		else if (par.nodeName.toLowerCase() == 'td')
		{
			if (par.className == 'menu' || par.className == 'menu-selected')
			{
				macaron.selectMenu(par.id);
			}
			if (par.className == 'expander') 
			{
				var next=par.nextSibling;
				while(next.nodeName=="#text") { next=next.nextSibling; } 
				var last=next.lastChild;
				while (last.nodeName=="#text") { last=last.previousSibling; }
				if (last.className == 'expander-closed') 
				{
					last.className = 'expander-open';
					par.firstChild.data = '\u2212';
				}
				else 
				{
					last.className = 'expander-closed';
					par.firstChild.data = '+';
				}

			}
		}
	} 
	catch (e) 
	{
	}
};

]]></script>
<table>
<tbody>
<tr>
<td class="menu-selected" id="menuComponents">Components</td>
<xsl:if test="$sealed">
<td class="menu" id="menuSealedPackages">Sealed packages</td>
</xsl:if>
<xsl:if test="not($sealed)">
<td class="menu" id="menuPackages">Packages</td>
<td class="menu" id="menuFilenames">Filenames</td>
<td class="menu" id="menuBasenames">Basenames</td>
<td class="menu" id="menuServices">Services</td>
</xsl:if>
<td class="cmd" width="100%"><a href="javascript:window.print()">Print</a></td>
<td class="cmd" ><a href="http://macaron.googlecode.com">Help</a></td>
</tr>
</tbody>
</table>
<div class="logo">
<a href="http://macaron.googlecode.com"><img alt="Macaron" src="data:image/jpeg;base64,%2F9j%2F4AAQSkZJRgABAQEASABIAAD%2F2wBDAAUDBAQEAwUEBAQFBQUGBwwIBwcHBw8LCwkMEQ8SEhEPERETFhwXExQaFRERGCEYGh0dHx8fExciJCIeJBweHx7%2F2wBDAQUFBQcGBw4ICA4eFBEUHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh7%2FwAARCAAvAEYDAREAAhEBAxEB%2F8QAGwABAAMAAwEAAAAAAAAAAAAABwQFBgACCAP%2FxAAxEAABAwMDAwIGAQMFAAAAAAABAgMEAAURBhIhBzFBUWETFCIycYGRCBUWQkNSobH%2FxAAbAQACAwEBAQAAAAAAAAAAAAAFBgIDBAABB%2F%2FEAC0RAAICAgEDAwMDBAMAAAAAAAECAAMEEQUSITETIkEGUWEUcZEVI0KBobHR%2F9oADAMBAAIRAxEAPwD2XXTp1cWhtBccWlCEjJUo4ArwnXmegEnQlTC1PZZlw%2BRjzEqdP2kjCVH0B8mqlvRjoGa7MC%2BuvrZe0l365xrNZ5V1mK2x4rZccPsKlbYK0Ln4meqs2uEHzD%2B19YrDPdAZVGUgnACZSSr%2BKD%2F1nR9yECGTwba9rgyTq3qI2w1EZshQp%2BQFKUpYz8MAgYx5NQzeY6UU0%2BTLMHhS7Mb%2FAAP%2BZ00%2FrqQ0UG7vJcZJ%2Bte0JKffiq8Tln2Bb3E9y%2BJXR9Id5uo1yYf5TnaT9J9fejyXBxsRfZek6Mm1bIz5yZEeK0XZL7TLY7rcWEj%2BTUWdVG2OpJEZzpRswc65a0ehLjMwXfmbetvdmMsK3rBwUkjt47kUJzcnqICN2jh9P8chVnsHvHgEf9bhJCv0q43mOZt0RAQHAppiOCtLGDne4ofcrj2A96xAu%2FZP5hy3ENSM9g2ddh%2F7FOFqQXq3SYn9xbuzTuUSGVOgLPPI9DQyy%2B9CVsY9%2FMUPS9JwyjREIdZsx7TqOHEtVqtMyPKXh1uW2UPxjjOSBwpOPI81OgLYD7iIdwLL8ywV9I%2FJ%2FE17sYuWaC1F%2BWhLhha0qbb4Xv5IVk5xwO1bLMVXqCk%2BPmb0rWu5iNnfbX7TOwLnqK%2FLVbHY7bCEOhLxbVuO0HOM9uR%2F0a8p4osQS0rtKgMANGNul9SF1z5FbCiWwOUnIArQcp8NhXZ3iflYPSercQbDKVJYUCOEng0YxL%2FWXcFuvSdQP63agmL1W9DcKwxFwltGfp%2FOPWlrmL3e4ofAjx9PYlYoDjyYa3q4O3C0vxgwtx1aDsS2PqJ9KG4xb1VAjE9YqUuDLrT1tiMWxlLDSEp2DgU5oFCwQ7ncoLvp6dddcWyHpqeuBJC9811rsloeo7E%2BBWDO9IJthuCeU7qrDsYpaw0xY49mRJkBa7nFjOPIluKKlJSlOVFXjBwOKCIPTICfMz8ZyFuPkBh4PY%2FsYa%2F5e2NOLdeYfaIbJ3rSQk8epoh6p106j82Ggf1g3tkHpvqOOLdc3N4Djyy62v1ylIx%2BQQR%2BqL1tpYu6FljN8bjL0faQnSsWUSFuyMvOrJyVFXOP12%2FVLNlnq5bFvg6i7yzEsRFXTCyXXEg8FOT%2BaZOPbewIvHzMF%2FUJYJsyFCnWa2MyJJcKZC1JVnZjjG3zn1qrlMdW0%2FTsw5w2W9TFOvQ%2FMA2tK3iXeUPXiU9aosV1DqUM5S4tYORySeM4obQorOwujGp6zlV%2B5tj7CStcOkxFJsnxxOSCW20KUovHPPAOT5rT%2BoCt7j2ms4VlWM1gOtfeROlruptGPXO%2FaktDx%2BbKENKWQFIABJyO6U5I5PmsWbkVXFRU4JEUrVttJayXDvUJvUt3essUiQtxG6a8lJ2lIP0tIz4z3PmoV4zbDtCXA01NeWP%2BI3LlcaO5bVpcYSUuAoUndnPfvRDpHTGI2N6utw50p0mmzZ9wlwr27AtTjyvgpQCpSjnB8gcHPNYsnl1pHQF2RAWZjCjIbpbQPfUZ%2BmNvn6QtYtMy4fPW9rJbf2YdQO%2B1Q5yM%2BaE%2FrBZd6hGt%2BYOysYWrpe5%2FMbNJtkwBLU2pHxgCkKGFY9xTjxa%2F2uvXmLNy9Dld%2BJcnkYNE5VPOXXm2IsuqFTZM3KZqVOMNJQT8Mdj7ZJP8UDzk6HP5n0T6YvW6oDp7r2mY0VbHpEJMph9UUPJBdlBILq8%2F7aM%2Fakf%2B0q5jnrPVNPI5T2XFT4HxLXU9ktCrczAEZwyJLyGUPuOqK8qUBnJPP4qGEj3Xqo%2BTBN97V0uzHsB4irpLS1msNvbjW6EzGQEYUUIG9w%2F8lKxkmvpCVLWoVRERrWY9RMyHWfTrjcFmdYYylzZLoYU0gHCyQSDgeeKG59GtMg7mOf01yzAtVc3tUbBPx%2BJS9MZjStMtQ1lCX42WnkD%2FAErB5pKya%2Bm1gfvNWUxtf1B8zc6ejl%2B6MISMhSwOfNS4%2BgvkKo%2B8H5b9NRi0AEgAAADgAV9GA12EVpyunTG9S9BQtaMsKdfMeTHBS25t3DB8EVlycUXgd%2B8M8RzD8axIGwYXy9H3rQYahpmR7hBcy4C4k5Qon7UjIwOM0r8rinH1sb3DQ5GvkbC4HSZGkaV1DqW5wZzdyQyYjiXmkhICEEHOcef3UOLS2y5SgAmTOspqqZT33E61TH32Ap6I4yvOMbkkK9xz2%2FNPMTg25f2JiQ7JccktISwE4QnOTu96i2pOvqJO%2FEO9T9M58K9Sblp9iO9GfcLqmi98JbaickA9iMnNK3I8PZZYbKvmM%2BFylYrCW9tf7mk0DpW7xJjdxvJZZDYJbjtuFxRUR9ylduM9hV%2FF8S9D%2BrZ5%2B0zZ%2BfXYvRX%2FAD4m%2Bpggef%2FZ" /></a>
</div>
<br/>
<div class="bar" ></div>


<div id="Components" class="view-open">
<h1 class="header">Components</h1>
<p class="comment">All components with warning. The class loader can isolate each version.</p>
<table>
<tbody>
<xsl:call-template name="context"/>
</tbody>
</table>
</div>

<div id="SealedPackages" class="never-print view-closed">
<h1 class="header">Sealed Packages</h1>
<p class="comment">Packages sealed.<br/>
All sealed packages. A sealed package is declared in META-INF/MANIFEST.MF.</p>
<xsl:if test="count(//audit:package) = 0">
<p class="item">Good news! No problem detected.</p>
</xsl:if>
<table>
<tbody>
<xsl:for-each select="//audit:package">
	<xsl:sort order="ascending" select="@name"/>
	<xsl:call-template name="audit">
		<xsl:with-param name="node" select="."/>
		<xsl:with-param name="prefix" select="'S_'"/>
	</xsl:call-template>
</xsl:for-each>
</tbody>
</table>
</div>

<xsl:if test="not($sealed)">
<div id="Packages" class="view-closed">
<h1 class="header">Packages</h1>
<p class="comment">Packages share by different components.<br />
May be different NLS versions of a classes, a same version of a classe include in different components, 
or a back-door. The class loader can isolate each version.</p>
<xsl:if test="count(//audit:package) = 0">
<p class="item">Good news! No problem detected.</p>
</xsl:if>
<table>
<tbody>
<xsl:for-each select="//audit:package">
	<xsl:sort order="ascending" select="@name"/>
	<xsl:call-template name="audit">
		<xsl:with-param name="node" select="."/>
	</xsl:call-template>
</xsl:for-each>
</tbody>
</table>
</div>

<div id="Filenames" class="view-closed">
<h1 class="header">Filenames</h1>
<p class="comment">Same file name use in different packages.<br />
May be a different parameters files can be localized in different places
or a back-door parameter file to inject code in a framework. The class loader can isolate each version.</p>

<xsl:if test="count(//audit:filename) = 0">
<p class="item">Good news! No problem detected.</p>
</xsl:if>
<table>
<tbody>
<xsl:for-each select="//audit:filename">
	<xsl:sort order="ascending" select="@name"/>
	<xsl:call-template name="audit">
		<xsl:with-param name="node" select="."/>
	</xsl:call-template>
</xsl:for-each>
</tbody>
</table>
</div>

<div id="Basenames" class="view-closed">
<h1 class="header">Basenames</h1>
<p class="comment">Same base file name use in same packages.<br />
May be a very different files,
or a back-door java class to substitute a properties or others files.
</p>
<xsl:if test="count(//audit:basename) = 0">
<p class="item">Good news! No problem detected.</p>
</xsl:if>
<table>
<tbody>
<xsl:for-each select="//audit:basename">
	<xsl:sort order="ascending" select="@name"/>
	<xsl:call-template name="audit">
		<xsl:with-param name="node" select="."/>
	</xsl:call-template>
</xsl:for-each>
</tbody>
</table>
</div>

<div id="Services" class="view-closed">
<h1 class="header">Services</h1>
<p class="comment">JAR services.<br />
Services published in META-INF/services. May be inject code and backdoor. 
</p>
<xsl:if test="count(//audit:service) = 0">
<p class="item">Good news! No services detected.</p>
</xsl:if>
<table>
<tbody>
<xsl:for-each select="//audit:service">
	<xsl:sort order="ascending" select="@name"/>
	<xsl:call-template name="audit">
		<xsl:with-param name="node" select="."/>
	</xsl:call-template>
</xsl:for-each>
</tbody>
</table>
</div>

</xsl:if>

</body>
</html>
</xsl:template>

<xsl:key name="context-key" match="//audit:context" use="text()" />
<xsl:variable name="unique-context"
                select="//audit:context[generate-id()=generate-id(key('context-key',text()))]" />

<xsl:template name="context">
	<xsl:for-each select="$unique-context">
	<xsl:sort order="ascending"/>
	
	<xsl:variable name="current" select="text()"/>

	<xsl:variable name="parent" select="//audit:context[text() = $current]/../@name"/>

			<tr>
				<td valign="top" class="expander">
					<xsl:choose>
						<xsl:when test="count($parent) &gt; 0">+</xsl:when>
						<xsl:otherwise>
						</xsl:otherwise>
					</xsl:choose>
				</td>
				<td>
					<div>
						<a name="{text()}" id="{text()}" class="item"><xsl:value-of select="text()" /></a>
					</div>
					<div class="expander-closed">
						<xsl:variable name="packages" select="//audit:package/audit:context[text() = $current]"/>
						<xsl:variable name="filenames" select="//audit:filename/audit:context[text() = $current]"/>
						<xsl:variable name="basenames" select="//audit:basename/audit:context[text() = $current]"/>
						<xsl:variable name="services" select="//audit:service/audit:context[text() = $current]"/>
						<xsl:if test="$sealed">
							Sealed packages:<br/>
							<xsl:for-each select="$packages">
								<span class="view"><a href="#S_{../@name}"><xsl:value-of select="../@name"/></a><br/></span>
							</xsl:for-each>
						</xsl:if>
						
						<xsl:if test="not($sealed)">
							<xsl:if test="count($packages) &gt; 0">
								Shared packages:<br/>
								<xsl:for-each select="$packages">
									<span class="view"><a href="#{../@name}"><xsl:value-of select="../@name"/></a><br/></span>
								</xsl:for-each>
							</xsl:if>
							
							<xsl:if test="count($filenames) &gt; 0">
								Shared filenames:<br/>
								<xsl:for-each select="$filenames">
									<span class="view"><a href="#{../@name}"><xsl:value-of select="../@name"/></a><br/></span>
								</xsl:for-each>
							</xsl:if>
							
							<xsl:if test="count($basenames) &gt; 0">
								Shared basenames:<br/>
								<xsl:for-each select="$basenames">
									<span class="view"><a href="#{../@name}"><xsl:value-of select="../@name"/></a><br/></span>
								</xsl:for-each>
							</xsl:if>

							<xsl:if test="count($services) &gt; 0">
								Published services:<br/>
								<xsl:for-each select="$services">
									<span class="view"><a href="#{../@name}"><xsl:value-of select="../@name"/></a><br/></span>
								</xsl:for-each>
							</xsl:if>
						</xsl:if>
					</div>
				</td>
			</tr>
	</xsl:for-each>
</xsl:template>

<!--  ******************** -->

<xsl:template match="audit:context">
	<span class="view"><a href="#{text()}"><xsl:value-of select="text()" /></a><br /></span>
</xsl:template>

<xsl:template name="audit">
	<xsl:param name="audit"/>
	<xsl:param name="prefix"/>

	<tr>
	<td valign="top" class="expander"><xsl:if test="./*">+</xsl:if></td>
	<td>
	<div><a name="{$prefix}{@name}" id="{$prefix}{@name}" class="item"><xsl:value-of select="@name" /></a></div>
	<div class="expander-closed"><xsl:apply-templates select="./audit:context" /></div>
	</td>
	</tr>
</xsl:template>


</xsl:stylesheet>
