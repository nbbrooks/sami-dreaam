set JAVA=java
set JARFLAGS=-Djava.library.path="..\..\lib\crw\worldwind\native64" -Djava.util.logging.config.file="..\..\logging.properties"
set CLASSPATH="..\..\build\classes;..\..\lib\swing-layout-1.0.4.jar;..\..\lib\jung\collections-generic-4.01.jar;..\..\lib\jung\jung-algorithms-2.0.jar;..\..\lib\jung\jung-api-2.0.jar;..\..\lib\jung\jung-graph-impl-2.0.jar;..\..\lib\jung\jung-visualization-2.0.jar;..\..\lib\crw\worldwind\gdal.jar;..\..\lib\crw\worldwind\gluegen-rt.jar;..\..\lib\crw\worldwind\jogl.jar;..\..\lib\crw\worldwind\worldwind.jar;..\..\lib\crw\worldwind\worldwindx.jar;..\..\lib\sami\sami-core.jar;..\..\lib\crw\sami-crw.jar"
set LOGPATH="..\logs"
set MAINCLASS="dreaam.developer.DREAAM"

start \b %JAVA% %JARFLAGS% -cp %CLASSPATH% %MAINCLASS% > %LOGPATH%\dreaam.log 2>&1


