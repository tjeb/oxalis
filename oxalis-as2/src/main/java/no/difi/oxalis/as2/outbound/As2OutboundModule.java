/*
 * Copyright 2010-2017 Norwegian Agency for Public Management and eGovernment (Difi)
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.oxalis.as2.outbound;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import no.difi.oxalis.api.outbound.MessageSender;

/**
 * Guice module providing AS2 implementation for outbound.
 *
 * @author erlend
 * @since 4.0.0
 */
public class As2OutboundModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Key.get(MessageSender.class, Names.named("oxalis-as2")))
                .to(As2MessageSenderFacade.class);

        bind(As2MessageSender.class);
    }
}
