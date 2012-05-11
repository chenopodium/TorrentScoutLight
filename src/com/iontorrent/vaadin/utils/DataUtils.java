package com.iontorrent.vaadin.utils;

import java.util.Collection;
import java.util.Iterator;

import com.vaadin.data.Item;
import com.vaadin.ui.Table;
import com.vaadin.ui.Window;

public class DataUtils {

	public static String export(Table table, Window mainwindow) {
		Collection<Integer> alldata = (Collection<Integer>) table.getItemIds();
		Iterator<Integer> it = alldata.iterator();
		int rows = alldata.size();
		String s = "";
		int row = 0;
		while (it.hasNext()) {
			Integer Row = it.next();
			Item rowdata = table.getItem(Row);
			Iterator<String> colids = (Iterator<String>) rowdata.getItemPropertyIds().iterator();
			int cols = rowdata.getItemPropertyIds().size();
			int col = 0;
			if (row == 0) {
				while (colids.hasNext()) {
					String colname = colids.next();
					s += colname;
					if (col + 1 < cols) s += ", ";
					else
						s += "\n";
					col++;
				}
			}
			col = 0;
			colids = (Iterator<String>) rowdata.getItemPropertyIds().iterator();
			while (colids.hasNext()) {

				String colname = colids.next();
				String val = "" + rowdata.getItemProperty(colname).getValue();
				s += val;
				if (col + 1 < cols) s += ", ";
				else
					s += "\n";
				col++;
			}
			row++;

		}
		DataDialog dia = new DataDialog(mainwindow, "Table data", s);
		return s;
	}
}
