package eu.sendregning.oxalis;

import com.sun.xml.ws.transport.http.client.HttpTransportPipe;
import eu.peppol.BusDoxProtocol;
import eu.peppol.identifier.ParticipantId;
import eu.peppol.identifier.PeppolDocumentTypeId;
import eu.peppol.identifier.PeppolProcessTypeId;
import eu.peppol.outbound.OxalisOutboundModule;
import eu.peppol.outbound.transmission.*;
import eu.peppol.util.GlobalConfiguration;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author ravnholt
 * @author Steinar O. Cook
 * @author Nigel Parker
 * @author Thore Johnsen
 */
public class Main {

    private static OptionSpec<File> xmlDocument;
    private static OptionSpec<String> sender;
    private static OptionSpec<String> recipient;
    private static OptionSpec<String> destinationUrl;
    private static OptionSpec<String> transmissionMethod;   // The protocol START or AS2
    private static OptionSpec<Boolean> trace;
    private static OptionSpec<String> destinationSystemId;  // The AS2 destination system identifier
    private static OptionSpec<String> docType;              // The PEPPOL document type (very long string)
    private static OptionSpec<String> profileType;          // The PEPPOL document profile
    private static OptionSpec<Boolean> replyToSender;
    private static OptionSpec<String> replyToIdentifier;
    private static OptionSpec<String> replyToEndpoint;

    public static void main(String[] args) throws Exception {

        GlobalConfiguration globalConfiguration = globalConfiguration = GlobalConfiguration.getInstance();

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

        File xmlInvoice = xmlDocument.value(optionSet);

        if (!xmlInvoice.exists()) {
            printErrorMessage("XML document " + xmlInvoice + " does not exist");
            return;
        }

        String recipientId = recipient.value(optionSet);
        String senderId = sender.value(optionSet);

        // Enable SOAP logging on the client side if -t was specified on the command line
        if (optionSet.has("t")) {
            HttpTransportPipe.dump = true;
        }

        try {

            System.out.println("");
            System.out.println("");

            // bootstraps the Oxalis outbound module
            OxalisOutboundModule oxalisOutboundModule = new OxalisOutboundModule();

            // creates a transmission request builder and enable tracing
            TransmissionRequestBuilder requestBuilder = oxalisOutboundModule.getTransmissionRequestBuilder();
            requestBuilder.trace(trace.value(optionSet));
            System.out.println("Trace mode of RequestBuilder: " + requestBuilder.isTraceEnabled());

            // add receiver participant
            if (recipientId != null) {
                requestBuilder.receiver(new ParticipantId(recipientId));
            }

            // add sender participant
            if (senderId != null) {
                requestBuilder.sender((new ParticipantId(senderId)));
            }

            if (docType != null && docType.value(optionSet) != null) {
                requestBuilder.documentType(PeppolDocumentTypeId.valueOf(docType.value(optionSet)));
            }

            if (profileType != null && profileType.value(optionSet) != null) {
                requestBuilder.processType(PeppolProcessTypeId.valueOf(profileType.value(optionSet)));
            }

            // Supplies the payload
            requestBuilder.payLoad(new FileInputStream(xmlInvoice));

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
                BusDoxProtocol busDoxProtocol = BusDoxProtocol.instanceFrom(transmissionMethod.value(optionSet));
                if (busDoxProtocol == BusDoxProtocol.START){
                    // ... and gives it to the transmission request builder
                    requestBuilder.overrideEndpointForStartProtocol(destination);
                } else if (busDoxProtocol == BusDoxProtocol.AS2) {
                    String accessPointSystemIdentifier = destinationSystemId.value(optionSet);
                    if (accessPointSystemIdentifier == null) {
                        throw new IllegalStateException("Must specify AS2 system identifier if using AS2 protocol");
                    }
                    requestBuilder.overrideAs2Endpoint(destination, accessPointSystemIdentifier);
                } else {
                    throw new IllegalStateException("Unknown busDoxProtocol : " + busDoxProtocol);
                }
            }

            // Set reply-to headers
            if (optionSet.has(replyToEndpoint)) {
                // TODO catch URL error?
                URL replyToEndpointURL = new URL(replyToEndpoint.value(optionSet));
                requestBuilder.replyToEndpoint(replyToEndpointURL);
                requestBuilder.replyToSender();
            } else if (optionSet.has(replyToIdentifier)) {
                ParticipantId replyToIdentifierId = new ParticipantId(replyToIdentifier.value(optionSet));
                requestBuilder.replyToIdentifier(replyToIdentifierId);
                requestBuilder.replyToSender();
            } else if (optionSet.has(replyToSender)) {
                requestBuilder.replyToSender();
            }

            // Specifying the details completed, creates the transmission request
            TransmissionRequest transmissionRequest = requestBuilder.build();

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

            // Store the outgoing message in the sent folder
            // should this be in the outbound module itself?
            String baseSentFilePath = globalConfiguration.getSentMessageStore() + "/" + transmissionResponse.getTransmissionId();
            File outFile = new File(baseSentFilePath + ".xml");
            Files.copy(xmlInvoice.toPath(), outFile.toPath());
            PrintWriter writer = new PrintWriter(baseSentFilePath + ".txt", "UTF-8");
            // perhaps store entire response?
            writer.println(transmissionResponse.getStandardBusinessHeader().toString());
            writer.close();
            System.out.println("STORED DATA:");
            System.out.println(transmissionResponse.toString());
        } catch (Exception e) {
            System.out.println("");
            System.out.println("Message failed : " + e.getMessage());
            //e.printStackTrace();
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
        docType = optionParser.accepts("d", "Document type").withRequiredArg();
        profileType = optionParser.accepts("p", "Profile type").withRequiredArg();
        xmlDocument = optionParser.accepts("f", "XML document file to be sent").withRequiredArg().ofType(File.class).required();
        sender = optionParser.accepts("s", "sender [e.g. 9908:976098897]").withRequiredArg();
        recipient = optionParser.accepts("r", "recipient [e.g. 9908:976098897]").withRequiredArg();
        destinationUrl = optionParser.accepts("u", "destination URL").withRequiredArg();
        transmissionMethod = optionParser.accepts("m", "method of transmission: start or as2").requiredIf("u").withRequiredArg();
        destinationSystemId = optionParser.accepts("id","AS2 System identifier, obtained from CN attribute of X.509 certificate").withRequiredArg();
        trace = optionParser.accepts("t", "Trace/log/dump SOAP on transport level").withOptionalArg().ofType(Boolean.class).defaultsTo(false);
        replyToSender = optionParser.accepts("z", "Add reply-to-sender header").withOptionalArg().ofType(Boolean.class);
        replyToIdentifier = optionParser.accepts("x", "Add reply-to-identifier header, overrides default sender address").withOptionalArg().ofType(String.class);
        replyToEndpoint = optionParser.accepts("c", "Add reply-to-Endpoint header, overrides default endpoint address").withOptionalArg().ofType(String.class);
        return optionParser;
    }

    @SuppressWarnings("unused")
    private static String enterPassword() {
        System.out.print("Keystore password: ");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String password = null;
        try {
            password = bufferedReader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                bufferedReader.close();
            } catch (Exception e) {
                /* do nothing */
            }
        }
        return password;
    }

}
