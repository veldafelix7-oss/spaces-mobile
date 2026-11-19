#!/usr/bin/env python3
"""Restore only the pinned, privately backed-up CI key. Never generate a fallback key."""
import base64
import json
import os
from pathlib import Path
import re
import subprocess
import sys


def restore():
    raw = os.environ.get('SPACES_PREVIEW_SIGNING_BUNDLE', '')
    if not raw:
        raise ValueError('Configure the private Actions secret SPACES_PREVIEW_SIGNING_BUNDLE. No ephemeral signing fallback is permitted.')
    if len(raw) > 32000:
        raise ValueError('Signing bundle exceeds expected size')
    try:
        bundle = json.loads(raw)
    except (ValueError, TypeError):
        raise ValueError('Signing bundle is not valid JSON') from None
    policy = json.loads(Path('config/preview-identity.json').read_text())
    if bundle.get('schema') != 1 or bundle.get('package') != policy['package']:
        raise ValueError('Signing bundle identity/schema differs from pinned policy')
    if bundle.get('certificate_sha256') != policy['certificate_sha256']:
        raise ValueError('Signing bundle certificate differs from pinned policy')
    for field in ['alias', 'store_password', 'key_password']:
        value = bundle.get(field, '')
        if not isinstance(value, str) or not re.fullmatch(r'[A-Za-z0-9_-]{12,128}' if field != 'alias' else r'[A-Za-z0-9_-]{1,64}', value):
            raise ValueError('Signing bundle contains an invalid credential field')
    try:
        data = base64.b64decode(bundle.get('keystore_base64', ''), validate=True)
    except (ValueError, TypeError):
        raise ValueError('Invalid encoded keystore') from None
    if not 1000 <= len(data) <= 20000:
        raise ValueError('Unexpected keystore size')
    directory = Path(os.environ['RUNNER_TEMP']) / 'spaces-private-signing'
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    keyfile = directory / 'preview.p12'
    # Restrict before writing. Do not follow or overwrite an existing key/symlink.
    fd = os.open(keyfile, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    try:
        with os.fdopen(fd, 'wb') as output:
            output.write(data)
        env = dict(os.environ, KEY_STORE_PASS=bundle['store_password'])
        tool = Path(os.environ['JAVA_HOME']) / 'bin/keytool'
        result = subprocess.run([str(tool), '-J-Duser.language=en', '-J-Duser.country=US', '-list', '-v',
                                 '-keystore', str(keyfile), '-storetype', 'PKCS12', '-storepass:env',
                                 'KEY_STORE_PASS', '-alias', bundle['alias']], env=env, capture_output=True, text=True)
        # Never include command output in an error: a malformed input must not leak secrets.
        certs = re.findall(r'SHA256:\s*([0-9A-Fa-f:]+)', result.stdout)
        if result.returncode or len(certs) != 1 or certs[0].replace(':', '').lower() != policy['certificate_sha256']:
            raise ValueError('Private keystore could not be verified against pinned certificate')
        if 'PrivateKeyEntry' not in result.stdout:
            raise ValueError('Signing bundle has no private-key entry')
        for field in ['store_password', 'key_password', 'keystore_base64']:
            print('::add-mask::' + bundle[field])
        with open(os.environ['GITHUB_ENV'], 'a') as output:
            output.write(f'SPACES_PREVIEW_KEYSTORE={keyfile}\n')
            output.write(f"SPACES_PREVIEW_STORE_PASSWORD={bundle['store_password']}\n")
            output.write(f"SPACES_PREVIEW_KEY_PASSWORD={bundle['key_password']}\n")
            output.write(f"SPACES_PREVIEW_KEY_ALIAS={bundle['alias']}\n")
        print('::notice title=Signing key restored::Private key matches the pinned certificate; no generated fallback.')
    except Exception:
        keyfile.unlink(missing_ok=True)
        raise


if __name__ == '__main__':
    try:
        restore()
    except Exception as error:
        # Fixed messages only; never dump the input bundle, subprocess output or credentials.
        safe = str(error) if isinstance(error, ValueError) else type(error).__name__
        print('::error title=Preview signing setup::' + safe)
        sys.exit(1)
