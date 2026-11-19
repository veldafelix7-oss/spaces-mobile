#!/usr/bin/env python3
"""Mock adb replacement/data checks. This is not evidence of an actual Android update."""
import os
from pathlib import Path
import subprocess
import tempfile

script = Path('scripts/check-preview-update.sh').resolve()
with tempfile.TemporaryDirectory() as temp:
    root = Path(temp)
    adb = root / 'adb'
    adb.write_text('''#!/usr/bin/env python3
import os, sys
from pathlib import Path
import xml.etree.ElementTree as ET
args=sys.argv[1:]; case=os.environ['CASE']; state=Path(os.environ['STATE'])
with open(os.environ['COMMANDS'],'a') as log: log.write(' '.join(args)+'\\n')
if args[:2]==['install','-r']:
    if case=='certificate': sys.exit(1)
    state.touch()
elif args[:4]==['shell','pm','list','packages']:
    print('package:id.veldafelix.spaces.preview2')
    if state.exists() and case=='package': print('package:id.veldafelix.spaces.preview3')
elif 'tee' in args:
    ET.fromstring(sys.stdin.read())
elif 'cat' in args:
    pref=args[-1].endswith('.xml')
    if case==('pref' if pref else 'file'): print('lost')
    else: print('<map><string name="check">retained-pref</string></map>' if pref else 'retained-file')
''')
    adb.chmod(0o755)
    for case in ['success', 'certificate', 'file', 'pref', 'package']:
        state, commands = root/'state', root/'commands'
        state.unlink(missing_ok=True)
        commands.write_text('')
        env = dict(os.environ, CASE=case, STATE=str(state), COMMANDS=str(commands),
                   PATH=temp+os.pathsep+os.environ['PATH'])
        result = subprocess.run(['bash', str(script)], cwd=root, env=env, capture_output=True, text=True)
        assert (result.returncode == 0) == (case == 'success'), (case, result.stdout, result.stderr)
        calls=commands.read_text()
        assert 'uninstall' not in calls and 'pm clear' not in calls
        assert 'install -r apks/debug/app-debug.apk' in calls
        print(f'{case}: update gate exit {result.returncode}')
