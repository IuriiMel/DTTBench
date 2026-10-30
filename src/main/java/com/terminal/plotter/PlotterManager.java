package com.terminal.plotter;

import com.terminal.port.PortID;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlotterManager {
    private static final Map<PortID, Plotter> plotters = new LinkedHashMap<>();

    public static void plot(PortID portID) {
        plotters.computeIfAbsent(portID, k -> new Plotter()).plotGraph();
    }

    public static void addData(PortID portID, String seriesName, double value, String units) {
        plotters.computeIfAbsent(portID, k -> new Plotter()).addData(seriesName, value, units);
    }

    public static void clear(PortID portID) {
        Plotter plotter = plotters.remove(portID);
        if (plotter != null) {
            plotter.clear();
        }
    }
}
