import importlib.util
import unittest
from pathlib import Path

class HookTest(unittest.TestCase):
    def load(self):
        path = Path(__file__).resolve().parents[1] / '.codex/hooks/quality_hook.py'
        spec = importlib.util.spec_from_file_location('quality_hook', path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def test_failure_blocks_stop(self):
        self.assertEqual(self.load().stop_result(False, False)['decision'], 'block')

    def test_repeated_failure_stops_without_success(self):
        self.assertFalse(self.load().stop_result(False, True)['continue'])

    def test_success_allows_stop(self):
        self.assertEqual(self.load().stop_result(True, False), {})

if __name__ == '__main__':
    unittest.main()

class HookIntegrationTest(HookTest):
    def test_receipt_invalidated_after_source_change(self):
        import tempfile
        import subprocess
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            (root / '.gitignore').write_text('.tool-cache/\n')
            (root / '.codex').mkdir()
            (root / '.codex/quality.json').write_text('{"project_dir":".","source_roots":["src"]}')
            (root / 'scripts').mkdir()
            (root / 'scripts/backend-gate.sh').write_text('echo run >> runs.txt\n')
            (root / 'src').mkdir()
            source = root / 'src/Order.java'
            source.write_text('class Order {}')
            module = self.load()
            module.ROOT = root.resolve()
            event = {'hook_event_name': 'Stop'}
            self.assertEqual(module.main(event), {})
            self.assertEqual(module.main(event), {})
            self.assertEqual((root / 'runs.txt').read_text().count('run'), 1)
            source.write_text('class Order { int id; }')
            self.assertEqual(module.main(event), {})
            self.assertEqual((root / 'runs.txt').read_text().count('run'), 2)

    def test_failed_gate_has_no_receipt(self):
        import tempfile
        import subprocess
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            (root / '.codex').mkdir()
            (root / '.codex/quality.json').write_text('{"project_dir":".","source_roots":["src"]}')
            (root / 'scripts').mkdir()
            (root / 'scripts/backend-gate.sh').write_text('exit 1\n')
            (root / 'src').mkdir()
            (root / 'src/Order.java').write_text('class Order {}')
            module = self.load()
            module.ROOT = root.resolve()
            self.assertEqual(module.main({'hook_event_name': 'Stop'})['decision'], 'block')
            self.assertFalse((root / '.tool-cache/hooks/receipt.json').exists())
