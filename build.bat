@echo off
del *.class > nul 2> nul
del *.jar > nul 2> nul
javac ScriptAssistant.java -Xlint:unchecked
jar cfe ScriptAssistant.jar ScriptAssistant *.class *.dat
pause
