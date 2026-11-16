"""只读对比summary新旧路径的HTTP状态及完整响应，不执行性能测试。"""
import json
import os
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
BEFORE = os.environ.get('SUMMARY_BEFORE_URL', 'http://127.0.0.1:18101')
AFTER = os.environ.get('SUMMARY_AFTER_URL', 'http://127.0.0.1:18102')
RESULTS = []
OUTPUT = Path(__file__).with_name('results.json')


def call(base, path, body):
    request = Request(base + '/api/v1/reports/' + path,
                      data=json.dumps(body).encode(),
                      headers={'Content-Type': 'application/json'})
    with urlopen(request, timeout=120) as response:
        return dict(httpStatus=response.status, body=json.load(response))


def compare(name, path, body, expected_code=200):
    before, after = call(BEFORE, path, body), call(AFTER, path, body)
    RESULTS.append(dict(name=name, path=path, request=body,
                        before=before, after=after, equal=before == after))
    OUTPUT.write_text(json.dumps(RESULTS, ensure_ascii=False, indent=2) + '\n')
    assert before['httpStatus'] == after['httpStatus'] == 200, RESULTS[-1]
    assert before['body']['code'] == after['body']['code'] == expected_code, RESULTS[-1]
    assert before == after, RESULTS[-1]
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
        ('unknown skill', dict(skillIds=['not-present'])),
        ('mismatched organization', dict(bbkId=ids['B2'], orgId=ids['O1'])),
        ('empty skills', dict(skillIds=[])), ('null skills', dict(skillIds=None)),
    ]
    if suite == 'report-validation':
        scenarios += [('null trace', dict(bbkId=ids['B1'], skillIds=[ids['S3']])),
                      ('no execution', dict(bbkId=ids['B1'], skillIds=[ids['S4']])),
                      ('205 targets', dict(bbkId=ids['B3']))]
    for label, changes in scenarios:
        data = compare(suite + ': ' + label, 'summary', dict(base, **changes))
        assert data['scheduledTaskCount'] == data['executedTaskCount'] + data['pendingTaskCount']
        if label == 'empty date':
            assert data['planCount'] == data['taskCount'] == data['recommendedCustomerCount'] == 0
            assert data['viewRate'] is None and data['contactRate'] is None
        if label == 'branch':
            assert data['planCount'] > 0 and data['recommendedCustomerCount'] > 0
    compare(suite + ': inverted dates', 'summary', dict(base, startDate='2026-11-12'), 10000)
    compare(suite + ': missing start', 'summary', dict(endDate='2026-11-11'), 10000)
    compare(suite + ': missing end', 'summary', dict(startDate='2026-11-10'), 10000)
    for path in ['filters', 'execution-overview', 'plan-coverage', 'plan-sources',
                 'execution-trend', 'skill-participation', 'attention-branches']:
        body = {} if path == 'filters' else dict(base, bbkId=ids['B1'])
        if path == 'skill-participation':
            body = dict(filters=body)
        compare(suite + ': unchanged ' + path, path, body)
print(f'{len(RESULTS)}/{len(RESULTS)} functional comparisons passed')
