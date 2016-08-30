#!/bin/bash
#
# Author: Steinar O. Cook
#
# Sample program illustrating how a single file may be sent using the stand alone client.
#
# The -t option switches the trace facility on
#
#
TRACE=""
CHANNEL="CH1"

# Location of the executable program
EXECUTABLE="target/oxalis-sendreply.jar"

function usage() {
    cat <<EOT

    usage:

    Sends a PEPOL document to a reciever using the supplied URL.

    $0 [-k password] [-f file] [-d doc.type] [-p profile ] [-c channel] [-m start|as2] [-i as2-identifer] [-r receiver] [-s sender] [-u url|-u 'smp'] [-t]

    -d doc.type optional, overrides the PEPPOL document type as can be found in the payload.

    -f "file"   denotes the xml document to be sent.

    -r receiver optional PEPPOL Participan ID of receiver, default receiver is $RECEIVER (SendRegning)

    -s sender optional PEPPOL Participan ID of sender, default is $SENDER (SendRegning)

    -m method of transmission, either 'start' or 'as2'. Required if you specify a url different from 'smp'

    -i as2 destination system identifier (X.509 common name of receiver when using as2 protocol)

    -u url indicates the URL of the access point. Specifying 'smp' causes the URL of the end point to be looked up
       in the SMP. Default URL is our own local host: $URL

    -t trace option, default is off

    -z enable reply-to header

    -x <identifier> set reply-to identifier

    -c <endpoint URI> set reply-to-endpoint
EOT

}

while getopts f:i: opt
do
    case $opt in
        f)  FILE="$OPTARG"
            ;;
        i)  DOCUMENTIDENTIFIER="$OPTARG"
            ;;
        *)  echo "Sorry, unknown option $opt"
            usage
            exit 4
            ;;
    esac
done

# Verifies that we can read the file holding the XML message to be sent
if [ ! -r "$FILE" ]; then
    echo "Can not read $FILE"
    exit 4;
fi

# Verifies that the .jar file is available to us
if [ ! -r "$EXECUTABLE" ]; then
    echo "Unable to locate the executable .jar file in $EXECUTABLE"
    echo "This script is expected to run from the root of the oxalis-standalone source dir"
    exit 4
fi

cat <<EOT
================================================================================
    Sending...
    File $FILE
    Sender: $SENDER
    Reciever: $RECEIVER
    Destination: $URL
    Method (protocol): $METHOD
================================================================================
EOT

echo "Executing ...."
echo java -jar "$EXECUTABLE" \
    -f "$FILE" \
    -i "$DOCUMENTIDENTIFIER"

# Executes the Oxalis outbound standalone Java program
java -jar "$EXECUTABLE" \
    -f "$FILE" \
    -i "$DOCUMENTIDENTIFIER"

# Other usefull PPIDs:
# ESV = 0088:7314455667781
#  
