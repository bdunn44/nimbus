!define PRODUCT_NAME "Nimbus Sync"

;These are for RefreshShellIcons
!define SHCNE_ASSOCCHANGED 0x08000000
!define SHCNF_IDLIST 0

;Folders with a destkop.ini file must be made system folders
!define PathMakeSystemFolder "!insertmacro PATH_MAKE_SYSTEM_FOLDER"
!macro PATH_MAKE_SYSTEM_FOLDER pszPath
  System::Call    "shlwapi::PathMakeSystemFolder(t '${pszPath}') i."
!macroend

;--------------------------------
;Includes

  !include "MUI2.nsh"
  !include "UAC.nsh"
  !include "JavaCheck.nsh"
  !include "StrRep.nsh"
  !include "LnkX64IconFix.nsh"

;--------------------------------
;Variables

  Var StartMenuFolder

;--------------------------------
;General

  ;Name and file
  Name "${PRODUCT_NAME}"
  OutFile "install-nimbus-sync-${VERSION}.exe"
  ShowInstDetails "show"
  ShowUninstDetails "show"

  ;Default installation folder
  InstallDir "$PROGRAMFILES32\${PRODUCT_NAME}"

  ;Request application privileges for Windows
  RequestExecutionLevel highest

;--------------------------------
;Interface Settings

  !define MUI_ABORTWARNING
  !define MUI_ICON "images\cloudsync.ico"
  !define MUI_UNICON "images\cloudsync-grey.ico"

;--------------------------------
;Pages

  !insertmacro MUI_PAGE_LICENSE "eula.txt"
  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_DIRECTORY
  
  ;Start Menu Folder Page Configuration
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${PRODUCT_NAME}" 
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Start Menu Folder"
  
  !insertmacro MUI_PAGE_STARTMENU "Application" $StartMenuFolder
  !insertmacro MUI_PAGE_INSTFILES
  
    ;These indented statements modify settings for MUI_PAGE_FINISH
	!define MUI_FINISHPAGE_NOAUTOCLOSE
    !define MUI_FINISHPAGE_RUN
    !define MUI_FINISHPAGE_RUN_NOTCHECKED
    !define MUI_FINISHPAGE_RUN_TEXT "Launch Nimbus Sync"
    !define MUI_FINISHPAGE_RUN_FUNCTION "Launch"
  !insertmacro MUI_PAGE_FINISH
  
  !insertmacro MUI_UNPAGE_WELCOME
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES
  !insertmacro MUI_UNPAGE_FINISH

;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Installer Section

Section "Install" SecInstall
  ;Check again that it's not installed
  ReadRegStr $R0 HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "UninstallString"
  StrCmp $R0 "" doinstall
  DetailPrint "A previous installation of ${PRODUCT_NAME} exists and was not uninstalled. Aborting installation."
  Abort
  
  doinstall:
    ;Require JRE
	DetailPrint "Checking for Java ${JRE_VERSION}..."
    Call GetJRE
    Pop $R0
    
    ;Write files
    DetailPrint "Extracting Files..."
	SetOutPath "$INSTDIR"
	File "Nimbus Sync.exe" ;Launcher
    SetOutPath "$INSTDIR\images"
	File "images\*"
    SetOutPath "$INSTDIR\lib"
    File "..\build\libs\nimbus-desktop-win-${VERSION}.jar"
	File "..\build\dependency-cache\*.jar"
    SetOutPath "$INSTDIR\conf"
	File "conf\log4j.properties"
	FileOpen $9 "nimbus-sync.properties" w
	${StrRep} $0 "com.kbdunn.nimbus.sync.syncdir=$PROFILE\${PRODUCT_NAME}" "\" "\\"
    FileWrite $9 $0
    FileClose $9
	
    SetOutPath "$INSTDIR\data"
	File "data\sync-state.dat"
    SetOutPath "$INSTDIR\logs"
	File "logs\nimbus-sync.log"
    SetOutPath "$INSTDIR"
	
    ;Create sync dir, add to favorite links
	DetailPrint "Create Nimbus Sync folder..."
    CreateDirectory "$PROFILE\${PRODUCT_NAME}"
    WriteINIStr "$PROFILE\${PRODUCT_NAME}\Desktop.ini" ".ShellClassInfo" "IconFile" "$INSTDIR\images\cloudsync-folder.ico"
    WriteINIStr "$PROFILE\${PRODUCT_NAME}\Desktop.ini" ".ShellClassInfo" "IconIndex" "0"
    WriteINIStr "$PROFILE\${PRODUCT_NAME}\Desktop.ini" ".ShellClassInfo" "InfoTip" "${PRODUCT_NAME} Folder"
    ${PathMakeSystemFolder} "$PROFILE\${PRODUCT_NAME}" 
    CreateShortCut "$PROFILE\Links\${PRODUCT_NAME}.lnk" "$PROFILE\${PRODUCT_NAME}" "" "$INSTDIR\images\cloudsync.ico"
	${lnkX64IconFix} "$PROFILE\Links\${PRODUCT_NAME}.lnk"
    Call RefreshShellIcons
    
	;Create Start Menu shortcuts
    SetOutPath "$INSTDIR\bin"
	DetailPrint "Creating Start Menu shortcuts..."
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
    CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Nimbus Sync.lnk" "$INSTDIR\Nimbus Sync.exe" "" "$INSTDIR\images\cloudsync.ico"
	${lnkX64IconFix} "$SMPROGRAMS\$StartMenuFolder\Nimbus Sync.lnk"
    CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\uninstall.exe"
	${lnkX64IconFix} "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
    !insertmacro MUI_STARTMENU_WRITE_END
    
    ;Add to startup programs
	WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "NimbusSync" "$INSTDIR\Nimbus Sync.exe"
    
    ; Create/Configure Uninstaller
	DetailPrint "Creating uninstaller..."
    WriteUninstaller "$INSTDIR\Uninstall.exe"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayName" "${PRODUCT_NAME}"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "InstallLocation" "$\"$INSTDIR$\""
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "UninstallString" "$\"$INSTDIR\Uninstall.exe$\""
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayIcon" "$INSTDIR\images\cloudsync.ico"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "Publisher" "Bryson Dunn"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayVersion" "${VERSION}"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "EstimatedSize" "22600"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "NoModify" "1"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "NoRepair" "1"
SectionEnd

;--------------------------------
;Uninstaller Section

Section "un.Install"
  ;Delete registry entries
  DetailPrint "Cleaning up registry..."
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
  DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "NimbusSync"
  DeleteRegKey /ifempty HKCU "Software\${PRODUCT_NAME}"
  
  ;Delete install dir files
  DetailPrint "Deleting installation files..."
  RMDir /r "$INSTDIR\*"
  
  ;Remove Start Menu shortcuts
  DetailPrint "Removing Start Menu shortcuts..."
  !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder
  RMDir /r "$SMPROGRAMS\$StartMenuFolder"
SectionEnd

;--------------------------------
;Functions

Function .onInit
  System::Call 'kernel32::CreateMutex(i 0, i 0, t "{1540B42B-C534-4E01-A073-01713C29D7B4}") i .r1 ?e'
  Pop $R0
  StrCmp $R0 0 +3
  MessageBox MB_OK|MB_ICONEXCLAMATION "The Nimbus Sync installer already running."
  Abort
  
  ;Check for previous version, uninstall if present
  ReadRegStr $R0 HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "UninstallString"
  StrCmp $R0 "" done
  
  MessageBox MB_OKCANCEL|MB_USERICON \
      "${PRODUCT_NAME} is already installed. $\n$\nClick 'OK' to remove the \
      previous version or 'Cancel' to cancel this upgrade." \
      IDOK uninst
  Abort
  
  ;Run the uninstaller
  uninst:
    ClearErrors
    Exec $INSTDIR\uninstall.exe 
	
  done:
FunctionEnd

Function un.onInit
  System::Call 'kernel32::CreateMutex(i 0, i 0, t "{7944E4AE-DA7F-4877-94CE-EA8F0929F1FF}") i .r1 ?e'
  Pop $R0
  StrCmp $R0 0 +3
  MessageBox MB_OK|MB_ICONEXCLAMATION "The Nimbus Sync uninstaller already running."
  Abort

  check_app_running:
    System::Call 'kernel32::OpenMutex(i 0x100000, b 0, t "{F87D4E7A-8935-46DF-B48F-FF5C463562C4}") i .R0'
    IntCmp $R0 0 done
	System::Call 'kernel32::CloseHandle(i $R0)'
    MessageBox MB_RETRYCANCEL|MB_USERICON "The Nimbus Sync application is currently running. Please shutdown the application before continuing." IDRETRY check_app_running
    Abort
	
  done:
FunctionEnd

Function RefreshShellIcons
  ; By jerome tremblay - april 2003
  System::Call 'shell32.dll::SHChangeNotify(i, i, i, i) v \
  (${SHCNE_ASSOCCHANGED}, ${SHCNF_IDLIST}, 0, 0)'
FunctionEnd

Function Launch
  ExecShell "" "$INSTDIR\Nimbus Sync.exe"
FunctionEnd