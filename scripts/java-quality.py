#!/usr/bin/env python3
"""Run pinned Java tools on staged copies of explicitly selected files."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('mode', choices=['check', 'format', 'verify', 'format-check'])
parser.add_argument('files', nargs='*', type=Path, help='Java files or directories to scan recursively')
parser.add_argument('--changed', action='store_true', help='Select staged, unstaged and untracked Java files')
parser.add_argument('--base', help='Also include Java changes relative to this Git ref')
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
if args.changed or args.base:
    if args.files:
        parser.error('Use file paths or Git selection, not both.')
    def git(*arguments):
        result = subprocess.run(['git', *arguments], capture_output=True)
        if result.returncode:
            parser.error(result.stderr.decode(errors='replace'))
        return result.stdout
    repo = Path(os.fsdecode(git('rev-parse', '--show-toplevel')).strip())
    paths = set()
    for arguments in [('diff', '--name-only', '--diff-filter=ACMR', '-z'),
                      ('diff', '--cached', '--name-only', '--diff-filter=ACMR', '-z'),
                      ('ls-files', '--others', '--exclude-standard', '-z')]:
        paths.update(git(*arguments).split(b'\0'))
    if args.base:
        paths.update(git('diff', '--name-only', '--diff-filter=ACMR', '-z', args.base, '--').split(b'\0'))
    args.files = [repo / os.fsdecode(x) for x in sorted(paths)
                  if x.endswith(b'.java') and (repo / os.fsdecode(x)).is_file()]
    if not args.files:
        print('No changed Java files.')
        raise SystemExit(0)
if not args.files:
    parser.error('Supply files/directories, --changed or --base REF.')
ignored = {'target', 'build', 'node_modules', 'dist', '.git', '.tool-cache'}
files = []
for supplied in args.files:
    path = supplied.resolve()
    if path.is_dir():
        for directory, subdirs, names in os.walk(path):
            subdirs[:] = sorted(x for x in subdirs if x not in ignored and not (Path(directory) / x).is_symlink())
            files.extend(Path(directory) / name for name in sorted(names)
                         if name.endswith('.java') and not (Path(directory) / name).is_symlink())
    elif path.is_file() and path.suffix == '.java':
        files.append(path)
    else:
        parser.error('Expected an existing Java file or directory: ' + str(path))
files = list(dict.fromkeys(files))
if not files:
    parser.error('No Java files found.')
if not shutil.which('mvn') or not shutil.which('java'):
    parser.error('Maven 3.6.3+ and a JDK compatible with the project and quality tools are required.')
maven_version = subprocess.run(['mvn', '-version'], capture_output=True, text=True)
if maven_version.returncode:
    parser.error('Cannot determine the Maven runtime; check JAVA_HOME and PATH.')
print('Selected {} Java file(s).'.format(len(files)), flush=True)
# Staging avoids accidental project-wide formatting and preserves original files on failure.
with tempfile.TemporaryDirectory(prefix='java-quality-') as directory:
    stage = Path(directory)
    shutil.copy2(root / 'tools/pom.xml', stage / 'pom.xml')
    inputs = stage / 'input'
    inputs.mkdir()
    originals = {}
    for index, path in enumerate(files):
        # Separate folders allow files with identical names in different packages.
        target = inputs / str(index) / path.name
        target.parent.mkdir()
        shutil.copy2(path, target)
        originals[target] = path.read_bytes()
    command = ['mvn', '-B', '-f', str(stage / 'pom.xml'),
               '-Dmaven.repo.local=' + str(root / '.tool-cache/m2'),
               '-Dquality.input=' + str(inputs), '-Dquality.config=' + str(root / 'config')]
    formatter = 'com.diffplug.spotless:spotless-maven-plugin:2.30.0:apply'
    checker = 'org.apache.maven.plugins:maven-checkstyle-plugin:3.3.1:check'
    goals = {'check': [checker], 'format': [formatter],
             'verify': [formatter, checker],
             'format-check': [formatter.replace(':apply', ':check')]}[args.mode]
    result = subprocess.run(command + goals)
    if result.returncode:
        raise SystemExit(result.returncode)
    if args.mode in ('format', 'verify'):
        for target, path in zip(originals, files):
            if path.read_bytes() != originals[target]:
                raise SystemExit('File changed while tools were running; no output copied: ' + str(path))
        for target, path in zip(originals, files):
            if target.read_bytes() != originals[target]:
                path.write_bytes(target.read_bytes())
