#!/bin/bash
#
# Author: Jelte Jansen
#
TRACE=""
CHANNEL="CH1"
URL=""


# Location of the executable program
EXECUTABLE="target/oxalis-sendreply.jar"

function usage() {
    cat <<EOT

    usage:

    Sends a PEPPOL reply to a receiver using the supplied URL.

    $0
    
    -f "file"   denotes the xml document to be sent.

    -t trace option, default is off
EOT

}

while getopts f:i:u: opt
do
    case $opt in
        f)  FILE="$OPTARG"
            ;;
        i)  DOCUMENTIDENTIFIER="$OPTARG"
            ;;
        u)  URL="-u $OPTARG"
            ;;
        *)  echo "Sorry, unknown option $opt"
            usage
            exit 4
            ;;
    esac
done

if [ -z "$FILE" ]
  then
    echo "No file to send supplied (-f)";
    usage;
    exit 3;
fi

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
    -i "$DOCUMENTIDENTIFIER" \
    $URL

# Executes the Oxalis outbound standalone Java program

#java -jar "$EXECUTABLE" \
#    -f "$FILE" \
#    -i "$DOCUMENTIDENTIFIER"\
#    $URL

# Other usefull PPIDs:
# ESV = 0088:7314455667781
#  
