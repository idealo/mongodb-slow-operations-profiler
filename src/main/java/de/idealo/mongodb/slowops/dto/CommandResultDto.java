package de.idealo.mongodb.slowops.dto;

import com.google.common.collect.Lists;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kay.agahd on 29.06.17.
 */
public class CommandResultDto {


    private List<String> tableHeader;
    private TableDto tableBody;
    private int jsonFormattedColumn = -1;

    public CommandResultDto(){
        tableHeader = Lists.newArrayList();
        tableBody = new TableDto();
    }


    public List<String> getTableHeader() {
        return tableHeader;
    }

    public void setTableHeader(List<String> tableHeader) {
        this.tableHeader = tableHeader;
    }

    public TableDto getTableBody() {
        return tableBody;
    }


    public void addTableBody(TableDto tableBodyToAdd) {
        this.tableBody.addRows(tableBodyToAdd);
    }

    public String getTableHeaderAsDatatableJson() {
        final List<Document> dataTableHeader = new ArrayList<>();

        for(String entry : tableHeader){
            dataTableHeader.add(new Document("title", entry));
        }
        final Document doc = new Document();
        doc.append("content", dataTableHeader);
        return doc.toJson();
    }

    public String getTableBodyAsJson() {
        final Document doc = new Document();
        doc.append("content", tableBody.getTableRows());
        //doc.toJson();
        return com.mongodb.util.JSON.serialize(doc);
    }

    public void setJsonFormattedColumn(int colNumber){
        jsonFormattedColumn = colNumber;
    }


    public String getJsonFormattedColumnAsDatatableCode(){
        if(jsonFormattedColumn >= 0){
            return ", \"targets\":" + jsonFormattedColumn;
        }
        return "";
    }

}
