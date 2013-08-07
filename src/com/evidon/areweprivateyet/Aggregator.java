// Copyright 2013 Evidon.  All rights reserved.
// Use of this source code is governed by a Apache License 2.0
// license that can be found in the LICENSE file.

package com.evidon.areweprivateyet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;
import org.json.JSONObject;

public class Aggregator {
	// VM prop: -Dawby_path=C:/Users/fixanoid-work/Desktop/arewebetteryet/bin/
	String path = System.getProperty("awby_path");

	Map<String, Analyzer> results = new LinkedHashMap<String, Analyzer>();
	Map<String, Map<String, String>> totals = new LinkedHashMap<String, Map<String, String>>();
	Map<String, Map<String, String>> decrease = new LinkedHashMap<String, Map<String, String>>();
	
	// list of baseline domains.
	List<String> domains = new ArrayList<String>();
	
	// list of suspected cdns and first parties for exclusion
	List<String> exclusions = new ArrayList<String>();
	
	public Aggregator() {
		// load exclusions
		try {
			BufferedReader in = new BufferedReader(new FileReader(path + "exclusions.list"));
			String line = in.readLine();
			while (line != null) {
				exclusions.add(line);
				line = in.readLine();
			}
			in.close();
		} catch (Exception e) {
			// exclusions are missing or unreadable
		}
	}
	
	public void addResults(String name, String dbFileName) {
		try {
			System.out.println("Adding " + name + " from " + dbFileName);
			Analyzer ra = new Analyzer(dbFileName);
			results.put(name, ra);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, Integer> getMap(String map, Analyzer ra) {
		Map<String, Integer> mapToUse = null;
		
		switch (map) {
			case "localStorageContents":
				mapToUse = ra.localStorageContents;
				break;
			case "cookieTotals":
				mapToUse = ra.cookieTotals;
				break;
			case "setCookieResponses":
				mapToUse = ra.setCookieResponses;
				break;
			case "requestCountPerDomain":
				mapToUse = ra.requestCountPerDomain;
				break;
		}
		
		return mapToUse;
	}
	
	private void createContent(Workbook wb, Sheet s, String map) {
		Map<String, String> out = new HashMap<String, String>();
		
		int rownum = 2;
		int cellnum = 0;

		// create a merged list of domains.
		domains.clear();
		for (String database : results.keySet()) {
			if (database.equals("baseline")) {
				Analyzer ra = results.get(database);
				Map<String, Integer> mapToUse = this.getMap(map, ra);

				for (String domain : mapToUse.keySet()) {
					if ( (!domains.contains(domain)) && !exclusions.contains(domain) ) {
						domains.add(domain);
						out.put(domain, "");
					}
				}
			}
		}

		CellStyle numberStyle = wb.createCellStyle();
		numberStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("number"));
		s.setColumnWidth(0, 5000);

		for (String domain : domains) {
			cellnum = 0;

			Row r = s.createRow(rownum);
			Cell c = r.createCell(cellnum);
			c.setCellValue(domain);
			cellnum++;

			for (String database : results.keySet()) {
				Analyzer ra = results.get(database);

				Map<String, Integer> mapToUse = this.getMap(map, ra);

				c = r.createCell(cellnum);
				try {
					if (mapToUse.containsKey(domain)) {
						c.setCellValue(mapToUse.get(domain));
					} else {
						c.setCellValue(0);
					}
				} catch (Exception e) {
					c.setCellValue(0);
				}
								
				c.setCellStyle(numberStyle);
						
				cellnum++;
			}
			rownum++;
		}

		
		// Totals.
		rownum++;
		cellnum = 1;
		Row r = s.createRow(rownum);
		
		Cell c = r.createCell(0);
		c.setCellValue("Totals:");
		
		for (int i = 0; i < results.keySet().size(); i++) {
			c = r.createCell(cellnum);
			c.setCellType(Cell.CELL_TYPE_FORMULA);
			c.setCellFormula("SUM(" + getCellLetter(i) + "3:" + getCellLetter(i) + (domains.size() + 2) + ")");
			
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateFormulaCell(c);

			if (!totals.containsKey(s.getRow(1).getCell(i + 1).getStringCellValue())) {
				Map<String, String> contents = new LinkedHashMap<String, String>();
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				totals.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			} else {
				Map<String, String> contents = totals.get(s.getRow(1).getCell(i + 1).getStringCellValue());
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				totals.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			}
			
			cellnum++;
		}

		// Delta/Reduction
		rownum++;
		cellnum = 1;
		r = s.createRow(rownum);
		
		c = r.createCell(0);
		c.setCellValue("Tracking Decrease:");
		
		for (int i = 0; i < results.keySet().size(); i++) {
			c = r.createCell(cellnum);
			c.setCellType(Cell.CELL_TYPE_FORMULA);
			c.setCellFormula("ROUND((100-(" + getCellLetter(i) + (rownum) + "*100/B" + (rownum) + ")),0)");
			
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			evaluator.evaluateFormulaCell(c);

			if (!decrease.containsKey(s.getRow(1).getCell(i + 1).getStringCellValue())) {
				Map<String, String> contents = new LinkedHashMap<String, String>();
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				decrease.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			} else {
				Map<String, String> contents = decrease.get(s.getRow(1).getCell(i + 1).getStringCellValue());
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				
				decrease.put(s.getRow(1).getCell(i + 1).getStringCellValue(), contents);
			}
			
			cellnum++;
		}
	}
	
	private static String getCellLetter(int i) {
		String letter = "";

		if (i == 0) {
			letter = "B";
		} else if (i == 1) {
			letter = "C";
		} else if (i == 2) {
			letter = "D";
		} else if (i == 3) {
			letter = "E";
		} else if (i == 4) {
			letter = "F";
		} else if (i == 5) {
			letter = "G";
		} else if (i == 6) {
			letter = "H";
		} else if (i == 7) {
			letter = "I";
		} else if (i == 8) {
			letter = "J";
		} else if (i == 9) {
			letter = "K";
		} else if (i == 10) {
			letter = "L";
		} else if (i == 12) {
			letter = "M";
		} else if (i == 13) {
			letter = "N";
		} else if (i == 14) {
			letter = "O";
		}
		
		return letter;
	}

	private void createHeader(Workbook wb, Sheet s, String sheetTitle, int skipCell) {
		int rownum = 0, cellnum = 0;
		Row r = null;
		Cell c = null;

		// Header
		r = s.createRow(rownum);
		c = r.createCell(0);
		c.setCellValue(sheetTitle);
		
		rownum++;
		r = s.createRow(rownum);
		
		if (skipCell > 0) {
			c = r.createCell(cellnum);
			c.setCellValue("");
			cellnum++;
		}

		for (String database : results.keySet()) {
			c = r.createCell(cellnum);
			c.setCellValue(database);
			
			CellStyle cs = wb.createCellStyle();
			Font f = wb.createFont();
			f.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cs.setFont(f);

			c.setCellStyle(cs);
			cellnum++;
		}
	}

	private void createTSV() throws Exception {
		// TSV only contains decreases numbers for the graph.
		StringBuilder top = new StringBuilder(),
				content = new StringBuilder();

		top.append("Points");

		// header
		for (String database : decrease.keySet()) {
			for (String type : decrease.get(database).keySet()) {
				top.append(",");
				top.append(type);
			}

			break;
		}

		// content
		for (String database : decrease.keySet()) {
			if (database.equals("baseline")) {
				continue;
			}

			content.append(database);

			for (String type : decrease.get(database).keySet()) {
				content.append(",");
				Double d = Double.parseDouble(decrease.get(database).get(type));
				content.append( (d.intValue() >= 0) ? d.intValue() : "0"  );
			}

			content.append("\n");
		}

		String o = top.toString();
		o += "\n" + content.toString();
		BufferedWriter bw = new BufferedWriter(new FileWriter(path + "tsv"));
		bw.write(o);
		bw.close();
	}
	
	private void createJson() throws Exception {
		JSONObject json = new JSONObject();
		json.put("date", System.currentTimeMillis());
		JSONArray jsonArray = new JSONArray();
		
		for (String database : results.keySet()) {
			jsonArray.put(database);
		}
		
		json.put("dataset", jsonArray);
		
		JSONObject jsonTotals = new JSONObject();
		for (String database : totals.keySet()) {
			JSONObject jsonTypes = new JSONObject();
			for (String type : totals.get(database).keySet()) {
				jsonTypes.put(type, Double.parseDouble(totals.get(database).get(type)));
			}
			jsonTotals.put(database, jsonTypes);
		}
		
		json.put("totals", jsonTotals);
		
		jsonTotals = new JSONObject();
		for (String database : decrease.keySet()) {
			JSONObject jsonTypes = new JSONObject();
			for (String type : decrease.get(database).keySet()) {
				jsonTypes.put(type, Double.parseDouble(decrease.get(database).get(type)));
			}
			jsonTotals.put(database, jsonTypes);
		}
		
		json.put("decrease", jsonTotals);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(path + "json"));
		bw.write(json.toString());
		bw.close();
	}

	public void createSpreadSheet() throws Exception {
		int row = 2, cell = 0, sheet = 0;
		FileOutputStream file = new FileOutputStream(path + "analysis.xls");

		Workbook wb = new HSSFWorkbook();

		// content: total content length sheet.
		Sheet s = wb.createSheet();
		wb.setSheetName(sheet, "Content Length");
		this.createHeader(wb, s, "Total Content Length in MB", 0);

		Row r = s.createRow(row);
		for (String database : results.keySet()) {
			Cell c = r.createCell(cell);
			c.setCellValue(results.get(database).totalContentLength / 1024 / 1024);
			cell ++;
		}
		
		row++;
		cell = 0;
		r = s.createRow(row);

		for (String database : results.keySet()) {
			Cell c = r.createCell(cell);
			if (database.equals("baseline")) {
				c.setCellValue("Decrease:");

				Map<String, String> contents = new LinkedHashMap<String, String>();
				contents.put(s.getSheetName(), "0");
				decrease.put(database, contents);
			} else {
				c = r.createCell(cell);
				c.setCellType(Cell.CELL_TYPE_FORMULA);
				c.setCellFormula("ROUND((100-(" + getCellLetter(cell - 1) + "3*100/A3)),0)");
				
				FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
				evaluator.evaluateFormulaCell(c);
				
				Map<String, String> contents = new LinkedHashMap<String, String>();
				contents.put(s.getSheetName(), c.getNumericCellValue() + "");
				decrease.put(database, contents);
			}
			cell ++;
		}
		sheet++;

		// content: HTTP Requests
		s = wb.createSheet();
		wb.setSheetName(sheet, "HTTP Requests");
		this.createHeader(wb, s, "Pages with One or More HTTP Requests to the Public Suffix", 1);
		this.createContent(wb, s, "requestCountPerDomain");
		sheet++;


		// content: HTTP Set-Cookie Responses
		s = wb.createSheet();
		wb.setSheetName(sheet, "HTTP Set-Cookie Responses");
		this.createHeader(wb, s, "Pages with One or More HTTP Responses from the Public Suffix That Include a Set-Cookie Header", 1);
		this.createContent(wb, s, "setCookieResponses");
		sheet++;

		
		// content: Cookie Added - Cookie Deleted
		s = wb.createSheet();
		wb.setSheetName(sheet, "Cookies Added-Deleted");
		this.createHeader(wb, s, "Cookies Added - Cookies Deleted Per Domain", 1);
		this.createContent(wb, s, "cookieTotals");
		sheet++;

		
		// content: Local Storage counts per domain
		s = wb.createSheet();
		wb.setSheetName(sheet, "Local Storage");
		this.createHeader(wb, s, "Local Storage counts per domain", 1);
		this.createContent(wb, s, "localStorageContents");
		sheet++;

		
		// content: Pretty Chart
		s = wb.createSheet();
		wb.setSheetName(sheet, "Overall");
		
		int rownum = 0, cellnum = 0;

		// Header
		r = s.createRow(rownum);
		Cell c = r.createCell(0);
		s.setColumnWidth(0, 8000);
		c.setCellValue("Overall effectiveness measured by percentage of decrease vs baseline (0 for any negative effect)");
		
		rownum++;
		r = s.createRow(rownum);

		cellnum++;

		for (String database : decrease.keySet()) {
			if (database.equals("baseline")) {
				continue;
			}
			
			c = r.createCell(cellnum);
			c.setCellValue(database);
			
			CellStyle cs = wb.createCellStyle();
			Font f = wb.createFont();
			f.setBoldweight(Font.BOLDWEIGHT_BOLD);
			cs.setFont(f);

			c.setCellStyle(cs);
			cellnum++;
		}
		
		CellStyle numberStyle = wb.createCellStyle();
		numberStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("number"));

		// Content
		for (String type : decrease.get("baseline").keySet()) {
			cellnum = 0;
			rownum++;
			
			r = s.createRow(rownum);
			
			c = r.createCell(cellnum);
			c.setCellValue(type);
			cellnum++;
			
			for (String database : decrease.keySet()) {
				if (database.equals("baseline")) {
					continue;
				}

				c = r.createCell(cellnum);
				c.setCellStyle(numberStyle);
				
				double decreaseValue = Double.parseDouble(decrease.get(database).get(type));
				
				if (decreaseValue < 0) 
					decreaseValue = 0;

				c.setCellValue(decreaseValue);
				cellnum++;
			}
		}

		/*
		for (String database : decrease.keySet()) {
			for (String type : decrease.get(database).keySet()) {
				System.out.println(database + "|" + type + "|" + decrease.get(database).get(type));
			}
		}
		*/
		
		wb.write(file);
		file.close();
	}

	public void output() throws Exception {
		createSpreadSheet();

		// Create JSON output object
		createJson();

		// Create TSV in the d3 format
		createTSV();
	}
	

	
	public static void main(String[] args) {
		Aggregator agg = new Aggregator();

		String[] profiles = {"baseline", "ghostery", "dntme", "disconnect", "abp-fanboy", "abp-easylist", "trackerblock", /*"requestpolicy", "noscript",*/ "cookies-blocked"};
		//String[] profiles = {"baseline", "ghostery"};
		for (String profile : profiles) {
			agg.addResults(profile, "fourthparty-" + profile + ".sqlite");	
		}
		
		try {
			agg.output();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
