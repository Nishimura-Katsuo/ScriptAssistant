@echo off
del *.class
del *.jar
javac ScriptAssistant.java
jar cfe ScriptAssistant.jar ScriptAssistant *.class
pause
