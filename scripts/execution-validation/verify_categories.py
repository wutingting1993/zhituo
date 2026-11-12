"""技能分类真实接口验证；仅临时修改既有验证技能，finally恢复。"""
import json
import os
import subprocess
import sys
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
FOLDER = Path(__file__).resolve().parent
IDS = json.loads((ROOT / 'scripts/report-validation/manifest.json').read_text())
BASE = sys.argv[1].rstrip('/')
PASSED = []


def sql(statement):
    env = dict(os.environ, MYSQL_PWD=os.environ.get('MYSQL_PASSWORD', 'root'))
    command = ['mysql', '-N', '-B', '-h', env.get('MYSQL_HOST', '127.0.0.1'),
               '-P', env.get('MYSQL_PORT', '3306'), '-u', env.get('MYSQL_USER', 'root'),
               env.get('MYSQL_DATABASE', 'zhituo')]
    return subprocess.run(command, input=statement, env=env, check=True,
                          capture_output=True, text=True).stdout.strip()


def api(path, body):
    request = Request(BASE + '/api/v1/reports/' + path, data=json.dumps(body).encode(),
                      headers={'Content-Type': 'application/json'}, method='POST')
    with urlopen(request, timeout=30) as response:
        result = json.load(response)
        assert response.status == 200 and result['code'] == 200, result
        return result['data']


def check(name, condition):
    assert condition, name
    PASSED.append(name)
    print('PASS', name, flush=True)


def main():
    filters = dict(startDate='2026-11-10', endDate='2026-11-11', bbkId=IDS['B1'])
    scope = dict(filters=filters, scopeBbkId=IDS['B1'], scopeOrgId=IDS['O1'])
    def skills():
        return api('execution-branches/skills', scope)
    fixture = sql("SELECT id,category_id,updated_at FROM swe_marketplace_skills WHERE source_id='RMASSIST' AND skill_id='" + IDS['S1'] + "'").splitlines()
    assert len(fixture) == 1, 'expected a single validation skill'
    skill_pk, original, updated_at = fixture[0].split('\t')
    assert skill_pk.isdigit() and (original == 'NULL' or original.lstrip('-').isdigit())
    before_count = int(sql('SELECT COUNT(1) FROM swe_marketplace_categories'))
    summary_before = api('summary', filters)
    skills_before = skills()
    created = []
    cleanup_verified = False
    try:
        # 分类主键沿用用户提供表的AUTO_INCREMENT规则。
        for source, name, visible in [('RMASSIST', '接口验证技能分类', 0), ('CATEGORY_VALIDATION_OTHER', '接口验证其他入口分类', 1)]:
            existing = sql("SELECT COUNT(1) FROM swe_marketplace_categories WHERE source_id='" + source + "' AND name='" + name + "'")
            assert existing == '0', 'validation category already exists'
            category_id = sql("INSERT INTO swe_marketplace_categories (source_id,name,branch_visible) VALUES ('" + source + "','" + name + "'," + str(visible) + '); SELECT LAST_INSERT_ID();')
            assert category_id.isdigit()
            created.append(category_id)
        def assign(value):
            sql('UPDATE swe_marketplace_skills SET category_id=' + value + ' WHERE id=' + skill_pk)
        def row():
            return next(r for r in skills()['rows'] if r['skillId'] == IDS['S1'])
        assign(created[0])
        matched = row()
        check('same-source category name returned', matched['categoryName'] == '接口验证技能分类')
        check('branch_visible does not filter report category', matched['categoryName'] is not None)
        check('category change preserves summary statistics', api('summary', filters) == summary_before)
        check('category change preserves skill summary', skills()['summary'] == skills_before['summary'])
        original_row = next(r for r in skills_before['rows'] if r['skillId'] == IDS['S1'])
        check('category change preserves skill row metrics', {k: v for k, v in matched.items() if k != 'categoryName'} == {k: v for k, v in original_row.items() if k != 'categoryName'})
        assign(created[1])
        check('different source returns null', row()['categoryName'] is None)
        assign('NULL')
        check('unconfigured category returns null', row()['categoryName'] is None)
        assign(created[0])
        sql('DELETE FROM swe_marketplace_categories WHERE id=' + created[0])
        missing = row()
        check('missing category returns null without removing skill', missing['categoryName'] is None and missing['skillId'] == IDS['S1'])
        empty_filters = dict(filters, startDate='2026-01-01', endDate='2026-01-01')
        empty = api('execution-branches/skills', dict(scope, filters=empty_filters))
        check('empty skill list skips category query safely', empty['rows'] == [])
    finally:
        timestamp = 'NULL' if updated_at == 'NULL' else "'" + updated_at + "'"
        sql('UPDATE swe_marketplace_skills SET category_id=' + original + ',updated_at=' + timestamp + ' WHERE id=' + skill_pk)
        if created:
            sql('DELETE FROM swe_marketplace_categories WHERE id IN (' + ','.join(created) + ')')
        restored = sql('SELECT category_id FROM swe_marketplace_skills WHERE id=' + skill_pk) == original
        count_restored = int(sql('SELECT COUNT(1) FROM swe_marketplace_categories')) == before_count
        cleanup_verified = restored and count_restored
        (FOLDER / 'category-results.json').write_text(json.dumps(dict(count=len(PASSED), passed=PASSED, cleanupVerified=cleanup_verified, skillCategoryRestored=restored, categoryCountRestored=count_restored), ensure_ascii=False, indent=2))
        assert cleanup_verified, 'category data not restored'
    print(str(len(PASSED)) + ' category checks PASS; temporary data restored')


if __name__ == '__main__':
    main()
