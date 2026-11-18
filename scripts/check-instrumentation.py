#!/usr/bin/env python3
from pathlib import Path
import re
import sys
output=Path('emulator-results/instrumentation.txt').read_text()
expected=sum(len(re.findall(r'@Test\b',p.read_text())) for p in Path('app/src/androidTest').rglob('*.java'))
if expected>0 and re.search(r'OK \('+str(expected)+r' tests\)',output) and 'FAILURES!!!' not in output:
    print(f'::notice title=Android emulator::{expected} instrumentation tests passed on Android 13.')
else:
    # GitHub truncates long annotations. Prefer JUnit's final failure summary, not raw status noise.
    marker='INSTRUMENTATION_RESULT: stream='
    summary=output.rsplit(marker,1)[-1] if marker in output else output[-4500:]
    steps='\n'.join(re.findall(r'INSTRUMENTATION_STATUS: spaces_step=(.*)',output)[-5:])
    diagnostic=steps+'\n'+summary[-6500:]
    escaped=diagnostic.replace('%','%25').replace('\r','%0D').replace('\n','%0A')
    print('::error title=Android instrumentation::'+escaped)
    sys.exit(1)
