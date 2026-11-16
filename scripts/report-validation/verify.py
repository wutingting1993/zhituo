import json
from pathlib import Path
from urllib.request import Request, urlopen
p=Path(__file__).resolve().parent; ids=json.loads((p/'manifest.json').read_text()); passed=[]; outputs={}
def request(path,**params):
 req=Request('http://localhost:8080/api/v1/reports/'+path,data=json.dumps(params).encode(),headers={'Content-Type':'application/json'},method='POST')
 with urlopen(req,timeout=30) as r:
  d=json.load(r); assert r.status==200 and d['code']==200,(params,d)
  return d['data']
def summary(name,expected,**params):
 d=request('summary',startDate=params.pop('startDate','2026-11-10'),endDate=params.pop('endDate','2026-11-11'),**params)
 for k,v in expected.items():assert d[k]==v,(name,k,'expected',v,'actual',d[k],d)
 outputs[name]=d;passed.append(name);print(name,'PASS')
def most(s,n):return dict(id=ids[s],name='同名验证技能' if s in ['S1','S2'] else '验证'+s,userCount=n)
summary('B1 full interval',dict(planCount=3,skillCount=3,taskCount=7,recommendedCustomerCount=3,viewRate=100,contactRate=133.33,coveredSubBranchCount=2,mostUsedSkill=most('S1',3),averageTaskCount=2,averageRecommendedCustomerCount=1,benchmarkViewRate=100,benchmarkContactRate=133.33,viewRateDifference=0,contactRateDifference=0),bbkId=ids['B1'])
summary('org level skill with independent benchmark',dict(planCount=2,skillCount=1,taskCount=5,recommendedCustomerCount=3,viewRate=66.67,contactRate=133.33,averageTaskCount=3,averageRecommendedCustomerCount=2,coveredSubBranchCount=1,mostUsedSkill=most('S1',2),benchmarkViewRate=100,benchmarkContactRate=133.33,viewRateDifference=-33.33,contactRateDifference=0),bbkId=ids['B1'],orgId=ids['O1'],level='L1',skillIds=[ids['S1']])
summary('multi skill OR and same names',dict(planCount=2,skillCount=2,taskCount=6,recommendedCustomerCount=3,viewRate=66.67,contactRate=133.33),bbkId=ids['B1'],orgId=ids['O1'],skillIds=[ids['S1'],ids['S2']])
summary('null trace preserves base metrics',dict(planCount=1,skillCount=1,taskCount=0,recommendedCustomerCount=0,viewRate=None,contactRate=None,averageTaskCount=0,averageRecommendedCustomerCount=0,mostUsedSkill=most('S3',1),benchmarkViewRate=100,viewRateDifference=None),bbkId=ids['B1'],skillIds=[ids['S3']])
summary('unviewed skill and negative benchmark gaps',dict(planCount=1,skillCount=1,taskCount=1,recommendedCustomerCount=1,viewRate=0,contactRate=0,viewRateDifference=-100,contactRateDifference=-133.33),bbkId=ids['B1'],skillIds=[ids['S2']])
summary('first date included',dict(planCount=3,skillCount=3,taskCount=5,recommendedCustomerCount=3,viewRate=100,contactRate=133.33),bbkId=ids['B1'],endDate='2026-11-10')
summary('last date included',dict(planCount=1,skillCount=1,taskCount=2,recommendedCustomerCount=2,viewRate=0,contactRate=0),bbkId=ids['B1'],startDate='2026-11-11')
summary('candidate without execution',dict(planCount=0,skillCount=0,taskCount=0,recommendedCustomerCount=0,viewRate=None,averageTaskCount=None,mostUsedSkill=None),bbkId=ids['B1'],skillIds=[ids['S4']])
summary('205 users across batches',dict(planCount=1,skillCount=1,taskCount=205,recommendedCustomerCount=1,viewRate=100,contactRate=0,averageTaskCount=1,averageRecommendedCustomerCount=0,mostUsedSkill=most('S5',205)),bbkId=ids['B3'])
summary('whole bank distinct branch org pairs',dict(planCount=7,skillCount=4,taskCount=222,recommendedCustomerCount=14,viewRate=64.29,contactRate=28.57,coveredSubBranchCount=6,mostUsedSkill=most('S5',205)))
summary('unrounded benchmark difference',dict(planCount=1,skillCount=1,taskCount=6,recommendedCustomerCount=6,viewRate=16.67,benchmarkViewRate=44.44,viewRateDifference=-27.78),bbkId=ids['B4'],orgId=ids['O4'])
summary('skill tie uses ascending ID',dict(planCount=2,skillCount=2,taskCount=9,recommendedCustomerCount=9,viewRate=44.44,mostUsedSkill=most('S1',1)),bbkId=ids['B4'])
summary('whole bank benchmark without branch selection',dict(planCount=4,skillCount=3,taskCount=7,recommendedCustomerCount=4,viewRate=75,contactRate=100,benchmarkViewRate=64.29,benchmarkContactRate=28.57,viewRateDifference=10.71,contactRateDifference=71.43),orgId=ids['O1'])
all_options=request('filters')
f=request('filters',bbkId=ids['B1'],orgId=ids['O1'],skillIds=[ids['S1']])
assert f['branches']==all_options['branches'] and f['levels']==all_options['levels'],f
assert f==request('filters',bbkId=ids['B1'],orgId=ids['O1'],skillIds=[ids['S3']]),f
passed.append('institution filters skills independently of selected skills');outputs['linked filters']=f;print(passed[-1],'PASS')
org_options=request('filters',orgId=ids['O1'],skillIds=[ids['S3']])
assert org_options==request('filters',orgId=ids['O1'])
assert org_options['branches']==all_options['branches'] and org_options['levels']==all_options['levels']
passed.append('organization tree and levels remain independent');print(passed[-1],'PASS')
summary('empty interval after seeding',dict(planCount=0,skillCount=0,taskCount=0,recommendedCustomerCount=0,viewRate=None,contactRate=None,averageTaskCount=None,mostUsedSkill=None),startDate='2026-01-01',endDate='2026-01-01')
(p/'results.json').write_text(json.dumps(dict(passed=passed,responses=outputs),ensure_ascii=False,indent=2))
print(len(passed),'data scenarios PASS')
