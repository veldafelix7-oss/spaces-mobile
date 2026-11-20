#!/usr/bin/env python3
"""Mock only. Real reset + 3->4 replacement on Android must still pass before publication."""
import os
from pathlib import Path
import subprocess
import tempfile

script = Path('scripts/check-preview-update.sh').resolve()
with tempfile.TemporaryDirectory() as temp:
    root = Path(temp)
    adb = root/'adb'
    adb.write_text('''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
import xml.etree.ElementTree as ET
args=sys.argv[1:]; case=os.environ['CASE']; mode=os.environ['INSTALL_MODE']; path=Path(os.environ['STATE'])
state=json.loads(path.read_text()) if path.exists() else {'phase':'old','file':False,'pref':False}
with open(os.environ['COMMANDS'],'a') as log: log.write(' '.join(args)+'\\n')
if args==['shell','getprop','ro.kernel.qemu']:
    print('0' if case=='not-emulator' else '1')
elif args[:4]==['shell','am','instrument','-w']:
    print('FAILURES!!!' if case=='storage' else 'OK (1 test)')
elif args[:2]==['install','-r']:
    probe=args[-1].startswith('probe/')
    if not probe and mode=='reset':
        if case!='unexpected-accept':
            print('Failure [INSTALL_FAILED_TEST_ONLY]' if case=='wrong-error' else 'Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]');sys.exit(1)
    if probe and case=='certificate': print('Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]');sys.exit(1)
    state['phase']='new'
    if probe and case in ['file','pref']: state[case]=False
elif args[:1]==['uninstall']:
    if case=='uninstall-failure': sys.exit(1)
    state={'phase':'absent','file':False,'pref':False}
elif args[:1]==['install']:
    if args[-1]=='apks/debug/app-debug.apk':
        state={'phase':'new','file':case=='dirty-reset','pref':False}
elif args[:4]==['shell','pm','list','packages']:
    print('package:id.veldafelix.spaces.preview2')
    if state['phase']=='new' and case=='package': print('package:id.veldafelix.spaces.preview3')
elif 'tee' in args:
    data=sys.stdin.read();pref=args[-1].endswith('.xml')
    if pref: ET.fromstring(data)
    state['pref' if pref else 'file']=True
elif 'cat' in args:
    pref=args[-1].endswith('.xml')
    if not state['pref' if pref else 'file']: sys.exit(1)
    print('<map><string name="check">retained-pref</string></map>' if pref else 'retained-file')
path.write_text(json.dumps(state))
''')
    adb.chmod(0o755)
    cases = [('update', case) for case in ['success', 'certificate', 'file', 'pref', 'package', 'storage']]
    cases += [('reset', case) for case in ['success', 'not-emulator', 'wrong-error', 'unexpected-accept', 'dirty-reset', 'uninstall-failure', 'certificate', 'file', 'pref', 'package', 'storage']]
    cases += [('unknown', 'bad-mode')]
    for mode, case in cases:
        state, commands = root/'state', root/'commands'
        state.unlink(missing_ok=True)
        commands.write_text('')
        env = dict(os.environ, CASE=case, CI='true', GITHUB_ACTIONS='true', INSTALL_MODE=mode, STATE=str(state), COMMANDS=str(commands),
                   PATH=temp+os.pathsep+os.environ['PATH'])
        result = subprocess.run(['bash', str(script)], cwd=root, env=env, capture_output=True, text=True)
        assert (result.returncode == 0) == (case == 'success'), (mode, case, result.stdout, result.stderr)
        calls=commands.read_text()
        assert 'pm clear' not in calls and ' -d ' not in calls
        if mode != 'reset': assert 'uninstall' not in calls
        if case == 'success': assert 'install -r probe/app-update-probe.apk' in calls
        print(f'{mode}/{case}: installation gate exit {result.returncode}')
