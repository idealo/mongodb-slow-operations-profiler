package de.idealo.mongodb.slowops.dto;

import java.util.List;

import com.google.common.collect.Lists;

public class TableDto {
	
	private final List<List<Object>> rows;

	public TableDto() {
		this.rows = Lists.newArrayList();
	}
	
	
	public void addRow(List<Object> row){
		if(row!=null){
			rows.add(row);
		}
	}
	
	public void addRows(TableDto tableToAdd){
		if(tableToAdd!=null){
			rows.addAll(tableToAdd.getTableRows());
		}
	}
	
	public List<List<Object>> getTableRows(){
		return rows;
	}
}
