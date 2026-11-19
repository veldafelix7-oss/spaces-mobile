#!/usr/bin/env python3
"""Prove the CI-only probe cannot replace the main deliverable, including on failure."""
import os
from pathlib import Path
import subprocess
import tempfile

script = Path('scripts/build-update-probe.sh').resolve()
with tempfile.TemporaryDirectory() as temp:
    root = Path(temp)
    main = root/'app/build/outputs/apk/debug/app-debug.apk'
    main.parent.mkdir(parents=True)
    tools=root/'sdk/build-tools/35.0.0'
    tools.mkdir(parents=True)
    aapt=tools/'aapt'
    aapt.write_text('''#!/usr/bin/env python3
from pathlib import Path
import sys
print("package: name='id.veldafelix.spaces.preview2' versionCode='"+Path(sys.argv[-1]).read_text()+"'")
''')
    aapt.chmod(0o755)
    (root/'scripts').mkdir()
    (root/'scripts/ci-build.py').write_text('''import os, sys
from pathlib import Path
assert sys.argv[1:]==['-PspacesUpdateProbe=true','assembleDebug']
case=os.environ['CASE']
Path('app/build/outputs/apk/debug/app-debug.apk').write_text('3' if case=='same-version' else '5' if case=='wrong-version' else '4')
sys.exit(1 if case=='failed-build' else 0)
''')
    for case in ['success', 'failed-build', 'same-version', 'wrong-version']:
        main.write_text('3')
        env=dict(os.environ, CASE=case, RUNNER_TEMP=temp, ANDROID_HOME=str(root/'sdk'))
        result=subprocess.run(['bash',str(script)],cwd=root,env=env,capture_output=True,text=True)
        assert (result.returncode==0)==(case=='success'), (case,result.stdout,result.stderr)
        assert main.read_text()=='3', 'Main publication artifact was not restored'
        print(f'{case}: probe gate exit {result.returncode}; main artifact preserved')
