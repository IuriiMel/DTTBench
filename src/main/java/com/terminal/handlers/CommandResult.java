package com.terminal.handlers;

import com.terminal.port.SessionListener.LogType;

/**
 * Data transfer object for parsed command responses.
 * Contains formatted output for display and optional numeric values for plotting.
 */
public class CommandResult {
    private String formattedData;
    private Double plotValue;
    private String plotUnit;
    private String comment;
    private LogType type;

    public String getFormattedData() {
        return formattedData;
    }
    public void setFormattedData(String formattedData) {
        this.formattedData = formattedData;
    }
    public Double getPlotValue() {
        return plotValue;
    }
    public void setPlotValue(Double plotValue) {
        this.plotValue = plotValue;
    }
    public String getPlotUnit() {
        return plotUnit;
    }
    public void setPlotUnit(String plotUnit) {
        this.plotUnit = plotUnit;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public LogType getType() {
        return type;
    }
    public void setType(LogType type){
        this.type = type;
    }
}
