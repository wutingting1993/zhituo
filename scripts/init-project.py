#!/usr/bin/env python3
"""Install the quality template and create four Java layers without overwriting files."""
import argparse
from pathlib import Path
import re
import shutil
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('target', type=Path, help='Project/Git root (created if missing)')
    parser.add_argument('--base-package', required=True, help='Java base package, e.g. com.example.orders')
    parser.add_argument('--project-dir', default='.', help='Maven project directory relative to target')
    parser.add_argument('--source-root', action='append', help='Java source directory relative to project-dir; repeatable')
    args = parser.parse_args()
    if not re.fullmatch(r'[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+', args.base_package):
        parser.error('Use a lowercase dotted Java package, e.g. com.example.orders')
    keywords = {'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
                'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
                'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int',
                'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
                'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
                'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'true', 'false', 'null', '_'}
    if keywords.intersection(args.base_package.split('.')):
        parser.error('Java package cannot contain keywords')
    template = Path(__file__).resolve().parents[1]
    target = args.target.expanduser().resolve()
    if target == template or template in target.parents:
        parser.error('Target must be outside the template directory')
    if target.exists() and not target.is_dir():
        parser.error('Target must be a directory')
    sources = args.source_root or ['src/main/java']

    def child(relative):
        path = Path(relative)
        if path.is_absolute() or '..' in path.parts:
            parser.error('Use relative paths without ..: ' + str(relative))
        dest = target / path
        for ancestor in [dest, *dest.parents]:
            if ancestor == target.parent:
                break
            if ancestor.is_symlink():
                parser.error('Symlink target is not allowed: ' + str(ancestor))
        return dest

    project = child(args.project_dir)
    planned = {}
    for entry in ['AGENTS.md', 'docs/standards', 'config', 'scripts', 'tools', '.codex', 'tests']:
        origin = template / entry
        candidates = [origin] if origin.is_file() else sorted(origin.rglob('*'))
        for source in candidates:
            if not source.is_file() or any(part in {'target', '__pycache__'} for part in source.relative_to(template).parts):
                continue
            planned[child(source.relative_to(template))] = source.read_bytes()
    import json
    planned[child('.codex/quality.json')] = (json.dumps({'project_dir': args.project_dir,
        'source_roots': sources}, ensure_ascii=False, indent=2) + '\n').encode()
    responsibilities = {'adapter': '请求入口与协议适配。', 'application': '应用用例与流程编排。',
                        'domain': '领域模型、业务规则及能力接口。', 'infrastructure': '持久化、外部调用与能力接口实现。'}
    for source_root in sources:
        base = child(str(Path(args.project_dir) / source_root / args.base_package.replace('.', '/')))
        for layer, description in responsibilities.items():
            planned[base / layer / 'package-info.java'] = (
                '/**\n * ' + description + '\n */\npackage ' + args.base_package + '.' + layer + ';\n').encode()
    # Shared contracts are created once, in the first source root, to avoid duplicate classes.
    contract_root = child(str(Path(args.project_dir) / sources[0] / args.base_package.replace('.', '/')))
    contracts = {'GlobalExceptionHandler.java': 'adapter/web/advice',
                 'Result.java': 'application/api/dto',
                 'ResponseEnum.java': 'infrastructure/common',
                 'BusinessException.java': 'infrastructure/common/exception',
                 'CUuid.java': 'infrastructure/common/util',
                 'HexEncoding.java': 'infrastructure/common/util'}
    for filename, package_path in contracts.items():
        text = (template / 'scripts/templates' / filename).read_text()
        planned[contract_root / package_path / filename] = text.replace('template.base', args.base_package).encode()
    # Local JDK installation paths must not leak into a reusable project copy.
    for dest, content in list(planned.items()):
        if dest.suffix == '.md':
            text = content.decode()
            text = re.sub(r'^本机 JDK 8：.*\n', '', text, flags=re.M)
            text = re.sub(r'^本机路径位于.*\n', '', text, flags=re.M)
            text = re.sub(r'export JAVA_HOME="/Users/natasha/[^"\n]+"',
                          'export JAVA_HOME="/path/to/project-jdk"', text)
            text = text.replace('/Users/natasha/data/code/规范/', './')
            planned[dest] = text.encode()
    conflicts = [str(path.relative_to(target)) for path in planned if path.exists()]
    # Other inline hooks could duplicate this template; leave merging to the owner.
    if (target / '.codex/config.toml').exists():
        conflicts.append('.codex/config.toml (merge existing configuration first)')
    for dest in planned:
        for parent in dest.parents:
            if parent == target.parent:
                break
            if parent.exists() and not parent.is_dir():
                conflicts.append(str(parent))
    if conflicts:
        parser.error('No files copied. Existing destinations require manual merge:\n' + '\n'.join(sorted(set(conflicts))))
    ignore = child('.gitignore')
    if ignore.exists() and not ignore.is_file():
        parser.error('.gitignore must be a file')
    for dest, content in planned.items():
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(content)
    existing = ignore.read_text() if ignore.exists() else ''
    additions = [line for line in ['.tool-cache/', '__pycache__/'] if line not in existing.splitlines()]
    if additions:
        ignore.write_text(existing + ('\n' if existing and not existing.endswith('\n') else '') + '\n'.join(additions) + '\n')
    print('Initialized: ' + str(target))
    print('Created adapter/application/domain/infrastructure for: ' + args.base_package)
    print('No business POM or Git repository was created. Keep your project build configuration.')
    print('Next: set the project JDK; verify .codex/quality.json; review and trust hooks in Codex.')
    print('Run quality checks after adding business code; architecture checks require compiled classes.')


if __name__ == '__main__':
    main()
