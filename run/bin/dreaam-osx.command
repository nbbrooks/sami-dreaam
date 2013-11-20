#!/bin/bash

JAVA="java"
JARFLAGS="-d32 -Djava.library.path=../../lib/crw/worldwind/native -Djava.util.logging.config.file=../../logging.properties"
CLASSPATH="../../build/classes:../../lib/swing-layout-1.0.4.jar:../../lib/jung/collections-generic-4.01.jar:../../lib/jung/jung-algorithms-2.0.jar:../../lib/jung/jung-api-2.0.jar:../../lib/jung/jung-graph-impl-2.0.jar:../../lib/jung/jung-visualization-2.0.jar:../../lib/crw/worldwind/gdal.jar:../../lib/crw/worldwind/gluegen-rt.jar:../../lib/crw/worldwind/jogl.jar:../../lib/crw/worldwind/worldwind.jar:../../lib/crw/worldwind/worldwindx.jar:../../lib/sami/sami-core.jar:../../lib/crw/sami-crw.jar"
LOGPATH="../logs"
MAINCLASS="dreaam.developer.DREAAM"

cd "$(dirname "$0")"
$JAVA $JARFLAGS -cp $CLASSPATH $MAINCLASS > $LOGPATH/dreaam.log 2>&1

osascript -e 'tell application "Terminal" to quit' &
exit

