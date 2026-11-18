import subprocess
import tempfile
import unittest
import json
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / 'scripts/init-project.py'

class InitTest(unittest.TestCase):
    def test_initialize_and_refuse_overwrite(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            command = ['python3', str(SCRIPT), str(target), '--project-dir', 'backend',
                       '--source-root', 'src/main/java', '--base-package', 'com.example']
            first = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(first.returncode, 0, first.stderr)
            config = json.loads((target / '.codex/quality.json').read_text())
            self.assertEqual(config['project_dir'], 'backend')
            self.assertTrue((target / 'tools/architecture/pom.xml').is_file())
            for layer in ['adapter', 'application', 'domain', 'infrastructure']:
                self.assertTrue((target / ('backend/src/main/java/com/example/' + layer + '/package-info.java')).is_file())
            self.assertTrue((target / 'backend/src/main/java/com/example/application/api/dto/Result.java').is_file())
            self.assertTrue((target / 'backend/src/main/java/com/example/infrastructure/common/exception/BusinessException.java').is_file())
            for source in (target / 'backend/src/main/java').rglob('*.java'):
                self.assertNotIn('template.base', source.read_text())
            self.assertTrue((target / 'backend/src/main/java/com/example/infrastructure/common/util/CUuid.java').is_file())
            self.assertTrue((target / 'backend/src/main/java/com/example/infrastructure/common/util/HexEncoding.java').is_file())
            self.assertTrue((target / 'backend/src/main/java/com/example/adapter/web/advice/GlobalExceptionHandler.java').is_file())
            self.assertIn('UNKNOWN_ERROR(99999', (target / 'backend/src/main/java/com/example/infrastructure/common/ResponseEnum.java').read_text())
            self.assertIn('.tool-cache/', (target / '.gitignore').read_text())
            self.assertNotIn('/Users/natasha/', (target / 'docs/standards/backend.md').read_text())
            second = subprocess.run(command, capture_output=True, text=True)
            self.assertNotEqual(second.returncode, 0)

    def test_conflict_is_atomic(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory)
            (target / 'AGENTS.md').write_text('keep')
            result = subprocess.run(['python3', str(SCRIPT), str(target)], capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual((target / 'AGENTS.md').read_text(), 'keep')
            self.assertFalse((target / 'config').exists())

if __name__ == '__main__':
    unittest.main()
