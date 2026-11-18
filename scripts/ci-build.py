#!/usr/bin/env python3
"""Preserve Gradle's exit code and expose a bounded diagnostic in GitHub Checks."""
import subprocess, sys
from pathlib import Path
p=subprocess.run(['./gradlew','--no-daemon',*sys.argv[1:]],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
Path('build.log').write_text(p.stdout)
print(p.stdout,flush=True)
if p.returncode:
    tail='\n'.join(p.stdout.splitlines()[-100:])
    escaped=tail.replace('%','%25').replace('\r','%0D').replace('\n','%0A')
    print('::error title=Gradle diagnostics::'+escaped,flush=True)
sys.exit(p.returncode)
