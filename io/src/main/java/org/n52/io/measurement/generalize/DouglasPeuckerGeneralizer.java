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
package org.n52.io.measurement.generalize;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Arrays;

import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.DataCollection;
import org.n52.io.response.dataset.measurement.MeasurementData;
import org.n52.io.response.dataset.measurement.MeasurementValue;
import org.n52.io.series.TvpDataCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a generalizer using the Douglas-Peucker Algorithm
 *
 * Characteristic measurement values are picked depending on a given tolerance
 * value. Values that differ less than this tolerance value from an ideal line
 * between some minima and maxima will be dropped.
 */
public final class DouglasPeuckerGeneralizer extends Generalizer<MeasurementData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DouglasPeuckerGeneralizer.class);

    /**
     * Config-key for {@link #maxEntries} of entries.
     */
    private static final String MAX_ENTRIES = "MAX_ENTRIES";

    /**
     * Config-key for {@link #reductionRate}.
     */
    private static final String REDUCTION_RATE = "REDUCTION_RATE";

    /**
     * Config-key for the {@link #toleranceValue}.
     */
    private static final String TOLERANCE_VALUE = "TOLERANCE_VALUE";

    /**
     * {@link #maxEntries} is the value for the maximum points the generalizer
     * will handle, otherwise an exception will be thrown; -1 is unlimited
     */
    private int maxEntries = -1; // fallback default

    /**
     * estimated reduction rate for this use case, where {@link #reductionRate}
     * = 3 means the time series is reduced to 1/3 of it's size; -1 means there
     * is no proper empirical value
     */
    private int reductionRate = -1; // fallback default

    /**
     * Absolute tolerance value.
     */
    private double toleranceValue = 0.1; // fallback default

    /**
     * Creates a new instance. Use static constructors for instantiation.
     *
     * @param parameters Configuration parameters. If <code>null</code> a
     * fallback configuration will be used.
     */
    public DouglasPeuckerGeneralizer(IoParameters parameters) {
        super(parameters);
        try {
            maxEntries = parameters.containsParameter(MAX_ENTRIES)
                    ? parseInt(parameters.getOther(MAX_ENTRIES))
                    : maxEntries;
            reductionRate = parameters.containsParameter(REDUCTION_RATE)
                    ? parseInt(parameters.getOther(REDUCTION_RATE))
                    : reductionRate;
            toleranceValue = parameters.containsParameter(TOLERANCE_VALUE)
                    ? parseDouble(parameters.getOther(TOLERANCE_VALUE))
                    : toleranceValue;
        } catch (NumberFormatException ne) {
            LOGGER.error("Error while reading properties!  Using fallback defaults.", ne);
            throw new IllegalStateException("Error while reading properties! Using fallback defaults.");
        }
    }

    @Override
    public String getName() {
        return "Douglas-Peucker";
    }

    @Override
    public DataCollection<MeasurementData> generalize(DataCollection<MeasurementData> data) throws GeneralizerException {
        TvpDataCollection<MeasurementData> generalizedDataCollection = new TvpDataCollection<MeasurementData>();
        for (String timeseriesId : data.getAllSeries().keySet()) {
            MeasurementData timeseries = data.getSeries(timeseriesId);
            generalizedDataCollection.addNewSeries(timeseriesId, generalize(timeseries));
        }
        return generalizedDataCollection;
    }

    private MeasurementData generalize(MeasurementData timeseries) throws GeneralizerException {
        MeasurementValue[] originalValues = getValueArray(timeseries);
        if (originalValues.length < 3 || toleranceValue <= 0) {
            return timeseries;
        }

        if (maxEntries != -1 && originalValues.length > maxEntries) {
            throw new GeneralizerException("Maximum number of entries exceeded (" + originalValues.length + ">"
                    + maxEntries + ")!");
        }

        MeasurementData generalizedTimeseries = new MeasurementData();
        MeasurementValue[] generalizedValues = recursiveGeneralize(timeseries);
        generalizedTimeseries.addValues(generalizedValues);

        // add first element if new list is empty
        if (generalizedValues.length == 0 && originalValues.length > 0) {
            generalizedTimeseries.addValues(originalValues[0]);
        }

        // add the last one if not already contained!
        if (generalizedValues.length > 0 && originalValues.length > 0) {
            MeasurementValue lastOriginialValue = originalValues[originalValues.length - 1];
            MeasurementValue lastGeneralizedValue = generalizedValues[generalizedValues.length - 1];
            if (lastGeneralizedValue.getTimestamp() != lastOriginialValue.getTimestamp()) {
                generalizedTimeseries.addValues(lastOriginialValue);
            }
        }
        return generalizedTimeseries;
    }

    private MeasurementValue[] getValueArray(MeasurementData timeseries) {
        return timeseries.getValues().toArray(new MeasurementValue[0]);
    }

    private MeasurementValue[] recursiveGeneralize(MeasurementData timeseries) {
        MeasurementValue[] values = getValueArray(timeseries);
        MeasurementValue startValue = getFirstValue(timeseries);
        MeasurementValue endValue = getLastValue(timeseries);
        Line2D.Double line = createTendencyLine(startValue, endValue);

        // find the point of maximum distance to the line
        int index = 0;
        double maxDist = 0;
        double distance;

        // start and end value are not mentioned
        for (int i = 1; i < values.length - 1; i++) {
            MeasurementValue timeseriesValue = values[i];
            distance = calculateDistance(line, timeseriesValue);
            if (distance > maxDist) {
                index = i;
                maxDist = distance;
            }
        }

        if (maxDist < toleranceValue) {
            return getValueArray(timeseries);
        } else {
            // split and handle both parts separately
            MeasurementData generalizedData = new MeasurementData();
            MeasurementData firstPartToBeGeneralized = new MeasurementData();
            MeasurementData restPartToBeGeneralized = new MeasurementData();
            firstPartToBeGeneralized.addValues(Arrays.copyOfRange(values, 0, index));
            restPartToBeGeneralized.addValues(Arrays.copyOfRange(values, index + 1, values.length));
            generalizedData.addValues(recursiveGeneralize(firstPartToBeGeneralized));
            generalizedData.addValues(recursiveGeneralize(restPartToBeGeneralized));
            return getValueArray(generalizedData);
        }

    }

    private double calculateDistance(Line2D.Double line, MeasurementValue timeseriesValue) {
        return line.ptLineDist(createPoint(timeseriesValue));
    }

    private Point2D.Double createPoint(MeasurementValue timeseriesValue) {
        Long timestamp = timeseriesValue.getTimestamp();
        double value = timeseriesValue.getValue();

        Point2D.Double p = new Point2D.Double();
        p.setLocation(timestamp, value);
        return p;
    }

    private Line2D.Double createTendencyLine(MeasurementValue start, MeasurementValue end) {
        Long startTime = start.getTimestamp();
        double startValue = start.getValue();
        Long endTime = end.getTimestamp();
        double endValue = end.getValue();
        return new Line2D.Double(startTime, startValue, endTime, endValue);
    }

    private MeasurementValue getFirstValue(MeasurementData timeseries) {
        MeasurementValue[] values = getValueArray(timeseries);
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Timeseries must not be empty.");
        }
        return values[0];
    }

    private MeasurementValue getLastValue(MeasurementData timeseries) {
        MeasurementValue[] values = getValueArray(timeseries);
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Timeseries must not be empty.");
        }
        return values[values.length - 1];
    }

}
