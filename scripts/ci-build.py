#!/usr/bin/env python3
"""Preserve Gradle's exit code and expose a bounded diagnostic in GitHub Checks."""
import subprocess, sys, time
from pathlib import Path
for attempt in range(3):
    p=subprocess.run(['./gradlew','--no-daemon',*sys.argv[1:]],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    if p.returncode==0 or '429' not in p.stdout or attempt==2:
        break
    print(f'::notice title=Dependency download::Maven Central rate limit; bounded retry {attempt+1}/2.',flush=True)
    time.sleep(15*(attempt+1))
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
