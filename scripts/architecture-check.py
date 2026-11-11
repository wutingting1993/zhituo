#!/usr/bin/env python3
"""Check all supplied compiled class directories, or discover target/classes under project roots."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import shutil

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('paths', nargs='+', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
classes = []
for item in args.paths:
    if not item.is_dir():
        parser.error('Missing directory: ' + str(item))
    candidates = [item] if item.name == 'classes' else item.glob('**/target/classes')
    classes.extend(p.resolve() for p in candidates if any(p.rglob('*.class')))
if not classes:
    parser.error('No compiled classes; run the project clean compile first.')
with tempfile.TemporaryDirectory(prefix='architecture-check-') as directory:
    stage = Path(directory)
    shutil.copytree(root / 'tools/architecture', stage / 'tool', ignore=shutil.ignore_patterns('target'))
    result = subprocess.run(['mvn', '-B', '-f', str(stage / 'tool/pom.xml'),
                             '-Dmaven.repo.local=' + str(root / '.tool-cache/m2'),
                             '-Dquality.classes=' + os.pathsep.join(map(str, dict.fromkeys(classes))), 'test'])
    raise SystemExit(result.returncode)
