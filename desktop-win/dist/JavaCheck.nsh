!define JRE_VERSION "8"
!define JAVAEXE "javaw.exe"

!include "FileFunc.nsh"
!insertmacro GetFileVersion
!insertmacro GetParameters
!include "WordFunc.nsh"
!insertmacro VersionCompare

;  returns the full path of a valid java.exe
;  looks in:
;  1 - .\jre directory (JRE Installed with application)
;  2 - JAVA_HOME environment variable
;  3 - the registry
;  4 - hopes it is in current dir or PATH
Function GetJRE
    Push $R0
    Push $R1
    Push $2
 
  ; 1) Check JRE packaged with app (not done currently)
  ;CheckLocal:
  ;  ClearErrors
  ;  StrCpy $R0 "$INSTDIR\jre\bin\${JAVAEXE}"
  ;  MessageBox MB_OK "Checking local : $INSTDIR\jre\bin\${JAVAEXE}"
  ;  IfFileExists $R0 JreFound
 
  ; 2) Check for JAVA_HOME
  ;CheckJavaHome:
  ClearErrors
  ReadEnvStr $R0 "JAVA_HOME"
  StrCpy $R0 "$R0\bin\${JAVAEXE}"
  ;MessageBox MB_OK "Checking JAVA_HOME : $R0"
  IfErrors CheckRegistry     
  IfFileExists $R0 0 CheckRegistry
  Call CheckJreVersion
  IfErrors CheckRegistry JreFound
 
  ; 3) Check for registry
  CheckRegistry:
    ClearErrors
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    ;MessageBox MB_OK "Checking Registry : $R0\bin\${JAVAEXE}"
    IfErrors NoJre
    IfFileExists $R0 0 NoJre
    Call CheckJreVersion
    IfErrors NoJre JreFound
 
  ; 4) No JRE found. Abort
  NoJre:
    StrCpy $R0 "${JAVAEXE}"
	DetailPrint "A compatible Java installation was not found. Exiting."
    MessageBox MB_ICONSTOP "Could not find Java Runtime Environment 1.8+. Please install it and try again."
    Abort
 
  JreFound:
	;MessageBox MB_OK "JRE Found!"
    Pop $2
    Pop $R1
    Exch $R0
FunctionEnd

; Pass the java exe path using $R0
Function CheckJreVersion
    Push $R1
 
    ; Get the file version of javaw.exe
    ${GetFileVersion} $R0 $R1
	;MessageBox MB_OK "Detected Java Version for $R0 is $R1"
    ${VersionCompare} ${JRE_VERSION} $R1 $R1
 
    ; Check whether $R1 != "1"
    ClearErrors
    StrCmp $R1 "1" 0 CheckDone
    SetErrors
	DetailPrint "Java version detected at $R0 is incompatible."
 
  CheckDone:
    DetailPrint "Compatible Java version ($R0) detected."
    Pop $R1
FunctionEnd