/*
 * Copyright (c) 2010 - 2015 Norwegian Agency for Pupblic Government and eGovernment (Difi)
 *
 * This file is part of Oxalis.
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission
 * - subsequent versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence
 *  is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific language
 *  governing permissions and limitations under the Licence.
 *
 */

package eu.peppol.inbound.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import eu.peppol.as2.inbound.As2InboundModule;
import no.difi.oxalis.commons.plugin.PluginModule;
import no.difi.oxalis.commons.statistics.StatisticsModule;
import no.difi.oxalis.inbound.guice.OxalisInboundModule;
import eu.peppol.persistence.guice.OxalisDataSourceModule;
import eu.peppol.persistence.guice.RepositoryModule;
import eu.peppol.util.OxalisKeystoreModule;
import eu.peppol.util.OxalisProductionConfigurationModule;
import no.difi.oxalis.commons.mode.ModeModule;
import no.difi.oxalis.commons.timestamp.TimestampModule;
import no.difi.oxalis.commons.tracing.TracingModule;
import no.difi.oxalis.commons.persist.PersisterModule;
import no.difi.oxalis.inbound.verifier.VerifierModule;

/**
 * Wires our object graph together using Google Guice.
 *
 * @author steinar
 *         Date: 29.11.13
 *         Time: 10:26
 * @author erlend
 */
public class OxalisGuiceContextListener extends GuiceServletContextListener {

    private Injector injector;

    public OxalisGuiceContextListener() {
        this(Guice.createInjector(
                new OxalisKeystoreModule(),

                // Mode
                new ModeModule(),

                // Tracing
                new TracingModule(),

                // Timestamp
                new TimestampModule(),

                // Persisters
                new PersisterModule(),

                // Verifier
                new VerifierModule(),

                // Provides the DBMS Repositories
                new RepositoryModule(),

                // Statistics
                new StatisticsModule(),

                // Plugins
                new PluginModule(),

                // And the Data source
                new OxalisDataSourceModule(),

                // Configuration data
                new OxalisProductionConfigurationModule(),

                // AS2
                new As2InboundModule(),

                // SevletModule is provided by Guice
                new OxalisInboundModule()
        ));
    }

    public OxalisGuiceContextListener(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Injector getInjector() {
        return injector;
    }
}
