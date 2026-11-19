#!/usr/bin/env python3
"""Mock public APK tools: reset approval is exact-version, never an arbitrary-key bypass."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

script = Path('scripts/check-preview-signing.sh').resolve()
with tempfile.TemporaryDirectory() as temp:
    root = Path(temp)
    tools = root / 'sdk/build-tools/35.0.0'
    tools.mkdir(parents=True)
    bin_dir = root / 'bin'
    bin_dir.mkdir()
    mocks = {
        bin_dir / 'gh': '#!/bin/bash\n[ "$CASE" != download ]\n',
        tools / 'apksigner': '''#!/usr/bin/env python3
import os, sys
case=os.environ['CASE']; old='spaces-update-baseline' in sys.argv[-1]
if case=='invalid' and not old: sys.exit(1)
if case=='missing' and not old: sys.exit(0)
prefix='Signer (minSdkVersion=33, maxSdkVersion=2147483647)' if case=='sdk-range' else 'V2 Signer:' if case=='v2' else 'Signer #1'
cert='a'
if not old and case in ['reset','unapproved','wrong-reset-version','pin-drift','future-drift']: cert='b'
if old and case=='baseline-drift': cert='c'
print(prefix+' certificate SHA-256 digest: '+cert*64)
if case=='multiple' and not old: print('Signer #2 certificate SHA-256 digest: '+'c'*64)
''',
        tools / 'aapt': '''#!/usr/bin/env python3
import os, sys
case=os.environ['CASE']; old='spaces-update-baseline' in sys.argv[-1]
package='id.veldafelix.spaces.preview3' if case=='package' and not old else 'id.veldafelix.spaces.preview2'
version=2 if old or case=='version' else 4 if case in ['wrong-reset-version','future-drift','higher-update'] else 3
print(f"package: name='{package}' versionCode='{version}' versionName='test'")
''',
    }
    for path, text in mocks.items():
        path.write_text(text)
        path.chmod(0o755)
    passing = {'success': 'update', 'sdk-range': 'update', 'v2': 'update', 'higher-update': 'update', 'reset': 'reset'}
    cases = list(passing) + ['unapproved', 'wrong-reset-version', 'future-drift', 'pin-drift', 'baseline-drift', 'package', 'version', 'multiple', 'invalid', 'missing', 'download']
    for case in cases:
        reset_case = case in ['reset', 'unapproved', 'wrong-reset-version', 'future-drift']
        policy = {'schema': 1, 'package': 'id.veldafelix.spaces.preview2', 'certificate_sha256': ('b' if reset_case else 'a')*64,
                  'baseline': {'release': 'fixture-release', 'certificate_sha256': 'a'*64, 'version_code': 2},
                  'one_time_reset': {'version_code': 3, 'user_approved_data_loss': case != 'unapproved'}}
        policy_file = root/'policy.json'
        policy_file.write_text(json.dumps(policy))
        output = root/'output'
        output.unlink(missing_ok=True)
        env = dict(os.environ, CASE=case, IDENTITY_POLICY=str(policy_file), GITHUB_OUTPUT=str(output),
                   ANDROID_HOME=str(root/'sdk'), RUNNER_TEMP=temp, GITHUB_REPOSITORY='test/repo',
                   PATH=str(bin_dir)+os.pathsep+os.environ['PATH'])
        result = subprocess.run(['bash', str(script)], cwd=root, env=env, capture_output=True, text=True)
        assert (result.returncode == 0) == (case in passing), (case, result.stdout, result.stderr)
        if case in passing:
            assert output.read_text() == f'mode={passing[case]}\n'
        print(f'{case}: identity gate exit {result.returncode}')
