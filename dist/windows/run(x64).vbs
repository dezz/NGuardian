Option explicit

Dim shell

Set shell = CreateObject("WScript.Shell")
Call shell.Run("bin\start_x64.bat", 0, False)
