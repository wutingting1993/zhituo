"""只读对比两个本机服务的完整 JSON 响应，不忽略数组顺序或可空字段。"""
import json
import sys
from collections import Counter
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
BEFORE, AFTER = [value.rstrip('/') for value in sys.argv[1:3]]
OUTPUT = Path(sys.argv[3])
RESULTS = []


def call(base, path, body):
    request = Request(base + '/api/v1/reports/' + path,
                      data=json.dumps(body).encode(),
                      headers={'Content-Type': 'application/json'})
    with urlopen(request, timeout=60) as response:
        return {'httpStatus': response.status, 'body': json.load(response)}


def compare(name, path, body, code=200):
    before = call(BEFORE, path, body)
    after = call(AFTER, path, body)
    result = dict(name=name, path=path, request=body, before=before, after=after,
                  equal=before == after)
    RESULTS.append(result)
    OUTPUT.write_text(json.dumps(RESULTS, ensure_ascii=False, indent=2) + '\n')
    assert before['httpStatus'] == after['httpStatus'] == 200, result
    assert before['body']['code'] == after['body']['code'] == code, result
    assert result['equal'], result
    return after['body']['data']


for suite in ['report-validation', 'plan-coverage-validation']:
    ids = json.loads((ROOT / 'scripts' / suite / 'manifest.json').read_text())
    base = dict(startDate='2026-11-10', endDate='2026-11-11')
    scenarios = [
        ('all', {}), ('branch', dict(bbkId=ids['B1'])),
        ('organization', dict(bbkId=ids['B1'], orgId=ids['O1'])),
        ('level', dict(bbkId=ids['B1'], level='L1')),
        ('single skill', dict(bbkId=ids['B1'], skillIds=[ids['S1']])),
        ('skills OR', dict(bbkId=ids['B1'], skillIds=[ids['S1'], ids['S2']])),
        ('combined', dict(bbkId=ids['B1'], orgId=ids['O1'], level='L1', skillIds=[ids['S1']])),
        ('first date', dict(bbkId=ids['B1'], endDate='2026-11-10')),
        ('last date', dict(bbkId=ids['B1'], startDate='2026-11-11')),
        ('empty date', dict(startDate='1900-01-01', endDate='1900-01-01')),
        ('unknown skill', dict(bbkId=ids['B1'], skillIds=['not-present'])),
        ('mismatched organization', dict(bbkId=ids['B2'], orgId=ids['O1'])),
        ('empty skills', dict(bbkId=ids['B1'], skillIds=[])),
        ('null skills', dict(bbkId=ids['B1'], skillIds=None)),
    ]
    if suite == 'report-validation':
        scenarios += [('null trace', dict(bbkId=ids['B1'], skillIds=[ids['S3']])),
                      ('no execution', dict(bbkId=ids['B1'], skillIds=[ids['S4']])),
                      ('205 targets', dict(bbkId=ids['B3']))]
    for label, changes in scenarios:
        filters = dict(base, **changes)
        name = suite + ': ' + label
        for path in ['summary', 'execution-overview', 'plan-coverage', 'plan-sources']:
            data = compare(name, path, filters)
            if label == 'branch' and path == 'summary' and suite == 'report-validation':
                actual = tuple(data[k] for k in ['planCount', 'taskCount', 'recommendedCustomerCount'])
                assert actual == (3, 7, 3), (name, actual)
            if label == 'branch' and path == 'plan-coverage' and suite == 'plan-coverage-validation':
                assert len(data['branches']) == 1 and data['branches'][0]['planCount'] == 3, data
            if label == '205 targets' and path == 'summary':
                assert data['taskCount'] == 205 and data['recommendedCustomerCount'] == 1, data
            if label == 'null trace' and path == 'summary':
                assert data['planCount'] == 1 and data['taskCount'] == 0, data
            if label == 'no execution' and path == 'summary':
                assert data['planCount'] == data['taskCount'] == 0, data
        compare(name, 'execution-branches', dict(filters=filters))
        scope = dict(filters=filters, scopeBbkId=filters.get('bbkId', ids['B1']),
                     scopeOrgId=filters.get('orgId', ids['O1']))
        for path in ['execution-branches/managers', 'execution-branches/skills', 'plan-coverage/details']:
            compare(name, path, scope)
    for path, body in [('filters', {}), ('filters', dict(bbkId=ids['B1'], orgId=ids['O1']))]:
        compare(suite + ': unchanged filters', path, body)
    for path in ['summary', 'execution-overview', 'plan-coverage', 'plan-sources']:
        compare(suite + ': reversed dates', path, dict(base, startDate='2026-11-12'), 10000)
        compare(suite + ': missing start date', path, dict(endDate=base['endDate']), 10000)
print(json.dumps(dict(passed=len(RESULTS), endpoints=dict(Counter(r['path'] for r in RESULTS))), ensure_ascii=False))
