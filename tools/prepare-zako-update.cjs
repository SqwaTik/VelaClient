const fs=require('fs'),path=require('path'),cp=require('child_process');
const root=process.cwd(),reference=path.resolve(root,'../references/zako-1.1.6');
const oldRoot=path.join(reference,'base/source',fs.readdirSync(path.join(reference,'base/source'))[0],'zakoopt');
const newRoot=path.join(reference,'source',fs.readdirSync(path.join(reference,'source'))[0],'zakoopt');
function files(dir){return fs.existsSync(dir)?fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(path.join(dir,e.name)):[path.join(dir,e.name)]):[];}
function sources(base){
 const layers=fs.readFileSync(path.join(base,'26.2/gradle.properties'),'utf8').match(/^layers=(.*)$/m)[1].trim().split(',').concat('26.2');
 const map=new Map(),renames=fs.existsSync(path.join(base,'26.2/renames.txt'))?fs.readFileSync(path.join(base,'26.2/renames.txt'),'utf8').trim().split(/\r?\n/).map(s=>s.split('=')):[];
 for(const layer of layers)for(const file of files(path.join(base,layer,'src/main/java'))){
   const key=path.relative(path.join(base,layer,'src/main/java/zako/opt'),file).replaceAll('\\','/');
   let text=fs.readFileSync(file,'utf8').replaceAll('\r\n','\n');
   if(layer!=='26.2')for(const [from,to] of renames){
     text=text.replaceAll(from,to).replaceAll(from.replaceAll('.','/'),to.replaceAll('.','/'));
   }
   text=text.replaceAll('zako.opt','dev.sqwat.vela.zako').replaceAll('zako/opt','dev/sqwat/vela/zako');
   // Local layout: feature subpackages and bench/cpu are flattened (mixin subpackages are kept, MixinPlugin keys on them).
   text=text.replace(/dev([.\/])sqwat\1vela\1zako\1feature\1(?:blocks|items|particles|render|spawners|ui)\b/g,(m,s)=>['dev','sqwat','vela','zako','feature'].join(s))
     .replace(/dev([.\/])sqwat\1vela\1zako\1(?:bench|cpu)\b/g,(m,s)=>['dev','sqwat','vela','zako'].join(s));
   map.set(key.replace(/^feature\/(?:blocks|items|particles|render|spawners|ui)\//,'feature/').replace(/^(?:bench|cpu)\//,''),text);
 }
 return map;
}
const old=sources(oldRoot),next=sources(newRoot);
const dir=path.join(reference,'merge');fs.mkdirSync(dir,{recursive:true});
const records=[];
for(const [key,incoming] of next){
 const target=path.join(root,'src/main/java/dev/sqwat/vela/zako',key);
 const current=fs.existsSync(target)?fs.readFileSync(target,'utf8').replaceAll('\r\n','\n'):null;
 const base=old.get(key);
 if(!current&&base)continue; // Previously omitted optional UI/entrypoints stay omitted.
 if(/^(ZakoOptConfig|ZakoOptions|MixinPlugin)\.java$/.test(key))continue;
 if(!current&&/^(.*Screen|.*Options|ZakoOpt|ZakoKey|Debug.*|Welcome.*)\.java$/.test(key))continue;
 const normalized=current?.replace(/^\/\/ Adapted[^\n]*\n\/\/ See[^\n]*\n/,'');
 if(normalized===incoming)continue;
 let merged=incoming,conflict=false;
 if(current&&base&&normalized!==base){
   fs.writeFileSync(path.join(dir,'current'),normalized);fs.writeFileSync(path.join(dir,'base'),base);fs.writeFileSync(path.join(dir,'incoming'),incoming);
   const result=cp.spawnSync('git',['merge-file','-p',path.join(dir,'current'),path.join(dir,'base'),path.join(dir,'incoming')],{encoding:'utf8',maxBuffer:8e6});
   merged=result.stdout;conflict=result.status!==0;
 }
 records.push({key,target,current,text:'// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.\n// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.\n'+merged,conflict});
}
fs.writeFileSync(path.join(dir,'plan.json'),JSON.stringify(records));
console.log(JSON.stringify(records.map(r=>({key:r.key,conflict:r.conflict,added:r.current===null})),null,2));
