package nl.sidnlabs.oxalis;

import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * @author Jelte Jansen
 */
public class Main {

    private static OptionSpec<String> responseLines;
    private static OptionSpec<String> responseCode;

    private static OptionSpec<String> userId;
    private static OptionSpec<String> recipientId;
    private static OptionSpec<String> transmissionId;
    
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

        String response = responseCode.value(optionSet);
        String user = userId.value(optionSet);
        String recipient = recipientId.value(optionSet);
        String transmission = transmissionId.value(optionSet);
        
        try {
            ReplySender replySender = new ReplySender(response, user, recipient, transmission);

            List<String> lines = responseLines.values(optionSet);
            //if (lines.size() > 0) {
            for (String line : lines) {
                replySender.addResponseLine(line);
            }
            
            
            replySender.sendReply();
        } catch (ReplySenderException rse) {
            System.out.println("Error: " + rse.toString());
            System.exit(1);
        }
    }


    private static void printErrorMessage(String message) {
        System.out.println("");
        System.out.println("*** " + message);
        System.out.println("");
    }

    static OptionParser getOptionParser() {
        OptionParser optionParser = new OptionParser();
        responseCode = optionParser.accepts("c", "Response code; AP for accepted, RE for rejected").withRequiredArg().ofType(String.class).required();
        transmissionId = optionParser.accepts("t", "Transmission ID of the document to reply to").withRequiredArg().ofType(String.class).required();
        userId = optionParser.accepts("u", "User ID").withRequiredArg().ofType(String.class).required();
        recipientId = optionParser.accepts("r", "Recipient ID; the party to send the response to (i.e. the sender of the business document)").withRequiredArg().ofType(String.class).required();
        responseLines = optionParser.accepts("a", "Additional response line (comma-separated <section>,<rcode>,[description],[statuscode])").withRequiredArg().ofType(String.class);
        return optionParser;
    }

}
