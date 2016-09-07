package nl.sidnlabs.oxalis;

import eu.peppol.identifier.ParticipantId;
import eu.peppol.outbound.OxalisOutboundModule;
import eu.peppol.outbound.transmission.*;
import eu.peppol.PeppolMessageMetaData;
import eu.peppol.util.GlobalConfiguration;
import eu.peppol.persistence.SimpleMessageRepository;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;

public class ReplySender {
    private String responseCode;
    private String xmlFile;
    //private String userId;
    //private String recipientId;
    //private String transmissionId;
    private String xmlDocumentPath;
    private String metadataDocumentPath;
    private String sentMlrDocumentPath;
    private ArrayList<ResponseLine> responseLines;
    private String apURL;

    PeppolMessageMetaData metadata;
    Document receivedDocument;
    GlobalConfiguration globalConfiguration;

    DocumentBuilder docBuilder;

    static final String cbc = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String cac = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String ns = "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2";

    class ResponseLine {
        String lineReferenceText;
        String responseCodeText;
        String descriptionText;
        String statusReasonCodeText;

        public ResponseLine(String inputline) throws ReplySenderException {
            String array[] = inputline.split("(?<!\\\\),");
            if (array.length < 2) {
                throw new ReplySenderException("Response line options need to have at least lineID and responsecode");
            }
            lineReferenceText = array[0];
            responseCodeText = array[1];
            if (!"AP".equals(responseCodeText) && !"RE".equals(responseCodeText)) {
                throw new ReplySenderException("Response code must be either AP or RE");
            }

            if (array.length >= 3) {
                String descriptionText = array[2];
            }
            if (array.length >= 4) {
                String statusReasonCodeText  = array[3];
                if (!(statusReasonCodeText.equals("SV") ||
                      statusReasonCodeText.equals("BV") ||
                      statusReasonCodeText.equals("BW"))) {
                    throw new ReplySenderException("Status reason code must be SV, BV or BW");
                }
            }
        }

        public boolean hasDescription() {
            return descriptionText != null;
        }

        public boolean hasStatusReasonCode() {
            return statusReasonCodeText != null;
        }

        public String getLineReference() {
            return lineReferenceText;
        }

        public String getResponseCode() {
            return responseCodeText;
        }

        public String getDescription() {
            return descriptionText;
        }

        public String getStatusReasonCode() {
            return statusReasonCodeText;
        }
    }
    
    public ReplySender(String responseCode, String xmlDocumentPath) throws ReplySenderException {
        this(responseCode, xmlDocumentPath, false);
    }
    
    public ReplySender(String responseCode, String xmlDocumentPath, boolean forceSend) throws ReplySenderException {
        globalConfiguration = GlobalConfiguration.getInstance();

        this.responseCode = responseCode;

        this.xmlDocumentPath = xmlDocumentPath;
        int ext_pos = xmlDocumentPath.lastIndexOf(".");
        String basePath = xmlDocumentPath;
        if (ext_pos > 0) {
            basePath = xmlDocumentPath.substring(0, ext_pos);
        }
        this.metadataDocumentPath = basePath + ".txt";
        this.sentMlrDocumentPath = basePath + ".response";

        this.responseLines = new ArrayList<ResponseLine>();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            throw new ReplySenderException(pce);
        }

        readMetaDataFile();
        readReceivedDocument();
        if (metadata.getReplied() && !forceSend) {
            throw new ReplySenderException("This document was already replied to");
        }
        if (metadata.getReplyToEndpoint() == null &&
            metadata.getReplyToIdentifier() == null &&
            !forceSend) {
            throw new ReplySenderException("Document sender did not specify either reply-to-identifier or reply-to-endpoint");
        }
    }

    private void readMetaDataFile() throws ReplySenderException {
        try {
            metadata = PeppolMessageMetaData.fromJson(metadataDocumentPath);
        } catch (Exception exc) {
            throw new ReplySenderException(exc);
        }
    }

    private void readReceivedDocument() throws ReplySenderException {
        try {
            receivedDocument = docBuilder.parse(xmlDocumentPath);
        } catch (org.xml.sax.SAXException saxe) {
            throw new ReplySenderException(saxe);
        } catch (java.io.IOException ioe) {
            throw new ReplySenderException(ioe);
        }
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public void setXmlFile(String xmlFilePath) {
        throw new RuntimeException("not necessary?");
    }

    public void addResponseLine(String line) throws ReplySenderException {
        this.responseLines.add(new ResponseLine(line));
    }

    //
    // Helper functions for building the xml document
    //
    private String findReceivedDocumentID() {
        // The document can in theory be any business document type, so
        // Order, Invoice, etc.
        // Additionally, Oxalis stores different formats depending on transport type
        // assuming there still is support for the old START), and it leaves
        // the envelope in the file in case of AS2, so if there is a BusinessDocument wrapper
        // we need to get through that as well.
        NodeList candidates = receivedDocument.getDocumentElement().getElementsByTagNameNS(cbc, "ID");
        if (candidates.getLength() > 0) {
            return candidates.item(0).getTextContent();
        } else {
            return null;
        }
    }

    private Element addElement(Document doc, Element base, String ns, String name) {
        return addElement(doc, base, ns, name, null);
    }

    private Element addElement(Document doc, Element base, String ns, String name, String value) {
        Element el = doc.createElementNS(ns, name);
        if (value != null) {
            el.setTextContent(value);
        }
        base.appendChild(el);
        return el;
    }

    private void printDocument(Document doc, OutputStream out) throws ReplySenderException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc), 
                 new StreamResult(new OutputStreamWriter(out, "UTF-8")));
        } catch (java.io.IOException ioe) {
            throw new ReplySenderException(ioe);
        } catch (TransformerException tfe) {
            throw new ReplySenderException(tfe);
        }
    }

    private void storeDocument(Document doc) throws ReplySenderException {
        // store the document alongside the incoming, or in outgoing?
        // hmz. alongside incoming for now
        File file = new File(sentMlrDocumentPath);
        try {
            OutputStream fop = new FileOutputStream(file);
            printDocument(doc, fop);
        } catch (java.io.IOException ioe) {
            throw new ReplySenderException(ioe);
        }
    }

    private Document buildResponseDocument() {
        Document doc = docBuilder.newDocument();

        Element root = doc.createElementNS(ns, "ApplicationResponse");

        addElement(doc, root, cbc, "UBLVersionID", "2.1");
        addElement(doc, root, cbc, "CustomizationID", "urn:www.cenbii.eu:transaction:biitms071:ver2.0:extended:urn:www.peppol.eu:bis:peppol36a:ver1.0:extended:urn:www.simplerinvoicing.org:si:si-ubl:ver1.2.0");
        addElement(doc, root, cbc, "ProfileID", "urn:www.cenbii.eu:profile:bii36:ver2.0");
        addElement(doc, root, cbc, "ID", metadata.getTransmissionId().toString());
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
        ep = addElement(doc, e, cbc, "ResponseCode", responseCode);
        ep.setAttribute("listID", "UNCL4343");
        // TODO: should this be from full xml of busdoc?
        addElement(doc, e, cbc, "ID", findReceivedDocumentID());

        for (ResponseLine responseLine : responseLines) {
            Element lresponse = addElement(doc, e, cac, "LineResponse");
            Element lreference = addElement(doc, lresponse, cac, "LineReference");
            addElement(doc, lreference, cbc, "LineID", responseLine.getLineReference());
            Element response = addElement(doc, lresponse, cac, "Response");
            Element responsecode = addElement(doc, response, cbc, "ResponseCode", responseLine.getResponseCode());
            responsecode.setAttribute("listID", "UNCL4343");
            if (responseLine.hasDescription()) {
                addElement(doc, response, cbc, "Description", responseLine.getDescription());
            }
            if (responseLine.hasStatusReasonCode()) {
                Element status = addElement(doc, response, cac, "Status");
                Element statusReasonCode = addElement(doc, status, cbc, "StatusReasonCode", responseLine.getStatusReasonCode());
                statusReasonCode.setAttribute("listID", "PEPPOLSTATUS");
            }
        }
        return doc;
    }

    public void sendReply() throws ReplySenderException {
        Document doc = buildResponseDocument();
        printDocument(doc, System.out);
        try {
            OxalisOutboundModule oxalisOutboundModule = new OxalisOutboundModule();

            // create a transmission request builder and enable tracing
            TransmissionRequestBuilder requestBuilder = oxalisOutboundModule.getTransmissionRequestBuilder();

            requestBuilder.sender(metadata.getRecipientId());
            if (metadata.getReplyToIdentifier() != null) {
                requestBuilder.receiver(new ParticipantId(metadata.getReplyToIdentifier()));
            } else {
                requestBuilder.receiver(metadata.getSenderId());
            }

            // Supply the payload
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(doc);
            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
            requestBuilder.payLoad(is);

            // Overrides the destination URL if so requested
            if (metadata.getReplyToEndpoint() != null) {
                URL destination = new URL(metadata.getReplyToEndpoint());

                //String accessPointSystemIdentifier = destinationSystemId.value(optionSet);
/*
                if (accessPointSystemIdentifier == null) {
                    throw new IllegalStateException("Must specify AS2 system identifier if using AS2 protocol");
                }
*/
                // Hmm, TODO: how to find out endpoint id?
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

            // if we succeeded, store the document.
            storeDocument(doc);

            // Should we also update metadata?
            metadata.setReplied(true);
            SimpleMessageRepository smr = new SimpleMessageRepository(globalConfiguration);
            smr.saveHeader(metadata, new File(metadataDocumentPath));
        } catch (Exception e) {
            System.out.println("");
            System.out.println("Message failed : " + e.getMessage());
            e.printStackTrace();
            System.out.println("");
        }

    }


}
