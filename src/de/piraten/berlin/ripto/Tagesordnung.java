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

/**
 * @author Arne Müller <arne.c.mueller@googlemail.com>
 */
import java.net.URL;
import java.util.ArrayList;

public class Tagesordnung {
	private ArrayList<TOP> tops;
	private String titel;
	private URL toURL;
	
	public Tagesordnung(URL toURL) {
		this.toURL = toURL;
		tops = new ArrayList<TOP>();
	}
	
	public String toString() {
		String out = "= "+titel+" =\n";
		for(TOP t : tops) {
			out = out + t.toString() + "\n";
		}
		return out;
	}
	
	public String toWiki() {
		String out = "== " + titel + "==\n"+
				"Originalfassung (mit Protokoll): "+toURL.toExternalForm()+"\n\n";
		for(TOP t : tops) {
			out = out + t.toWiki() + "\n";
		}
		out = out + "\n\n[[Kategorie:Steglitz-Zehlendorf/BVV/TOPs]]";
		return out;
	}
	
	public void setTitel(String titel) {
		this.titel = titel;
	}
	
	public void addTOP(TOP top) {
		tops.add(top);
	}
	
	public void initTOPs() {
		ArrayList<Thread> threads = new ArrayList<Thread>();
		for(TOP top: tops) {
			Thread t = new Thread(new InitTopRunner(top));
			t.start();
			threads.add(t);
		}
		for(Thread t : threads) {
			while(t.isAlive()) {
				try {
					t.join();
				} catch (InterruptedException e) {}
			}
			//System.out.print(".");
		}
		//System.out.println();
	}
	
	private class InitTopRunner implements Runnable {
		private TOP top;
		
		public InitTopRunner(TOP top) {
			this.top = top;
		}
		public void run() {
			try {
				top.init();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
}
