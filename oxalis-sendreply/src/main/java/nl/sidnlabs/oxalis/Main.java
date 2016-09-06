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
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;

import eu.peppol.PeppolMessageMetaData;
import eu.peppol.util.GlobalConfiguration;

import java.util.List;

/**
 * @author Jelte Jansen
 */
public class Main {

    //private static OptionSpec<String> metaDataFile;

    private static OptionSpec<String> responseLines;

    private static OptionSpec<String> responseCode;
    private static OptionSpec<File> xmlDocument;

    private static OptionSpec<String> userId;
    private static OptionSpec<String> recipientId;
    private static OptionSpec<String> transmissionId;
    
    private static OptionSpec<String> documentIdentifier;
    private static OptionSpec<String> sender;
    private static OptionSpec<String> recipient;
    private static OptionSpec<String> destinationUrl;

    private static OptionSpec<Boolean> trace;

    static final String cbc = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String cac = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String ns = "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2";

    public static void main(String[] args) throws Exception {
        GlobalConfiguration globalConfiguration = GlobalConfiguration.getInstance();

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

        String rcode = responseCode.value(optionSet);
        //String metaDataF = metaDataFile.value(optionSet);
        File xmlFile = xmlDocument.value(optionSet);

        String storePath = globalConfiguration.getInboundMessageStore();
        String user = userId.value(optionSet);
        String recipient = recipientId.value(optionSet);
        String transmission = transmissionId.value(optionSet);

        if (!"AP".equals(rcode) && !"RE".equals(rcode)) {
            System.out.println("Response code must be either AP or RE");
            System.exit(1);
        }

        String metaDataFile = storePath + "/" + user + "/" + recipient + "/" + transmission + ".txt";
        String xmlDocFile = storePath + "/" + user + "/" + recipient + "/" + transmission + ".xml";
        
        System.out.println("Metadata file: " + metaDataFile);
        PeppolMessageMetaData metadata = PeppolMessageMetaData.fromJson(metaDataFile);
        //System.out.println(metadata.toString());

        // Build that doc

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document xmlDoc = docBuilder.parse(xmlDocFile);


        System.out.println("XML DOC: ");
        String documentID = findDocumentID(xmlDoc);

        System.out.println("Reply to message from: " + metadata.getSenderId());
        if (metadata.getReplyToEndpoint() != null) {
            System.out.println("Send reply to endpoint: " + metadata.getReplyToEndpoint());
        } else if (metadata.getReplyToIdentifier() != null) {
            System.out.println("Send reply to identifier: " + metadata.getReplyToIdentifier());
        } else {
            System.out.println("Message has no reply-to option set, sender does not accept replies, aborting.");
            return;
        }

        // root elements
        Document doc = docBuilder.newDocument();

        Element root = doc.createElementNS(ns, "ApplicationResponse");

        addElement(doc, root, cbc, "UBLVersionID", "2.1");
        addElement(doc, root, cbc, "CustomizationID", "urn:www.cenbii.eu:transaction:biitms071:ver2.0:extended:urn:www.peppol.eu:bis:peppol36a:ver1.0:extended:urn:www.simplerinvoicing.org:si:si-ubl:ver1.2.0");
        addElement(doc, root, cbc, "ProfileID", "urn:www.cenbii.eu:profile:bii36:ver2.0");
        addElement(doc, root, cbc, "ID", transmission);
        // is this today or date of invoice?
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        addElement(doc, root, cbc, "IssueDate", dateFormat.format(date));
        Element e = addElement(doc, root, cac, "SenderParty");
        Element ep = addElement(doc, e, cbc, "EndpointID", metadata.getRecipientId().toString());
        // TODO: get schemeid from endpointid
        ep.setAttribute("schemeID", "NL:KVK");

        e = addElement(doc, root, cac, "ReceiverParty");
        ep = addElement(doc, e, cbc, "EndpointID", metadata.getSenderId().toString());
        // TODO: get schemeid from endpointid
        ep.setAttribute("schemeID", "NL:KVK");
        doc.appendChild(root);

        e = addElement(doc, root, cac, "DocumentResponse");
        ep = addElement(doc, e, cbc, "ResponseCode", rcode);
        ep.setAttribute("listID", "UNCL4343");
        // TODO: should this be from full xml of busdoc?
        addElement(doc, e, cbc, "ID", documentID);
        
        List<String> lines = responseLines.values(optionSet);
        if (lines.size() > 0) {
            for (String l : lines) {
                // TODO: make this quoted comma-separated? need to make an actual parser then.
                // Also, do we want to do this before we start building the xml? or meh?
                String array[] = l.split("(?<!\\\\),");
                // linereference and responsecode are mandatory,
                // description and statusreasoncode are optional
                String lineReferenceText = array[0];
                String responseCodeText = array[1];
                if (array.length < 2) {
                    System.out.println("Additional line options need to have at least lineID and responsecode");
                    System.exit(1);
                }
                Element lresponse = addElement(doc, e, cac, "LineResponse");
                Element lreference = addElement(doc, lresponse, cac, "LineReference");
                addElement(doc, lreference, cbc, "LineID", lineReferenceText);
                Element response = addElement(doc, lresponse, cac, "Response");
                Element responsecode = addElement(doc, response, cbc, "ResponseCode", responseCodeText);
                responsecode.setAttribute("listID", "UNCL4343");

                if (array.length >= 3) {
                    String descriptionText = array[2];
                    if (!descriptionText.equals("")) {
                        addElement(doc, response, cbc, "Description", descriptionText);
                    }
                }
                if (array.length >= 4) {
                    String statusReasonCodeText  = array[3];
                    if (statusReasonCodeText.equals("SV") ||
                        statusReasonCodeText.equals("BV") ||
                        statusReasonCodeText.equals("BW")) {
                        Element status = addElement(doc, response, cac, "Status");
                        Element statusReasonCode = addElement(doc, status, cbc, "StatusReasonCode", statusReasonCodeText);
                        statusReasonCode.setAttribute("listID", "PEPPOLSTATUS");
                    } else {
                        System.out.println("Error, status reason code must be SV, BV or BW");
                        System.exit(1);
                    }
                }
                
                System.out.println("RLINE: " + l);
            }
        }

        System.out.println("Document:");
        printDocument(doc, System.out);
        System.out.println("End of document");
        System.exit(0);
    }
//urn:www.cenbii.eu:transaction:biitms071:ver2.0:extended:urn:www.peppol.eu:bis:peppol36a:ver1.0:extended:urn:www.simplerinvoicing.org:si:si-ubl:ver1.2.0

    private static Element addElement(Document doc, Element base, String ns, String name) {
        return addElement(doc, base, ns, name, null);
    }

    private static Element addElement(Document doc, Element base, String ns, String name, String value) {
        Element el = doc.createElementNS(ns, name);
        if (value != null) {
            el.setTextContent(value);
        }
        base.appendChild(el);
        return el;
    }
    
    public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc), 
             new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }
    public static void foo(String[] args) throws Exception {

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
                requestBuilder.overrideAs2Endpoint(destination, "APP_1000000247");
*/
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
        //metaDataFile = optionParser.accepts("m", "Metadata file of the document to reply to").withRequiredArg().ofType(String.class).required();
        responseCode = optionParser.accepts("c", "Response code; AP for accepted, RE for rejected").withRequiredArg().ofType(String.class).required();
        xmlDocument = optionParser.accepts("f", "XML snippet file with additional line information; - for stdin").withRequiredArg().ofType(File.class);
        transmissionId = optionParser.accepts("t", "Transmission ID of the document to reply to").withRequiredArg().ofType(String.class).required();
        userId = optionParser.accepts("u", "User ID").withRequiredArg().ofType(String.class).required();
        recipientId = optionParser.accepts("r", "User ID of the party to send the response to (i.e. the sender of the business document)").withRequiredArg().ofType(String.class).required();
        responseLines = optionParser.accepts("a", "Additional response line (comma-separated <section>,<rcode>,[description],[statuscode])").withRequiredArg().ofType(String.class);
        /*
        documentIdentifier = optionParser.accepts("i", "Document Identifier to reply to").withRequiredArg().ofType(String.class).required();
        xmlDocument = optionParser.accepts("f", "XML document file to be sent").withRequiredArg().ofType(File.class).required();
        sender = optionParser.accepts("s", "sender [e.g. 9908:976098897]").withRequiredArg();
        recipient = optionParser.accepts("r", "recipient [e.g. 9908:976098897]").withRequiredArg();
        destinationUrl = optionParser.accepts("u", "destination URL").withRequiredArg();
        */
        return optionParser;
    }

    static String findDocumentID(Document xmlDocument) {
        // The document can in theory be any business document type, so
        // Order, Invoice, etc.
        // Additionally, Oxalis stores different formats depending on transport type
        // assuming there still is support for the old START), and it leaves
        // the envelope in the file in case of AS2, so if there is a BusinessDocument wrapper
        // we need to get through that as well.
        NodeList candidates = xmlDocument.getDocumentElement().getElementsByTagNameNS(cbc, "ID");
        if (candidates.getLength() > 0) {
            return candidates.item(0).getTextContent();
        } else {
            return null;
        }
    }
}
