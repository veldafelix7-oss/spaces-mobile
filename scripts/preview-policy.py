#!/usr/bin/env python3
"""Public certificate/package/version policy, including one explicitly approved reset."""
import json
import os
from pathlib import Path
import re
import sys


def evaluate(policy, old_cert, new_cert, old_badging, new_badging):
    def identity(text):
        match = re.search(r"^package: name='([^']+)' versionCode='([0-9]+)'", text, re.M)
        if not match:
            raise ValueError('APK package/version metadata is missing')
        return match[1], int(match[2])
    baseline = policy['baseline']
    if policy.get('schema') != 1 or policy.get('package') != 'id.veldafelix.spaces.preview2':
        raise ValueError('Unexpected policy schema/package; no third application identity is allowed')
    if any(not re.fullmatch('[a-f0-9]{64}', cert) for cert in [old_cert, new_cert]):
        raise ValueError('Expected one certificate digest per APK')
    if old_cert != baseline['certificate_sha256']:
        raise ValueError('Downloaded baseline certificate differs from the pinned release')
    if new_cert != policy['certificate_sha256']:
        raise ValueError('Candidate is not signed with the privately backed-up pinned key')
    old_package, old_code = identity(old_badging)
    new_package, new_code = identity(new_badging)
    if old_package != policy['package'] or new_package != policy['package']:
        raise ValueError('Application ID changed; no third app is allowed')
    if old_code != baseline['version_code'] or new_code <= old_code:
        raise ValueError('Wrong baseline version or candidate versionCode did not increase')
    if new_cert == old_cert:
        return 'update'
    reset = policy.get('one_time_reset', {})
    if reset.get('user_approved_data_loss') is True and reset.get('version_code') == new_code:
        return 'reset'
    raise ValueError('Different certificate without this exact version\'s one-time reset approval')


if __name__ == '__main__':
    try:
        policy = json.loads(Path(sys.argv[1]).read_text())
        if len(sys.argv) == 2:
            print(policy['baseline']['release'])
        else:
            mode = evaluate(policy, sys.argv[2], sys.argv[3], Path(sys.argv[4]).read_text(), Path(sys.argv[5]).read_text())
            print('::notice title=Preview installation mode::' + ('RESET REQUIRED: approved one-time data loss; NOT an update of old 0.1.1.' if mode == 'reset' else 'Same-key in-place update.'))
            if os.environ.get('GITHUB_OUTPUT'):
                with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
                    output.write(f'mode={mode}\n')
    except (ValueError, KeyError, OSError, IndexError) as error:
        print('::error title=Preview identity blocked::' + str(error))
        sys.exit(1)
