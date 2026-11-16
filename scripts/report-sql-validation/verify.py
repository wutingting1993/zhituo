"""只读比较SQL改造前后的完整接口响应，不造数，不测性能。"""
import json
import os
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
BEFORE = os.environ.get('REPORT_SQL_BEFORE_URL', 'http://127.0.0.1:18111')
AFTER = os.environ.get('REPORT_SQL_AFTER_URL', 'http://127.0.0.1:18112')
OUTPUT = Path(__file__).with_name('results.json')
RESULTS = []


def call(base, path, body):
    request = Request(base + '/api/v1/reports/' + path, data=json.dumps(body).encode(),
                      headers={'Content-Type': 'application/json'})
    with urlopen(request, timeout=120) as response:
        return dict(httpStatus=response.status, body=json.load(response))


def compare(name, path, body, code=200):
    before, after = call(BEFORE, path, body), call(AFTER, path, body)
    RESULTS.append(dict(name=name, path=path, request=body, before=before, after=after, equal=before == after))
    OUTPUT.write_text(json.dumps(RESULTS, ensure_ascii=False, indent=2) + '\n')
    assert before['httpStatus'] == after['httpStatus'] == 200, RESULTS[-1]
    assert before['body']['code'] == after['body']['code'] == code, RESULTS[-1]
    assert before == after, RESULTS[-1]
    return after['body']['data']


for suite in ['report-validation', 'plan-coverage-validation']:
    ids = json.loads((ROOT / 'scripts' / suite / 'manifest.json').read_text())
    base = dict(startDate='2026-11-10', endDate='2026-11-11')
    cases = [('all', {}), ('branch', dict(bbkId=ids['B1'])),
             ('organization', dict(bbkId=ids['B1'], orgId=ids['O1'])),
             ('level', dict(level='L1')), ('skill', dict(skillIds=[ids['S1']])),
             ('skills OR', dict(skillIds=[ids['S1'], ids['S2']])),
             ('combined', dict(bbkId=ids['B1'], orgId=ids['O1'], level='L1', skillIds=[ids['S1']])),
             ('first day', dict(endDate='2026-11-10')), ('last day', dict(startDate='2026-11-11')),
             ('empty', dict(startDate='1900-01-01', endDate='1900-01-01')),
             ('unknown skill', dict(skillIds=['not-present'])),
             ('mismatched', dict(bbkId=ids['B2'], orgId=ids['O1'])),
             ('empty skills', dict(skillIds=[])), ('null skills', dict(skillIds=None))]
    if suite == 'report-validation':
        cases += [('null trace', dict(skillIds=[ids['S3']])), ('no execution', dict(skillIds=[ids['S4']])),
                  ('205 targets', dict(bbkId=ids['B3']))]
    for label, changes in cases:
        filters = dict(base, **changes)
        name = suite + ': ' + label
        for path in ['summary', 'execution-overview', 'plan-coverage', 'plan-sources', 'execution-trend', 'attention-branches']:
            data = compare(name, path, filters)
            if label == 'branch' and path == 'summary':
                assert data['planCount'] > 0 and data['recommendedCustomerCount'] > 0
        compare(name, 'execution-branches', dict(filters=filters))
        compare(name, 'skill-participation', dict(filters=filters))
        # 下钻只在顶部机构范围允许时测试，保留日期/等级/技能筛选。
        scope_filters = dict(filters, bbkId=ids['B1'], orgId=ids['O1'])
        detail = dict(filters=scope_filters, scopeBbkId=ids['B1'], scopeOrgId=ids['O1'])
        for path in ['execution-branches/managers', 'execution-branches/skills', 'plan-coverage/details']:
            compare(name + ' drilldown', path, detail)
    for key in ['participantCount', 'participationRate', 'assignedManagerCount']:
        for top in [1, 5, 100]:
            compare(suite + ': participation sort', 'skill-participation', dict(filters=base, sortKey=key, topN=top))
    for key in ['skillCount', 'taskCount', 'recommendedCustomerCount', 'viewRate', 'insightRate', 'callRate', 'contactRate']:
        for direction in ['asc', 'desc']:
            compare(suite + ': branch sort', 'execution-branches', dict(filters=base, sortKey=key, sortDir=direction))
    for source in ['全部', '我的关注', '分行关注', '行长关注']:
        compare(suite + ': source', 'plan-coverage/details', dict(filters=base, scopeBbkId=ids['B1'], source=source))
    for top in [1, 5, 100]:
        compare(suite + ': sources top', 'plan-sources', dict(base, topN=top))
    for path in ['summary', 'execution-overview', 'plan-coverage', 'plan-sources', 'execution-trend', 'attention-branches']:
        compare(suite + ': inverted date', path, dict(base, startDate='2026-11-12'), 10000)
    compare(suite + ': invalid sort', 'execution-branches', dict(filters=base, sortKey='invalid'), 10000)
    compare(suite + ': invalid top', 'skill-participation', dict(filters=base, topN=0), 10000)
    compare(suite + ': invalid source', 'plan-coverage/details', dict(filters=base, scopeBbkId=ids['B1'], source='invalid'), 10000)
    compare(suite + ': scope outside', 'execution-branches/managers',
            dict(filters=dict(base, bbkId=ids['B2']), scopeBbkId=ids['B1'], scopeOrgId=ids['O1']), 10000)
    compare(suite + ': unchanged filters', 'filters', {})
print(f'{len(RESULTS)}/{len(RESULTS)} functional comparisons passed')
