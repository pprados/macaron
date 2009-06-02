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
<td class="menu" id="menuSealedPackages">Sealed packages</td>
<td class="cmd" width="100%"><a href="javascript:window.print()">Print</a></td>
<td class="cmd" ><a href="http://macaron.googlecode.com">Help</a></td>
</tr>
</tbody>
</table>
<div class="logo">
<a href="http://macaron.googlecode.com"><img width="70" height="43" alt="Macaron" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEYAAAArCAYAAADFRP4AAAAAAXNSR0IArs4c6QAAAAZiS0dEAP8A%2FwD%2FoL2nkwAAHMVJREFUaN7NenmYXUW1729V1Z7OfE4Pp7vT3Zk7MwkZJGFIQAQEYgCZFBFBUEQBvYiCDA9BfZfBy6RwEa%2FILCCjMoTAlTkJUyBzOmMnnZ779HDmPdV6fwS84PU90ae%2Bt75vf9%2BuYa9d9atVq9b%2BrQ38FXLDV08HAPzbV0%2FHbWefRgBw%2Bzmn4ZYvnAMA%2BOmio8SHfW%2F92ndS9yw68sTfzll473MLFz27cukXvtv5ersDADuuPJU%2BqvdHQPLBKy9LvXrlefFBZvPhm2%2BJfdh25wG19MytdwIAtm7tx4v3%2Fky8dv3p6o1rPmu8dvcP5Zur3gQAvPr4r%2FH3FPprH%2FjFRRfi3BtvBQDcduYp6NPANfc%2BghtOPIu%2B99ivmZlx29RF37Fyw9%2FNlMvNSQph2waijc2gGTPun%2FPwnV8GgMfufngqb3tladDdPt0DzfODUFZ6%2B8uZadP7Rzs7UlYk%2BWSUzQdPffDZbgBYcddP8fYNF%2BPyzf99TG88eT8OOv70fz4wl5x0InY%2B%2Bhh%2B%2B0F5zYpXI72lijzmhKMKH%2B1301vvZcTJX%2Fm13d%2BzbIJpImkB8EJEnBjMdAalmAVeOOvENxLCyYx0%2FXSkfV1DxfWRnTABpT27sHPjbsw76TgkWlsgPA8q2fzA4Rf%2Bj4%2FNeMVdPzvY6X31EFEdjAYTD3998RmXLycivDzAsF%2B4HgtPu%2BSfA8yVpxyHjY88hcc%2FKD%2F58Ir6Ry%2F%2F8hPjopRsnrTfVQPS6k5bwZ43f%2FisO%2FmYgx9r2rp5cZ1gHteQYK6UhGlI2Ml6SAKXKkXqSBijhSOmO0Nbt5hxi4JSYGPs7KkC0STKg3k0tNZwcfcO5kpZZRYftWH%2BqZfut7wHRvjMtd9yeORodhoWyeHOmO3ugYjHq179onZD2BcuPP6MVwGgWi7DjkT%2B8cD8%2BPb%2FwBXfPAdP%2FOgB67mHLrvVSUS%2B7pWKqAuLqM9m0dk%2FiJpwpGJOPHhAvdTeOqWUQ0xF2VYGzGSC4vWtLHyf%2FGIXDwVFdENRZUkr6udO1Om2STS8aSvCwghl5x7AQd9ucre8x4N7%2Byg2bSLSM2eiIMbeNlzEMTLIj0%2BbQ0BtGzxrDFAZgRrdDSPTDB0gZ43sGDfn7BuLb3zxfDKJYFoSsqmRZ%2F7kb7Mg9acVbz%2FxOBac8HkAwPkXX0lXfPMcfpQ5%2BeaJR94z3L%2FnuOoAo2n8BLhIIdU2BdVqHvWxVic%2BpbbViQnEn3iDM6YkN56AFUtDuB5VhnaiqDwK4XCNF2DHpg6IhfsRdEDVjr2IZ1IIczkq9gdIzj2c6u1nYToDXN27g4qW%2FJaKNkBZDqqyEZZbYmn4kLZBBjXAjFuo5IZrRmPjjgHwyEG%2F%2BTn%2FuYm%2B%2FbVLsOCX1%2F31FrPi1P1x5MPv%2FdlOZx%2B28Fd17uhXDaVRV5PmURdkxuI8%2F8Rl5Of6Ea%2BrZX%2BwF0pWqHrfCo6%2Ftp0yqQawCFB1y1yxopDRGJIBU87No2uGhbpDZnNs4nhCOY9kcw2KnUPMpWGKZRSX9nZQ33AZA9UssjNmhdEJE4RSipQ0WSgbSgQkJbOADy6P0tCuXSjmebfT33wW7eluNQ1pmbYKqLX19f2u%2FpetfwTn8uux4Cff%2F%2BTA3Da7lr61dpAB4JFzv5Kup%2B7JlvAzCytrtl7gnXJqT%2Be2%2F3nK4ilonjuLfRIoD5dgCR9OXT3Z8TRHHBveaD%2BG1r6BzrWb4b%2B%2BAfXJWlSGR2D4FsYZ9Ui4RCLIY2dLU%2FeOei9Va%2FZGMhOzMCM2c1hCMMpIc47USD%2BraApsGGA2wPWtQNscyOx4SCMClgoAEwdV1gO7UNy5G8X1QyTbyzBdAWXYkNICCY3QshBkal%2FBhMbL5t%2F0o5UA0MeMLP3lM4euBehSgA8%2F4pyJZ7pP%2F8uk2vBg0u5MgwMZS2VRiNTxM52CMpkUH3DkQQhjaSpu34xEOsOpafOpuO4lzo8WacR3MbhlLVfLLpQM0DMCklKiydFcM1Sh5v4E6kTj3cM3PnjpjgcueTheGy6xmyz4lQoPvP86Neb3cEsTCNJkIU1SERumFTKUDQ8JVFKTSLfOBhlgBB7R4AYuj5Sp9%2BkNyGy32I5mCUJBxZLQlgIRgUMGSQk%2F4cBtaThp4a9ve2zlws%2FKA1cvD%2F%2Bij7kU4Msvual1%2FLt3PNtIA22qHMIyCHbU1CRCqil00Wk1Ia%2FtqVBxdY4jU%2BdzJBKFyqQRGhLr33oH615%2FizO1KYyptTE8wIAUGBP3WRgWSjmBqmToGXlUJ81ct%2Fiohr7fnrhsF8Znl6SmzMCezVs5GZRpXAsgkxKsQ0hTs%2BmEQOhCcwADAsHetVxECSrmgEQcWlgYHo7z6Pq30GT5iAiL2XBARCCXIY04ICTpcESL7pLQhaHzSsyPRYnCT%2BR8n7%2FsmwuHV99%2BZzO2t0WizEwEM%2BKQEVcicIfAVoh0PdEcqmLP3g1UySQROrUIZBmb3n8Lz6%2FaRhkZB%2Fshtu%2BpQAU%2BHJORrLNAQRGlUhROlLiUG6UhuebbzPyL04i%2Btkwd8XZ3vvqNba%2B%2FOuuoukEdbbBECAChJmGKfSsubUBIEDHsUh8KpTFwE%2BOgSMFz6mlk%2BD0YEQ3fZyBfgZO2IKSGcuqgqx50cQhhOEJ%2ByYWmuHfzPtfBzIx3f3cDquU%2BgmHDtJI85oBjMaZ%2Bxn8Bs%2F2NF5%2BdFm5LJ9IEUgQnLuEkBaQd4aBSJLIY2jJZygAYraB79YtkZwScmix3d7lUyo1yMmKTZwiEFeZ42oItXOT2lsgGkBBVLvdUUVE%2BBsoFa8sTl898kE9%2Fj%2Bj%2B269YeLBp9%2FXfpFqFJoNJhIpgmCyUSSQIgMXSlJC6hKEwQj25CAL22c93kQh2sJIVsg9JY7Qn4KCHUStcGIJJSwuBLjF7IwR4VLUNlCNi9%2BVEvBxQV42j4JrdAICPnGDfx%2Fsv3og5n7kIj18KqKFKYCZTBNM2IE1BJMBB1QfRKByHYKSirIlRdhmmFJA%2Bs6kV5XMhegdcdAUCVpmhKh43mEBkGHDiAkE%2BYFiCyKxABz5tGQYG%2B0dSsYduvVtJs9z%2B9NjBvg17pnW9wOjtVLKlWSOWBchMAMoCWLKQCgoe9u5NYlfYyAWOUqG9AyaXYKWTiKsAeRlh3RZF2JyH002whxMcDO6lsBrAC4GqF6CajEKnUjkA%2BCwQYDcw8QGmW9yn6oTFZn9w%2FMCZXyF3zmcuAgBMOOB4qHgyvl1JNVuZCmQQIAiOoSEtiV44eOfdkLYZSR5esBDaiUOUNWWSaXyqfizEPc9g7xsrIeKEAjFVNXErBzBgABBUcYGiJ1Ea46M5wzw%2Fq%2BzJ%2B1nTlFOBFQkQX6zQPCuh%2B%2F9QFAO7mEmGiNS5ICMGIpPIK3L7xkFsHMzASjFp3YegOArbMWApwAsIPhIUNyT3ewJ%2BvYHmkibdawDKROBXUChpJDJpRI3JNgCsvuPT5wqr92R44ycYMVOajiHH4opg%2FdOf3hNw2037f%2B6OJ%2Bac8CRUn8jeOWQM3JZxRhCaBiJRm4a0gd9HW7A8b6Fvbwfi45oQnz0PRjSFVGEAfZEUdsaicA%2BchMyGdriDFez1Anh5D4gSdlU8lDzAtKKYMKuKI88B6mtSqM3Ww0gQ%2FOIIax8IA0aiVYjoSRb6Hiog3%2BOgMlKEpAqsiIGwxFj5roYaY0EFIYq9g3AcAow0Sp1FJOsF0kkPuc4qhCVRoCIC00ONoRFGUuBoHGm7CE%2BYxcr%2Bw5E1ZzS9J3j1HKUIiZoUtHYBEmAokM6P9at7D1n7%2BOd%2FMPvzj19LAPDvS%2BcvPyDScVRz2g26ZFTenJpM7dOms1OTIqEJsAhJYYGkBCwL0jSgAp8TdoRGNm7i%2FuWrqGFPEZlIBDZFYCdiKJYJu7ZsxrKjO3nJsSYJZbAdlaQcE%2FAA8iUQKMi4Ce1WeejNAfJWGdA6hAKzGYtgYEDT3ZsVZk5ohlMagSkY8ZTB5SBKUntsSBDcHIyaDETVRT4%2FCpEzMSfXiEiuj6ueR%2FnQxN7FDV7Led0ynhiUmiSUUEhk4hyGTGEowaHJYICrVXLdum15ffGhEgCa2w5cj5HC58q5cuqXyTHUsWAW6uvSEKYJyzFhSAlpmrBsB9IggBkERgSgVEMaA%2Fl%2B5F%2FchAQzUg21cFIxZAIfW7p3Y%2BnSCmQJVOiREEVGfpuLZKMNYaYgrCikMkEyoGDY1apXlBwDgK2UrWx6f3fIW3UMI9BojivYwkDEMKFG%2B8gxyuDhEtwSIR7xUOgZxtodLirxBJrGZkHlCiotbfSOZbFcskmNn1sQphXRylBkRAAmDYKAgIRAQJKIFEL22a5xg9xz6sYjF8iLnn507XeOOOWC53NvP9Izc6LKZtNQZEEIgwwdQAgJUhJCmQgphBQEhZCZJGxLIGXHaGC4ihF3FMXiDjiWRFgJUMiXUK6A6yMh8gMKmhTVTzRhRFOAiAMhEFYL8Ap5BFZmqCcz7V6xq%2F2zxcLw9BAKO3NMbXUCr27LYReHWDCxAa1cYhVaUGUbkl0yInn0DkcRaZmPpa1jUZdugZ12gPmCI7aFTw2NwrNtiMG1sJpcQcoEKQsASAgCETEpA9DMgBeEhcBgrZrVy2WbAGDbYFdjORNTNbVxCBIQpklKSLCywUrAZA0%2FKEJbNkhJ2EwUlTa8cpnmJFrR%2Br3zkG1sgp1KwrQEVBhgYO8uEPeSEd2EbONGEr4PCjSCgVGwGQB%2BALdrANVSCVvXysTu93adS%2BUgKjkK0uBJGZvCso%2FjslFsGChgw7pOrGdQxpbgQJNSwCgFWHr0MThwzjwY%2FRraV%2FDKPhBxSHohGuIx0nQc3A0zwMaTsMcWQCTAKgaCCbAgUgx2y%2BS5ChgNoISyVaFYIAAolUbHcYpgKQeGEhCSGYaCZ9gQCkCgYVAIx7AAacERAp5bpbGb%2B3lRfAJiS7IINENoH5Yhwb6H1JwDAEiU8keRJVZw0vwdWFcBV4NLJehyCJDAxpUR7HqzasZowDSVyaQEWIFEqFlygHrBWJyNYHc6Csybj2RhL1WDCK%2FtryDV04%2BDAxO8bTcCqxbadyFjHlg54MAnTQ6TJCgxDu7Wk2GmnoKVGd4XEigCuwztgsJiwG5HCS7moxKb4yi27X1BTjTaly%2FkkOopItFcBzJMSNsmkwiCAGkarFSaHA4hpQKZPlc27kJbkERqTJY0CA5pCDPChmMROIT2XLBX4mjMQmgdjVLVonh6O4Y7NKxogq3WNK199B3sWrUHSanYdgRBM5hAggDhMrEAM5hs0hiPEBWq8JRlh0KLOPw3t1HLjnokMw2oGmBhOSRgw0yZgAEOymWw74MRkEE%2BUMmi9P5M5rYVpKJAUArBoc0QWQTcjEp6kZBmGWnxzgKVbmjRwGrUt85YVdrxQtHt6ozptmauaxAQAghBMEmClE1kpmAigBzZhPZXXkCrNwbjZkyFEASpFIRhg3RArF0QJMiwAF2BLO2AcuoR6Nk8OpSm3lefQ2pyHYVBI%2FasyXM00GQZBMsyoZSFoFICaYKQAqFQBNIQpJEQzAOr38ZTa7Zi2CvRO%2F0uX7v4M4RiO2RqDAmrDmAb2o1DSAmhqmAxCOYyoH2Aq%2FCLk6kaNkAJQDijCFULPDUdPgsYwRZthS9LTzWWxOGfP00DwCNP3bUyRGJNzGeE%2FT1ULQuEUDCkAdNMwlZRkAhQdfsw2LkHQzt8GN2jiGTikBJQBkMIhhQBBAUgoaEEw7BjEIkmKApgoszSW4%2BJSxnpqV3YufJxjHYOIBo1YJkGdMCgUMM0LCipoCRDKYKpAEUGTMOE9AMudvag0DWKUslDUM5BRVJgswYw0iDHAQwFDhisGSAHJAEYIYQKIXQI7WXhYzpcngkt42SEL3EyuJli9FtDWSJUscb71PlnnIDzL7kKP7%2FuatiJ5uvdntLixjYPor8XYTXCBZtImhFYdgxCSvaH2hHIAsnGFKEjr4PyiLDSSZCUIPZAREDog%2BCDhAOC3hdRE6hSqBC7IwN2TX7I1yHt2N7dVq6CnIyEUAakEoDvQZgWhDQhwgCG1NAsIaWApJBcBlljJm4rjowmjvIr2cH2bShPmgvDSkCgAkgJmDGw9iCEAAsLEFEQM2uPQbJCdvAo1EgIimVAQgIqIFaAL6ZDizlXjJ97w2sCAH5%2B3dUAgJUrn3%2BG%2FTEnD3arISv0yS7nqK7QrxOlvWEs1x5Gc%2BuolixqaJ5Tyun4zo7evPAK%2FRx6miVCNgRYiYANU7IyHZYmsVIuDOlDVwoo7tmK4d7mF9KLO6fWH7plSgUTVw2wD61FGLUARQTHisBRDpuOxVYszpZpsSmJLUey74UYDAhBdtLtW7W96pADZyE9vYWH0h6HtcSB6bPex%2ByxjFsQEYZwJIQymaQJZiI2Jcsx2WHR0ABVYwcinhhGrOX9IHLgHeXYZYta5t5w7cc43xO%2FfCoeu%2B9hPP3Ew4%2Bes%2Bja7bnR3ksTDcllkWjciTsRSCFgSLPiRiMPt0xecou4%2BtkJrS01j4nKcBgUcwplA0YkBcPWENIEhALCCkRQgT%2Faj%2FKeXcyjg6QretaaixZ9bua847c%2Bu2rF77etdxa91z2KI6bUkbQEpLBJKgswBGQoAU8BUkBxgB2DRfSMhMER%2BzcfctBx%2B8%2BZPKEZ8eZm0mGIUlVDuD6oVIYqBHAKAaQG2LCJWYCLowiYESQm35Ov2Lc6Xt84o9zvsawbLvLF68YfdmARuB8A0Ln%2Bxn3UZhiGkFLiV89uwK23nSjWPtOuAeDSZ%2B6dkmoMa6UZiSiTqrYqDJw34ewtAHDyt09YvLQp%2Fcqi6fuHfjggLVGFacUAKSHMJIQUUEEBpc4OHnx%2FJbkDPagWgdT4Nk%2FF0%2F7gmjXV5sM%2BW91e9Ma89KtHeJLvYuakWookallFImREIwhcBnketJtH%2B%2FYevNiRx%2FRTjg6PPfs0GctkEVKEdRASA%2Ft4GwTQgsF%2BCbJniO2tg6RccAgmXdgBPxYDzT7s1DFHHv3InxJTfWuWSc86SbfMOIP%2Ft%2BmTw64%2BGi9d9dz%2FkeE67tzjZx87Y8qaxdOmCMMijUKnMLjEikZABIjKMHq3bsXy%2B9ajbc4BNHHxIoYg2IZLfrEEd7AHNZ85GWaklvsfuYG2bO7l7es6aeaYNGfiMZTdELGYBZtC9A4X8YeNvYgtmIZzrr%2BKzEg9%2FNAAhMk6BMAeCAwmC0QMCI1QV6D7eshcsx3Gnq0asYoIxh4yKhaetF%2FjzOY9f4nBE3%2Bu8kNQVu3Z%2FrH6V7dvhNYaAPDUL55c25V3r93e1Q1pmsKsbQFqJpKOT4brx5HrAa19JUe2mUHzpHpEsylE5x5GavYxMEyGMbQB3qp74Ks4ZZadj4NvfoziByzmtTt66Y0tvfT%2B7gHa0zWE59b30vOb%2BuldH1h0yjJKJGs40AJEgkEgkkQkFZE0iQxJkESkJUk4UPVNCParYVjDgqgBFGu47ENQetet%2FeuB%2BVAWtU76WHnxpBkQQuD7118FALj6ipsv7%2BgbvLJz7xA8mQZFs1DZ6dSx%2Bl307%2BjCzONOxOxxLtzeHeB4I8KhLlS3vQPz4HPZXvRFNlr2Z%2BTaoU0HqORw9DfPw5w585A2FCZlojAMi8qhRNWJIdmSQbahEQE5gDBBQkIICWGYIMNiELMgsFAGyDBA0gDBANU2kT9vCSgy%2BWdNX%2FjC7QDQ%2FfLv0bDf7L9vUv9DufRHF9K1V97KAPDLa745dXJzy7mpprrP5Do2Teu%2B%2F05Zn4hi7Je%2BBuH2M0fHkLZr2WwYS0aqDn77anZ3vA23awuM0KfkcReD%2FF6tZp4kypvW37Tuptu3l4LK8dXhvqmxhpZ3Htzeb46rLx176XXXhKJ%2BstAatG%2FPGMQkoL0KC78EitYAwiCEVTCHjKBEYbWI0A16zJbD5zc1xLq7nnpaZhYfEjrp5D8GGAC4%2F2eX4PQL%2Fiu79%2BRb22a2X73sjbktkUS6dRzb0Rilln2PiysfoP7nfsPpxhjVLP0Wj%2F7u5xh5ZSPsqQ5qv%2F4fMKI2ITU%2BDHId0khmLs1OOfRjKcPjxk16%2BNAFiVPO%2BOEPQz82RiDQhA%2B2EiCYSQK%2BBzIMACGAkDQTU1Am7VcQhrrfiNYf3jhh3obu7atF06SF%2Bm%2FyMZ9UPgTljqu%2FJwFg4JXfHK1i6YTKtGDnk4%2FR0FP3QL%2F1K1CliJpZB8KIROG9dT%2FFll6ImhMWo%2Bas22Bkx1K44hLobX%2BA%2F5%2FXIdy5qvej72Bmsy6dmRCPMKDLRAyQoH3RNQEQREQMMh1ABwD7BAiQUIBUDMMGyLTckh8FAL%2Bq%2F7bc9d8iw%2BU8AUDPQG%2BjGzD8Yh9q28ai5cvfBerGUqQwgDDaSQFNg04k4b%2F9NJnJLAwD4N7NoOw8yImfZiPRCDat%2FMdMmsj71zPP2h2hdfO9nm1MyelCMINYE4QEoEAcEiMElPqASggBYtIQTJAAeEhZqa0AIA2LP8mcxN8DmGTzNA0AsZZZv8sNlYKgfiLGLjuTUTuJZd007lm9Ejsf%2Fy1Xc2U2rYBj0%2BYzl4e48puzWdoWm5%2B%2Fk6VhkHC7wJW%2BeX%2Bqv67O%2Fn06UwPiUa1Lw9AkwWQysWYBj4WgDy6DBUkmIZkgmIRBTAYg7eXNbVOHd659hZqnzPvnAfO187%2BtAeCiC8572clOWvX%2Bmi0Yfu9lUKGPuNCDwvvPIb9xE8zqLnKmHEFGSxvih5%2BJ%2BBFnQTXsR7z5adJbHyKq9EPVzV74x%2FDguTcAAG6hvNvKNiGohAZV%2BlkX9wLCAAmbBEkiaUIIA4IkQPQBUbLPqPxiAWGx%2FCwAGOKTWcvfDRjjI0nyhs999esDnjXSxWky6sZoUdyFKad%2FAwuuvxqpE77POtbKFB8LUTcdctFFCFWSw%2BpuZsqxmngYjGhy5x%2FDg6MPAgD8gO5%2BRcVaVlRCGzSyDiK%2Fg4NcB3QYsIbJrAVABkAC0D6z9jnwXO0Pd4BHu7rzA4kXACCH8QjD8J8HDABcd%2BUP9v2W9qWlW%2BoyNd%2FwEWJ4zXKu9naRajsU6oCzAWURcu2kjQRpTRQWh4h3309C9pBoOEQIqwEcer%2F5qN67zj2B8v9O7Hp0uY6PK2qnmfRIB4uRVYSe5cSj20kXO0iX9pAu90GX91CY38Fc2Cl4cCdEYfT62cfMdwFgzqwspJT%2FPOcLAEu%2FegE8aFz5o%2BuwamV5xQHfnblFRZ2pHAY8%2BvpjsONRMhpbIA0bZNhAMAwqbAYMh0X9kYBKEDNWFMfNfRUAhjreRmbcAsw84kv8THKQjvjONe%2B888B15zjJCQ%2BZDXMFj3SAgxKouJOJyqTJYsgYIdSgwBOQMch00y1jPnXaLQCw65VHMH7JKf%2B4vzY%2Fqdzz44tP32%2BCdV99cxP87W9ChjmWzY1ETob3HSY1oOgEiFgTIfTBpUEgpDPq5p1439rX7sTsQ77%2BZ%2FVueeKnS5Ljp9ygbHOB9ipgZpBfAJFGqA2QIJBUDDt7cXbqkhsBoPPl%2B6nl0NP5rxn%2F3x2Ym398Ob5zxU8AAI9f9MV%2FmdiW%2Fbf0jJlk1yQDIcAsEoIMk4UksKpRYAHd%2BXo13Pz8CxbFvpQ%2B897C0N52ZJqnfExv%2BzN3YMqx3%2FhjeWDL8qWA%2BT1hWa0c%2BgJBVWtp50gZ%2FymSE2%2BvyTTsBoCeLS9SrHEOx5O1%2BH8KDADc%2B69X44wf7PueevTyC48YGx2%2BvX5CzSSVaoaZTO5LiwoH0H5B2NG7yvn07a2LD98KAH2rH6TswtP%2B2%2BqWRofQ%2B%2BYDmHjkBR%2BrdzvW26WwmBLJlmKqtrn4MSph60vIth2G%2F6%2FkyWsv%2FuP9az%2F80tTN59rL2q%2F%2B9Lf33H7Scz0%2FX%2FJg111nfXfXjw%2Ba9dFnul59%2FC%2Fq7ex4aJ%2FFrPs9Pv0nbe%2Ff90Xkdq%2FGYOf6%2F%2Bvx%2Fy%2BFwkGGKQBJ1QAAAABJRU5ErkJggg%3D%3D" /></a>
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
<p class="comment">All sealed packages.
A sealed package is declared in META-INF/MANIFEST.MF.</p>
<xsl:if test="count(//audit:package) = 0">
<p class="item">No package are sealed !</p>
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
