#!/usr/bin/env python3
from pathlib import Path
import re
import sys
output=Path('emulator-results/instrumentation.txt').read_text()
if re.search(r'OK \(12 tests\)',output) and 'FAILURES!!!' not in output:
    print('::notice title=Android emulator::12 instrumentation tests passed on Android 13.')
else:
    escaped=output[-16000:].replace('%','%25').replace('\r','%0D').replace('\n','%0A')
    print('::error title=Android instrumentation::'+escaped)
    sys.exit(1)
