/*
 * Copyright (C) 2013-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.io.measurement.img;

import static org.n52.io.measurement.img.BarRenderer.createBarRenderer;
import static org.n52.io.measurement.img.LineRenderer.createStyledLineRenderer;
import static org.n52.io.style.BarStyle.createBarStyle;
import static org.n52.io.style.LineStyle.createLineStyle;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Month;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;
import org.n52.io.IoProcessChain;
import org.n52.io.IoStyleContext;
import org.n52.io.request.RequestParameterSet;
import org.n52.io.request.RequestSimpleParameterSet;
import org.n52.io.request.StyleProperties;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.dataset.DataCollection;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.measurement.MeasurementData;
import org.n52.io.response.dataset.measurement.MeasurementDatasetMetadata;
import org.n52.io.response.dataset.measurement.MeasurementValue;
import org.n52.io.style.BarStyle;
import org.n52.io.style.LineStyle;

public class MultipleChartsRenderer extends ChartIoHandler {

    public MultipleChartsRenderer(RequestParameterSet request,
            IoProcessChain<MeasurementData> processChain,
            IoStyleContext context) {
        super(request, processChain, context);
    }

    @Override
    public void writeDataToChart(DataCollection<MeasurementData> data) {
        Map<String, MeasurementData> allTimeseries = data.getAllSeries();
        List<? extends DatasetOutput> timeseriesMetadatas = getMetadataOutputs();

        int rendererCount = timeseriesMetadatas.size();
        for (int rendererIndex = 0; rendererIndex < timeseriesMetadatas.size(); rendererIndex++) {

            /*
             * For each index put data and its renderer configured to a particular style.
             *
             * As each timeseries may define its custom styling and different chart types we have to loop over
             * all timeseries to configure chart rendering.
             */
            DatasetOutput timeseriesMetadata = timeseriesMetadatas.get(rendererIndex);

            String timeseriesId = timeseriesMetadata.getId();
            StyleProperties style = getTimeseriesStyleFor(timeseriesId);
            MeasurementData timeseriesData = allTimeseries.get(timeseriesId);

            String chartId = createChartId(timeseriesMetadata);
            ChartIndexConfiguration configuration = new ChartIndexConfiguration(chartId, rendererIndex);
            configuration.setData(timeseriesData, timeseriesMetadata, style);
            configuration.setRenderer(createRenderer(style));

            if (timeseriesData.hasReferenceValues()) {
                int referenceIndex = rendererCount;

                /*
                 * Configure timeseries reference value renderers with the same metadata and add it at the end
                 * of the plot's renderer list.
                 */
                MeasurementDatasetMetadata metadata = timeseriesData.getMetadata();
                Map<String, MeasurementData> referenceValues = metadata.getReferenceValues();
                for (Entry<String, MeasurementData> referencedTimeseries : referenceValues.entrySet()) {
                    String referenceTimeseriesId = referencedTimeseries.getKey();
                    ReferenceValueOutput referenceOutput = getReferenceValue(referenceTimeseriesId, timeseriesMetadata);
                    String referenceChartId = createChartId(timeseriesMetadata, referenceOutput.getLabel());

                    MeasurementData referenceData = referenceValues.get(referenceTimeseriesId);
                    ChartIndexConfiguration referenceConfiguration = new ChartIndexConfiguration(referenceChartId,
                            referenceIndex);
                    StyleProperties referenceStyle = getTimeseriesStyleFor(timeseriesId, referenceTimeseriesId);
                    referenceConfiguration.setReferenceData(referenceData, timeseriesMetadata, referenceStyle);
                    referenceConfiguration.setRenderer(createRenderer(referenceStyle));
                    referenceIndex++;
                }
            }
        }
    }

    private String createChartId(DatasetOutput metadata) {
        return createChartId(metadata, null);
    }

    private String createChartId(DatasetOutput metadata, String referenceId) {
        ParameterOutput feature = metadata.getSeriesParameters().getFeature();
        StringBuilder timeseriesLabel = new StringBuilder();
        timeseriesLabel.append(feature.getLabel());
        if (referenceId != null) {
            timeseriesLabel.append(", ").append(referenceId);
        }
        timeseriesLabel.append(" (").append(createRangeLabel(metadata)).append(")");
        return timeseriesLabel.toString();
    }

    private Renderer createRenderer(StyleProperties properties) {
        if (isBarStyle(properties)) {
            // configure bar chart renderer
            BarStyle barStyle = createBarStyle(properties);
            return createBarRenderer(barStyle);
        }
        // configure line chart renderer
        LineStyle lineStyle = createLineStyle(properties);
        return createStyledLineRenderer(lineStyle);
    }

    private ReferenceValueOutput getReferenceValue(String id, DatasetOutput metadata) {
        for (ReferenceValueOutput referenceOutput : metadata.getReferenceValues()) {
            if (referenceOutput.getReferenceValueId().equals(id)) {
                return referenceOutput;
            }
        }
        return null;
    }

    private class ChartIndexConfiguration {

        private int timeseriesIndex;

        private String chartId;

        public ChartIndexConfiguration(String chartId, int index) {
            if (chartId == null) {
                throw new NullPointerException("ChartId must not be null.");
            }
            this.timeseriesIndex = index;
            this.chartId = chartId;
        }

        public void setRenderer(Renderer renderer) {
            getXYPlot().setRenderer(timeseriesIndex, renderer.getXYRenderer());
            //renderer.setColorForSeries(timeseriesIndex);
            renderer.setColorForSeries();
        }

        public void setData(MeasurementData data, DatasetOutput timeMetadata, StyleProperties style) {
            getXYPlot().setDataset(timeseriesIndex, createTimeseriesCollection(data, style));
            ValueAxis rangeAxis = createRangeAxis(timeMetadata);
            getXYPlot().setRangeAxis(timeseriesIndex, rangeAxis);
            getXYPlot().mapDatasetToRangeAxis(timeseriesIndex, timeseriesIndex);
        }

        public void setReferenceData(MeasurementData data, DatasetOutput timeMetadata, StyleProperties style) {
            getXYPlot().setDataset(timeseriesIndex, createTimeseriesCollection(data, style));
        }

        private TimeSeriesCollection createTimeseriesCollection(MeasurementData data, StyleProperties style) {
            TimeSeriesCollection timeseriesCollection = new TimeSeriesCollection();
            timeseriesCollection.addSeries(createDiscreteTimeseries(data, style));
            timeseriesCollection.setGroup(new DatasetGroup(chartId));
            return timeseriesCollection;
        }

        private TimeSeries createDiscreteTimeseries(MeasurementData timeseriesData, StyleProperties style) {
            TimeSeries timeseries = new TimeSeries(chartId);
            if (hasValues(timeseriesData)) {
                if (isBarStyle(style)) {
                    MeasurementValue timeseriesValue = timeseriesData.getValues().get(0);
                    Date timeOfFirstValue = new Date(timeseriesValue.getTimestamp());
                    RegularTimePeriod timeinterval = determineTimeInterval(timeOfFirstValue, style);

                    double intervalSum = 0.0;
                    for (MeasurementValue value : timeseriesData.getValues()) {
                        if (isValueInInterval(value, timeinterval)) {
                            intervalSum += value.getValue();
                        } else {
                            timeseries.add(timeinterval, intervalSum);
                            timeinterval = determineTimeInterval(new Date(value.getTimestamp()), style);
                            intervalSum = value.getValue();
                        }
                    }
                } else if (isLineStyle(style)) {
                    for (MeasurementValue value : timeseriesData.getValues()) {
                        Second second = new Second(new Date(value.getTimestamp()));
                        timeseries.addOrUpdate(second, value.getValue());
                    }
                }
            }
            return timeseries;
        }

        private boolean hasValues(MeasurementData timeseriesData) {
            return timeseriesData.getValues().size() > 0;
        }

        private RegularTimePeriod determineTimeInterval(Date date, StyleProperties styleProperties) {
            if (styleProperties.getProperties().containsKey("interval")) {
                String interval = styleProperties.getProperties().get("interval");
                if (interval.equals("byHour")) {
                    return new Hour(date);
                } else if (interval.equals("byDay")) {
                    return new Day(date);
                } else if (interval.equals("byMonth")) {
                    return new Month(date);
                }
            }
            return new Week(date);
        }

        /**
         * @param interval the interval to check.
         * @return <code>true</code> if timestamp is within the given interval,
         * otherwise <code>false</code> is returned. If passed interval was
         * <code>null</code> false will be returned.
         * @throws IllegalArgumentException if passed in value is
         * <code>null</code>.
         */
        private boolean isValueInInterval(MeasurementValue value, RegularTimePeriod interval) {
            if (value == null) {
                throw new IllegalArgumentException("TimeseriesValue must not be null.");
            }
            return interval == null
                    || interval.getStart().getTime() <= value.getTimestamp()
                    && value.getTimestamp() < interval.getEnd().getTime();
        }

    }

}
