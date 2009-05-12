<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<!-- 
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

    $Id$
    $Log$
-->

<xsl:stylesheet version="1.0"
	xmlns:audit="http://macaron.googlecode.com/1.0/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation=
		"http://www.w3.org/1999/XSL/Transform http://www.w3.org/2007/schema-for-xslt20.xsd
		 http://macaron.googlecode.com/1.0/ http://macaron.googlecode.com/svn/trunk/audit/src/main/resources/com/googlecode/macaron/audit/audit.xsd"
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
<td class="menu" id="menuPackages">Packages</td>
<td class="menu" id="menuFilenames">Filenames</td>
<td class="menu" id="menuBasenames">Basenames</td>
<td class="menu" id="menuServices">Services</td>
<td class="cmd" width="100%"><a href="javascript:window.print()">Print</a></td>
<td class="cmd" ><a href="http://macaron.googlecode.com">Help</a></td>
</tr>
</tbody>
</table>
<div class="logo">
<a href="http://macaron.googlecode.com"><img width="70" height="43" alt="Macaron" src="data:image/png,%89PNG%0D%0A%1A%0A%00%00%00%0DIHDR%00%00%00F%00%00%00%2B%08%06%00%00%00%C5D%FE%00%00%00%00%01sRGB%00%AE%CE%1C%E9%00%00%00%06bKGD%00%FF%00%FF%00%FF%A0%BD%A7%93%00%00%1C%C5IDATh%DE%CDzy%98%5DE%B5%EFoU%D5%9E%CE%7CN%0F%A7%BB%D3%DD%99%3B3%09%19%24aH%40%04%04b%00%99%14%11APD%01%BD%88%82%0C%0FA%7D%97%C1%CB%A4p%11%AF%C8%2C%20%A32%84%C0%959%09S%20s%3Ac'%9D%9E%FB%F4p%E6%3D%D5z%7F%04%BC%E0%F5%3D%D1%A7%BE%B7%BEo%7F%DF%AEa%AF%5D%F5%ABU%AB%D6%FE%AD%0D%FC%15r%C3WO%07%00%FC%DBWO%C7mg%9FF%00p%FB9%A7%E1%96%2F%9C%03%00%F8%E9%A2%A3%C4%87%7Do%FD%DAwR%F7%2C%3A%F2%C4%DF%CEYx%EFs%0B%17%3D%BBr%E9%17%BE%DB%F9z%BB%03%00%3B%AE%3C%95%3E%AA%F7G%40%F2%C1%2B%2FK%BDz%E5y%F1Af%F3%E1%9Bo%89%7D%D8v%E7%01%B5%F4%CC%ADw%02%00%B6n%ED%C7%8B%F7%FEL%BCv%FD%E9%EA%8Dk%3Ek%BCv%F7%0F%E5%9B%AB%DE%04%00%BC%FA%F8%AF%F1%F7%14%FAk%1F%F8%C5E%17%E2%DC%1Bo%05%00%DCv%E6)%E8%D3%C05%F7%3E%82%1BN%3C%8B%BE%F7%D8%AF%99%99q%DB%D4E%DF%B1r%C3%DF%CD%94%CB%CDI%0Aa%DB%06%A2%8D%CD%A0%193%EE%9F%F3%F0%9D_%06%80%C7%EE~x*o%7Bei%D0%DD%3E%DD%03%CD%F3%83PVz%FB%CB%99i%D3%FBG%3B%3BRV%24%F9d%94%CD%07O%7D%F0%D9n%00Xq%D7O%F1%F6%0D%17%E3%F2%CD%FF%7DLo%3Cy%3F%0E%3A%FE%F4%7F%3E0%97%9Ct%22v%3E%FA%18~%FBAy%CD%8AW%23%BD%A5%8A%3C%E6%84%A3%0A%1F%EDw%D3%5B%EFe%C4%C9_%F9%B5%DD%DF%B3l%82i%22i%01%F0BD%9C%18%CCt%06%A5%98%05%5E8%EB%C47%12%C2%C9%8Ct%FDt%A4%7D%5DC%C5%F5%91%9D0%01%A5%3D%BB%B0s%E3n%CC%3B%E98%24Z%5B%20%3C%0F*%D9%FC%C0%E1%17%FE%8F%8F%CDx%C5%5D%3F%3B%D8%E9%7D%F5%10Q%1D%8C%06%13%0F%7F%7D%F1%19%97%2F'%22%BC%3C%C0%B0_%B8%1E%0BO%BB%E4%9F%03%CC%95%A7%1C%87%8D%8F%3C%85%C7%3F(%3F%F9%F0%8A%FAG%2F%FF%F2%13%E3%A2%94l%9E%B4%DFU%03%D2%EAN%5B%C1%9E7%7F%F8%AC%3B%F9%98%83%1Fk%DA%BAyq%9D%60%1E%D7%90%60%AE%94%84iH%D8%C9zH%02%97*E%EAH%18%A3%85%23%A6%3BC%5B%B7%98q%8B%82R%60c%EC%EC%A9%02%D1%24%CA%83y4%B4%D6pq%F7%0E%E6JYe%16%1F%B5a%FE%A9%97%EE%B7%BC%07F%F8%CC%B5%DFrx%E4hv%1A%16%C9%E1%CE%98%ED%EE%81%88%C7%AB%5E%FD%A2vC%D8%17.%3C%FE%8CW%01%A0Z.%C3%8ED%FE%F1%C0%FC%F8%F6%FF%C0%15%DF%3C%07O%FC%E8%01%EB%B9%87.%BB%D5ID%BE%EE%95%8A%A8%0B%8B%A8%CFf%D1%D9%3F%88%9Ap%A4bN%3Cx%40%BD%D4%DE%3A%A5%94CLE%D9V%06%CCd%82%E2%F5%AD%2C%7C%9F%FCb%17%0F%05EtCQeI%2B%EA%E7N%D4%E9%B6I4%BCi%2B%C2%C2%08e%E7%1E%C0A%DFnr%B7%BC%C7%83%7B%FB(6m%22%D23g%A2%20%C6%DE6%5C%C412%C8%8FO%9BC%40m%1B%3Ck%0CP%19%81%1A%DD%0D%23%D3%0C%1D%20g%8D%EC%187%E7%EC%1B%8Bo%7C%F1%7C2%89%60Z%12%B2%A9%91g%FE%E4o%B3%20%F5%A7%15o%3F%F18%16%9C%F0y%00%C0%F9%17_IW%7C%F3%1C~%949%F9%E6%89G%DE3%DC%BF%E7%B8%EA%00%A3i%FC%04%B8H!%D56%05%D5j%1E%F5%B1V'%3E%A5%B6%D5%89%09%C4%9Fx%833%A6%247%9E%80%15KC%B8%1EU%86v%A2%A8%3C%0A%E1p%8D%17%60%C7%A6%0E%88%85%FB%11t%40%D5%8E%BD%88gR%08s9*%F6%07H%CE%3D%9C%EA%EDga%3A%03%5C%DD%BB%83%8A%96%FC%96%8A6%40Y%0E%AA%B2%11%96%5Bbi%F8%90%B6A%065%C0%8C%5B%A8%E4%86kFc%E3%8E%01%F0%C8A%BF%F99%FF%B9%89%BE%FD%B5K%B0%E0%97%D7%FD%F5%16%B3%E2%D4%FDq%E4%C3%EF%FD%D9Ng%1F%B6%F0Wu%EE%E8W%0D%A5QW%93%E6Q%17d%C6%E2%3C%FF%C4e%E4%E7%FA%11%AF%ABe%7F%B0%17JV%A8z%DF%0A%8E%BF%B6%9D2%A9%06%B0%08Pu%CB%5C%B1%A2%90%D1%18%92%01S%CE%CD%A3k%86%85%BACfsl%E2xB9%8Fds%0D%8A%9DC%CC%A5a%8Ae%14%97%F6vP%DFp%19%03%D5%2C%B23f%85%D1%09%13%84R%8A%944Y(%1BJ%04%24%25%B3%80%0F.%8F%D2%D0%AE%5D(%E6y%B7%D3%DF%7C%16%ED%E9n5%0Di%99%B6%0A%A8%B5%F5%F5%FD%AE%FE%97%AD%7F%04%E7%F2%EB%B1%E0'%DF%FF%E4%C0%DC6%BB%96%BE%B5v%90%01%E0%91s%BF%92%AE%A7%EE%C9%96%F03%0B%2Bk%B6%5E%E0%9DrjO%E7%B6%FFy%CA%E2)h%9E%3B%8B%7D%12(%0F%97%60%09%1FN%5D%3D%D9%F14G%1C%1B%DEh%3F%86%D6%BE%81%CE%B5%9B%E1%BF%BE%01%F5%C9ZT%86G%60%F8%16%C6%19%F5H%B8D%22%C8cgKS%F7%8Ez%2FUk%F6F2%13%B30%236sXB0%CAHs%8E%D4H%3F%ABh%0Al%18%606%C0%F5%AD%40%DB%1C%C8%ECxH%23%02%96%0A%00%13%07U%D6%03%BBP%DC%B9%1B%C5%F5C%24%DB%CB0%5D%01e%D8%90%D2%02%09%8D%D0%B2%10dj_%C1%84%C6%CB%E6%DF%F4%A3%95%00%D0%C7%8C%2C%FD%E53%87%AE%05%E8R%80%0F%3F%E2%9C%89g%BAO%FF%CB%A4%DA%F0%60%D2%EEL%83%03%19KeQ%88%D4%F13%9D%822%99%14%1Fp%E4A%08ci*n%DF%8CD%3A%C3%A9i%F3%A9%B8%EE%25%CE%8F%16i%C4w1%B8e-W%CB.%94%0C%D03%02%92R%A2%C9%D1%5C3T%A1%E6%FE%04%EAD%E3%DD%C37%3Ex%E9%8E%07.y8%5E%1B.%B1%9B%2C%F8%95%0A%0F%BC%FF%3A5%E6%F7pK%13%08%D2d!MR%11%1B%A6%152%94%0D%0F%09TR%93H%B7%CE%06%19%60%04%1E%D1%E0%06.%8F%94%A9%F7%E9%0D%C8l%B7%D8%8Ef%09BA%C5%92%D0%96%02%11%81C%06I%09%3F%E1%C0mi8i%E1%AFo%7Bl%E5%C2%CF%CA%03W%2F%0F%FF%A2%8F%B9%14%E0%CB%2F%B9%A9u%FC%BBw%3C%DBH%03m%AA%1C%C22%08v%D4%D4%24B%AA)t%D1i5!%AF%ED%A9Pqu%8E%23S%E7s%24%12%85%CA%A4%11%1A%12%EB%DFz%07%EB%5E%7F%8B3%B5)%8C%A9%B51%3C%C0%80%14%18%13%F7Y%18%16J9%81%AAd%E8%19yT'%CD%5C%B7%F8%A8%86%BE%DF%9E%B8l%17%C6g%97%A4%A6%CC%C0%9E%CD%5B9%19%94i%5C%0B%20%93%12%ACCHS%B3%E9%84%40%E8Bs%00%03%02%C1%DE%B5%5CD%09*%E6%80D%1CZX%18%1E%8E%F3%E8%FA%B7%D0d%F9%88%08%8B%D9p%40D%20%97!%8D8%20%24%E9pD%8B%EE%92%D0%85%A1%F3J%CC%8FE%89%C2O%E4%7C%9F%BF%EC%9B%0B%87W%DF~g3%B6%B7E%A2%CCL%043%E2%90%11W%22p%87%C0V%88t%3D%D1%1C%AAb%CF%DE%0DT%C9%24%11%3A%B5%08d%19%9B%DE%7F%0B%CF%AF%DAF%19%19%07%FB!%B6%EF%A9%40%05%3E%1C%93%91%AC%B3%40A%11%A5R%14N%94%B8%94%1B%A5!%B9%E6%DB%CC%FC%8B%D3%88%BE%B6L%1D%F1vw%BE%FA%8Dm%AF%BF%3A%EB%A8%BAA%1Dm%B0D%08%00%A1%26a%8A%7D%2B.m%40H%101%ECR%1F%0A%A51p%13%E3%A0H%C1s%EAid%F8%3D%18%11%0D%DFg%20_%81%93%B6%20%A4%86r%EA%A0%AB%1Etq%08a8B~%C9%85%A6%B8w%F3%3E%D7%C1%CC%8Cw%7Fw%03%AA%E5%3E%82a%C3%B4%92%3C%E6%80c1%A6~%C6%7F%01%B3%FD%8D%17%9F%9D%16nK'%D2%04R%04'.%E1%24%05%A4%1D%E1%A0R%24%B2%18%DA2Y%CA%00%18%AD%A0%7B%F5%8Bdg%04%9C%9A%2Cww%B9T%CA%8Dr2b%93g%08%84%15%E6x%DA%82-%5C%E4%F6%96%C8%06%90%10U.%F7TQQ%3E%06%CA%05k%CB%13%97%CF%7C%90O%7F%8F%E8%FE%DB%AFXx%B0i%F7%F5%DF%A4Z%85%26%83I%84%8A%60%98%2C%94I%24%08%80%C5%D2%94%90%BA%84%A10B%3D%B9%08%02%F6%D9%CFw%91%08v%B0%92%15%B2%0FIc%B4'%E0%A0%87Q%2B%5C%18%82IK%0B%81.1%7B%23%04xT%B5%0D%94%23b%F7%E5D%BC%1CPW%8D%A3%E0%9A%DD%00%80%8F%9C%60%DF%C7%FB%2F%DE%889%9F%B9%08%8F_%0A%A8%A1J%60%26S%04%D36%20MA%24%C0A%D5%07%D1(%1C%87%60%A4%A2%AC%89Qv%19%A6%14%90%3E%B3%A9%15%E5s!z%07%5Ct%05%02V%99%A1*%1E7%98%40d%18p%E2%02A%3E%60X%82%C8%AC%40%07%3Em%19%06%06%FBGR%B1%87n%BD%5BI%B3%DC%FE%F4%D8%C1%BE%0D%7B%A6u%BD%C0%E8%EDT%B2%A5Y%23%96%05%C8L%00%CA%02X%B2%90%0A%0A%1E%F6%EEMbW%D8%C8%05%8ER%A1%BD%03%26%97%60%A5%93%88%AB%00y%19a%DD%16E%D8%9C%87%D3M%B0%87%13%1C%0C%EE%A5%B0%1A%C0%0B%81%AA%17%A0%9A%8CB%A7R9%00%F8%2C%10%6070%F1%01%A6%5B%DC%A7%EA%84%C5f%7Fp%FC%C0%99_!w%CEg.%02%00L8%E0x%A8x2%BE%5DI5%5B%99%0Ad%10%20%08%8E%A1!-%89%5E8x%E7%DD%90%B6%19I%1E%5E%B0%10%DA%89C%945e%92i%7C%AA~%2C%C4%3D%CF%60%EF%1B%2B!%E2%84%021U5q%2B%070%60%00%10Tq%81%A2'Q%1A%E3%A39%C3%3C%3F%AB%EC%C9%FBY%D3%94S%81%15%09%10_%AC%D0%3C%2B%A1%FB%FFP%14%03%BB%98I%86%88%D4%B9%20%23%06%22%93%C8%2Br%FB%C6Al%1C%CC%C0J1i%DD%87%A08%0A%DB1%60)%C0%0B%08%3E%12%147%24%F7%7B%02~%BD%81%E6%92%26%DDk%00%CAD%E0WP(i%242iD%8D%C96%00%AC%BE%E3%D3%E7%0A%AB%F7dx%E3'%181S%9A%8E!%C7%E2%8A%60%FD%D3%9F%DE%13p%DBM%FB%7F%EE%8E'%E6%9C%F0%24T%9F%C8%DE9d%0C%DC%96qF%10%9A%06%22Q%9B%86%B4%81%DFG%5B%B0%3Co%A1oo%07%E2%E3%9A%10%9F%3D%0FF4%85Ta%00%7D%91%14v%C6%A2p%0F%9C%84%CC%86v%B8%83%15%EC%F5%02xy%0F%88%12vU%3C%94%3C%C0%B4%A2%980%AB%8A%23%CF%01%EAkR%A8%CD%D6%C3H%10%FC%E2%08k%1F%08%03F%A2U%88%E8I%16%FA%1E*%20%DF%E3%A02R%84%A4%0A%AC%88%81%B0%C4X%F9%AE%86%1AcA%05!%8A%BD%83p%1C%02%8C4J%9DE%24%EB%05%D2I%0F%B9%CE*%84%25Q%A0%22%02%D3C%8D%A1%11FR%E0h%1Ci%BB%08O%98%C5%CA%FE%C3%915g4%BD'x%F5%1C%A5%08%89%9A%14%B4v%01%12%60(%90%CE%8F%F5%AB%7B%0FY%FB%F8%E7%7F0%FB%F3%8F_K%00%F0%EFK%E7%2F%3F%20%D2qTs%DA%0D%BAdT%DE%9C%9AL%ED%D3%A6%B3S%93%22%A1%09%B0%08Ia%81%A4%04%2C%0B%D24%A0%02%9F%13v%84F6n%E2%FE%E5%AB%A8aO%11%99H%046E%60'b(%96%09%BB%B6l%C6%B2%A3%3By%C9%B1%26%09e%B0%1D%95%A4%1C%13%F0%00%F2%25%10(%C8%B8%09%EDVy%E8%CD%01%F2V%19%D0%3A%84%02%B3%19%8B%60%60%40%D3%DD%9B%15fNh%86S%1A%81)%18%F1%94%C1%E5%20JR%7BlH%10%DC%1C%8C%9A%0CD%D5E%3E%3F%0A%9131'%D7%88H%AE%8F%AB%9EG%F9%D0%C4%DE%C5%0D%5E%CBy%DD2%9E%18%94%9A%24%94PHd%E2%1C%86La(%C1%A1%C9%60%80%ABUr%DD%BAmy%7D%F1%A1%12%00%9A%DB%0E%5C%8F%91%C2%E7%CA%B9r%EA%97%C91%D4%B1%60%16%EA%EB%D2%10%A6%09%CB1aH%09i%9A%B0l%07%D2%20%80%19%04F%04%A0TC%1A%03%F9~%E4_%DC%84%043R%0D%B5pR1d%02%1F%5B%BAwc%E9%D2%0Ad%09T%E8%91%10EF~%9B%8Bd%A3%0Da%A6%20%AC(%A42A2%A0%60%D8%D5%AAW%94%1C%03%80%AD%94%ADlz%7Fw%C8%5Bu%0C%23%D0h%8E%2B%D8%C2%40%C40%A1F%FB%C81%CA%E0%E1%12%DC%12!%1E%F1P%E8%19%C6%DA%1D.*%F1%04%9A%C6fA%E5%0A*-m%F4%8Ee%B1%5C%B2I%8D%9F%5B%10%A6%15%D1%CAPdD%00%26%0D%82%80%80%84%40%40%92%88%14B%F6%D9%AEq%83%DCs%EA%C6%23%17%C8%8B%9E~t%EDw%8E8%E5%82%E7so%3F%D23s%A2%CAf%D3PdA%08%83%0C%1D%40%08%09R%12B%99%08)%84%14%04%85%90%99%24lK%20e%C7h%60%B8%8A%11w%14%C5%E2%0E8%96DX%09P%C8%97P%AE%80%EB%23!%F2%03%0A%9A%14%D5O4aDS%80%88%03!%10V%0B%F0%0Ay%04Vf%A8'3%ED%5E%B1%AB%FD%B3%C5%C2%F0%F4%10%0A%3BsLmu%02%AFn%CBa%17%87X0%B1%01%AD%5Cb%15ZPe%1B%92%5D2%22y%F4%0EG%11i%99%8F%A5%ADcQ%97n%81%9Dv%80%F9%82%23%B6%85O%0D%8D%C2%B3m%88%C1%B5%B0%9A%5CA%CA%04)%0B%00H%08%02%111)%03%D0%CC%80%17%84%85%C0%60%AD%9A%D5%CBe%9B%00%60%DB%60Wc9%13S5%B5q%08%12%10%A6IJH%B0%B2%C1J%C0d%0D%3F(B%5B6HI%D8L%14%956%BCr%99%E6%24Z%D1%FA%BD%F3%90ml%82%9DJ%C2%B4%04T%18%60%60%EF.%10%F7%92%11%DD%84l%E3F%12%BE%0F%0A4%82%81Q%B0%19%00~%00%B7k%00%D5R%09%5B%D7%CA%C4%EE%F7v%9DK%E5%20*9%0A%D2%E0I%19%9B%C2%B2%8F%E3%B2Ql%18(%60%C3%BAN%ACgP%C6%96%E0%40%93R%C0(%05Xz%F418p%CE%3C%18%FD%1A%DAW%F0%CA%3E%10qHz!%1A%E21%D2t%1C%DC%0D3%C0%C6%93%B0%C7%16%40%24%C0*%06%82%09%B0%20R%0Cv%CB%E4%B9%0A%18%0D%A0%84%B2U%A1X%20%00(%95F%C7q%8A%60)%07%86%12%10%92%19%86%82g%D8%10%0A%40%A0aP%08%C7%B0%00i%C1%11%02%9E%5B%A5%B1%9B%FByQ%7C%02bK%B2%084Ch%1F%96!%C1%BE%87%D4%9C%03%00H%94%F2G%91%25Vp%D2%FC%1DXW%01W%83K%25%E8r%08%90%C0%C6%95%11%ECz%B3j%C6h%C04%95%C9%A4%04X%81D%A8Yr%80z%C1X%9C%8D%60w%3A%0A%CC%9B%8Fda%2FU%83%08%AF%ED%AF%20%D5%D3%8F%83%03%13%BCm7%02%AB%16%DAw!c%1EX9%E0%C0'M%0E%93%24(1%0E%EE%D6%93a%A6%9E%82%95%19%DE%17%12(%02%BB%0C%ED%82%C2b%C0nG%09.%E6%A3%12%9B%E3(%B6%ED%7DAN4%DA%97%2F%E4%90%EA)%22%D1%5C%072LH%DB%26%93%08%82%00i%1A%ACT%9A%1C%0E!%A5%02%99%3EW6%EEB%5B%90DjL%964%08%0Ei%083%C2%86c%118%84%F6%5C%B0W%E2h%CCBh%1D%8DR%D5%A2xz%3B%86%3B4%ACh%82%AD%D64%AD%7D%F4%1D%ECZ%B5%07I%A9%D8v%04A3%98%40%82%00%E12%B1%003%98l%D2%18%8F%10%15%AA%F0%94e%87B%8B8%FC7%B7Q%CB%8Ez%243%0D%A8%1A%60a9%24%60%C3L%99%80%01%0E%CAe%B0%EF%83%11%90A%3EP%C9%A2%F4%FEL%E6%B6%15%A4%A2%40P%0A%C1%A1%CD%10Y%04%DC%8CJz%91%90f%19i%F1%CE%02%95nh%D1%C0j%D4%B7%CEXU%DA%F1B%D1%ED%EA%8C%E9%B6f%AEk%10%10%02%08A0I%82%94Md%A6%60%22%80%1C%D9%84%F6W%5E%40%AB7%06%E3fL%85%10%04%A9%14%84a%83t%40%AC%5D%10%24%C8%B0%00%5D%81%2C%ED%80r%EA%11%E8%D9%3C%3A%94%A6%DEW%9FCjr%1D%85A%23%F6%AC%C9s4%D0d%19%04%CB2%A1%94%85%A0R%02i%82%90%02%A1P%04%D2%10%A4%91%10%CC%03%AB%DF%C6Sk%B6b%D8%2B%D1%3B%FD._%BB%F83%84b%3Bdj%0C%09%AB%0E%60%1B%DA%8DCH%09%A1%AA%601%08%E62%A0%7D%80%AB%F0%8B%93%A9%1A6%40%09%408%A3%08U%0B%3C5%1D%3E%0B%18%C1%16m%85%2FKO5%96%C4%E1%9F%3FM%03%C0%23O%DD%B52DbM%CCg%84%FD%3DT-%0B%84P0%A4%01%D3L%C2VQ%90%08Pu%FB0%D8%B9%07C%3B%7C%18%DD%A3%88d%E2%90%12P%06C%08%86%14%01%04%05%20%A1%A1%04%C3%B0c%10%89%26(%0A%60%A2%CC%D2%5B%8F%89K%19%E9%A9%5D%D8%B9%F2q%8Cv%0E%20%1A5%60%99%06t%C0%A0P%C34%2C(%A9%A0%24C)%82%A9%00E%06L%C3%84%F4%03.v%F6%A0%D05%8AR%C9CP%CEAER%60%B3%060%D2%20%C7%01%0C%05%0E%18%AC%19%20%07%24%01%18!%84%0A!t%08%EDe%E1c%3A%5C%9E%09-%E3d%84%2Fq2%B8%99b%F4%5BCY%22T%B1%C6%FB%D4%F9g%9C%80%F3%2F%B9%0A%3F%BF%EEj%D8%89%E6%EB%DD%9E%D2%E2%C66%0F%A2%BF%17a5%C2%05%9BH%9A%11Xv%0CBJ%F6%87%DA%11%C8%02%C9%C6%14%A1%23%AF%83%F2%88%B0%D2I%90%94%20%F6%40D%40%E8%83%E0%83%84%03%82%DE%17Q%13%A8R%A8%10%BB%23%03vM~%C8%D7!%ED%D8%DE%DDV%AE%82%9C%8C%84P%06%A4%12%80%EFA%98%16%844!%C2%00%86%D4%D0%2C!%A5%80%A4%90%5C%06Yc%26n%2B%8E%8C%26%8E%F2%2B%D9%C1%F6m(O%9A%0B%C3J%40%A0%02H%09%981%B0%F6%20%84%00%0B%0B%10Q%103k%8FA%B2Bv%F0(%D4H%08%8Ae%40B%02*%20V%80%2F%A6C%8B9W%8C%9F%7B%C3k%02%00~~%DD%D5%00%80%95%2B%9F%7F%86%FD1'%0Fv%AB!%2B%F4%C9.%E7%A8%AE%D0%AF%13%A5%BDa%2C%D7%1EFs%EB%A8%96%2Cjh%9ES%CA%E9%F8%CE%8E%DE%BC%F0%0A%FD%1Cz%9A%25B6%04X%89%80%0DS%B22%1D%96%26%B1R.%0C%E9CW%0A(%EE%D9%8A%E1%DE%E6%17%D2%8B%3B%A7%D6%1F%BAeJ%05%13W%0D%B0%0F%ADE%18%B5%00E%04%C7%8A%C0Q%0E%9B%8E%C5V%2C%CE%96i%B1)%89-G%B2%EF%85%18%0C%08Av%D2%ED%5B%B5%BD%EA%90%03g!%3D%BD%85%87%D2%1E%87%B5%C4%81%E9%B3%DE%C7%EC%B1%8C%5B%10%11%86p%24%842%99%A4%09f%226%25%CB1%D9a%D1%D0%00Uc%07%22%9E%18F%AC%E5%FD%20r%E0%1D%E5%D8e%8BZ%E6%DEp%ED%C78%DF%13%BF%7C*%1E%BB%EFa%3C%FD%C4%C3%8F%9E%B3%E8%DA%ED%B9%D1%DEK%13%0D%C9e%91h%DC%89%3B%11H!%60H%B3%E2F%23%0F%B7L%5Er%8B%B8%FA%D9%09%AD-5%8F%89%CAp%18%14s%0Ae%03F%24%05%C3%D6%10%D2%04%84%02%C2%0ADP%81%3F%DA%8F%F2%9E%5D%CC%A3%83%A4%2Bz%D6%9A%8B%16%7Dn%E6%BC%E3%B7%3E%BBj%C5%EF%B7%ADw%16%BD%D7%3D%8A%23%A6%D4%91%B4%04%A4%B0I*%0B0%04d(%01O%01R%40q%80%1D%83E%F4%8C%84%C1%11%FB7%1Fr%D0q%FB%CF%99%3C%A1%19%F1%E6f%D2a%88RUC%B8%3E%A8T%86*%04p%0A%01%A4%06%D8%B0%89Y%80%8B%A3%08%98%11%24%26%DF%93%AF%D8%B7%3A%5E%DF8%A3%DC%EF%B1%AC%1B.%F2%C5%EB%C6%1Fv%60%11%B8%1F%00%D0%B9%FE%C6%7D%D4f%18%86%90R%E2W%CFn%C0%AD%B7%9D(%D6%3E%D3%AE%01%E0%D2g%EE%9D%92j%0Ck%A5%19%89(%93%AA%B6*%0C%9C7%E1%EC-%00p%F2%B7OX%BC%B4)%FD%CA%A2%E9%FB%87~8%20-Q%85i%C5%00)!%CC%24%84%14PA%01%A5%CE%0E%1E%7C%7F%25%B9%03%3D%A8%16%81%D4%F86O%C5%D3%FE%E0%9A5%D5%E6%C3%3E%5B%DD%5E%F4%C6%BC%F4%ABGx%92%EFb%E6%A4Z%8A%24jYE%22dD%23%08%5C%06y%1E%B4%9BG%FB%F6%1E%BC%D8%91%C7%F4S%8E%0E%8F%3D%FB4%19%CBd%11R%84u%10%12%03%FBx%1B%04%D0%82%C1~%09%B2g%88%ED%AD%83%A4%5Cp%08%26%5D%D8%01%3F%16%03%CD%3E%EC%D41G%1E%FD%C8%9F%12S%7Dk%96I%CF%3AI%B7%CC8%83%FF%B7%E9%93%C3%AE%3E%1A%2F%5D%F5%DC%FF%91%E1%3A%EE%DC%E3g%1F%3Bc%CA%9A%C5%D3%A6%08%C3%22%8DB%A70%B8%C4%8AF%40%04%88%CA0z%B7n%C5%F2%FB%D6%A3m%CE%014q%F1%22%86%20%D8%86K~%B1%04w%B0%075%9F9%19f%A4%96%FB%1F%B9%81%B6l%EE%E5%ED%EB%3Ai%E6%984g%E21%94%DD%10%B1%98%05%9BB%F4%0E%17%F1%87%8D%BD%88-%98%86s%AE%BF%8A%CCH%3D%FC%D0%00%84%C9%3A%04%C0%1E%08%0C%26%0BD%0C%08%8DPW%A0%FBz%C8%5C%B3%1D%C6%9E%AD%1A%B1%8A%08%C6%1E2*%16%9E%B4_%E3%CC%E6%3D%7F%89%C1%13%7F%AE%F2CPV%ED%D9%FE%B1%FAW%B7o%84%D6%1A%00%F0%D4%2F%9E%5C%DB%95w%AF%DD%DE%D5%0Di%9A%C2%ACm%01j%26%92%8EO%86%EB%C7%91%EB%01%AD%7D%25G%B6%99A%F3%A4zD%B3)D%E7%1EFj%F610L%861%B4%01%DE%AA%7B%E0%AB8e%96%9D%8F%83o~%8C%E2%07%2C%E6%B5%3Bz%E9%8D-%BD%F4%FE%EE%01%DA%D35%84%E7%D6%F7%D2%F3%9B%FA%E9%5D%1FXt%CA2J%24k8%D0%02D%82A%20%92D%24%15%914%89%0CI%90D%A4%25I8P%F5M%08%F6%ABaX%C3%82%A8%01%14k%B8%ECCPz%D7%AD%FD%EB%81%F9P%16%B5N%FAXy%F1%A4%19%10B%E0%FB%D7_%05%00%B8%FA%8A%9B%2F%EF%E8%1B%BC%B2s%EF%10%3C%99%06E%B3P%D9%E9%D4%B1%FA%5D%F4%EF%E8%C2%CC%E3N%C4%ECq.%DC%DE%1D%E0x%23%C2%A1.T%B7%BD%03%F3%E0s%D9%5E%F4E6Z%F6g%E4%DA%A1M%07%A8%E4p%F47%CF%C3%9C9%F3%906%14%26e%A20%0C%8B%CA%A1D%D5%89!%D9%92A%B6%A1%11%019%800ABB%08%09a%98%20%C3b%10%B3%20%B0P%06%C80%40%D2%00%C1%00%D56%91%3Fo%09(2%F9gM_%F8%C2%ED%00%D0%FD%F2%EF%D1%B0%DF%EC%BFoR%FFC%B9%F4G%17%D2%B5W%DE%CA%00%F0%CBk%BE9urs%CB%B9%A9%A6%BA%CF%E4%3A6M%EB%BE%FFNY%9F%88b%EC%97%BE%06%E1%F63G%C7%90%B6k%D9l%18KF%AA%0E~%FBjvw%BC%0D%B7k%0B%8C%D0%A7%E4q%17%83%FC%5E%ADf%9E%24%CA%9B%D6%DF%B4%EE%A6%DB%B7%97%82%CA%F1%D5%E1%BE%A9%B1%86%96w%1E%DC%DEo%8E%AB%2F%1D%7B%E9u%D7%84%A2~%B2%D0%1A%B4o%CF%18%C4%24%A0%BD%0A%0B%BF%04%8A%D6%00%C2%20%84U0%87%8C%A0Da%B5%88%D0%0Dz%CC%96%C3%E775%C4%BA%BB%9EzZf%16%1F%12%3A%E9%E4%3F%06%18%00%B8%FFg%97%E0%F4%0B%FE%2B%BB%F7%E4%5B%DBf%B6_%BD%EC%8D%B9-%91D%BAu%1C%DB%D1%18%A5%96%7D%8F%8B%2B%1F%A0%FE%E7~%C3%E9%C6%18%D5%2C%FD%16%8F%FE%EE%E7%18ye%23%EC%A9%0Ej%BF%FE%1F0%A26!5%3E%0Cr%1D%D2Hf.%CDN9%F4c)%C3%E3%C6Mz%F8%D0%05%89S%CE%F8%E1%0FC%3F6F%20%D0%84%0F%B6%12%20%98I%02%BE%072%0C%00!%80%904%13SP%26%EDW%10%86%BA%DF%88%D6%1F%DE8a%DE%86%EE%ED%ABE%D3%A4%85%FAo%F21%9FT%3E%04%E5%8E%AB%BF'%01%60%E0%95%DF%1C%ADb%E9%84%CA%B4%60%E7%93%8F%D1%D0S%F7%40%BF%F5%2BP%A5%88%9AY%07%C2%88D%E1%BDu%3F%C5%96%5E%88%9A%13%16%A3%E6%AC%DB%60d%C7R%B8%E2%12%E8m%7F%80%FF%9F%D7!%DC%B9%AA%F7%A3%EF%60f%B3.%9D%99%10%8F0%A0%CBD%0C%90%A0%7D%D15%01%10DD%0C2%1D%40%07%00%FB%04%08%90P%80T%0C%C3%06%C8%B4%DC%92%1F%05%00%BF%AA%FF%B6%DC%F5%DF%22%C3%E5%3C%01%40%CF%40o%A3%1B0%FCb%1Fj%DB%C6%A2%E5%CB%DF%05%EA%C6R%A40%800%DAI%01M%83N%24%E1%BF%FD4%99%C9%2C%0C%03%E0%DE%CD%A0%EC%3C%C8%89%9Ff%23%D1%086%AD%FC%C7L%9A%C8%FB%D73%CF%DA%1D%A1u%F3%BD%9EmL%C9%E9B0%83X%13%84%04%A0%40%1C%12%23%04%94%FA%80J%08%01b%D2%10L%90%00xHY%A9%AD%00%20%0D%8B%3F%C9%9C%C4%DF%03%98d%F34%0D%00%B1%96Y%BF%CB%0D%95%82%A0~%22%C6.%3B%93Q%3B%89e%DD4%EEY%BD%12%3B%1F%FF-Wse6%AD%80c%D3%E63%97%87%B8%F2%9B%B3Y%DA%16%9B%9F%BF%93%A5a%90p%BB%C0%95%BEy%7F%AA%BF%AE%CE%FE%7D%3AS%03%E2Q%ADK%C3%D0%24%C1d2%B1f%01%8F%85%A0%0F.%83%05I%26!%99%20%98%84AL%06%20%ED%E5%CDmS%87w%AE%7D%85%9A%A7%CC%FB%E7%01%F3%B5%F3%BF%AD%01%E0%A2%0B%CE%7B%D9%C9NZ%F5%FE%9A-%18~%EFeP%A1%8F%B8%D0%83%C2%FB%CF!%BFq%13%CC%EA.r%A6%1CAFK%1B%E2%87%9F%89%F8%11gA5%ECG%BC%F9i%D2%5B%1F%22%AA%F4C%D5%CD%5E%F8%C7%F0%E0%B97%00%00n%A1%BC%DB%CA6!%A8%84%06U%FAY%17%F7%02%C2%00%09%9B%04I%22iB%08%03%82%24%40%F4%01Q%B2%CF%A8%FCb%01a%B1%FC%2C%00%18%E2%93Y%CB%DF%0D%18%E3%23I%F2%86%CF%7D%F5%EB%03%9E5%D2%C5i2%EA%C6hQ%DC%85)%A7%7F%03%0B%AE%BF%1A%A9%13%BE%CF%3A%D6%CA%14%1F%0BQ7%1Dr%D1E%08U%92%C3%EAnf%CA%B1%9Ax%18%8Chr%E7%1F%C3%83%A3%0F%02%00%FC%80%EE~E%C5ZVTB%1B4%B2%0E%22%BF%83%83%5C%07t%18%B0%86%C9%AC%05%40%06%40%02%D0%3E%B3%F69%F0%5C%ED%0Fw%80G%BB%BA%F3%03%89%17%00%20%87%F1%08%C3%F0%9F%07%0C%00%5Cw%E5%0F%F6%FD%96%F6%A5%A5%5B%EA25%DF%F0%11bx%CDr%AE%F6v%91j%3B%14%EA%80%B3%01e%11r%ED%A4%8D%04iM%14%16%87%88w%DFOB%F6%90h8D%08%AB%01%1Cz%BF%F9%A8%DE%BB%CE%3D%81%F2%FFN%ECzt%B9%8E%8F%2Bj%A7%99%F4H%07%8B%91U%84%9E%E5%C4%A3%DBI%17%3BH%97%F6%90.%F7A%97%F7P%98%DF%C1%5C%D8)xp'Da%F4%FA%D9%C7%CCw%01%60%CE%AC%2C%A4%94%FF%3C%E7%0B%00K%BFz%01%3Ch%5C%F9%A3%EB%B0jey%C5%01%DF%9D%B9EE%9D%A9%1C%06%3C%FA%FAc%B0%E3Q2%1A%5B%20%0D%1Bd%D8%400%0C*l%06%0C%87E%FD%91%80J%103V%14%C7%CD%7D%15%00%86%3A%DEFf%DC%02%CC%3C%E2K%FCLr%90%8E%F8%CE5%EF%BC%F3%C0u%E78%C9%09%0F%99%0Ds%05%8Ft%80%83%12%A8%B8%93%89%CA%A4%C9b%C8%18!%D4%A0%C0%13%901%C8t%D3-c%3Eu%DA-%00%B0%EB%95G0~%C9)%FF%B8%BF6%3F%A9%DC%F3%E3%8BO%DFo%82u_%7Ds%13%FC%EDoB%869%96%CD%8DDN%86%F7%1D%265%A0%E8%04%88X%13!%F4%C1%A5A%20%A43%EA%E6%9Dx%DF%DA%D7%EE%C4%ECC%BE%FEg%F5ny%E2%A7K%92%E3%A7%DC%A0ls%81%F6*%60f%90_%00%91F%A8%0D%90%20%90T%0C%3B%7Bqv%EA%92%1B%01%A0%F3%E5%FB%A9%E5%D0%D3%F9%AF%19%FF%DF%1D%98%9B%7F%7C9%BEs%C5O%00%00%8F_%F4%C5%7F%99%D8%96%FD%B7%F4%8C%99d%D7%24%03!%C0%2C%12%82%0C%93%85%24%B0%AAQ%60%01%DD%F9z5%DC%FC%FC%0B%16%C5%BE%94%3E%F3%DE%C2%D0%DEvd%9A%A7%7CLo%FB3w%60%CA%B1%DF%F8cy%60%CB%F2%A5%80%F9%3DaY%AD%1C%FA%02AUki%E7H%19%FF)%92%13o%AF%C94%EC%06%80%9E-%2FR%ACq%0E%C7%93%B5%F8%7F%0A%0C%00%DC%FB%AFW%E3%8C%1F%EC%FB%9Ez%F4%F2%0B%8F%18%1B%1D%BE%BD~B%CD%24%95j%86%99L%EEK%8B%0A%07%D0~A%D8%D1%BB%CA%F9%F4%ED%AD%8B%0F%DF%0A%00%7D%AB%1F%A4%EC%C2%D3%FE%DB%EA%96F%87%D0%FB%E6%03%98x%E4%05%1F%ABw%3B%D6%DB%A5%B0%98%12%C9%96b%AA%B6%B9%F81*a%EBK%C8%B6%1D%86%FF%AF%E4%C9k%2F%FE%E3%FDk%3F%FC%D2%D4%CD%E7%DA%CB%DA%AF%FE%F4%B7%F7%DC~%D2s%3D%3F_%F2%60%D7%5Dg%7Dw%D7%8F%0F%9A%F5%D1g%BA%5E%7D%FC%2F%EA%ED%ECxh%9F%C5%AC%FB%3D%3E%FD'm%EF%DF%F7E%E4v%AF%C6%60%E7%FA%FF%EB%F1%FF%2F%85%C2A%86)%00I%D5%00%00%00%00IEND%AEB%60%82" /></a>
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
<p class="comment">All sealed packages.<br/>
A sealed package is declared in META-INF/MANIFEST.MF.</p>
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

<div id="Packages" class="view-closed">
<h1 class="header">Packages</h1>
<p class="comment">Packages share by different components.<br />
Could be: several NLS versions of a classe, a same version of a 
class included in several components, or a backdoor class. 
The class loader can isolate each version.</p>
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
Could be: parameters files can be localized in different places, 
or a backdoor parameter file used to inject code in a framework. 
The class loader can isolate each version.</p>

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
Could be: a unique base file for very different files,
or a backdoor java class used to substitute a property or others files.
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
<h1 class="header">JAR Plug&amp;Play services.</h1>
<p class="comment">Services published in META-INF/services. Could be: injected code and backdoor. 
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
