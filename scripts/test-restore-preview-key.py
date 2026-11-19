#!/usr/bin/env python3
"""Exercise real PKCS12/keytool validation with a temporary TEST key, never the private backup."""
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

script=Path('scripts/restore-preview-key.py').resolve()
with tempfile.TemporaryDirectory() as temp:
    root=Path(temp)
    keytool=Path(os.environ['JAVA_HOME'])/'bin/keytool'
    password='fixture-only-not-a-production-password'
    key=root/'fixture.p12'
    commands=[['-genkeypair','-alias','fixture','-keyalg','RSA','-keysize','2048','-validity','1','-dname','CN=TEST ONLY','-keystore',str(key),'-storetype','PKCS12','-storepass',password,'-noprompt'],
              ['-exportcert','-alias','fixture','-keystore',str(key),'-storepass',password,'-file',str(root/'cert.der')]]
    for args in commands:
        result=subprocess.run([str(keytool),*args],capture_output=True)
        assert result.returncode==0, 'Temporary test fixture could not be created'
    cert=hashlib.sha256((root/'cert.der').read_bytes()).hexdigest()
    bundle={'schema':1,'package':'id.veldafelix.spaces.preview2','alias':'fixture','store_password':password,'key_password':password,'certificate_sha256':cert,'keystore_base64':base64.b64encode(key.read_bytes()).decode()}
    for case in ['success','missing','invalid-json','wrong-certificate','wrong-package','bad-password','invalid-keystore','existing-key']:
        work=root/case
        (work/'config').mkdir(parents=True)
        (work/'config/preview-identity.json').write_text(json.dumps({'package':bundle['package'],'certificate_sha256':cert}))
        changed=dict(bundle)
        if case=='wrong-certificate': changed['certificate_sha256']='a'*64
        if case=='wrong-package': changed['package']='id.veldafelix.spaces.preview3'
        if case=='bad-password': changed['store_password']='incorrect-fixture-password'
        if case=='invalid-keystore': changed['keystore_base64']=base64.b64encode(b'not a keystore'*100).decode()
        raw='' if case=='missing' else '{invalid' if case=='invalid-json' else json.dumps(changed)
        if case=='existing-key':
            directory=work/'spaces-private-signing'
            directory.mkdir()
            (directory/'preview.p12').write_text('do not overwrite')
        env=dict(os.environ, SPACES_PREVIEW_SIGNING_BUNDLE=raw, RUNNER_TEMP=str(work), GITHUB_ENV=str(work/'env'))
        result=subprocess.run(['python3',str(script)],cwd=work,env=env,capture_output=True,text=True)
        assert (result.returncode==0)==(case=='success'), f'Key restore test failed: {case}'
        if case=='success':
            assert ((work/'spaces-private-signing/preview.p12').stat().st_mode & 0o777)==0o600
            assert 'SPACES_PREVIEW_KEYSTORE=' in (work/'env').read_text()
        else:
            assert not (work/'env').exists(), 'Failed restore must not export signing credentials'
            if case=='existing-key': assert (work/'spaces-private-signing/preview.p12').read_text()=='do not overwrite'
            else: assert not (work/'spaces-private-signing/preview.p12').exists()
            for secret in [password,bundle['keystore_base64']]: assert secret not in result.stdout+result.stderr
        print(f'{case}: real test-keystore restore exit {result.returncode}; credentials suppressed')
