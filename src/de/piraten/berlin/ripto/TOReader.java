/*
    ripTO - parses BVV agendas published on berlin.de for further processing 
    Copyright (C) 2011  Arne Müller

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.piraten.berlin.ripto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * @author Arne Müller <arne.c.mueller@googlemail.com>
 */
public class TOReader {
	
	private Tagesordnung to;
	
	private final boolean DEBUG;
	
	public TOReader() {
		DEBUG = false;
	}
	
	public TOReader(boolean debug) {
		DEBUG = true;
	}

	/**
	 * Liest die TO von berlin.de ein
	 * @param to die URL zu der Website, wo das drauf steht
	 */
	public Tagesordnung read(String toURI) throws ParseException {
		try {
			return read(new URL( toURI) );
		} catch (MalformedURLException e) {
			throw new ParseException("Improper URL, please check your URL", e);
		}
	}
	
	public Tagesordnung read(URL toURL) throws ParseException {
		to = new Tagesordnung(toURL);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(toURL.openStream(), Charset.forName("ISO-8859-1")));
			String buffer = skipTo(br, "<h1>Tagesordnung");
			int begin = buffer.indexOf("<h1>Tagesordnung")+4;
			int end = buffer.indexOf("</h1>", begin);
			to.setTitel(buffer.substring(begin, end));
			skipTo(br, "</table>");
			skipTo(br, "</table>");
			//skipTo(br, "</table>");
			//skipTo(br, "</table>");
			//skipTo(br, "</table>");
			while(readTOP(br));
		} catch (IOException e) {
			throw new ParseException("Unable to read TO from berlin.de", e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {}			
		}
		to.initTOPs();
		return to;
	}
	
	private String skipTo(BufferedReader br, String text) throws IOException {
		String buffer = br.readLine();
		while(buffer != null && !buffer.contains(text)) {
			buffer = br.readLine();
		}
		return buffer;
	}
	
	private boolean readTOP(BufferedReader br) throws ParseException {
		try {
			if(skipTo(br, "<tr class=\"zl1") == null) {
				// keine weiteren TOPs gefunden
				return false;
			}
		} catch (IOException e) {
			throw new ParseException("Error while scanning for next TOP", e);
		}
		
		// weiteren TOP gefunden
		TOP top = new TOP();
		
		// Tagesordnungsnummer parsen
		String buffer;
		try {
			buffer = br.readLine();
		} catch (IOException e) {
			throw new ParseException("Error while parsing next TOP", e);
		}
		int end = buffer.indexOf("</");
		if(end == -1) throw new ParseException("unable to get end index of tonr", null);
		int begin = buffer.lastIndexOf(">", end);
		if(begin == -1) throw new ParseException("unable to get begin index of tonr", null);
		top.tonr = buffer.substring(begin+1, end);
		top.tonr = top.tonr.replaceAll("&nbsp;", " ");
		if(DEBUG) System.out.println(top.tonr);
		
		// Titel parsen
		try {
			skipTo(br, "<td>&nbsp;</td>"); // dann gibt es eine Spalte, die nur Leerzeichen enthält
			skipTo(br, "<td>"); // dann gibt es eine Spalte, die nur andere Meta-Inf enthält
			buffer = br.readLine();
		} catch (IOException e) {
			throw new ParseException("Error while parsing next TOP", e);
		}
		String titel;
		if(buffer.contains("<a href")) {
			titel = buffer.substring(buffer.indexOf("\">")+2); // end of href=" ... ">
		}
		else {
			titel = buffer.substring(buffer.indexOf(">")+1); // end of <td>
		}
		while(!titel.contains("<")) {
			try {
				titel = titel+"\n"+br.readLine();
			} catch (IOException e) {
				throw new ParseException("Error while parsing title: \n"+titel, e);
			}
		}
		titel = titel.substring(0, titel.indexOf("<"));
		top.titel = titel;
		if(DEBUG) System.out.println(top.titel);
		
		// Drucksachennr. parsen
		try {
			buffer = skipTo(br, "<td>");
		} catch (IOException e) {
			throw new ParseException("Error while checking if TOP has Drucksache", e);
		}
		begin = buffer.indexOf("name=\"VOLFDNR\"");
		if(begin != -1) {
			top.hasDrucksache = true;
			begin = buffer.indexOf("value=\"", begin);
			if(begin == -1) throw new ParseException("Error trying to get VOLFDNR", null);
			end = buffer.indexOf("\"", begin+7);
			top.volfdnr = buffer.substring(begin+7, end);
			
			// nun hole dir auch noch die Nummer der Drucksache
			try {
				buffer = skipTo(br, "<td nowrap>");
			} catch (IOException e) {
				throw new ParseException("Error while reading Drucksache Nummer", e);
			}
			end = buffer.indexOf("</");
			if(end == -1) throw new ParseException("unable to get end index of Drucksache Nummer", null);
			begin = buffer.lastIndexOf(">", end);
			if(begin == -1) throw new ParseException("unable to get begin index of Durcksache Nummer", null);
			top.drucksnr = buffer.substring(begin+1, end);
		}
		else {
			top.hasDrucksache = false;
		}
		if(DEBUG) System.out.println("has Drucksache: "+top.hasDrucksache+" "+top.drucksnr+ " ("+ top.volfdnr+ ")");
		
		to.addTOP(top);
		return true;
	}
	
	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException {
		TOReader reader = new TOReader(true);
		Tagesordnung to = reader.read("https://www.berlin.de/ba-steglitz-zehlendorf/bvv-online/to010.asp?SILFDNR=2239");
		//Tagesordnung to = reader.read("https://www.berlin.de/ba-steglitz-zehlendorf/bvv-online/to010.asp?SILFDNR=2021");
		
		System.out.println("Printing Parsed Data:");
		System.out.println(to);
	}

}
