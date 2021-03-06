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
package org.n52.io;

import java.io.File;

import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetOutput;

public class DefaultIoFactory<D extends Data<V>, DS extends DatasetOutput<V, ?>, V extends AbstractValue<?>> extends ConfigTypedFactory<IoFactory<D, DS, V>> {

    private static final String DEFAULT_CONFIG_FILE = "dataset-io-factory.properties";

    public DefaultIoFactory() {
        super(DEFAULT_CONFIG_FILE);
    }

    public DefaultIoFactory(File configFile) {
        super(configFile);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static DefaultIoFactory<Data<AbstractValue< ? >>, DatasetOutput<AbstractValue< ? >, ? >, AbstractValue< ? >> create() {
        return new DefaultIoFactory();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static DefaultIoFactory<Data<AbstractValue< ? >>, DatasetOutput<AbstractValue< ? >, ? >, AbstractValue< ? >> create(File configFile) {
        return new DefaultIoFactory(configFile);
    }

    @Override
    protected String getFallbackConfigResource() {
        return "/" + DEFAULT_CONFIG_FILE;
    }

    @Override
    protected IoFactory<D, DS, V> initInstance(IoFactory<D, DS, V> instance) {
        return instance;
    }

    @Override
    protected Class<?> getTargetType() {
        return IoFactory.class;
    }


}
