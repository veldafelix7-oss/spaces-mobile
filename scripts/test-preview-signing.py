#!/usr/bin/env python3
"""Mock only: prove publication is denied on certificate, package, or version drift."""
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
print(prefix+' certificate SHA-256 digest: '+('b' if case=='certificate' and not old else 'a')*64)
''',
        tools / 'aapt': '''#!/usr/bin/env python3
import os, sys
case=os.environ['CASE']; old='spaces-update-baseline' in sys.argv[-1]
package='id.veldafelix.spaces.preview3' if case=='package' and not old else 'id.veldafelix.spaces.preview2'
version=2 if old or case=='version' else 3
print(f"package: name='{package}' versionCode='{version}' versionName='test'")
''',
    }
    for path, text in mocks.items():
        path.write_text(text)
        path.chmod(0o755)
    for case in ['success', 'sdk-range', 'v2', 'certificate', 'package', 'version', 'invalid', 'missing', 'download']:
        env = dict(os.environ, CASE=case, ANDROID_HOME=str(root/'sdk'), RUNNER_TEMP=temp,
                   GITHUB_REPOSITORY='test/repo', PATH=str(bin_dir)+os.pathsep+os.environ['PATH'])
        result = subprocess.run(['bash', str(script)], cwd=root, env=env, capture_output=True, text=True)
        assert (result.returncode == 0) == (case in ['success', 'sdk-range', 'v2']), (case, result.stdout, result.stderr)
        print(f'{case}: signing gate exit {result.returncode}')
