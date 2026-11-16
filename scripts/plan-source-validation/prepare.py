"""生成隔离日期的排名边界数据和精确清理脚本，不直接写数据库。"""
import json
from pathlib import Path
folder = Path(__file__).resolve().parent
ids = iter(Path('/tmp/plan-source-ids.txt').read_text().splitlines())
def uid():
    return next(ids)
def insert(table, **values):
    sql.append('INSERT INTO ' + table + ' (' + ','.join('`'+k+'`' for k in values) + ') VALUES (' + ','.join("'"+str(v)+"'" for v in values.values()) + ');')
sql = ['START TRANSACTION;']
skill, item = uid(), uid()
insert('swe_marketplace_skills', source_id='RMASSIST', item_id=item, skill_id=skill, skill_name='来源排名边界技能')
manifest = dict(skill=skill, branches=[], plans=[], jobs=[], users=[])
# 第五、第六名并列；按编号较小者入选。
for count in [7, 6, 5, 4, 3, 3, 1]:
    b, o, u = uid(), uid(), uid()
    manifest['branches'].append(dict(id=b, count=count, org=o))
    manifest['users'].append(u)
    insert('rm_jkh_user_info', first_bbk_id=b, first_bbk_nm='来源排名'+b, org_id=o, org_nm='排名支行', user_id=u, user_name='排名验证', pst_lvl='SOURCE_TOP5')
    for n in range(count):
        p, j, t = uid(), uid(), uid()
        manifest['plans'].append(p)
        manifest['jobs'].append(j)
        insert('swe_wealth_plans', id=p, sap_id=u, creator_name='排名验证', name='来源排名规划', source_label=['我的关注','分行中台','行长关注'][n % 3], status='published', period_start='2026-01-01', period_end='2026-12-31', created_at='2026-01-01 00:00:00')
        insert('swe_wealth_plan_targets', plan_id=p, sap_id=u, target_name='排名验证')
        insert('swe_wealth_plan_scenes', plan_id=p, scene_id=skill, scene_name='排名技能', category='finance', cron_expr='0 9 * * *')
        insert('swe_plan_sence_cron', plan_id=p, sence_id=skill, execute_day='2026-09-15')
        insert('swe_cron_jobs', id=j, name='来源排名验证', tenant_id=u, source_id='RMASSIST', task_type='agent', cron_expr='0 9 * * *', channel='test', plan_id=p, skill_ids=skill)
        insert('swe_cron_executions', tenant_id=u, job_id=j, trace_id=t, actual_time='2026-09-15 09:00:00', status='success', is_read=0)
sql.append('COMMIT;')
(folder/'boundary-seed.sql').write_text('\n'.join(sql)+'\n')
(folder/'boundary-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n')
def values(items):
    return ','.join("'"+x+"'" for x in items)
cleanup = ['START TRANSACTION;']
for table, field, items in [('swe_cron_executions','job_id',manifest['jobs']), ('swe_cron_jobs','id',manifest['jobs']), ('swe_plan_sence_cron','plan_id',manifest['plans']), ('swe_wealth_plan_scenes','plan_id',manifest['plans']), ('swe_wealth_plan_targets','plan_id',manifest['plans']), ('swe_wealth_plans','id',manifest['plans']), ('rm_jkh_user_info','user_id',manifest['users']), ('swe_marketplace_skills','skill_id',[skill])]:
    cleanup.append('DELETE FROM '+table+' WHERE '+field+' IN ('+values(items)+');')
cleanup.append('COMMIT;')
(folder/'boundary-cleanup.sql').write_text('\n'.join(cleanup)+'\n')
print('prepared 7 branches, 29 plans')
