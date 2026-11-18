#!/usr/bin/env python3
"""Exercise consecutive-suite fail-closed behavior without an Android SDK."""
import os
from pathlib import Path
import re
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
expected = sum(len(re.findall(r'@Test\b', p.read_text())) for p in (root / 'app/src/androidTest').rglob('*.java'))
for case, count, code in [('success', 2, 0), ('first-failure', 1, 1), ('second-failure', 2, 1), ('timeout', 1, 124)]:
    with tempfile.TemporaryDirectory() as directory:
        base = Path(directory)
        (base / 'bin').mkdir()
        (base / 'scripts').mkdir()
        (base / 'app').symlink_to(root / 'app', target_is_directory=True)
        (base / 'scripts/check-instrumentation.py').symlink_to(root / 'scripts/check-instrumentation.py')
        adb = base / 'bin/adb'
        adb.write_text('''#!/usr/bin/env python3
import os, sys
from pathlib import Path
args = ' '.join(sys.argv[1:])
p = Path('calls.txt')
with p.open('a') as f:
    f.write(args + '\\n')
if args.startswith('shell am instrument'):
    n = sum('shell am instrument' in x for x in p.read_text().splitlines())
    case = os.environ['CASE']
    if case == 'timeout':
        sys.exit(124)
    if case == 'first-failure' or (case == 'second-failure' and n == 2):
        print('FAILURES!!!')
    else:
        print('OK (' + os.environ['EXPECTED'] + ' tests)')
''')
        adb.chmod(0o755)
        env = dict(os.environ, CASE=case, EXPECTED=str(expected), PATH=str(base / 'bin') + ':' + os.environ['PATH'])
        result = subprocess.run(['bash', str(root / 'scripts/run-emulator-tests.sh')], cwd=base, env=env, capture_output=True, text=True)
        calls = (base / 'calls.txt').read_text()
        actual = sum(line.startswith('shell am instrument') for line in calls.splitlines())
        assert result.returncode == code, (case, result.returncode, result.stdout, result.stderr)
        assert actual == count, (case, actual)
        assert 'pull ' in calls and 'logcat ' in calls, (case, calls)
        print(f'{case}: exit {code}, {actual} suite(s), diagnostics collected')
