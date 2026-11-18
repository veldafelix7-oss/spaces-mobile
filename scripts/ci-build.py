#!/usr/bin/env python3
"""Preserve Gradle's exit code and expose a bounded diagnostic in GitHub Checks."""
import subprocess, sys
from pathlib import Path
p=subprocess.run(['./gradlew','--no-daemon',*sys.argv[1:]],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
Path('build.log').write_text(p.stdout)
print(p.stdout,flush=True)
import xml.etree.ElementTree as ET
reports=list(Path('app/build/test-results').glob('**/TEST-*.xml'))
if reports:
    counts={k:0 for k in ['tests','failures','errors','skipped']}
    for report in reports:
        root=ET.parse(report).getroot()
        for key in counts: counts[key]+=int(root.get(key,0))
    print('::notice title=JVM test results::'+', '.join(f'{v} {k}' for k,v in counts.items()),flush=True)
if p.returncode:
    tail='\n'.join(p.stdout.splitlines()[-100:])
    escaped=tail.replace('%','%25').replace('\r','%0D').replace('\n','%0A')
    print('::error title=Gradle diagnostics::'+escaped,flush=True)
sys.exit(p.returncode)
