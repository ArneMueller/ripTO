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
public class TOP {
	
	enum Type {
		KLEINE_ANFRAGE,
		GROSSE_ANFRAGE,
		ANTRAG,
		DRINGLICHKEITSANTRAG,
		WAHLVORSCHLAG,
		VORLAGE_ZUR_BESCHLUSSFASSUNG,
		DRINGLICHE_VORLAGE_ZUR_BESCHLUSSFASSUNG,
		SONSTIGES
	}
	
	/** Titel des TOPs */
	String titel;
	/** Nummer der Drucksache */
	String drucksnr;
	/** Interne Id der Drucksache (wird zum Verlinken gebraucht) */
	String volfdnr;
	/** Antragsteller */
	String antragsteller;
	/** Nummer auf der TO */
	String tonr;
	
	/** Gibt an was für ein Typ TOP dieses TOP ist. */
	Type type;
	
	/** Gibt an, ob der TOP öffentlich ist oder nicht. */
	String status;
	/** Gibt an, ob das TOP das erste Mal in der TO ist, oder vertagt wurde */
	String ursprung;
	
	/** true, wenn eine Drucksache dazu existiert */
	boolean hasDrucksache;
	
	/** true, wenn es ein Antrag ist, über den abgestimmt wird*/
	boolean needsVote() {
		return type == Type.ANTRAG 
				|| type == Type.WAHLVORSCHLAG 
				|| type == Type.DRINGLICHKEITSANTRAG 
				|| type == Type.VORLAGE_ZUR_BESCHLUSSFASSUNG
				|| type == Type.DRINGLICHE_VORLAGE_ZUR_BESCHLUSSFASSUNG;
	}
	
	public void init() throws ParseException {
		if(hasDrucksache) {
			BufferedReader br = null;
			try {
				URL url = new URL("https://www.berlin.de/ba-steglitz-zehlendorf/bvv-online/vo020.asp?VOLFDNR="+volfdnr);
				br = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("ISO-8859-1")));
				String buffer;
				while( (buffer= br.readLine()) != null) {
					if(buffer.contains("<td class=\"kb1\">Status:</td>")) {
						int begin = buffer.indexOf("<td class=\"text3\">");
						if(begin == -1) throw new ParseException("unable to read Status", null);
						begin += 18;
						int end = buffer.indexOf("</td>", begin);
						if(begin == -1) throw new ParseException("unable to read Status", null);
						status = buffer.substring(begin, end);
					}
					if(buffer.contains("<td class=\"kb1\">Ursprung:</td>")) {
						int begin = buffer.indexOf("<td class=\"kb1\" colspan=\"2\">");
						if(begin == -1) throw new ParseException("unable to read Ursprung", null);
						begin += 28;
						int end = buffer.indexOf("</td>", begin);
						if(begin == -1) throw new ParseException("unable to read Ursprung", null);
						ursprung = buffer.substring(begin, end);
					}
					if(buffer.contains("<td class=\"kb1\">Initiator:</td>")) {
						int begin = buffer.indexOf("<td class=\"text4\">");
						if(begin == -1) throw new ParseException("unable to read Initiator", null);
						begin += 18;
						int end = buffer.indexOf("</td>", begin);
						if(begin == -1) throw new ParseException("unable to read Initiator", null);
						antragsteller = buffer.substring(begin, end);
					}
					if(buffer.contains("<td class=\"kb1\">Drucksache-Art:</td>")) {
						int begin = buffer.indexOf("<td class=\"text4\">");
						if(begin == -1) throw new ParseException("unable to read Drucksache-Art", null);
						begin += 18;
						int end = buffer.indexOf("</td>", begin);
						if(begin == -1) throw new ParseException("unable to read Drucksache-Art", null);
						String art = buffer.substring(begin, end);
						if(art.equalsIgnoreCase("Antrag")) {
							type = Type.ANTRAG;
						} else if(art.equalsIgnoreCase("Dringlichkeitsantrag")) {
							type = Type.DRINGLICHKEITSANTRAG;
						} else if(art.equalsIgnoreCase("Vorlage zur Beschlussfassung")) {
							type = Type.VORLAGE_ZUR_BESCHLUSSFASSUNG;
						} else if(art.equalsIgnoreCase("Dringliche Vorlage zur Beschlussfassung")) {
							type = Type.DRINGLICHE_VORLAGE_ZUR_BESCHLUSSFASSUNG;
						}else if(art.equalsIgnoreCase("Kleine Anfrage")) {
							type = Type.KLEINE_ANFRAGE;
						} else if(art.equalsIgnoreCase("Große Anfrage")) {
							type = Type.GROSSE_ANFRAGE;
						} else if(art.equalsIgnoreCase("Wahlvorschlag")) {
							type = Type.WAHLVORSCHLAG;
						} else {
							type = Type.SONSTIGES;
						}
					}
				}
			} catch (MalformedURLException e) {
				throw new ParseException("Internal Error: URL to Drucksache broken", e);
			} catch (IOException e) {
				throw new ParseException("Failed to read Drucksache", e);
			} finally {
				if(br != null) try {
					br.close();
				} catch (IOException e) {
					// not worth an error message
				}
			}
		}
		else {
			type = Type.SONSTIGES;
		}
	}
	
	public String toWiki() {
		/* Beispiel Format:
		# {{BE:Steglitz-Zehlendorf/BVV/BVVItem 
			|Initiator=CDU-Fraktion 
			|Nr=4037 
			|Vote=1 
			|Titel=0001 Gültigkeit der aktuellen Geschäftsordnung der BVV }}
		*/
		String out;
		if(hasDrucksache) {
			out = "* '''"+tonr+"''' {{BE:Steglitz-Zehlendorf/BVV/BVVItem \n" +
					"|Initiator="+antragsteller+"\n" +
					"|Nr="+volfdnr+"\n" +
					"|Vote="+(needsVote() ? 1:0)+"\n" +
					"|Titel="+wikiTitel()+" }}";
		}
		else {
			out = "* '''"+tonr+"''' "+titel.replace("\n", " ");
		}
		return out;
	}
	
	private String wikiTitel() {
		String druck = drucksnr.substring(0, drucksnr.indexOf("/"));
		return druck+" "+titel.replace('/', '|').replace("\n", " ");
	}
	
	public String toString() {
		return type + " to:" + tonr + " druck:" + drucksnr + " volfd:" + volfdnr + " von " + antragsteller + ": " + titel; 
	}
}
