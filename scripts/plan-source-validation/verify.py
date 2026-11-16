"""以真实覆盖接口为对照，验证来源接口的排名与筛选。"""
import json
import os
from pathlib import Path
import urllib.request

folder = Path(__file__).resolve().parent
ids = json.loads((folder.parent / 'plan-coverage-validation/manifest.json').read_text())
base = os.environ.get('REPORT_BASE_URL', 'http://localhost:8082') + '/api/v1/reports/'
results = []


def call(path, body, code=200):
    req = urllib.request.Request(base + path, data=json.dumps(body).encode(),
                                 headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(req, timeout=30) as response:
        assert response.status == 200
        result = json.load(response)
    assert result['code'] == code, result
    return result['data']


filters = dict(startDate='2026-11-10', endDate='2026-11-11')
fields = ('id', 'name', 'planCount', 'myCount', 'branchCount', 'presidentCount')
for name, changes in [
    ('all', {}), ('branch', {'bbkId': ids['B1']}),
    ('organization', {'orgId': ids['O1']}), ('level', {'level': 'L1'}),
    ('skill', {'skillIds': [ids['S1']]}),
    ('skills OR', {'skillIds': [ids['S1'], ids['S2']]}),
    ('first day', {'endDate': '2026-11-10'}),
    ('last day', {'startDate': '2026-11-11'}),
    ('empty', {'startDate': '1900-01-01', 'endDate': '1900-01-01'}),
]:
    body = dict(filters, **changes)
    coverage = call('plan-coverage', body)['branches']
    expected = [{k: row[k] for k in fields} for row in
                sorted(coverage, key=lambda r: (-r['planCount'], r['id']))[:5]]
    actual = call('plan-sources', body)['branches']
    assert actual == expected, (name, expected, actual)
    assert all(r['planCount'] == r['myCount'] + r['branchCount'] + r['presidentCount'] for r in actual)
    if name == 'branch':
        assert len(actual) == 1 and actual[0]['planCount'] == 3, actual
        assert (actual[0]['myCount'], actual[0]['branchCount'], actual[0]['presidentCount']) == (1, 1, 1)
    if name == 'empty':
        assert actual == []
    results.append(dict(name=name, coverageCount=len(coverage), actual=actual))
    print(name, 'PASS')
for name, body in [
    ('missing dates', {}),
    ('reversed dates', dict(filters, startDate='2026-11-12')),
    ('mismatched organization', dict(filters, bbkId=ids['B2'], orgId=ids['O1'])),
    ('invalid date', dict(filters, startDate='bad')),
]:
    call('plan-sources', body, 10000)
    results.append(dict(name=name, code=10000))
    print(name, 'PASS')
(folder / 'results.json').write_text(json.dumps(results, ensure_ascii=False, indent=2) + '\n')
print(f'{len(results)}/{len(results)} PASS')
