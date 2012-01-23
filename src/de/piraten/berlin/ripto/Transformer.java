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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Arne Müller <arne.c.mueller@googlemail.com>
 */
public class Transformer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length < 1) {
			System.out.println("Please give the SILFDNR as first parameter");
			System.out.println();
			System.out.println(
	"ripTO - parses BVV agendas published on berlin.de for further processing\n"+ 
    "Copyright (C) 2011  Arne Müller\n"+
	"\n"+
    "This program is free software: you can redistribute it and/or modify\n"+
    "it under the terms of the GNU General Public License as published by\n"+
    "the Free Software Foundation, either version 3 of the License, or\n"+
    "(at your option) any later version.\n"+
    "\n"+
    "This program is distributed in the hope that it will be useful,\n"+
    "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"+
    "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"+
    "GNU General Public License for more details.\n"+
    "\n"+
    "You should have received a copy of the GNU General Public License\n"+
    "along with this program.  If not, see <http://www.gnu.org/licenses/>."
					);
			return;
		}
		String silfdnr = args[0];
		
		// try to read the config file
		Properties config = new Properties();
		try {
			config.load(new FileReader("ripto.conf"));
		} catch (FileNotFoundException e1) {
			System.err.println("unable to read config file, using default");
			config.setProperty("baseurl", "https://www.berlin.de/ba-steglitz-zehlendorf/bvv-online/to010.asp");
		} catch (IOException e1) {
			System.err.println("unable to read config file, using default");
			config.setProperty("baseurl", "https://www.berlin.de/ba-steglitz-zehlendorf/bvv-online/to010.asp");
		}
		
		TOReader reader = new TOReader();
		try {
			Tagesordnung to = reader.read(config.getProperty("baseurl")+"?SILFDNR="+silfdnr);
			System.out.println(to.toWiki());
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
