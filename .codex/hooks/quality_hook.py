#!/usr/bin/env python3
"""Codex quality hooks. No shell evaluation of hook payloads."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
FAILURE = 'Quality gate failed. Fix the reported violations and rerun; do not claim completion.'


def stop_result(passed, continued):
    if passed:
        return {}
    if continued:
        return {'continue': False, 'stopReason': FAILURE + ' Task remains incomplete; see .tool-cache/hooks/stop.log.'}
    return {'decision': 'block', 'reason': FAILURE + ' See .tool-cache/hooks/stop.log.'}


def git(*arguments):
    result = subprocess.run(['git', '-C', str(ROOT), *arguments], capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    return result.stdout


def changed_paths():
    paths = set()
    for arguments in [('diff', '--name-only', '-z'), ('diff', '--cached', '--name-only', '-z'),
                      ('ls-files', '--others', '--exclude-standard', '-z')]:
        paths.update(os.fsdecode(x) for x in git(*arguments).split(b'\0') if x)
    ignored = {".tool-cache", "target", "build", "node_modules", "dist", ".git"}
    return sorted(path for path in paths if not (set(Path(path).parts) & ignored))


def relevant(path):
    item = Path(path)
    return item.suffix in {'.java', '.xml', '.gradle', '.kts', '.properties', '.yaml', '.yml'} or path.startswith(
        ('scripts/', 'config/', 'tools/', '.codex/'))


def snapshot(paths):
    digest = hashlib.sha256()
    head = subprocess.run(['git', '-C', str(ROOT), 'rev-parse', 'HEAD'], capture_output=True)
    digest.update(head.stdout)
    digest.update(os.environ.get("JAVA_HOME", "").encode())
    digest.update(os.environ.get("PATH", "").encode())
    for name in paths:
        if not relevant(name):
            continue
        path = ROOT / name
        digest.update(name.encode())
        if path.is_file():
            digest.update(path.read_bytes())
        else:
            digest.update(b'<deleted>')
    return digest.hexdigest()


def configured():
    config = json.loads((ROOT / '.codex/quality.json').read_text())
    project = (ROOT / config['project_dir']).resolve()
    if project != ROOT and ROOT not in project.parents:
        raise ValueError('project_dir must remain inside the repository')
    sources = config['source_roots']
    if not sources or not all(isinstance(x, str) for x in sources):
        raise ValueError('Configure nonempty source_roots')
    for source in sources:
        path = (project / source).resolve()
        if project != path and project not in path.parents:
            raise ValueError('source_roots must remain inside project_dir')
        if not path.is_dir():
            raise ValueError('Missing source directory: ' + str(path))
    return project, sources


def main(event):
    name = event.get('hook_event_name')
    if name not in {'PostToolUse', 'Stop'}:
        raise ValueError('Unsupported hook event')
    repository = Path(os.fsdecode(git('rev-parse', '--show-toplevel')).strip()).resolve()
    if repository != ROOT:
        raise ValueError('Copy the hook template into the actual Git repository root')
    paths = changed_paths()
    if not any(relevant(path) for path in paths):
        return {}
    cache = ROOT / '.tool-cache/hooks'
    cache.mkdir(parents=True, exist_ok=True)
    receipt = cache / 'receipt.json'
    current = snapshot(paths)
    state = json.loads(receipt.read_text()) if receipt.exists() else {}
    stage = 'post' if name == 'PostToolUse' else 'stop'
    if state.get(stage) == current:
        return {}
    if stage == 'post':
        java = [str(ROOT / path) for path in paths if path.endswith('.java') and (ROOT / path).is_file()]
        if not java:
            return {}
        command = ['bash', str(ROOT / 'scripts/java-quality.sh'), 'check', *java]
        working = ROOT
    else:
        working, sources = configured()
        command = ['bash', str(ROOT / 'scripts/backend-gate.sh'), *sources]
    with (cache / (stage + '.log')).open('w') as output:
        result = subprocess.run(command, cwd=working, stdout=output, stderr=subprocess.STDOUT, timeout=540)
    if result.returncode:
        if stage == 'stop':
            return stop_result(False, event.get('stop_hook_active', False))
        print('Java static check failed; see .tool-cache/hooks/post.log.', file=sys.stderr)
        raise SystemExit(2)
    # A concurrent edit invalidates the result rather than earning a stale receipt.
    if snapshot(changed_paths()) != current:
        raise RuntimeError('Files changed while checks ran; rerun quality checks')
    state[stage] = current
    with tempfile.NamedTemporaryFile(mode='w', dir=str(cache), delete=False) as output:
        json.dump(state, output)
        temporary = Path(output.name)
    temporary.replace(receipt)
    return {}


if __name__ == '__main__':
    payload = {}
    try:
        payload = json.load(sys.stdin)
        print(json.dumps(main(payload)))
    except Exception as error:
        print('Quality hook error (not a pass): ' + str(error), file=sys.stderr)
        if payload.get('hook_event_name') == 'Stop':
            response = stop_result(False, payload.get('stop_hook_active', False))
            response['systemMessage'] = 'Quality checks could not run: ' + str(error)
            print(json.dumps(response))
        else:
            raise SystemExit(2)
