package nl.sidnlabs.oxalis;

import com.sun.xml.ws.transport.http.client.HttpTransportPipe;
import eu.peppol.BusDoxProtocol;
import eu.peppol.identifier.ParticipantId;
import eu.peppol.identifier.PeppolDocumentTypeId;
import eu.peppol.identifier.PeppolProcessTypeId;
import eu.peppol.outbound.OxalisOutboundModule;
import eu.peppol.outbound.transmission.*;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Jelte Jansen
 */
public class Main {

    private static OptionSpec<String> documentIdentifier;
    private static OptionSpec<File> xmlDocument;
    private static OptionSpec<String> sender;
    private static OptionSpec<String> recipient;
    private static OptionSpec<String> destinationUrl;

    private static OptionSpec<Boolean> trace;

    public static void main(String[] args) throws Exception {

        OptionParser optionParser = getOptionParser();

        if (args.length == 0) {
            System.out.println("");
            optionParser.printHelpOn(System.out);
            System.out.println("");
            return;
        }

        OptionSet optionSet;

        try {
            optionSet = optionParser.parse(args);
        } catch (Exception e) {
            printErrorMessage(e.getMessage());
            return;
        }

        String senderId = sender.value(optionSet);
        String recipientId = recipient.value(optionSet);
        File xmlFile = xmlDocument.value(optionSet);

        if (!xmlFile.exists()) {
            printErrorMessage("XML document " + xmlFile + " does not exist");
            return;
        }

        try {

            System.out.println("");
            System.out.println("");
            System.out.println("nothing here just yet");

            // find the document the ID refers to
            // bootstraps the Oxalis outbound module
            OxalisOutboundModule oxalisOutboundModule = new OxalisOutboundModule();

            // creates a transmission request builder and enable tracing
            TransmissionRequestBuilder requestBuilder = oxalisOutboundModule.getTransmissionRequestBuilder();

            if (recipientId != null) {
                requestBuilder.receiver(new ParticipantId(recipientId));
            }

            // add sender participant
            if (senderId != null) {
                requestBuilder.sender((new ParticipantId(senderId)));
            }




            // Supplies the payload
            // Should we make this optional and instead derive the content on a local invoice?
            // what about the response reason (if any)
            // Keep it seperate? If so, why not simply use oxalis-standalone to send response?
            requestBuilder.payLoad(new FileInputStream(xmlFile));


            // Overrides the destination URL if so requested
            if (optionSet.has(destinationUrl)) {
                String destinationString = destinationUrl.value(optionSet);
                URL destination;

                try {
                    destination = new URL(destinationString);
                } catch (MalformedURLException e) {
                    printErrorMessage("Invalid destination URL " + destinationString);
                    return;
                }

                // Fetches the transmission method, which was overridden on the command line
/*
                String accessPointSystemIdentifier = destinationSystemId.value(optionSet);
                if (accessPointSystemIdentifier == null) {
                    throw new IllegalStateException("Must specify AS2 system identifier if using AS2 protocol");
                }
                requestBuilder.overrideAs2Endpoint(destination, accessPointSystemIdentifier);
*/
                requestBuilder.overrideAs2Endpoint(destination, "APP_1000000247");
            }



            // Specifying the details completed, creates the transmission request
            TransmissionRequest transmissionRequest = requestBuilder.build();

            System.out.println("REQUEST:::::");
            System.out.println(transmissionRequest);
            System.out.println("REQUEST:END:");
            // Fetches a transmitter ...
            Transmitter transmitter = oxalisOutboundModule.getTransmitter();

            // ... and performs the transmission
            TransmissionResponse transmissionResponse = transmitter.transmit(transmissionRequest);

            // Write the transmission id and where the message was delivered

            System.out.printf("Message using messageId %s sent to %s using %s was assigned transmissionId %s\n",
                    transmissionResponse.getStandardBusinessHeader().getMessageId().stringValue(),
                    transmissionResponse.getURL().toExternalForm(),
                    transmissionResponse.getProtocol().toString(),
                    transmissionResponse.getTransmissionId()
                );


        } catch (Exception e) {
            System.out.println("");
            System.out.println("Message failed : " + e.getMessage());
            e.printStackTrace();
            System.out.println("");
        }
    }

    private static void printErrorMessage(String message) {
        System.out.println("");
        System.out.println("*** " + message);
        System.out.println("");
    }

    static OptionParser getOptionParser() {
        OptionParser optionParser = new OptionParser();
        documentIdentifier = optionParser.accepts("i", "Document Identifier to reply to").withRequiredArg().ofType(String.class).required();
        xmlDocument = optionParser.accepts("f", "XML document file to be sent").withRequiredArg().ofType(File.class).required();
        sender = optionParser.accepts("s", "sender [e.g. 9908:976098897]").withRequiredArg();
        recipient = optionParser.accepts("r", "recipient [e.g. 9908:976098897]").withRequiredArg();
        destinationUrl = optionParser.accepts("u", "destination URL").withRequiredArg();
        return optionParser;
    }
}
