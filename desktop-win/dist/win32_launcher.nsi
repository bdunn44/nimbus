Name "Nimbus Sync"
Caption "Nimbus Sync - Caption"
Icon "images/cloudsync.ico"
OutFile "Nimbus Sync.exe"
 
SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

!include "JavaCheck.nsh"
 
Section ""
  System::Call 'kernel32::CreateMutex(i 0, i 0, t "{F87D4E7A-8935-46DF-B48F-FF5C463562C4}") i .r1 ?e'
  Pop $R0
  StrCmp $R0 0 +3
  MessageBox MB_OK|MB_ICONEXCLAMATION "The installer is already running."
  Abort
  
  Call GetJRE
  Pop $R0
  
  StrCpy $0 '"$R0" -jar -Dcom.kbdunn.nimbus.sync.installdir="$EXEDIR" -jar "$EXEDIR\lib\nimbus-desktop-win-${VERSION}.jar"'
 
  SetOutPath $EXEDIR
  ExecWait $0
SectionEnd