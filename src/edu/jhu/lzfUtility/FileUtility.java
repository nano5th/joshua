/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.lzfUtility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

/**
 * utility functions for file operations
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class FileUtility{
	
	public static void print_hash_tbl(HashMap tbl, String f_out, boolean key_only, boolean vector_value){
		//	#### write hashtable
			BufferedWriter out = FileUtility.getWriteFileStream(f_out);
			System.out.println("########### write hash table to file " + f_out);
			for(Iterator it = tbl.keySet().iterator(); it.hasNext(); ){
				String key = (String) it.next();
				if(key_only)
					FileUtility.write_lzf(out, key + "\n");
				else{
					if(vector_value){
						FileUtility.write_lzf(out, key + " |||");
						Double[] vals = (Double[]) tbl.get(key);
						for(int i=0; i<vals.length; i++)
							FileUtility.write_lzf(out, " " + vals[i]);
						FileUtility.write_lzf(out, "\n");
					}else
						FileUtility.write_lzf(out, key + " ||| " + tbl.get(key) +"\n");
				}
			}
			FileUtility.close_write_file(out);
	}	
	
	public static void print_hash_tbl_above_threshold(HashMap tbl, String f_out, boolean key_only, double threshold){
		//	#### write hashtable
			BufferedWriter out = FileUtility.getWriteFileStream(f_out);
			System.out.println("########### write hash table to file " + f_out);
			for(Iterator it = tbl.keySet().iterator(); it.hasNext(); ){
				String key = (String) it.next();
				Double val = (Double)tbl.get(key);
				if(val>threshold){
					if(key_only)
						FileUtility.write_lzf(out, key + "\n");
					else{
						FileUtility.write_lzf(out, key + " ||| " + val +"\n");
					}
				}
			}
			FileUtility.close_write_file(out);
	}	
	
	public static BufferedReader getReadFileStream(String filename, String enc) {
		BufferedReader in =null;
		try {
			if(filename.endsWith(".gz")==true){
				in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)),enc));
			}else{
				in = new BufferedReader(new InputStreamReader(new FileInputStream(filename),enc));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return in;
	}
	
	public static BufferedReader getReadFileStream(String filename) {
		BufferedReader in =null;
		try {
			if(filename.endsWith(".gz")==true){
				in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)),"UTF-8"));
			}else{
				in = new BufferedReader(new InputStreamReader(new FileInputStream(filename),"UTF-8"));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return in;
	}

	public static BufferedWriter handle_null_writer(BufferedWriter out) {
		BufferedWriter out2=null;
		if(out==null)
			out2 = new BufferedWriter(new OutputStreamWriter(System.out));
		else
			out2 = out;	
		return out2;
	}
	public static BufferedWriter handle_null_file(String f_out) {
		BufferedWriter out=null;
		if(f_out==null)
			out = new BufferedWriter(new OutputStreamWriter(System.out));
		else
			out = FileUtility.getWriteFileStream(f_out,"UTF-8");
		return out;
	}
	
	public static int number_lines_in_file(String file){
		BufferedReader t_reader_test = FileUtility.getReadFileStream(file,"UTF-8");
		int i=0;
		while((read_line_lzf(t_reader_test))!=null){
			i++;
		}
		close_read_file(t_reader_test);
		return i;
	}
	
	public static BufferedWriter getWriteFileStream(String filename, String enc) {
		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename) , enc));	
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	//do not overwrite, append
	public static BufferedWriter getWriteFileStream_append(String filename, String enc) {
		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,true) , enc));	
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	public static 	BufferedWriter getWriteFileStream(String filename) {
		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename) , "UTF-8"));	
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	public static String read_line_lzf(BufferedReader in){
		String str="";
		try {
			str = in.readLine();	
		}
		catch (IOException e) {
			e.printStackTrace();
		}	
		return str;
	}
	
	public static void write_lzf(BufferedWriter out, String str){
		try {
			//if(out==null)System.out.println("out handler is null");
			//if(str==null)System.out.println("str handler is null");
			out.write(str);	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void flush_lzf(BufferedWriter out){
		try {
			out.flush();	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void close_write_file(BufferedWriter out){
		try {
			out.close();	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
	public static void close_read_file(BufferedReader in){
		try {
			in.close();	
		}
		catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
//end of utility for file options
