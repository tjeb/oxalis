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
/*
    private static OptionSpec<String> sender;
    private static OptionSpec<String> recipient;
    private static OptionSpec<String> destinationUrl;
*/
    private static OptionSpec<Boolean> trace;
//    private static OptionSpec<String> destinationSystemId;  // The AS2 destination system identifier

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

        File xmlInvoice = xmlDocument.value(optionSet);

        if (!xmlInvoice.exists()) {
            printErrorMessage("XML document " + xmlInvoice + " does not exist");
            return;
        }

        try {

            System.out.println("");
            System.out.println("");
            System.out.println("nothing here just yet");

            // find the document the ID refers to
/*
            // bootstraps the Oxalis outbound module
            OxalisOutboundModule oxalisOutboundModule = new OxalisOutboundModule();

            // creates a transmission request builder and enable tracing
            TransmissionRequestBuilder requestBuilder = oxalisOutboundModule.getTransmissionRequestBuilder();
            requestBuilder.trace(trace.value(optionSet));
            System.out.println("Trace mode of RequestBuilder: " + requestBuilder.isTraceEnabled());

            // Supplies the payload
            requestBuilder.payLoad(new FileInputStream(xmlInvoice));

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
*/
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
        documentIdentifier = optionParser.accepts("i", "Document Identifier to reply to").withRequiredArg().ofType(String.class).required();
        xmlDocument = optionParser.accepts("f", "XML document file to be sent").withRequiredArg().ofType(File.class).required();
/*
        sender = optionParser.accepts("s", "sender [e.g. 9908:976098897]").withRequiredArg();
        recipient = optionParser.accepts("r", "recipient [e.g. 9908:976098897]").withRequiredArg();
        destinationUrl = optionParser.accepts("u", "destination URL").withRequiredArg();
        transmissionMethod = optionParser.accepts("m", "method of transmission: start or as2").requiredIf("u").withRequiredArg();
        destinationSystemId = optionParser.accepts("id","AS2 System identifier, obtained from CN attribute of X.509 certificate").withRequiredArg();
*/
        return optionParser;
    }

/*
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
*/
                /* do nothing */
/*
            }
        }
        return password;
    }
*/

}
